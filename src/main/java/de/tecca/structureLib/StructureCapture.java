package de.tecca.structureLib;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.*;
import org.bukkit.block.data.*;
import org.bukkit.block.data.type.Gate;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

class StructureCapture {
    private static final int MIN_OPTIMIZATION_SIZE = 8;
    private static final int MAX_REGION_SIZE = 10000;
    private static final Set<Material> BLACKLISTED_MATERIALS = Set.of(
            Material.AIR, Material.CAVE_AIR, Material.VOID_AIR,
            Material.STRUCTURE_VOID, Material.BARRIER
    );
    private static final Set<EntityType> BLACKLISTED_ENTITIES = Set.of(
            EntityType.PLAYER, EntityType.UNKNOWN, EntityType.AREA_EFFECT_CLOUD,
            EntityType.EXPERIENCE_ORB, EntityType.LIGHTNING_BOLT
    );

    public Structure capture(Region region, World world, String id) {
        if (region == null || world == null || id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid capture parameters");
        }

        Structure structure = new Structure();
        structure.setId(id.trim());
        structure.setCreated(System.currentTimeMillis());

        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();

        long totalBlocks = (long)(max.getX() - min.getX() + 1) *
                (max.getY() - min.getY() + 1) *
                (max.getZ() - min.getZ() + 1);

        if (totalBlocks > 1000000) {
            throw new IllegalArgumentException("Region too large: " + totalBlocks + " blocks");
        }

        int[] size = {
                max.getX() - min.getX() + 1,
                max.getY() - min.getY() + 1,
                max.getZ() - min.getZ() + 1
        };
        structure.setSize(size);

        try {
            analyzeAndOptimize(region, world, structure, min);
            captureBlockEntities(region, world, structure, min);
            captureEntities(region, world, structure, min);
        } catch (Exception e) {
            throw new RuntimeException("Failed to capture structure: " + e.getMessage(), e);
        }

        return structure;
    }

    private void analyzeAndOptimize(Region region, World world, Structure structure, BlockVector3 origin) {
        Set<BlockVector3> processed = new HashSet<>();

        for (BlockVector3 pos : region) {
            if (processed.contains(pos)) continue;

            Block block = world.getBlockAt(pos.getX(), pos.getY(), pos.getZ());
            Material material = block.getType();

            if (BLACKLISTED_MATERIALS.contains(material)) {
                continue;
            }

            BlockRegion optimizedRegion = findOptimalRegion(pos, region, world, processed, origin);
            if (optimizedRegion != null) {
                structure.getRegions().add(optimizedRegion);
            }
        }
    }

    private BlockRegion findOptimalRegion(BlockVector3 start, Region region, World world,
                                          Set<BlockVector3> processed, BlockVector3 origin) {

        Block startBlock = world.getBlockAt(start.getX(), start.getY(), start.getZ());
        Material material = startBlock.getType();

        BlockRegion fillRegion = tryFindFillRegion(start, region, world, material, processed, origin);
        if (fillRegion != null) return fillRegion;

        BlockRegion hollowRegion = tryFindHollowRegion(start, region, world, material, processed, origin);
        if (hollowRegion != null) return hollowRegion;

        return createIndividualBlockRegion(start, startBlock, processed, origin);
    }

    private BlockRegion tryFindFillRegion(BlockVector3 start, Region region, World world,
                                          Material material, Set<BlockVector3> processed, BlockVector3 origin) {

        BlockVector3 end = findMaximumCuboid(start, region, world, material);

        int volume = calculateVolume(start, end);
        if (volume < MIN_OPTIMIZATION_SIZE) return null;

        markCuboidAsProcessed(start, end, processed);

        BlockRegion fillRegion = new BlockRegion("fill");
        fillRegion.setStart(toRelativePosition(start, origin));
        fillRegion.setEnd(toRelativePosition(end, origin));
        fillRegion.setMaterial(material.toString());

        Block block = world.getBlockAt(start.getX(), start.getY(), start.getZ());
        Map<String, Object> properties = extractBlockProperties(block);
        if (properties != null && !properties.isEmpty()) {
            fillRegion.setProperties(properties);
        }

        return fillRegion;
    }

    private BlockRegion tryFindHollowRegion(BlockVector3 start, Region region, World world,
                                            Material material, Set<BlockVector3> processed, BlockVector3 origin) {

        BlockVector3 end = findHollowBox(start, region, world, material);
        if (end == null) return null;

        int volume = calculateVolume(start, end);
        int surfaceArea = calculateSurfaceArea(start, end);

        if (surfaceArea >= volume * 0.6) return null;

        markHollowBoxAsProcessed(start, end, processed);

        BlockRegion hollowRegion = new BlockRegion("hollow");
        hollowRegion.setStart(toRelativePosition(start, origin));
        hollowRegion.setEnd(toRelativePosition(end, origin));
        hollowRegion.setMaterial(material.toString());

        Block block = world.getBlockAt(start.getX(), start.getY(), start.getZ());
        Map<String, Object> properties = extractBlockProperties(block);
        if (properties != null && !properties.isEmpty()) {
            hollowRegion.setProperties(properties);
        }

        return hollowRegion;
    }

