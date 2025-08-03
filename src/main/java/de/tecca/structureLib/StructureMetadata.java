package de.tecca.structureLib;

import java.util.HashSet;
import java.util.Set;

public class StructureMetadata {
    private String sourceDimension;
    private String sourceBiome;
    private int sourceY;
    private String sourceWorldType;

    private final Set<String> allowedDimensions;
    private final Set<String> allowedBiomes;
    private final Set<String> forbiddenBiomes;
    private IntRange spawnHeightRange;
    private final SpawnConditions spawnConditions;

    private float spawnChance;
    private int minDistanceFromSame;
    private int minDistanceFromAny;
    private boolean naturalSpawning;

    private final Set<String> tags;
    private String category;

    public StructureMetadata() {
        this.allowedDimensions = new HashSet<>();
        this.allowedBiomes = new HashSet<>();
        this.forbiddenBiomes = new HashSet<>();
        this.spawnHeightRange = new IntRange(-64, 320);
        this.spawnConditions = new SpawnConditions();
        this.spawnChance = 0.01f;
        this.minDistanceFromSame = 200;
        this.minDistanceFromAny = 50;
        this.naturalSpawning = false;
        this.tags = new HashSet<>();
        this.category = "uncategorized";
    }

    public String getSourceDimension() { return sourceDimension; }
    public void setSourceDimension(String sourceDimension) { this.sourceDimension = sourceDimension; }
    public String getSourceBiome() { return sourceBiome; }
    public void setSourceBiome(String sourceBiome) { this.sourceBiome = sourceBiome; }
    public int getSourceY() { return sourceY; }
    public void setSourceY(int sourceY) { this.sourceY = sourceY; }
    public String getSourceWorldType() { return sourceWorldType; }
    public void setSourceWorldType(String sourceWorldType) { this.sourceWorldType = sourceWorldType; }

    public Set<String> getAllowedDimensions() { return allowedDimensions; }
    public Set<String> getAllowedBiomes() { return allowedBiomes; }
    public Set<String> getForbiddenBiomes() { return forbiddenBiomes; }
    public IntRange getSpawnHeightRange() { return spawnHeightRange; }
    public void setSpawnHeightRange(IntRange spawnHeightRange) { this.spawnHeightRange = spawnHeightRange; }
    public SpawnConditions getSpawnConditions() { return spawnConditions; }

    public float getSpawnChance() { return spawnChance; }
    public void setSpawnChance(float spawnChance) { this.spawnChance = spawnChance; }
    public int getMinDistanceFromSame() { return minDistanceFromSame; }
    public void setMinDistanceFromSame(int minDistanceFromSame) { this.minDistanceFromSame = minDistanceFromSame; }
    public int getMinDistanceFromAny() { return minDistanceFromAny; }
    public void setMinDistanceFromAny(int minDistanceFromAny) { this.minDistanceFromAny = minDistanceFromAny; }
    public boolean isNaturalSpawning() { return naturalSpawning; }
    public void setNaturalSpawning(boolean naturalSpawning) { this.naturalSpawning = naturalSpawning; }

    public Set<String> getTags() { return tags; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}