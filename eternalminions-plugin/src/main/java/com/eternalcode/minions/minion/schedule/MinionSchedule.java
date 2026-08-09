package com.eternalcode.minions.minion.schedule;

import com.eternalcode.minions.minion.MinionId;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.Arrays;

public final class MinionSchedule {

    private final Long2IntOpenHashMap positionsByMinionId;
    private ScheduledMinion[] scheduledMinions;
    private long[] deadlineTicks;
    private int size;

    public MinionSchedule(int initialCapacity) {
        if (initialCapacity < 1) {
            throw new IllegalArgumentException("Initial capacity must be positive");
        }

        this.scheduledMinions = new ScheduledMinion[initialCapacity];
        this.deadlineTicks = new long[initialCapacity];
        this.positionsByMinionId = new Long2IntOpenHashMap(initialCapacity);
        this.positionsByMinionId.defaultReturnValue(-1);
    }

    public void schedule(ScheduledMinion scheduledMinion, long deadlineTick) {
        int position = this.positionsByMinionId.get(scheduledMinion.minionId().value());
        if (position >= 0) {
            this.deadlineTicks[position] = deadlineTick;
            position = this.siftUp(position);
            this.siftDown(position);
            return;
        }

        this.ensureCapacity();
        int newPosition = this.size++;
        this.scheduledMinions[newPosition] = scheduledMinion;
        this.deadlineTicks[newPosition] = deadlineTick;
        this.positionsByMinionId.put(scheduledMinion.minionId().value(), newPosition);
        this.siftUp(newPosition);
    }

    public ScheduledMinion pollDue(long currentTick) {
        if (this.size == 0 || this.deadlineTicks[0] > currentTick) {
            return null;
        }

        return this.removeAt(0);
    }

    public boolean cancel(MinionId minionId) {
        int position = this.positionsByMinionId.get(minionId.value());
        if (position < 0) {
            return false;
        }

        this.removeAt(position);
        return true;
    }

    public int size() {
        return this.size;
    }

    private ScheduledMinion removeAt(int position) {
        ScheduledMinion removedMinion = this.scheduledMinions[position];
        this.positionsByMinionId.remove(removedMinion.minionId().value());

        int lastPosition = --this.size;
        if (position != lastPosition) {
            this.scheduledMinions[position] = this.scheduledMinions[lastPosition];
            this.deadlineTicks[position] = this.deadlineTicks[lastPosition];
            this.positionsByMinionId.put(this.scheduledMinions[position].minionId().value(), position);
            position = this.siftUp(position);
            this.siftDown(position);
        }

        this.scheduledMinions[lastPosition] = null;
        return removedMinion;
    }

    private int siftUp(int position) {
        while (position > 0) {
            int parentPosition = (position - 1) >>> 1;
            if (!this.isBefore(position, parentPosition)) {
                return position;
            }

            this.swap(position, parentPosition);
            position = parentPosition;
        }

        return position;
    }

    private void siftDown(int position) {
        int firstLeafPosition = this.size >>> 1;
        while (position < firstLeafPosition) {
            int leftChildPosition = (position << 1) + 1;
            int rightChildPosition = leftChildPosition + 1;
            int firstChildPosition = rightChildPosition < this.size
                    && this.isBefore(rightChildPosition, leftChildPosition)
                    ? rightChildPosition
                    : leftChildPosition;
            if (!this.isBefore(firstChildPosition, position)) {
                return;
            }

            this.swap(position, firstChildPosition);
            position = firstChildPosition;
        }
    }

    private boolean isBefore(int firstPosition, int secondPosition) {
        long firstDeadlineTick = this.deadlineTicks[firstPosition];
        long secondDeadlineTick = this.deadlineTicks[secondPosition];
        if (firstDeadlineTick != secondDeadlineTick) {
            return firstDeadlineTick < secondDeadlineTick;
        }

        return this.scheduledMinions[firstPosition].minionId().value()
                < this.scheduledMinions[secondPosition].minionId().value();
    }

    private void swap(int firstPosition, int secondPosition) {
        ScheduledMinion firstMinion = this.scheduledMinions[firstPosition];
        this.scheduledMinions[firstPosition] = this.scheduledMinions[secondPosition];
        this.scheduledMinions[secondPosition] = firstMinion;

        long firstDeadlineTick = this.deadlineTicks[firstPosition];
        this.deadlineTicks[firstPosition] = this.deadlineTicks[secondPosition];
        this.deadlineTicks[secondPosition] = firstDeadlineTick;

        this.positionsByMinionId.put(this.scheduledMinions[firstPosition].minionId().value(), firstPosition);
        this.positionsByMinionId.put(this.scheduledMinions[secondPosition].minionId().value(), secondPosition);
    }

    private void ensureCapacity() {
        if (this.size < this.scheduledMinions.length) {
            return;
        }

        int newCapacity = this.scheduledMinions.length + (this.scheduledMinions.length >>> 1) + 1;
        this.scheduledMinions = Arrays.copyOf(this.scheduledMinions, newCapacity);
        this.deadlineTicks = Arrays.copyOf(this.deadlineTicks, newCapacity);
    }
}
