package de.tecca.structureLib;

import java.util.HashMap;
import java.util.Map;

public class BlockEntity {
    private final int[] pos;
    private final String type;
    private final Map<String, Object> data;

    public BlockEntity(int[] pos, String type) {
        this.pos = pos;
        this.type = type;
        this.data = new HashMap<>();
    }

    public int[] getPos() { return pos; }
    public String getType() { return type; }
    public Map<String, Object> getData() { return data; }
}
