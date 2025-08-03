package de.tecca.structureLib;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Biome;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

class MetadataBasedSpawner implements Listener {
    private final StructureLib plugin;
    private final Map<String, StructureMetadata> activeSpawners;

    private static final Set<Material> LOG_MATERIALS = Set.of(
            Material.OAK_LOG, Material.BIRCH_LOG, Material.SPRUCE_LOG, Material.JUNGLE_LOG,
            Material.ACACIA_LOG, Material.DARK_OAK_LOG, Material.CHERRY_LOG, Material.MANGROVE_LOG,
            Material.OAK_WOOD, Material.BIRCH_WOOD, Material.SPRUCE_WOOD, Material.JUNGLE_WOOD,
            Material.ACACIA_WOOD, Material.DARK_OAK_WOOD, Material.CHERRY_WOOD, Material.MANGROVE_WOOD,
            Material.STRIPPED_OAK_LOG, Material.STRIPPED_BIRCH_LOG, Material.STRIPPED_SPRUCE_LOG,
            Material.STRIPPED_JUNGLE_LOG, Material.STRIPPED_ACACIA_LOG, Material.STRIPPED_DARK_OAK_LOG,
            Material.STRIPPED_CHERRY_LOG, Material.STRIPPED_MANGROVE_LOG
    );

    private static final Set<Material> LEAF_MATERIALS = Set.of(
            Material.OAK_LEAVES, Material.BIRCH_LEAVES, Material.SPRUCE_LEAVES, Material.JUNGLE_LEAVES,
            Material.ACACIA_LEAVES, Material.DARK_OAK_LEAVES, Material.CHERRY_LEAVES, Material.MANGROVE_LEAVES
    );

    private static final Set<Material> SAPLING_MATERIALS = Set.of(
            Material.OAK_SAPLING, Material.BIRCH_SAPLING, Material.SPRUCE_SAPLING, Material.JUNGLE_SAPLING,
            Material.ACACIA_SAPLING, Material.DARK_OAK_SAPLING, Material.CHERRY_SAPLING, Material.MANGROVE_PROPAGULE
    );

    private static final Set<Material> NATURAL_GROUND = Set.of(
            Material.GRASS_BLOCK, Material.DIRT, Material.COARSE_DIRT, Material.PODZOL,
            Material.MYCELIUM, Material.DIRT_PATH, Material.ROOTED_DIRT
    );

    public MetadataBasedSpawner(StructureLib plugin) {
        this.plugin = plugin;
        this.activeSpawners = new HashMap<>();
    }

    public void registerStructure(String structureId, StructureMetadata metadata) {
        if (metadata.isNaturalSpawning()) {
            activeSpawners.put(structureId, metadata);
        }
    }

    public void unregisterStructure(String structureId) {
        activeSpawners.remove(structureId);
    }

