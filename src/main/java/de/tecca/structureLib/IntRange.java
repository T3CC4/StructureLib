package de.tecca.structureLib;

public class IntRange {
    private final int min;
    private final int max;

    public IntRange(int min, int max) {
        this.min = Math.min(min, max);
        this.max = Math.max(min, max);
    }

    public int getMin() { return min; }
    public int getMax() { return max; }

    public boolean contains(int value) {
        return value >= min && value <= max;
    }

    public int clamp(int value) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    @Override
    public String toString() {
        return min + " to " + max;
    }
}