    private BlockRegion createIndividualBlockRegion(BlockVector3 pos, Block block,
                                                    Set<BlockVector3> processed, BlockVector3 origin) {

        processed.add(pos);

        BlockRegion individual = new BlockRegion("individual");
        List<BlockData> blocks = new ArrayList<>();

        BlockData blockData = new BlockData(
                toRelativePosition(pos, origin),
                block.getType().toString()
        );

        Map<String, Object> properties = extractBlockProperties(block);
        if (properties != null && !properties.isEmpty()) {
            blockData.setProperties(properties);
        }

        blocks.add(blockData);
        individual.setBlocks(blocks);

        return individual;
    }

    private void captureBlockEntities(Region region, World world, Structure structure, BlockVector3 origin) {
        for (BlockVector3 pos : region) {
            try {
                Block block = world.getBlockAt(pos.getX(), pos.getY(), pos.getZ());

                if (BLACKLISTED_MATERIALS.contains(block.getType())) {
                    continue;
                }

                if (block.getState() instanceof Container ||
                        block.getState() instanceof Furnace ||
                        block.getState() instanceof CreatureSpawner) {

                    BlockEntity blockEntity = new BlockEntity(
                            toRelativePosition(pos, origin),
                            block.getType().toString()
                    );

                    extractBlockEntityData(block.getState(), blockEntity.getData());

                    structure.getBlockEntities().add(blockEntity);
                }
            } catch (Exception e) {
                System.err.println("Failed to capture block entity at " + pos + ": " + e.getMessage());
            }
        }
    }

    private void captureEntities(Region region, World world, Structure structure, BlockVector3 origin) {
        for (Entity entity : world.getEntities()) {
            try {
                Location loc = entity.getLocation();
                BlockVector3 pos = BlockVector3.at(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

                if (region.contains(pos) && isValidEntityType(entity)) {
                    double[] relativePos = {
                            loc.getX() - origin.getX(),
                            loc.getY() - origin.getY(),
                            loc.getZ() - origin.getZ()
                    };

                    EntityData entityData = new EntityData(relativePos, entity.getType().toString());
                    extractEntityData(entity, entityData.getData());

                    structure.getEntities().add(entityData);
                }
            } catch (Exception e) {
                System.err.println("Failed to capture entity " + entity.getType() + ": " + e.getMessage());
            }
        }
    }

    private boolean isValidEntityType(Entity entity) {
        EntityType type = entity.getType();

        return !BLACKLISTED_ENTITIES.contains(type) &&
                !type.toString().contains("SPECTRAL") &&
                !type.toString().contains("FIREWORK") &&
                entity.isValid() &&
                !entity.isDead();
    }

    private BlockVector3 findMaximumCuboid(BlockVector3 start, Region region, World world, Material material) {
        int maxX = start.getX();
        int maxY = start.getY();
        int maxZ = start.getZ();

        expandX: while (maxX + 1 <= region.getMaximumPoint().getX()) {
            for (int y = start.getY(); y <= maxY; y++) {
                for (int z = start.getZ(); z <= maxZ; z++) {
                    Block block = world.getBlockAt(maxX + 1, y, z);
                    if (block.getType() != material) {
                        break expandX;
                    }
                }
            }
            maxX++;
        }

        expandY: while (maxY + 1 <= region.getMaximumPoint().getY()) {
            for (int x = start.getX(); x <= maxX; x++) {
                for (int z = start.getZ(); z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, maxY + 1, z);
                    if (block.getType() != material) {
                        break expandY;
                    }
                }
            }
            maxY++;
        }

        expandZ: while (maxZ + 1 <= region.getMaximumPoint().getZ()) {
            for (int x = start.getX(); x <= maxX; x++) {
                for (int y = start.getY(); y <= maxY; y++) {
                    Block block = world.getBlockAt(x, y, maxZ + 1);
                    if (block.getType() != material) {
                        break expandZ;
                    }
                }
            }
            maxZ++;
        }

        return BlockVector3.at(maxX, maxY, maxZ);
    }

    private BlockVector3 findHollowBox(BlockVector3 start, Region region, World world, Material material) {
        return null;
    }

    private int calculateVolume(BlockVector3 start, BlockVector3 end) {
        return (end.getX() - start.getX() + 1) *
                (end.getY() - start.getY() + 1) *
                (end.getZ() - start.getZ() + 1);
    }

    private int calculateSurfaceArea(BlockVector3 start, BlockVector3 end) {
        int width = end.getX() - start.getX() + 1;
        int height = end.getY() - start.getY() + 1;
        int depth = end.getZ() - start.getZ() + 1;

        return 2 * (width * height + width * depth + height * depth);
    }

