package com.eternalcode.minions.minion.limit;

public record MinionLimitStatus(int current, int max, boolean unlimited) {

    public boolean reached() {
        return !this.unlimited && this.current >= this.max;
    }

    public String maxDisplay() {
        return this.unlimited ? "∞" : Integer.toString(this.max);
    }
}
