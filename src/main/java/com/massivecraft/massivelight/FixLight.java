package com.massivecraft.massivelight;

import org.bukkit.Chunk;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

public class FixLight extends JavaPlugin implements CommandExecutor, Listener {
	private final Logger LOGGER = this.getLogger();

	public boolean fixChunkGeneration = false;
	public int maxRadius = 8;
	public Properties messages;

	public Set<ChunkWrap> fixed = new HashSet<>();

	public FixLightScheduler scheduler;

	@Override
	public void onEnable() {
        try {
            this.saveDefaultConfig();
            this.saveResource("messages.properties", false);

            messages = new Properties();
            try {
                messages.load(new FileInputStream(new File(getDataFolder(), "messages.properties")));
            } catch (IOException e) {
                try {
                    messages.load(getResource("messages.properties"));
                } catch (IOException e1) {
                    LOGGER.severe("Could not load messages.");
                    throw e1;
                }
            }

            fixChunkGeneration = this.getConfig().getBoolean("fixChunkGeneration", false);
            maxRadius = this.getConfig().getInt("maxRadius", 8);


			int chunksPerTick = this.getConfig().getInt("chunksPerTick", 1);
			int tickDelay = this.getConfig().getInt("tickDelay", 1);
			String taskDoneMessage = messages.getProperty("task_done_message");
			scheduler = new FixLightScheduler(chunksPerTick, tickDelay, taskDoneMessage);
			scheduler.start(this);

            getCommand("fixlight").setExecutor(this);

            PluginManager pm = getServer().getPluginManager();
            pm.registerEvents(this, this);
        } catch (Exception e) {
            getPluginLoader().disablePlugin(this);
        }
	}

	@Override
	public void onDisable() {
		scheduler.stop();
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void genfix(ChunkPopulateEvent event) {
		// If genfix is enabled ...
		if (!fixChunkGeneration)
			return;

		// ... and a chunk is being populated ...
		Chunk chunk = event.getChunk();
		ChunkWrap populatedcw = new ChunkWrap(chunk);

		// ... then for each surrounding chunk and the chunk itself ...
		for (ChunkWrap cw : populatedcw.getSurrounding(true)) {
			// ... if the chunk isn't already fixed ...
			if (this.fixed.contains(cw))
				continue;

			// ... try recalculating the light level ...
			if (!cw.recalcLightLevel())
				continue;

			// ... and remember the success on success.
			this.fixed.add(cw);
		}
	}

	public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
		if (args.length < 1) {
			sender.sendMessage(messages.getProperty("not_enough_arguments"));
			return true;
		}

        if (!sender.hasPermission("fixlight")) {
            sender.sendMessage(messages.getProperty("insufficient_permissions"));
            return true;
        }

		if (args[0].equalsIgnoreCase("list")) {
            sender.sendMessage(messages.getProperty("task_list_first"));

            if (scheduler.tasks.size() == 0) {
                sender.sendMessage(messages.getProperty("task_list_no_entries"));
            } else {
                for (int i = 0; i < scheduler.tasks.size(); i++) {
                    FixLightScheduler.Task task = scheduler.tasks.get(i);

                    OfflinePlayer player = getServer().getOfflinePlayer(task.player);
                    String playerName = "{unknown}";
                    if (player != null) playerName = player.getName();
                    if (playerName == null) playerName = "{unknown}";

                    String worldName = task.world.getName();
                    if (worldName == null) worldName = "{unknown}";

                    String line = String.format(messages.getProperty("task_list_entry"),
                            i + 1, playerName, task.getCurrent(), task.getTotal(), worldName);
                    sender.sendMessage(line);
                }
            }
            return true;
		} else if (args[0].equalsIgnoreCase("cancel")) {
            if (args.length < 2) {
                sender.sendMessage(messages.getProperty("not_enough_arguments"));
                return true;
            }

            int index;
            try {
                index = Integer.valueOf(args[1]);
            } catch (NumberFormatException nfe) {
                sender.sendMessage(messages.getProperty("cancel_argument_must_be_an_integer"));
                return true;
            }

            if (index <= 0 || index > scheduler.tasks.size()) {
                sender.sendMessage(messages.getProperty("task_index_is_out_of_range"));
                return true;
            }

            scheduler.removeTask(index - 1);
            return true;
		} else {
			int radius;
			try {
				radius = Integer.valueOf(args[0]);
			} catch (NumberFormatException nfe) {
				sender.sendMessage(messages.getProperty("radius_must_be_an_integer"));
				return true;
			}

			if (radius < 0) {
				sender.sendMessage(messages.getProperty("radius_may_not_be_negative"));
				return true;
			}

			if (radius > maxRadius) {
				sender.sendMessage(String.format(messages.getProperty("max_radius_is"), maxRadius));
				return true;
			}

			if (!(sender instanceof Player)) {
				sender.sendMessage(messages.getProperty("must_be_executed_as_a_player"));
				return true;
			}

			Player player = (Player) sender;

			Chunk origin = player.getLocation().getChunk();
			int originX = origin.getX();
			int originZ = origin.getZ();
			World world = player.getWorld();

			// Pre Inform
			int side = (1 + radius * 2);
			int target = side * side;
			sender.sendMessage(messages.getProperty("chunks_will_be_relighted"));
			sender.sendMessage(String.format(messages.getProperty("relight_task_info"), radius, side, target));

			scheduler.addTask(new FixLightScheduler.Task(
					player, player.getWorld(),
					originX - radius, originZ - radius,
					2 * radius + 1, 2 * radius + 1));

			return true;
		}
	}
}