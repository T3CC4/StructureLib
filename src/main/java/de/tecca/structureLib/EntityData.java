package de.tecca.structureLib;

import java.util.HashMap;
import java.util.Map;

public class EntityData {
    private final double[] pos;
    private final String type;
    private final Map<String, Object> data;

    public EntityData(double[] pos, String type) {
        this.pos = pos;
        this.type = type;
        this.data = new HashMap<>();
    }

    public double[] getPos() { return pos; }
    public String getType() { return type; }
    public Map<String, Object> getData() { return data; }
}
