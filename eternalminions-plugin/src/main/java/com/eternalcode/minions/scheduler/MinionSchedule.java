package com.eternalcode.minions.scheduler;

import com.eternalcode.minions.minion.MinionId;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.Arrays;

public final class MinionSchedule {

    private final Long2IntOpenHashMap positions;
    private ScheduledMinion[] minions;
    private long[] deadlines;
    private int size;

    public MinionSchedule(int initialCapacity) {
        if (initialCapacity < 1) {
            throw new IllegalArgumentException("Initial capacity must be positive");
        }

        this.minions = new ScheduledMinion[initialCapacity];
        this.deadlines = new long[initialCapacity];
        this.positions = new Long2IntOpenHashMap(initialCapacity);
        this.positions.defaultReturnValue(-1);
    }

    public void schedule(ScheduledMinion minion, long deadline) {
        int position = this.positions.get(minion.id().value());
        if (position >= 0) {
            this.deadlines[position] = deadline;
            position = this.siftUp(position);
            this.siftDown(position);
            return;
        }

        this.ensureCapacity();
        int newPosition = this.size++;
        this.minions[newPosition] = minion;
        this.deadlines[newPosition] = deadline;
        this.positions.put(minion.id().value(), newPosition);
        this.siftUp(newPosition);
    }

    public ScheduledMinion pollDue(long currentTick) {
        if (this.size == 0 || this.deadlines[0] > currentTick) {
            return null;
        }

        return this.removeAt(0);
    }

    public boolean cancel(MinionId minionId) {
        int position = this.positions.get(minionId.value());
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
        ScheduledMinion removed = this.minions[position];
        this.positions.remove(removed.id().value());

        int lastPosition = --this.size;
        if (position != lastPosition) {
            this.minions[position] = this.minions[lastPosition];
            this.deadlines[position] = this.deadlines[lastPosition];
            this.positions.put(this.minions[position].id().value(), position);
            position = this.siftUp(position);
            this.siftDown(position);
        }

        this.minions[lastPosition] = null;
        return removed;
    }

    private int siftUp(int position) {
        while (position > 0) {
            int parent = (position - 1) >>> 1;
            if (!this.before(position, parent)) {
                return position;
            }

            this.swap(position, parent);
            position = parent;
        }
        return position;
    }

    private void siftDown(int position) {
        int half = this.size >>> 1;
        while (position < half) {
            int left = (position << 1) + 1;
            int right = left + 1;
            int first = right < this.size && this.before(right, left) ? right : left;
            if (!this.before(first, position)) {
                return;
            }

            this.swap(position, first);
            position = first;
        }
    }

    private boolean before(int first, int second) {
        long firstDeadline = this.deadlines[first];
        long secondDeadline = this.deadlines[second];
        if (firstDeadline != secondDeadline) {
            return firstDeadline < secondDeadline;
        }
        return this.minions[first].id().value() < this.minions[second].id().value();
    }

    private void swap(int first, int second) {
        ScheduledMinion minion = this.minions[first];
        this.minions[first] = this.minions[second];
        this.minions[second] = minion;

        long deadline = this.deadlines[first];
        this.deadlines[first] = this.deadlines[second];
        this.deadlines[second] = deadline;

        this.positions.put(this.minions[first].id().value(), first);
        this.positions.put(this.minions[second].id().value(), second);
    }

    private void ensureCapacity() {
        if (this.size < this.minions.length) {
            return;
        }

        int newCapacity = this.minions.length + (this.minions.length >>> 1) + 1;
        this.minions = Arrays.copyOf(this.minions, newCapacity);
        this.deadlines = Arrays.copyOf(this.deadlines, newCapacity);
    }
}
