package de.tecca.structureLib;

import java.util.Map;

public class BlockData {
    private final int[] pos;
    private final String material;
    private Map<String, Object> properties;

    public BlockData(int[] pos, String material) {
        this.pos = pos;
        this.material = material;
    }

    public int[] getPos() { return pos; }
    public String getMaterial() { return material; }
    public Map<String, Object> getProperties() { return properties; }
    public void setProperties(Map<String, Object> properties) { this.properties = properties; }
}
