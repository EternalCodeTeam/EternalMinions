package com.eternalcode.minions.database;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.commons.scheduler.Task;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class DatabaseScheduler implements Scheduler, AutoCloseable {

    private final ScheduledThreadPoolExecutor executor;

    public DatabaseScheduler(String threadName) {
        this.executor = new ScheduledThreadPoolExecutor(
                1, runnable -> {
            Thread thread = new Thread(runnable, threadName);
            thread.setDaemon(true);
            return thread;
        });
        this.executor.setRemoveOnCancelPolicy(true);
    }

    @Override
    public Task run(Runnable runnable) {
        return this.runAsync(runnable);
    }

    @Override
    public Task runAsync(Runnable runnable) {
        return new DatabaseTask(this.executor.submit(runnable), false);
    }

    @Override
    public Task runLater(Runnable runnable, Duration delay) {
        return this.runLaterAsync(runnable, delay);
    }

    @Override
    public Task runLaterAsync(Runnable runnable, Duration delay) {
        ScheduledFuture<?> future = this.executor.schedule(runnable, delay.toMillis(), TimeUnit.MILLISECONDS);
        return new DatabaseTask(future, false);
    }

    @Override
    public Task timer(Runnable runnable, Duration delay, Duration interval) {
        return this.timerAsync(runnable, delay, interval);
    }

    @Override
    public Task timerAsync(Runnable runnable, Duration delay, Duration interval) {
        ScheduledFuture<?> future = this.executor.scheduleAtFixedRate(
                runnable,
                delay.toMillis(),
                interval.toMillis(),
                TimeUnit.MILLISECONDS
        );
        return new DatabaseTask(future, true);
    }

    @Override
    public <T> CompletableFuture<T> complete(Supplier<T> supplier) {
        return this.completeAsync(supplier);
    }

    @Override
    public <T> CompletableFuture<T> completeAsync(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, this.executor);
    }

    @Override
    public void close() {
        this.executor.shutdown();
        try {
            if (!this.executor.awaitTermination(5L, TimeUnit.SECONDS)) {
                this.executor.shutdownNow();
            }
        }
        catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            this.executor.shutdownNow();
        }
    }

    private record DatabaseTask(Future<?> future, boolean repeating) implements Task {

        @Override
        public void cancel() {
            this.future.cancel(false);
        }

        @Override
        public boolean isCanceled() {
            return this.future.isCancelled();
        }

        @Override
        public boolean isAsync() {
            return true;
        }

        @Override
        public boolean isRunning() {
            return !this.future.isDone();
        }

        @Override
        public boolean isRepeating() {
            return this.repeating;
        }
    }
}
