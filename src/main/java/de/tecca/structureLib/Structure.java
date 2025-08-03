package de.tecca.structureLib;

import java.util.ArrayList;
import java.util.List;

public class Structure {
    private String id;
    private String author;
    private long created;
    private int[] size;
    private final List<BlockRegion> regions;
    private final List<BlockEntity> blockEntities;
    private final List<EntityData> entities;

    private StructureMetadata metadata;

    public Structure() {
        this.regions = new ArrayList<>();
        this.blockEntities = new ArrayList<>();
        this.entities = new ArrayList<>();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public long getCreated() { return created; }
    public void setCreated(long created) { this.created = created; }
    public int[] getSize() { return size; }
    public void setSize(int[] size) { this.size = size; }
    public List<BlockRegion> getRegions() { return regions; }
    public List<BlockEntity> getBlockEntities() { return blockEntities; }
    public List<EntityData> getEntities() { return entities; }

    public StructureMetadata getMetadata() { return metadata; }
    public void setMetadata(StructureMetadata metadata) { this.metadata = metadata; }

    public Structure clone() {
        Structure cloned = new Structure();
        cloned.setId(this.id);
        cloned.setAuthor(this.author);
        cloned.setCreated(this.created);
        cloned.setSize(this.size != null ? this.size.clone() : null);

        cloned.getRegions().addAll(this.regions);

        cloned.getBlockEntities().addAll(this.blockEntities);

        cloned.getEntities().addAll(this.entities);

        if (this.metadata != null) {
            StructureMetadata clonedMetadata = new StructureMetadata();
            clonedMetadata.setSourceDimension(this.metadata.getSourceDimension());
            clonedMetadata.setSourceBiome(this.metadata.getSourceBiome());
            clonedMetadata.setSourceY(this.metadata.getSourceY());
            clonedMetadata.setSourceWorldType(this.metadata.getSourceWorldType());
            clonedMetadata.getAllowedDimensions().addAll(this.metadata.getAllowedDimensions());
            clonedMetadata.getAllowedBiomes().addAll(this.metadata.getAllowedBiomes());
            clonedMetadata.getForbiddenBiomes().addAll(this.metadata.getForbiddenBiomes());
            clonedMetadata.setSpawnHeightRange(this.metadata.getSpawnHeightRange());
            clonedMetadata.setSpawnChance(this.metadata.getSpawnChance());
            clonedMetadata.setMinDistanceFromSame(this.metadata.getMinDistanceFromSame());
            clonedMetadata.setMinDistanceFromAny(this.metadata.getMinDistanceFromAny());
            clonedMetadata.setNaturalSpawning(this.metadata.isNaturalSpawning());
            clonedMetadata.getTags().addAll(this.metadata.getTags());
            clonedMetadata.setCategory(this.metadata.getCategory());
            cloned.setMetadata(clonedMetadata);
        }

        return cloned;
    }
}