package org.zkaleejoo.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

public class SchedulerUtils {

    private final Plugin plugin;
    private final boolean isFolia;

    public SchedulerUtils(Plugin plugin, boolean isFolia) {
        this.plugin = plugin;
        this.isFolia = isFolia;
    }

    public void runTask(Runnable task) {
        if (isFolia) {
            Bukkit.getGlobalRegionScheduler().execute(plugin, task);
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public TaskWrapper runTaskLater(Runnable task, long delayTicks) {
        if (isFolia) {
            io.papermc.paper.threadedregions.scheduler.ScheduledTask t = Bukkit.getGlobalRegionScheduler().runDelayed(plugin, scheduledTask -> task.run(), Math.max(1, delayTicks));
            return new TaskWrapper(t);
        } else {
            org.bukkit.scheduler.BukkitTask t = Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
            return new TaskWrapper(t);
        }
    }

    public TaskWrapper runTaskTimer(Runnable task, long delayTicks, long periodTicks) {
        if (isFolia) {
            io.papermc.paper.threadedregions.scheduler.ScheduledTask t = Bukkit.getGlobalRegionScheduler()
                    .runAtFixedRate(plugin, scheduledTask -> task.run(), Math.max(1, delayTicks), Math.max(1, periodTicks));
            return new TaskWrapper(t);
        } else {
            org.bukkit.scheduler.BukkitTask t = Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
            return new TaskWrapper(t);
        }
    }

    public void runTaskAsync(Runnable task) {
        if (isFolia) {
            Bukkit.getAsyncScheduler().runNow(plugin, t -> task.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        }
    }

    public void runAtLocation(Location location, Runnable task) {
        if (isFolia) {
            Bukkit.getRegionScheduler().execute(plugin, location, task);
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public TaskWrapper runAtLocationLater(Location location, Runnable task, long delayTicks) {
        if (isFolia) {
            io.papermc.paper.threadedregions.scheduler.ScheduledTask t = Bukkit.getRegionScheduler().runDelayed(plugin, location, scheduledTask -> task.run(), Math.max(1, delayTicks));
            return new TaskWrapper(t);
        } else {
            org.bukkit.scheduler.BukkitTask t = Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
            return new TaskWrapper(t);
        }
    }

    public TaskWrapper runAtLocationTimer(Location location, Runnable task, long delayTicks, long periodTicks) {
        if (isFolia) {
            io.papermc.paper.threadedregions.scheduler.ScheduledTask t = Bukkit.getRegionScheduler()
                    .runAtFixedRate(plugin, location, scheduledTask -> task.run(), Math.max(1, delayTicks), Math.max(1, periodTicks));
            return new TaskWrapper(t);
        } else {
            org.bukkit.scheduler.BukkitTask t = Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
            return new TaskWrapper(t);
        }
    }

    public void runAtEntity(Entity entity, Runnable task) {
        if (isFolia) {
            entity.getScheduler().execute(plugin, task, null, 0L);
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public TaskWrapper runAtEntityLater(Entity entity, Runnable task, long delayTicks) {
        if (isFolia) {
            io.papermc.paper.threadedregions.scheduler.ScheduledTask t = entity.getScheduler().runDelayed(plugin, scheduledTask -> task.run(), null, Math.max(1, delayTicks));
            return new TaskWrapper(t);
        } else {
            org.bukkit.scheduler.BukkitTask t = Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
            return new TaskWrapper(t);
        }
    }

    public TaskWrapper runAtEntityTimer(Entity entity, Runnable task, long delayTicks, long periodTicks) {
        if (isFolia) {
            io.papermc.paper.threadedregions.scheduler.ScheduledTask t = entity.getScheduler()
                    .runAtFixedRate(plugin, scheduledTask -> task.run(), null, Math.max(1, delayTicks), Math.max(1, periodTicks));
            return new TaskWrapper(t);
        } else {
            org.bukkit.scheduler.BukkitTask t = Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
            return new TaskWrapper(t);
        }
    }

    public void teleportEntity(Entity entity, Location location) {
        if (isFolia) {
            entity.teleportAsync(location);
        } else {
            entity.teleport(location);
        }
    }

    public static class TaskWrapper {
        private Object foliaTask;
        private org.bukkit.scheduler.BukkitTask bukkitTask;

        public TaskWrapper(Object foliaTask) {
            this.foliaTask = foliaTask;
        }

        public TaskWrapper(org.bukkit.scheduler.BukkitTask bukkitTask) {
            this.bukkitTask = bukkitTask;
        }

        public void cancel() {
            if (foliaTask != null) {
                ((io.papermc.paper.threadedregions.scheduler.ScheduledTask) foliaTask).cancel();
            } else if (bukkitTask != null) {
                bukkitTask.cancel();
            }
        }
    }
}
