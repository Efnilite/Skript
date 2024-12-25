package ch.njol.skript.util;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus.ScheduledForRemoval;
import org.skriptlang.skript.scheduler.TaskManager;
import org.jetbrains.annotations.Nullable;

import ch.njol.skript.Skript;
import ch.njol.util.Closeable;

/**
 * @deprecated Moved package to {@link com.skriptlang.skript.scheduler.Task}
 */
@ScheduledForRemoval
@Deprecated
public abstract class Task implements Runnable, Closeable {
	
	private final Plugin plugin;
	private final boolean async;
	private long period = -1;
	
	private int taskID = -1;
	
	public Task(final Plugin plugin, final long delay, final long period) {
		this(plugin, delay, period, false);
	}
	
	public Task(final Plugin plugin, final long delay, final long period, final boolean async) {
		this.plugin = plugin;
		this.period = period;
		this.async = async;
		schedule(delay);
	}
	
	public Task(final Plugin plugin, final long delay) {
		this(plugin, delay, false);
	}
	
	public Task(final Plugin plugin, final long delay, final boolean async) {
		this.plugin = plugin;
		this.async = async;
		schedule(delay);
	}
	
	/**
	 * Only call this if the task is not alive.
	 * 
	 * @param delay
	 */
	private void schedule(final long delay) {
		assert !isAlive();
		if (!Skript.getInstance().isEnabled())
			return;
		
		if (period == -1) {
			if (async) {
				taskID = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, this, delay).getTaskId();
			} else {
				taskID = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, this, delay);
			}
		} else {
			if (async) {
				taskID = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this, delay, period).getTaskId();
			} else {
				taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this, delay, period);
			}
		}
		assert taskID != -1;
	}
	
	/**
	 * @return Whether this task is still running, i.e. whether it will run later or is currently running.
	 */
	public final boolean isAlive() {
		if (taskID == -1)
			return false;
		return Bukkit.getScheduler().isQueued(taskID) || Bukkit.getScheduler().isCurrentlyRunning(taskID);
	}
	
	/**
	 * Cancels this task.
	 */
	public final void cancel() {
		if (taskID != -1) {
			Bukkit.getScheduler().cancelTask(taskID);
			taskID = -1;
		}
	}
	
	@Override
	public void close() {
		cancel();
	}
	
	/**
	 * Re-schedules the task to run next after the given delay. If this task was repeating it will continue so using the same period as before.
	 * 
	 * @param delay
	 */
	public void setNextExecution(final long delay) {
		assert delay >= 0;
		cancel();
		schedule(delay);
	}
	
	/**
	 * Sets the period of this task. This will re-schedule the task to be run next after the given period if the task is still running.
	 * 
	 * @param period Period in ticks or -1 to cancel the task and make it non-repeating
	 */
	public void setPeriod(final long period) {
		assert period == -1 || period > 0;
		if (period == this.period)
			return;
		this.period = period;
		if (isAlive()) {
			cancel();
			if (period != -1)
				schedule(period);
		}
	}

	/**
	 * Calls a method on Bukkit's main thread.
	 * <p>
	 * Hint: Use a Callable&lt;Void&gt; to make a task which blocks your current thread until it is completed.
	 * 
	 * @param callable The method
	 * @param p The plugin that owns the task. Must be enabled.
	 * @return What the method returned or null if it threw an error or was stopped (usually due to the server shutting down)
	 * 
	 * @deprecated callSync has been moved in to {@link org.skriptlang.skript.scheduler.platforms.SpigotScheduler#callSync(Callable, Plugin)} and you must cast
	 * {@link org.skriptlang.skript.scheduler.TaskManager#getScheduler()} to {@link org.skriptlang.skript.scheduler.platforms.SpigotScheduler} for access.
	 */
	@Nullable
	@Deprecated
	public static <T> T callSync(Callable<T> callable) {
		if (Bukkit.isPrimaryThread()) {
			try {
				return callable.call();
			} catch (final Exception e) {
				Skript.exception(e);
			}
		}
		try {
			Future<T> future = TaskManager.submitSafely(callable);
			return future.get();
		} catch (Exception e) {
			Skript.exception(e);
			return null;
		}
	}

}
