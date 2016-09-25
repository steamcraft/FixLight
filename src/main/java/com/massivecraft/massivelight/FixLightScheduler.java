package com.massivecraft.massivelight;

import com.google.common.base.Preconditions;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class FixLightScheduler extends BukkitRunnable {
    public final int chunksPerTick;
    public final int tickDelay;
    public final String taskDoneMessage;
    public final List<Task> tasks;
    public int currentTask;
    public BukkitTask bukkitTask;

    public FixLightScheduler(int chunksPerTick, int tickDelay, String taskDoneMessage) {
        this.chunksPerTick = chunksPerTick;
        this.tickDelay = tickDelay;
        this.taskDoneMessage = taskDoneMessage;
        this.tasks = new ArrayList<>();
    }

    public void addTask(Task task) {
        tasks.add(task);
    }

    public void removeTask(int index) {
        Preconditions.checkElementIndex(index, tasks.size());
        tasks.remove(index);
    }

    @Override
    public void run() {
        for (int i = 0; i < chunksPerTick; i++) {
            if (currentTask >= tasks.size()) {
                currentTask = 0;
            }

            if (tasks.size() == 0) {
                return;
            }

            Task task = tasks.get(currentTask);

            new ChunkWrap(task.world, task.getCurrentX(), task.getCurrentZ()).recalcLightLevel();

            if (task.next()) {
                Player player = Bukkit.getPlayer(task.player);
                if (player != null) {
                    player.sendMessage(String.format(
                            taskDoneMessage, task.world.getName(), task.x, task.z, task.width, task.height));
                }
                tasks.remove(currentTask);
            } else {
                currentTask = (currentTask + 1) % tasks.size();
            }
        }
    }

    public void start(Plugin plugin) {
        stop();
        bukkitTask = this.runTaskTimer(plugin, 1, tickDelay);
    }

    public void stop() {
        if (bukkitTask != null) {
            bukkitTask.cancel();
            bukkitTask = null;
        }
    }

    public final static class Task {
        public final UUID player;
        public final World world;
        public final int x;
        public final int z;
        public final int width;
        public final int height;
        public int index;

        public int getCurrentX() {
            int rx = index % width;
            return rx + x;
        }
        public int getCurrentZ() {
            int rz = index / width;
            return rz + z;
        }

        public int getTotal() {
            return width * height;
        }
        public int getCurrent() {
            return index;
        }

        public boolean next() {
            if (index != width * height) {
                index++;
                return index == width * height;
            } else return true;
        }


        public Task(Player player, World world, int x, int z, int width, int height) {
            this.player = player.getUniqueId();
            this.world = world;

            this.x = x;
            this.z = z;
            this.width = width;
            this.height = height;
            this.index = 0;
        }
    }
}
