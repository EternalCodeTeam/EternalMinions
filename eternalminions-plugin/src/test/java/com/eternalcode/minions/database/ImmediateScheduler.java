package com.eternalcode.minions.database;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.commons.scheduler.Task;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

final class ImmediateScheduler implements Scheduler {

    @Override
    public Task run(Runnable runnable) {
        runnable.run();
        return ImmediateTask.INSTANCE;
    }

    @Override
    public Task runAsync(Runnable runnable) {
        runnable.run();
        return ImmediateTask.INSTANCE;
    }

    @Override
    public Task runLater(Runnable runnable, Duration delay) {
        return this.run(runnable);
    }

    @Override
    public Task runLaterAsync(Runnable runnable, Duration delay) {
        return this.runAsync(runnable);
    }

    @Override
    public Task timer(Runnable runnable, Duration delay, Duration interval) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Task timerAsync(Runnable runnable, Duration delay, Duration interval) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> CompletableFuture<T> complete(Supplier<T> supplier) {
        return CompletableFuture.completedFuture(supplier.get());
    }

    @Override
    public <T> CompletableFuture<T> completeAsync(Supplier<T> supplier) {
        return this.complete(supplier);
    }

    private enum ImmediateTask implements Task {
        INSTANCE;

        @Override
        public void cancel() {
        }

        @Override
        public boolean isCanceled() {
            return false;
        }

        @Override
        public boolean isAsync() {
            return true;
        }

        @Override
        public boolean isRunning() {
            return false;
        }

        @Override
        public boolean isRepeating() {
            return false;
        }
    }
}