    public void loadAllActiveSpawners() {
        File[] files = plugin.getStructuresFolder().listFiles((dir, name) -> name.endsWith(".json"));

        if (files != null) {
            for (File file : files) {
                try {
                    Structure structure = plugin.getStructureAPI().loadStructure(file);
                    StructureMetadata metadata = structure.getMetadata();

                    if (metadata != null && metadata.isNaturalSpawning()) {
                        registerStructure(structure.getId(), metadata);
                    }

                } catch (IOException e) {
                    plugin.getLogger().warning("Failed to load structure for spawning: " + file.getName());
                }
            }
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        World world = chunk.getWorld();

        for (Map.Entry<String, StructureMetadata> entry : activeSpawners.entrySet()) {
            String structureId = entry.getKey();
            StructureMetadata metadata = entry.getValue();

            if (shouldSpawnInChunk(chunk, metadata)) {
                Location spawnLoc = findBestLocationInChunk(chunk, structureId, metadata);
                if (spawnLoc != null) {
                    try {
                        Structure structure = plugin.getStructureAPI().loadStructure(
                                new File(plugin.getStructuresFolder(), structureId + ".json")
                        );

                        prepareTerrainForStructure(spawnLoc, structure, metadata);

                        plugin.getStructureAPI().placeStructure(structure, spawnLoc);

                        plugin.getLogger().info("Naturally spawned structure '" + structureId + "' at " +
                                spawnLoc.getBlockX() + ", " + spawnLoc.getBlockY() + ", " + spawnLoc.getBlockZ());

                    } catch (IOException e) {
                        plugin.getLogger().severe("Failed to place natural structure: " + e.getMessage());
                    }
                }
            }
        }
    }

    private boolean shouldSpawnInChunk(Chunk chunk, StructureMetadata metadata) {
        World world = chunk.getWorld();
        String dimension = getDimensionName(world);

        if (!metadata.getAllowedDimensions().contains(dimension)) {
            return false;
        }

        if (ThreadLocalRandom.current().nextFloat() > metadata.getSpawnChance()) {
            return false;
        }

        Biome biome = world.getBiome(chunk.getX() * 16 + 8, 64, chunk.getZ() * 16 + 8);
        if (!metadata.getAllowedBiomes().isEmpty() && !metadata.getAllowedBiomes().contains(biome.toString())) {
            return false;
        }

        if (metadata.getForbiddenBiomes().contains(biome.toString())) {
            return false;
        }

        return true;
    }

    private Location findBestLocationInChunk(Chunk chunk, String structureId, StructureMetadata metadata) {
        try {
            Structure structure = plugin.getStructureAPI().loadStructure(
                    new File(plugin.getStructuresFolder(), structureId + ".json")
            );

            int[] size = structure.getSize();
            World world = chunk.getWorld();

            List<TerrainCandidate> candidates = new ArrayList<>();

            for (int attempts = 0; attempts < 20; attempts++) {
                int x = chunk.getX() * 16 + ThreadLocalRandom.current().nextInt(Math.max(1, 16 - size[0]));
                int z = chunk.getZ() * 16 + ThreadLocalRandom.current().nextInt(Math.max(1, 16 - size[2]));

                Location surface = findSurface(world, x, z);
                if (surface != null) {
                    TerrainCandidate candidate = evaluateTerrainSuitability(surface, structure, metadata);
                    if (candidate.score > 40) {
                        candidates.add(candidate);
                    }
                }
            }

            if (!candidates.isEmpty()) {
                candidates.sort((a, b) -> Float.compare(b.score, a.score));
                return candidates.get(0).location;
            }

        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load structure for location finding: " + structureId);
        }

        return null;
    }

    private TerrainCandidate evaluateTerrainSuitability(Location location, Structure structure, StructureMetadata metadata) {
        TerrainCandidate candidate = new TerrainCandidate(location);
        int[] size = structure.getSize();
        World world = location.getWorld();

        if (!metadata.getSpawnHeightRange().contains(location.getBlockY())) {
            candidate.score = 0;
            return candidate;
        }

        List<Integer> heights = new ArrayList<>();
        int waterBlocks = 0;
        int lavaBlocks = 0;
        int treeBlocks = 0;

        for (int x = 0; x < size[0]; x++) {
            for (int z = 0; z < size[2]; z++) {
                Location checkLoc = location.clone().add(x, 0, z);
                Location surface = findSurface(world, checkLoc.getBlockX(), checkLoc.getBlockZ());

                if (surface != null) {
                    heights.add(surface.getBlockY());

                    Block block = surface.getBlock();
                    if (block.getType().toString().contains("WATER")) waterBlocks++;
                    if (block.getType().toString().contains("LAVA")) lavaBlocks++;
                    if (isTreeBlock(block.getType())) treeBlocks++;
                }
            }
        }

        if (heights.isEmpty()) {
            candidate.score = 0;
            return candidate;
        }

        int minHeight = Collections.min(heights);
        int maxHeight = Collections.max(heights);
        int heightDifference = maxHeight - minHeight;
        double averageHeight = heights.stream().mapToInt(Integer::intValue).average().orElse(0);

        float baseScore = 100;

        if (heightDifference <= 2) {
            baseScore -= 0;
        } else if (heightDifference <= 4) {
            baseScore -= 10;
        } else if (heightDifference <= 6) {
            baseScore -= 25;
        } else if (heightDifference <= 10) {
            baseScore -= 40;
        } else {
            baseScore -= 70;
        }

        SpawnConditions conditions = metadata.getSpawnConditions();

        if (conditions.isAvoidWater() && waterBlocks > 0) {
            baseScore -= 30;
        }

        if (conditions.isAvoidLava() && lavaBlocks > 0) {
            baseScore -= 50;
        }

        if (treeBlocks > 0) {
            baseScore -= 5;
        }

        double heightVariance = heights.stream()
                .mapToDouble(h -> Math.abs(h - averageHeight))
                .average().orElse(0);

        if (heightVariance < 1.0) {
            baseScore += 10;
        } else if (heightVariance < 2.0) {
            baseScore += 5;
        }

        candidate.score = Math.max(0, baseScore);
        candidate.heightDifference = heightDifference;
        candidate.treeBlocks = treeBlocks;
        candidate.waterBlocks = waterBlocks;
        candidate.lavaBlocks = lavaBlocks;

        return candidate;
    }

    private void prepareTerrainForStructure(Location location, Structure structure, StructureMetadata metadata) {
        int[] size = structure.getSize();
        World world = location.getWorld();

        removeTreesInArea(location, size, world);

        if (metadata.getSpawnConditions().isRequiresFlatGround()) {
            adaptTerrainForStructure(location, size, world);
        }
    }

    private void removeTreesInArea(Location location, int[] size, World world) {
        Set<Location> processedTrees = new HashSet<>();
        int bufferZone = 2;

        for (int x = -bufferZone; x < size[0] + bufferZone; x++) {
            for (int z = -bufferZone; z < size[2] + bufferZone; z++) {
                for (int y = -5; y < size[1] + 15; y++) {
                    Location checkLoc = location.clone().add(x, y, z);
                    Block block = world.getBlockAt(checkLoc);

                    if (isTreeBlock(block.getType()) && !processedTrees.contains(checkLoc)) {
                        removeCompleteTree(checkLoc, world, processedTrees);
                    }
                }
            }
        }
    }

    private void removeCompleteTree(Location treeLocation, World world, Set<Location> processedTrees) {
        if (processedTrees.contains(treeLocation)) return;

        Block startBlock = world.getBlockAt(treeLocation);
        if (!isTreeBlock(startBlock.getType())) return;

        Set<Location> treeBlocks = new HashSet<>();
        Queue<Location> toCheck = new LinkedList<>();
        toCheck.add(treeLocation);

        while (!toCheck.isEmpty() && treeBlocks.size() < 500) {
            Location current = toCheck.poll();
            if (treeBlocks.contains(current)) continue;

            Block block = world.getBlockAt(current);
            if (!isTreeBlock(block.getType())) continue;

            treeBlocks.add(current);
            processedTrees.add(current);

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;

                        Location adjacent = current.clone().add(dx, dy, dz);
                        if (!treeBlocks.contains(adjacent)) {
                            Block adjBlock = world.getBlockAt(adjacent);

                            if (LOG_MATERIALS.contains(block.getType())) {
                                if (isTreeBlock(adjBlock.getType())) {
                                    toCheck.add(adjacent);
                                }
                            } else if (LEAF_MATERIALS.contains(block.getType())) {
                                if (LOG_MATERIALS.contains(adjBlock.getType()) ||
                                        (LEAF_MATERIALS.contains(adjBlock.getType()) && Math.abs(dy) <= 2)) {
                                    toCheck.add(adjacent);
                                }
                            }
                        }
                    }
                }
            }
        }

        for (Location loc : treeBlocks) {
            Block block = world.getBlockAt(loc);

            if (LOG_MATERIALS.contains(block.getType())) {
                if (isAtGroundLevel(loc, world)) {
                    block.setType(Material.GRASS_BLOCK);
                } else {
                    block.setType(Material.AIR);
                }
            } else if (LEAF_MATERIALS.contains(block.getType()) || SAPLING_MATERIALS.contains(block.getType())) {
                block.setType(Material.AIR);
            }
        }

        cleanupFloatingLeaves(treeBlocks, world);
    }

    private void cleanupFloatingLeaves(Set<Location> removedTreeArea, World world) {
        for (Location treeBlock : removedTreeArea) {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dy = -3; dy <= 3; dy++) {
                    for (int dz = -3; dz <= 3; dz++) {
                        Location checkLoc = treeBlock.clone().add(dx, dy, dz);
                        Block block = world.getBlockAt(checkLoc);

                        if (LEAF_MATERIALS.contains(block.getType())) {
                            if (!isConnectedToLog(checkLoc, world, new HashSet<>())) {
                                block.setType(Material.AIR);
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isConnectedToLog(Location leafLocation, World world, Set<Location> checked) {
        if (checked.size() > 50 || checked.contains(leafLocation)) return false;
        checked.add(leafLocation);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;

                    Location adjacent = leafLocation.clone().add(dx, dy, dz);
                    Block block = world.getBlockAt(adjacent);

                    if (LOG_MATERIALS.contains(block.getType())) {
                        return true;
                    } else if (LEAF_MATERIALS.contains(block.getType()) && Math.abs(dy) <= 1) {
                        if (isConnectedToLog(adjacent, world, checked)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private void adaptTerrainForStructure(Location location, int[] size, World world) {
        List<Integer> sampleHeights = new ArrayList<>();
        List<Location> samplePoints = getSamplePoints(location, size, world);

        for (Location point : samplePoints) {
            Location surface = findSurface(world, point.getBlockX(), point.getBlockZ());
            if (surface != null) {
                sampleHeights.add(surface.getBlockY());
            }
        }

        if (sampleHeights.isEmpty()) {
            sampleHeights.add(location.getBlockY());
        }

        int targetHeight = (int) sampleHeights.stream().mapToInt(Integer::intValue).average().orElse(location.getBlockY());

        applyLayeredTerraforming(location, size, world, targetHeight);

        createBlendingZones(location, size, world, targetHeight);

        addNaturalMicroFeatures(location, size, world);
    }

    private List<Location> getSamplePoints(Location location, int[] size, World world) {
        List<Location> points = new ArrayList<>();

        points.add(location.clone());
        points.add(location.clone().add(size[0] - 1, 0, 0));
        points.add(location.clone().add(0, 0, size[2] - 1));
        points.add(location.clone().add(size[0] - 1, 0, size[2] - 1));

        points.add(location.clone().add(size[0] / 2, 0, 0));
        points.add(location.clone().add(size[0] - 1, 0, size[2] / 2));
        points.add(location.clone().add(size[0] / 2, 0, size[2] - 1));
        points.add(location.clone().add(0, 0, size[2] / 2));

        points.add(location.clone().add(size[0] / 2, 0, size[2] / 2));

        return points;
    }

    private void applyLayeredTerraforming(Location location, int[] size, World world, int targetHeight) {
        for (int x = 0; x < size[0]; x++) {
            for (int z = 0; z < size[2]; z++) {
                Location groundLoc = location.clone().add(x, 0, z);
                Location surface = findSurface(world, groundLoc.getBlockX(), groundLoc.getBlockZ());

                if (surface != null) {
                    int currentHeight = surface.getBlockY();

                    int variation = 0;
                    if ((x + z) % 3 == 0 && ThreadLocalRandom.current().nextBoolean()) {
                        variation = ThreadLocalRandom.current().nextBoolean() ? 1 : -1;
                    }
                    int adjustedTarget = targetHeight + variation;

                    int heightDiff = adjustedTarget - currentHeight;

                    if (heightDiff > 0) {
                        buildUpWithLayers(world, surface, heightDiff);
                    } else if (heightDiff < 0) {
                        digDownNaturally(world, surface, Math.abs(heightDiff));
                    }
                }
            }
        }
    }

    private void buildUpWithLayers(World world, Location surface, int heightDiff) {
        int maxFill = Math.min(heightDiff, 6);

        for (int y = 1; y <= maxFill; y++) {
            Block fillBlock = world.getBlockAt(surface.getBlockX(), surface.getBlockY() + y, surface.getBlockZ());
            if (fillBlock.getType() == Material.AIR || LEAF_MATERIALS.contains(fillBlock.getType())) {

                Material fillMaterial;
                if (y == maxFill) {
                    fillMaterial = getBiomeAppropriateSurface(world, surface);
                } else if (y >= maxFill - 2) {
                    fillMaterial = Material.DIRT;
                } else {
                    fillMaterial = world.getBlockAt(surface.getBlockX(), surface.getBlockY() - 1, surface.getBlockZ()).getType();
                    if (!fillMaterial.isSolid()) {
                        fillMaterial = Material.STONE;
                    }
                }

                fillBlock.setType(fillMaterial);
            }
        }
    }

    private void digDownNaturally(World world, Location surface, int heightDiff) {
        int maxDig = Math.min(heightDiff, 6);

        for (int y = 0; y < maxDig; y++) {
            Block digBlock = world.getBlockAt(surface.getBlockX(), surface.getBlockY() - y, surface.getBlockZ());

            if (NATURAL_GROUND.contains(digBlock.getType()) || isTreeBlock(digBlock.getType())) {
                digBlock.setType(Material.AIR);
            } else {
                break;
            }
        }

        Block newSurface = world.getBlockAt(surface.getBlockX(), surface.getBlockY() - maxDig, surface.getBlockZ());
        if (newSurface.getType() == Material.DIRT) {
            newSurface.setType(getBiomeAppropriateSurface(world, surface));
        }
    }

    private Material getBiomeAppropriateSurface(World world, Location location) {
        String biome = world.getBiome(location).toString().toLowerCase();

        if (biome.contains("desert")) return Material.SAND;
        if (biome.contains("beach")) return Material.SAND;
        if (biome.contains("snow") || biome.contains("frozen")) return Material.SNOW_BLOCK;
        if (biome.contains("mesa") || biome.contains("badlands")) return Material.RED_SAND;
        if (biome.contains("mushroom")) return Material.MYCELIUM;
        if (biome.contains("swamp")) return Material.MUD;

        return Material.GRASS_BLOCK;
    }

    private void createBlendingZones(Location location, int[] size, World world, int targetHeight) {
        int blendRadius = 3;

        for (int x = -blendRadius; x < size[0] + blendRadius; x++) {
            for (int z = -blendRadius; z < size[2] + blendRadius; z++) {
                if (x >= 0 && x < size[0] && z >= 0 && z < size[2]) continue;

                Location blendLoc = location.clone().add(x, 0, z);
                Location surface = findSurface(world, blendLoc.getBlockX(), blendLoc.getBlockZ());

                if (surface != null) {
                    double distanceFromStructure = getDistanceFromStructureEdge(x, z, size);
                    double blendFactor = Math.max(0.0, Math.min(1.0, distanceFromStructure / blendRadius));

                    blendFactor = (1 - Math.cos(blendFactor * Math.PI)) / 2;

                    int currentHeight = surface.getBlockY();
                    int blendedHeight = (int) (targetHeight * (1 - blendFactor) + currentHeight * blendFactor);

                    int heightDiff = blendedHeight - currentHeight;

                    if (Math.abs(heightDiff) <= 2) {
                        if (heightDiff > 0) {
                            Block fillBlock = world.getBlockAt(surface.getBlockX(), currentHeight + 1, surface.getBlockZ());
                            if (fillBlock.getType() == Material.AIR) {
                                fillBlock.setType(getBiomeAppropriateSurface(world, surface));
                            }
                        } else if (heightDiff < 0) {
                            Block digBlock = world.getBlockAt(surface.getBlockX(), currentHeight, surface.getBlockZ());
                            if (NATURAL_GROUND.contains(digBlock.getType())) {
                                digBlock.setType(Material.AIR);
                            }
                        }
                    }
                }
            }
        }
    }

    private double getDistanceFromStructureEdge(int x, int z, int[] size) {
        double dx = 0;
        double dz = 0;

        if (x < 0) dx = -x;
        else if (x >= size[0]) dx = x - size[0] + 1;

        if (z < 0) dz = -z;
        else if (z >= size[2]) dz = z - size[2] + 1;

        return Math.sqrt(dx * dx + dz * dz);
    }

    private void addNaturalMicroFeatures(Location location, int[] size, World world) {
        for (int attempt = 0; attempt < size[0] * size[2] / 10; attempt++) {
            int x = ThreadLocalRandom.current().nextInt(-2, size[0] + 2);
            int z = ThreadLocalRandom.current().nextInt(-2, size[2] + 2);

            if (x >= 0 && x < size[0] && z >= 0 && z < size[2]) continue;

            Location featureLoc = location.clone().add(x, 0, z);
            Location surface = findSurface(world, featureLoc.getBlockX(), featureLoc.getBlockZ());

            if (surface != null) {
                Block surfaceBlock = world.getBlockAt(surface);

                if (ThreadLocalRandom.current().nextFloat() < 0.2f) {
                    addMicroFeature(world, surface, surfaceBlock.getType());
                }
            }
        }
    }

    private void addMicroFeature(World world, Location surface, Material surfaceType) {
        float rand = ThreadLocalRandom.current().nextFloat();

        if (rand < 0.1f) {
            Block above = world.getBlockAt(surface.getBlockX(), surface.getBlockY() + 1, surface.getBlockZ());
            if (above.getType() == Material.AIR) {
                above.setType(Material.COBBLESTONE);
            }
        }
        else if (rand < 0.4f && surfaceType == Material.GRASS_BLOCK) {
            Block above = world.getBlockAt(surface.getBlockX(), surface.getBlockY() + 1, surface.getBlockZ());
            if (above.getType() == Material.AIR) {
                if (ThreadLocalRandom.current().nextBoolean()) {
                    above.setType(Material.TALL_GRASS);
                } else {
                    above.setType(Material.FERN);
                }
            }
        }
        else if (rand < 0.6f && surfaceType == Material.GRASS_BLOCK) {
            Block above = world.getBlockAt(surface.getBlockX(), surface.getBlockY() + 1, surface.getBlockZ());
            if (above.getType() == Material.AIR) {
                Material[] flowers = {Material.DANDELION, Material.POPPY, Material.BLUE_ORCHID, Material.ALLIUM};
                above.setType(flowers[ThreadLocalRandom.current().nextInt(flowers.length)]);
            }
        }
        else if (rand < 0.8f && surfaceType == Material.GRASS_BLOCK) {
            Block surf = world.getBlockAt(surface);
            if (ThreadLocalRandom.current().nextBoolean()) {
                surf.setType(Material.COARSE_DIRT);
            } else {
                surf.setType(Material.GRAVEL);
            }
        }
    }

    private boolean isTreeBlock(Material material) {
        return LOG_MATERIALS.contains(material) ||
                LEAF_MATERIALS.contains(material) ||
                SAPLING_MATERIALS.contains(material);
    }

    private boolean isAtGroundLevel(Location location, World world) {
        Block below = world.getBlockAt(location.clone().add(0, -1, 0));
        return NATURAL_GROUND.contains(below.getType()) ||
                below.getType() == Material.STONE ||
                below.getType() == Material.DEEPSLATE;
    }

    private Location findSurface(World world, int x, int z) {
        int startY = Math.min(world.getMaxHeight() - 1, 320);
        int endY = Math.max(world.getMinHeight(), -64);

        for (int y = startY; y >= endY; y--) {
            Block block = world.getBlockAt(x, y, z);

            if (block.getType() == Material.AIR || LEAF_MATERIALS.contains(block.getType())) {
                continue;
            }

            Block above = world.getBlockAt(x, y + 1, z);
            if (above.getType() == Material.AIR || LEAF_MATERIALS.contains(above.getType())) {
                return new Location(world, x, y + 1, z);
            }
        }

        for (int y = startY; y >= endY; y--) {
            Block block = world.getBlockAt(x, y, z);
            if (block.getType().isSolid() && block.getType() != Material.BEDROCK) {
                return new Location(world, x, y + 1, z);
            }
        }

        return new Location(world, x, 64, z);
    }

    private String getDimensionName(World world) {
        switch (world.getEnvironment()) {
            case NORMAL: return "overworld";
            case NETHER: return "nether";
            case THE_END: return "end";
            default: return world.getName();
        }
    }

    private static class TerrainCandidate {
        final Location location;
        float score;
        int heightDifference;
        int treeBlocks;
        int waterBlocks;
        int lavaBlocks;

        TerrainCandidate(Location location) {
            this.location = location;
            this.score = 0;
        }
    }
}