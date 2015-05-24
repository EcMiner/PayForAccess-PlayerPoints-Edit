package com.spaceemotion.payforaccess;

import net.milkbowl.vault.permission.Permission;

import org.black_ixx.playerpoints.PlayerPoints;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.spaceemotion.payforaccess.config.ConfigManager;
import com.spaceemotion.payforaccess.config.PlayerConfigManager;
import com.spaceemotion.payforaccess.config.SavesConfigManager;
import com.spaceemotion.payforaccess.listener.PlayerListener;
import com.spaceemotion.updater.Updater;

public class PayForAccessPlugin extends JavaPlugin {
	private Updater pluginUpdater;

	private static PlayerPoints econ = null;
	private static Permission perm = null;
	private static WorldGuardPlugin wgPlugin = null;

	private SavesConfigManager configManager;
	private PlayerConfigManager playerConfigManager;
	private ConfigManager languageConfigManager;

	public void onEnable() {
		/* Check for updates first! */
		pluginUpdater = new Updater(this, getConfig().getBoolean("auto-update", true));

		/* Vault and WorldGuard integration */
		if (!setupPermissions()) {
			getLogger().info("Permission integration with Vault failed or no permission plugin found!");
			getServer().getPluginManager().disablePlugin(this);

			return;
		}

		if (!setupEconomy()) {
			getLogger().info("Economy integration with Vault failed or no economy plugin found!");
			getServer().getPluginManager().disablePlugin(this);

			return;
		}

		if (!setupWorldGuard()) {
			getLogger().info("WorldGuard integration failed, please make sure that you have it installed!");
			getServer().getPluginManager().disablePlugin(this);

			return;
		}

		/* set up the listeners */
		new PlayerListener(this);

		/* set up command executor */
		getCommand("pfa").setExecutor(new CommandManager(this));

		/* set up configuration file(s) */
		getConfig().options().copyDefaults(true);
		this.saveConfig();

		/* Enable PluginMetrics to collect data */
		try {
			Metrics metrics = new Metrics(this);
			metrics.start();
		} catch (Exception e) {
			getLogger().info("Failed to enable metrics!");
		}

		configManager = new SavesConfigManager();
		playerConfigManager = new PlayerConfigManager();
		languageConfigManager = new ConfigManager(configManager.get().getString("language", getConfig().getString("language", "english")));
		if (languageConfigManager.getName() != "english")
			languageConfigManager.setFallbackConfiguration("english");

		/* finally done! */
		getLogger().info("Activated");
	}

	public void onDisable() {
		getLogger().info("Disabled");
	}

	public SavesConfigManager getSavesConfigManager() {
		return configManager;
	}

	public ConfigManager getLanguageConfigManager() {
		return languageConfigManager;
	}

	/*
	 * WorldGuard implementation functions
	 */

	private boolean setupWorldGuard() {
		Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");

		// WorldGuard may not be loaded
		if (plugin == null || !(plugin instanceof WorldGuardPlugin))
			return false;
		else {
			wgPlugin = (WorldGuardPlugin) plugin;
			return true;
		}
	}

	public WorldGuardPlugin getWorldGuard() {
		if (wgPlugin != null)
			return wgPlugin;

		return null; // TODO: Maybe throw an exception in here
	}

	/*
	 * Vault implementation functions
	 */

	private boolean setupEconomy() {
		final Plugin plugin = this.getServer().getPluginManager().getPlugin("PlayerPoints");
		econ = PlayerPoints.class.cast(plugin);
		return econ != null;
	}

	private boolean setupPermissions() {
		if (getServer().getPluginManager().getPlugin("Vault") != null) {
			RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);

			if (rsp != null) {
				perm = rsp.getProvider();
				return perm != null;
			}
		}

		return false;
	}

	public PlayerPoints getEconomy() {
		return econ;
	}

	public Permission getPermission() {
		return perm;
	}

	public PlayerConfigManager getPlayerConfigManager() {
		return playerConfigManager;
	}

	public Updater getPluginUpdater() {
		return pluginUpdater;
	}
}