    private void markCuboidAsProcessed(BlockVector3 start, BlockVector3 end, Set<BlockVector3> processed) {
        for (int x = start.getX(); x <= end.getX(); x++) {
            for (int y = start.getY(); y <= end.getY(); y++) {
                for (int z = start.getZ(); z <= end.getZ(); z++) {
                    processed.add(BlockVector3.at(x, y, z));
                }
            }
        }
    }

    private void markHollowBoxAsProcessed(BlockVector3 start, BlockVector3 end, Set<BlockVector3> processed) {
        for (int x = start.getX(); x <= end.getX(); x++) {
            for (int y = start.getY(); y <= end.getY(); y++) {
                for (int z = start.getZ(); z <= end.getZ(); z++) {
                    if (x == start.getX() || x == end.getX() ||
                            y == start.getY() || y == end.getY() ||
                            z == start.getZ() || z == end.getZ()) {
                        processed.add(BlockVector3.at(x, y, z));
                    }
                }
            }
        }
    }

    private int[] toRelativePosition(BlockVector3 pos, BlockVector3 origin) {
        return new int[] {
                pos.getX() - origin.getX(),
                pos.getY() - origin.getY(),
                pos.getZ() - origin.getZ()
        };
    }

    private Map<String, Object> extractBlockProperties(Block block) {
        Map<String, Object> properties = new HashMap<>();
        org.bukkit.block.data.BlockData data = block.getBlockData();

        try {
            if (data instanceof Directional) {
                properties.put("facing", ((Directional) data).getFacing().toString());
            }
            if (data instanceof Bisected) {
                properties.put("half", ((Bisected) data).getHalf().toString());
            }
            if (data instanceof Openable) {
                properties.put("open", ((Openable) data).isOpen());
            }
            if (data instanceof Powerable) {
                properties.put("powered", ((Powerable) data).isPowered());
            }
            if (data instanceof Waterlogged) {
                properties.put("waterlogged", ((Waterlogged) data).isWaterlogged());
            }
            if (data instanceof Orientable) {
                properties.put("axis", ((Orientable) data).getAxis().toString());
            }

            if (data instanceof MultipleFacing multipleFacing) {
                Map<String, Boolean> faces = new HashMap<>();

                Set<BlockFace> allowedFaces = multipleFacing.getAllowedFaces();
                for (BlockFace face : allowedFaces) {
                    faces.put(face.toString(), multipleFacing.hasFace(face));
                }

                if (!faces.isEmpty()) {
                    properties.put("faces", faces);
                    properties.put("allowedFaces", allowedFaces.stream()
                            .map(BlockFace::toString)
                            .collect(Collectors.toSet()));
                }
            }

            if (data instanceof Gate gate) {
                properties.put("in_wall", gate.isInWall());
            }

        } catch (Exception e) {
            StructureLib.getPlugin().getLogger().warning("Failed to extract properties for " + block.getType() + ": " + e.getMessage());
        }

        return properties.isEmpty() ? null : properties;
    }

    private void extractBlockEntityData(BlockState state, Map<String, Object> data) {
        try {
            if (state instanceof Container container) {
                Map<Integer, Map<String, Object>> inventory = new HashMap<>();

                for (int i = 0; i < container.getInventory().getSize(); i++) {
                    ItemStack item = container.getInventory().getItem(i);
                    if (item != null && item.getType() != Material.AIR) {
                        Map<String, Object> itemData = new HashMap<>();
                        itemData.put("material", item.getType().toString());
                        itemData.put("amount", item.getAmount());
                        inventory.put(i, itemData);
                    }
                }

                if (!inventory.isEmpty()) {
                    data.put("inventory", inventory);
                }
            }

            if (state instanceof Furnace furnace) {
                data.put("burnTime", furnace.getBurnTime());
                data.put("cookTime", furnace.getCookTime());
            }

            if (state instanceof CreatureSpawner spawner) {
                data.put("spawnedType", Objects.requireNonNull(spawner.getSpawnedType()).toString());
                data.put("delay", spawner.getDelay());
            }
        } catch (Exception e) {
            // Continue if data extraction fails
        }
    }

    private void extractEntityData(Entity entity, Map<String, Object> data) {
        try {
            data.put("yaw", entity.getLocation().getYaw());
            data.put("pitch", entity.getLocation().getPitch());

            if (entity instanceof LivingEntity living) {
                data.put("health", living.getHealth());
            }

            if (entity instanceof ItemFrame frame) {
                frame.getItem();
                if (frame.getItem().getType() != Material.AIR) {
                    Map<String, Object> itemData = new HashMap<>();
                    itemData.put("material", frame.getItem().getType().toString());
                    itemData.put("amount", frame.getItem().getAmount());
                    data.put("item", itemData);
                    data.put("rotation", frame.getRotation().ordinal());
                }
            }
        } catch (Exception e) {
            // Continue if data extraction fails
        }
    }
}