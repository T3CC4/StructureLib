package de.tecca.structureLib;

public class SpawnConditions {
    private boolean requiresFlatGround;
    private boolean avoidWater;
    private boolean avoidLava;
    private boolean requiresSkyAccess;
    private double maxSlope;
    private int minClearHeight;

    public SpawnConditions() {
        this.requiresFlatGround = true;
        this.avoidWater = true;
        this.avoidLava = true;
        this.requiresSkyAccess = false;
        this.maxSlope = 0.3;
        this.minClearHeight = 3;
    }

    public boolean isRequiresFlatGround() { return requiresFlatGround; }
    public void setRequiresFlatGround(boolean requiresFlatGround) { this.requiresFlatGround = requiresFlatGround; }
    public boolean isAvoidWater() { return avoidWater; }
    public void setAvoidWater(boolean avoidWater) { this.avoidWater = avoidWater; }
    public boolean isAvoidLava() { return avoidLava; }
    public void setAvoidLava(boolean avoidLava) { this.avoidLava = avoidLava; }
    public boolean isRequiresSkyAccess() { return requiresSkyAccess; }
    public void setRequiresSkyAccess(boolean requiresSkyAccess) { this.requiresSkyAccess = requiresSkyAccess; }
    public double getMaxSlope() { return maxSlope; }
    public void setMaxSlope(double maxSlope) { this.maxSlope = maxSlope; }
    public int getMinClearHeight() { return minClearHeight; }
    public void setMinClearHeight(int minClearHeight) { this.minClearHeight = minClearHeight; }
}