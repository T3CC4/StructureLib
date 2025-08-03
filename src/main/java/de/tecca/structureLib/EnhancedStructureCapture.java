package de.tecca.structureLib;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.HashSet;
import java.util.Set;

class EnhancedStructureCapture extends StructureCapture {

    @Override
    public Structure capture(Region region, World world, String id) {
        Structure structure = super.capture(region, world, id);

        StructureMetadata metadata = captureMetadata(region, world);
        structure.setMetadata(metadata);

        return structure;
    }

    private StructureMetadata captureMetadata(Region region, World world) {
        BlockVector3 center = region.getCenter().toBlockPoint();
        Location centerLoc = new Location(world, center.getX(), center.getY(), center.getZ());

        StructureMetadata metadata = new StructureMetadata();

        metadata.setSourceDimension(getDimensionName(world));
        metadata.setSourceBiome(world.getBiome(centerLoc).toString());
        metadata.setSourceY(center.getY());
        metadata.setSourceWorldType(getWorldType(world));

        setDefaultSpawnRules(metadata, world, centerLoc);

        return metadata;
    }

    private void setDefaultSpawnRules(StructureMetadata metadata, World world, Location location) {
        metadata.getAllowedDimensions().add(getDimensionName(world));
        metadata.getAllowedBiomes().add(world.getBiome(location).toString());

        int sourceY = location.getBlockY();
        if (sourceY < 0) {
            metadata.setSpawnHeightRange(new IntRange(-64, 50));
        } else if (sourceY > 100) {
            metadata.setSpawnHeightRange(new IntRange(80, 320));
        } else {
            metadata.setSpawnHeightRange(new IntRange(50, 120));
        }

        autoTag(metadata, world, location);
    }

    private void autoTag(StructureMetadata metadata, World world, Location location) {
        String biome = world.getBiome(location).toString().toLowerCase();

        if (biome.contains("village")) metadata.getTags().add("village");
        if (biome.contains("desert")) metadata.getTags().add("desert");
        if (biome.contains("ocean")) metadata.getTags().add("ocean");
        if (biome.contains("mountain")) metadata.getTags().add("mountain");

        if (location.getBlockY() < 0) metadata.getTags().add("underground");
        if (location.getBlockY() > 100) metadata.getTags().add("elevated");

        String dimension = getDimensionName(world);
        metadata.getTags().add(dimension.toLowerCase());
    }

    private String getDimensionName(World world) {
        switch (world.getEnvironment()) {
            case NORMAL: return "overworld";
            case NETHER: return "nether";
            case THE_END: return "end";
            default: return world.getName();
        }
    }

    private String getWorldType(World world) {
        if (isModdedWorld(world)) return "MODDED";
        return world.getEnvironment().toString();
    }

    private boolean isModdedWorld(World world) {
        return !world.getName().equals("world") &&
                !world.getName().equals("world_nether") &&
                !world.getName().equals("world_the_end");
    }
}