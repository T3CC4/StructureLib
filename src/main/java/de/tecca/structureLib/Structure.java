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
}
