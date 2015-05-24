package com.spaceemotion.payforaccess.listener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import net.milkbowl.vault.permission.Permission;

import org.black_ixx.playerpoints.PlayerPoints;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.util.Vector;

import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.spaceemotion.payforaccess.CommandManager;
import com.spaceemotion.payforaccess.PayForAccessPlugin;
import com.spaceemotion.payforaccess.PermissionManager;
import com.spaceemotion.payforaccess.command.ForgetCommand;
import com.spaceemotion.payforaccess.util.ArrayUtil;
import com.spaceemotion.payforaccess.util.ChatUtil;
import com.spaceemotion.payforaccess.util.CommandUtil;
import com.spaceemotion.payforaccess.util.MessageUtil;
import com.spaceemotion.updater.Updater;

public class PlayerListener implements Listener {

	public PlayerListener(PayForAccessPlugin plugin) {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	@EventHandler
	public void onPlayerJoinEvent(PlayerJoinEvent event) {
		PayForAccessPlugin plugin = CommandManager.getPlugin();
		Player player = event.getPlayer();

		if (player.hasPermission(PermissionManager.UPDATE_MSG)) {
			Updater updater = plugin.getPluginUpdater();

			if (updater.updateIsAvailable()) {
				ChatUtil.sendPlayerMessage(player, updater.getUpdateMessage());
			}
		}
	}

	@EventHandler
	public void onPlayerInteractEvent(PlayerInteractEvent event) {
		PayForAccessPlugin plugin = CommandManager.getPlugin();
		Player player = event.getPlayer();

		if (!player.hasPermission(PermissionManager.BUY))
			return;

		Block block = event.getClickedBlock();

		if (block == null || !CommandManager.getPlugin().getConfig().getIntegerList("triggers").contains(block.getType().getId())) {
			return;
		}

		ArrayList<String> list = plugin.getSavesConfigManager().getTriggerList();
		if (list == null)
			return;

		for (String name : list) {
			ConfigurationSection section = plugin.getSavesConfigManager().get().getConfigurationSection(name);
			ArrayList<String> locationList = (ArrayList<String>) section.getStringList("locations");

			if (locationList != null && !locationList.isEmpty()) {
				for (int j = 0; j < locationList.size(); j++) {
					String loc = locationList.get(j);

					ArrayList<String> locs = (ArrayList<String>) ArrayUtil.stringToArrayList(loc, ",");
					Vector lVec = new Vector(Float.parseFloat(locs.get(0)), Float.parseFloat(locs.get(1)), Float.parseFloat(locs.get(2)));

					Location dist = block.getLocation().subtract(lVec);

					if (dist.getBlockX() == 0 && dist.getBlockY() == 0 && dist.getBlockZ() == 0) {
						Set<String> players = plugin.getPlayerConfigManager().getPlayerListOfTrigger(name);

						if (players == null) {
							players = new HashSet<String>();
						} else if (players.contains(player.getName())) {
							/* Already bought access */
							String msg = section.getString("messages.paid", MessageUtil.parseMessage("buy.alreadymember", name));
							ChatUtil.sendPlayerMessage(player, msg);

							return;
						}

						int maxPlayers = section.getInt("max-players", 0);

						if (maxPlayers != 0 && players.size() + 1 > maxPlayers) {
							String msg = section.getString("messages.limit", MessageUtil.parseMessage("buy.limit", Integer.toString(maxPlayers)));
							ChatUtil.sendPlayerMessage(player, msg);

							return;
						}

						PlayerPoints econ = plugin.getEconomy();
						Permission perm = plugin.getPermission();

						int price = Integer.parseInt(section.getString("price"));
						if (econ.getAPI().look(player.getUniqueId()) >= price) {
							econ.getAPI().take(player.getUniqueId(), price);

							/* Apply effects */
							ConfigurationSection effects = section.getConfigurationSection("effects");

							if (effects.isSet("regions")) {
								ArrayList<String> regionList = (ArrayList<String>) effects.getStringList("regions");

								for (String region : regionList) {
									try {
										RegionManager regionMng = plugin.getWorldGuard().getRegionManager(player.getWorld());
										ProtectedRegion protRegion = regionMng.getRegions().get(region);

										if (section.getBoolean("is-owner", false))
											protRegion.getOwners().addPlayer(player.getName());
										else
											protRegion.getMembers().addPlayer(player.getName());

										regionMng.save();
									} catch (Exception e) {
										player.sendMessage(ChatColor.RED + "Could not save region: " + e.getMessage());
									}
								}
							}

							if (effects.isSet("groups")) {
								ArrayList<String> groupList = (ArrayList<String>) effects.getStringList("groups");

								for (String group : groupList) {
									if (section.getBoolean("overwrite-groups", true)) {
										String[] userGroups = perm.getPlayerGroups(player);

										for (String str : userGroups)
											perm.playerRemoveGroup(player, str);
									}

									perm.playerAddGroup(player, group);
								}
							}

							if (effects.isSet("permissions")) {
								ArrayList<String> permissionsList = (ArrayList<String>) effects.getStringList("permissions");

								for (String permission : permissionsList) {
									perm.playerAdd(player, permission);
								}
							}

							if (effects.isSet("forgets")) {
								ArrayList<String> forgetList = (ArrayList<String>) effects.getStringList("forgets");
								ForgetCommand cmd = new ForgetCommand(plugin);
								cmd.useMessages(false);

								for (String forget : forgetList) {
									String[] args = { "forget", forget, player.getName() };

									if (!cmd.execute(player, args)) {
										ChatUtil.sendPlayerMessage(player, ChatColor.RED + "Error: " + cmd.getLastError());
									}

									cmd.clearErrors();
								}
							}

							if (effects.isSet("commands")) {
								ArrayList<String> cmdList = (ArrayList<String>) effects.getStringList("commands");

								for (String cmd : cmdList) {
									if (section.getBoolean("server-cmd", true))
										CommandUtil.execAsServer(cmd);
									else
										CommandUtil.execAsPlayer(cmd, player);
								}

							}

							plugin.getPlayerConfigManager().addPlayerToList(name, player);
							plugin.getPlayerConfigManager().save();

							String msg = section.getString("messages.buy", MessageUtil.parseMessage("buy.success", name));
							ChatUtil.sendPlayerMessage(player, msg);

						} else {
							String msg = section.getString("messages.notenoughmoney", MessageUtil.parseMessage("buy.notenoughmoney", name, section.getString("price")));
							ChatUtil.sendPlayerMessage(player, msg);
						}

						return;
					}
				}
			}
		}
	}

}
