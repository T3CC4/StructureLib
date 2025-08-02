package de.tecca.structureLib;

import java.util.List;
import java.util.Map;

public class BlockRegion {
    private final String type;
    private int[] start;
    private int[] end;
    private String material;
    private Map<String, Object> properties;
    private List<BlockData> blocks;

    public BlockRegion(String type) {
        this.type = type;
    }

    public String getType() { return type; }
    public int[] getStart() { return start; }
    public void setStart(int[] start) { this.start = start; }
    public int[] getEnd() { return end; }
    public void setEnd(int[] end) { this.end = end; }
    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }
    public Map<String, Object> getProperties() { return properties; }
    public void setProperties(Map<String, Object> properties) { this.properties = properties; }
    public List<BlockData> getBlocks() { return blocks; }
    public void setBlocks(List<BlockData> blocks) { this.blocks = blocks; }
}
