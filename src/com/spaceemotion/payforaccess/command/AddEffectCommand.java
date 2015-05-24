package com.spaceemotion.payforaccess.command;

import java.util.ArrayList;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.spaceemotion.payforaccess.CommandManager;
import com.spaceemotion.payforaccess.PayForAccessPlugin;
import com.spaceemotion.payforaccess.PermissionManager;
import com.spaceemotion.payforaccess.config.SavesConfigManager;
import com.spaceemotion.payforaccess.util.ChatUtil;
import com.spaceemotion.payforaccess.util.LanguageUtil;
import com.spaceemotion.payforaccess.util.MessageUtil;


public class AddEffectCommand extends AbstractCommand {

	public AddEffectCommand(PayForAccessPlugin plugin) {
		super(PermissionManager.EDIT, plugin);

		description = "Add effect to trigger";
		usage = "[<type> <content>]";
	}

	@Override
	public boolean execute(CommandSender sender, String[] args) {
		Player player = (Player) sender;
		
		if (!workingTriggerIsSet(player.getName())) return false;

		if (args.length == 1) {
			String str = "";

			ArrayList<String> types = new ArrayList<String>();
			types.add("region");
			types.add("permission");
			types.add("group");
			types.add("forget");
			types.add("command");

			for (int s = 0; s < types.size(); s++) {
				str += "&7" + types.get(s);

				if (s < types.size() - 2) str += "&f, ";
				else if (s < types.size() - 1) str += " &f" + LanguageUtil.getString("vocab.and") + " ";
			}

			ChatUtil.sendPlayerMessage(player, MessageUtil.parseMessage("create.info", str));
			return true;
		}
		
		if (!workingTriggerIsSet(player.getName())) return false;
		if (!checkArguments(args, 2)) return false;
		
		SavesConfigManager config = CommandManager.getPlugin().getSavesConfigManager();

		String name = config.getWorkingTrigger(player); 
		String type = args[1];
		String content = args[2];
		

		ConfigurationSection effects = config.get().getConfigurationSection(name+".effects");

		/* Check for the effect type */
		if (type.equalsIgnoreCase("region")) {
			ArrayList<String> regions = (ArrayList<String>) effects.getStringList("regions");

			RegionManager regionMng = CommandManager.getPlugin().getWorldGuard().getRegionManager(player.getWorld());
			ProtectedRegion protRegion = regionMng.getRegions().get(content);

			if (protRegion == null) {
				setLastError(MessageUtil.parseMessage("error.region.notexists", content));
			} else {
				if (regions.contains(content)) {
					setLastError(LanguageUtil.getString("error.region.defined"));
				} else {
					regions.add(content);
					effects.set("regions", regions);
				}
			}
		} else if (type.equalsIgnoreCase("permission")) {
			ArrayList<String> permissions = (ArrayList<String>) effects.getStringList("permissions");

			if (permissions.contains(content)) {
				setLastError(LanguageUtil.getString("error.permission.defined"));
			} else {
				permissions.add(content);
				effects.set("permissions", permissions);
			}
		} else if (type.equalsIgnoreCase("group")) {
			ArrayList<String> groups = (ArrayList<String>) effects.getStringList("groups");

			if (groups.contains(content)) {
				setLastError(LanguageUtil.getString("error.group.defined"));
			} else {
				groups.add(content);
				effects.set("groups", groups);
			}
		} else if (type.equalsIgnoreCase("forget")) {
			ArrayList<String> forgets = (ArrayList<String>) effects.getStringList("forgets");

			if (!config.getTriggerList().contains(content)) {
				setLastError(LanguageUtil.getString("error.notexists"));
			} else if (forgets.contains(content)) {
				setLastError(LanguageUtil.getString("error.forget.defined"));
			} else {
				forgets.add(content);
				effects.set("forgets", forgets);
			}
		} else if (type.equalsIgnoreCase("command")) {
			ArrayList<String> cmds = (ArrayList<String>) effects.getStringList("commands");

			if (cmds.contains(content)) {
				setLastError(MessageUtil.parseMessage("error.cmd.defined", content));
			} else {
				cmds.add(content);
				effects.set("commands", cmds);
			}
		}

		if(getLastError().isEmpty()) {
			config.save();

			ChatUtil.sendPlayerMessage(player, MessageUtil.parseMessage("create.success", name, type, content));
			
			return true;
		} else {
			return false;
		}
	}

}
