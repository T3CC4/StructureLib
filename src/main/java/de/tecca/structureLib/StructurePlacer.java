package de.tecca.structureLib;

import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.*;
import org.bukkit.block.data.type.Gate;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

class StructurePlacer {
    private static final int BLOCKS_PER_TICK = 100;
    private static final Set<Material> PHYSICS_SENSITIVE = Set.of(
            Material.SAND, Material.GRAVEL, Material.ANVIL, Material.SCAFFOLDING,
            Material.RED_SAND, Material.POINTED_DRIPSTONE, Material.POWDER_SNOW
    );

    private final StructureLib plugin;
    private final List<ContainerInfo> pendingContainers = new ArrayList<>();

    private static class ContainerInfo {
        final Location location;
        final String type;

        ContainerInfo(Location location, String type) {
            this.location = location;
            this.type = type;
        }
    }

    public StructurePlacer(StructureLib plugin) {
        this.plugin = plugin;
    }

    public void place(Structure structure, Location location) {
        place(structure, location, 0, false, null);
    }

    public void place(Structure structure, Location location, int rotation, boolean randomRotation) {
        place(structure, location, rotation, randomRotation, null);
    }

    public void place(Structure structure, Location location, int rotation, boolean randomRotation, LootProcessor lootProcessor) {
        if (randomRotation) {
            rotation = new Random().nextInt(4) * 90;
        }

        World world = location.getWorld();
        pendingContainers.clear();

        assert world != null;
        boolean originalPhysics = world.isAutoSave();
        if (getTotalBlocks(structure) > 1000) {
            world.setAutoSave(false);
        }

        try {
            placePhase1(structure, location, world, rotation);
            placePhase2(structure, location, world, rotation);
            placePhase3(structure, location, world, rotation);

            placeBlockEntities(structure, location, world, rotation, lootProcessor);
            placeEntities(structure, location, world, rotation);

            postProcessConnectingBlocks(structure, location, world, rotation);

            if (lootProcessor != null) {
                processLootContainers(lootProcessor);
            }

        } finally {
            if (getTotalBlocks(structure) > 1000) {
                world.setAutoSave(originalPhysics);
            }
            pendingContainers.clear();
        }
    }

    private void placePhase1(Structure structure, Location origin, World world, int rotation) {
        for (BlockRegion region : structure.getRegions()) {
            if (!requiresSpecialHandling(region)) {
                placeBlockRegion(region, origin, world, rotation);
            }
        }
    }

    private void placePhase2(Structure structure, Location origin, World world, int rotation) {
        for (BlockRegion region : structure.getRegions()) {
            if (requiresSpecialHandling(region) && !isComplexBlock(region)) {
                placeBlockRegion(region, origin, world, rotation);
            }
        }
    }

    private void placePhase3(Structure structure, Location origin, World world, int rotation) {
        for (BlockRegion region : structure.getRegions()) {
            if (isComplexBlock(region)) {
                placeBlockRegion(region, origin, world, rotation);
            }
        }
    }

    private boolean requiresSpecialHandling(BlockRegion region) {
        if (region.getType().equals("individual") && region.getBlocks() != null) {
            return region.getBlocks().stream()
                    .anyMatch(block -> PHYSICS_SENSITIVE.contains(Material.valueOf(block.getMaterial())));
        }

        if (region.getMaterial() != null) {
            return PHYSICS_SENSITIVE.contains(Material.valueOf(region.getMaterial()));
        }

        return false;
    }

    private boolean isComplexBlock(BlockRegion region) {
        if (region.getType().equals("individual") && region.getBlocks() != null) {
            return region.getBlocks().stream()
                    .anyMatch(block -> isDoorMaterial(Material.valueOf(block.getMaterial())) ||
                            isMultiBlock(Material.valueOf(block.getMaterial())));
        }

        if (region.getMaterial() != null) {
            Material material = Material.valueOf(region.getMaterial());
            return isDoorMaterial(material) || isMultiBlock(material);
        }

        return false;
    }

    private boolean isMultipleFacingMaterial(Material material) {

        String name = material.toString();

        if (name.contains("_PANE")) return true;

        if (name.contains("_FENCE") && !name.contains("_GATE")) return true;

        switch (material) {
            case IRON_BARS:           // MultipleFacing
            case FIRE:                // Fire interface
            case SOUL_FIRE:           // Fire interface
            case TRIPWIRE:            // Tripwire interface
            case GLOW_LICHEN:         // GlowLichen interface
            case SCULK_VEIN:          // SculkVein interface
            case VINE:                // MultipleFacing
            case CHORUS_PLANT:        // MultipleFacing
                return true;
            default:
                return false;
        }
    }


    private boolean isMultiBlock(Material material) {
        return material.toString().contains("BED") ||
                material.toString().contains("DOOR") ||
                material == Material.TALL_GRASS ||
                material == Material.LARGE_FERN ||
                material == Material.SUNFLOWER ||
                material == Material.LILAC ||
                material == Material.ROSE_BUSH ||
                material == Material.PEONY;
    }

    private int getTotalBlocks(Structure structure) {
        return structure.getRegions().stream()
                .mapToInt(this::getRegionBlockCount)
                .sum();
    }

    private int getRegionBlockCount(BlockRegion region) {
        switch (region.getType()) {
            case "fill":
            case "hollow":
            case "plane":
            case "line":
                if (region.getStart() != null && region.getEnd() != null) {
                    int[] start = region.getStart();
                    int[] end = region.getEnd();
                    return Math.abs((end[0] - start[0] + 1) * (end[1] - start[1] + 1) * (end[2] - start[2] + 1));
                }
                break;
            case "individual":
                return region.getBlocks() != null ? region.getBlocks().size() : 0;
        }
        return 0;
    }

    private void postProcessConnectingBlocks(Structure structure, Location origin, World world, int rotation) {
        plugin.getLogger().info("Post-processing connecting blocks...");

        Set<Location> connectingBlocks = new HashSet<>();

        for (BlockRegion region : structure.getRegions()) {
            collectMultipleFacingBlocks(region, origin, world, rotation, connectingBlocks);
        }

        plugin.getLogger().info("Found " + connectingBlocks.size() + " connecting blocks to update");

        for (Location loc : connectingBlocks) {
            updateConnections(loc);
        }

        plugin.getLogger().info("Updated all connecting blocks");
    }

    private void collectMultipleFacingBlocks(BlockRegion region, Location origin, World world, int rotation, Set<Location> connectingBlocks) {
        switch (region.getType()) {
            case "fill":
            case "hollow":
            case "plane":
            case "line":
                if (region.getMaterial() != null && isMultipleFacingMaterial(Material.valueOf(region.getMaterial()))) {
                    addRegionBlocks(region, origin, rotation, connectingBlocks);
                }
                break;
            case "individual":
                if (region.getBlocks() != null) {
                    for (BlockData blockData : region.getBlocks()) {
                        Material material = Material.valueOf(blockData.getMaterial());
                        if (isMultipleFacingMaterial(material)) {
                            int[] pos = blockData.getPos();
                            int[] rotatedPos = rotatePosition(pos[0], pos[1], pos[2], rotation);
                            Location blockLoc = origin.clone().add(rotatedPos[0], rotatedPos[1], rotatedPos[2]);
                            connectingBlocks.add(blockLoc);
                        }
                    }
                }
                break;
        }
    }

    private void addRegionBlocks(BlockRegion region, Location origin, int rotation, Set<Location> connectingBlocks) {
        int[] start = region.getStart();
        int[] end = region.getEnd();

        for (int x = start[0]; x <= end[0]; x++) {
            for (int y = start[1]; y <= end[1]; y++) {
                for (int z = start[2]; z <= end[2]; z++) {
                    if (region.getType().equals("hollow")) {
                        if (x == start[0] || x == end[0] ||
                                y == start[1] || y == end[1] ||
                                z == start[2] || z == end[2]) {

                            int[] rotatedPos = rotatePosition(x, y, z, rotation);
                            Location blockLoc = origin.clone().add(rotatedPos[0], rotatedPos[1], rotatedPos[2]);
                            connectingBlocks.add(blockLoc);
                        }
                    } else {
                        int[] rotatedPos = rotatePosition(x, y, z, rotation);
                        Location blockLoc = origin.clone().add(rotatedPos[0], rotatedPos[1], rotatedPos[2]);
                        connectingBlocks.add(blockLoc);
                    }
                }
            }
        }
    }

    private void updateConnections(Location location) {
        try {
            Block block = Objects.requireNonNull(location.getWorld()).getBlockAt(location);
            org.bukkit.block.data.BlockData data = block.getBlockData();

            if (!(data instanceof MultipleFacing multipleFacing)) return;

            Material blockMaterial = block.getType();

            for (BlockFace face : multipleFacing.getAllowedFaces()) {
                Block neighbor = block.getRelative(face);
                boolean shouldConnect = shouldConnect(blockMaterial, neighbor.getType(), face);
                multipleFacing.setFace(face, shouldConnect);
            }

            block.setBlockData(multipleFacing, true);

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to update connections at " + location + ": " + e.getMessage());
        }
    }

    private boolean shouldConnect(Material blockMaterial, Material neighborMaterial, BlockFace face) {
        if (blockMaterial.toString().contains("_PANE")) {
            return neighborMaterial.toString().contains("_PANE") ||
                    neighborMaterial.toString().contains("GLASS") ||
                    neighborMaterial == Material.IRON_BARS ||
                    neighborMaterial.isSolid();
        }

        if (blockMaterial == Material.IRON_BARS) {
            return neighborMaterial == Material.IRON_BARS ||
                    neighborMaterial.toString().contains("_PANE") ||
                    neighborMaterial.isSolid();
        }

        if (blockMaterial.toString().contains("_FENCE")) {
            return neighborMaterial.toString().contains("_FENCE") ||
                    neighborMaterial.toString().contains("_GATE") ||
                    neighborMaterial.isSolid();
        }

        if (blockMaterial == Material.VINE) {
            return neighborMaterial.isSolid();
        }

        if (blockMaterial == Material.FIRE || blockMaterial == Material.SOUL_FIRE) {
            return neighborMaterial.isSolid() || neighborMaterial.isFlammable();
        }

        return blockMaterial == neighborMaterial;
    }

    private void placeBlockRegion(BlockRegion region, Location origin, World world, int rotation) {
        switch (region.getType()) {
            case "fill":
                placeFillRegion(region, origin, world, rotation);
                break;
            case "hollow":
                placeHollowRegion(region, origin, world, rotation);
                break;
            case "plane":
                placePlaneRegion(region, origin, world, rotation);
                break;
            case "line":
                placeLineRegion(region, origin, world, rotation);
                break;
            case "individual":
                placeIndividualBlocks(region, origin, world, rotation);
                break;
        }
    }

    private void placeFillRegion(BlockRegion region, Location origin, World world, int rotation) {
        Material material = Material.valueOf(region.getMaterial());
        int[] start = region.getStart();
        int[] end = region.getEnd();

        int blockCount = 0;

        for (int x = start[0]; x <= end[0]; x++) {
            for (int y = start[1]; y <= end[1]; y++) {
                for (int z = start[2]; z <= end[2]; z++) {
                    int[] rotatedPos = rotatePosition(x, y, z, rotation);
                    Location blockLoc = origin.clone().add(rotatedPos[0], rotatedPos[1], rotatedPos[2]);

                    if (isValidLocation(blockLoc)) {
                        Block block = world.getBlockAt(blockLoc);
                        block.setType(material, false);

                        if (region.getProperties() != null) {
                            applyBlockProperties(block, region.getProperties(), rotation);
                        }

                        blockCount++;
                        if (blockCount % BLOCKS_PER_TICK == 0) {
                            try {
                                Thread.sleep(1);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    private void placeHollowRegion(BlockRegion region, Location origin, World world, int rotation) {
        Material material = Material.valueOf(region.getMaterial());
        int[] start = region.getStart();
        int[] end = region.getEnd();

        int blockCount = 0;

        for (int x = start[0]; x <= end[0]; x++) {
            for (int y = start[1]; y <= end[1]; y++) {
                for (int z = start[2]; z <= end[2]; z++) {
                    if (x == start[0] || x == end[0] ||
                            y == start[1] || y == end[1] ||
                            z == start[2] || z == end[2]) {

                        int[] rotatedPos = rotatePosition(x, y, z, rotation);
                        Location blockLoc = origin.clone().add(rotatedPos[0], rotatedPos[1], rotatedPos[2]);

                        if (isValidLocation(blockLoc)) {
                            Block block = world.getBlockAt(blockLoc);
                            block.setType(material, false);

                            if (region.getProperties() != null) {
                                applyBlockProperties(block, region.getProperties(), rotation);
                            }

                            blockCount++;
                            if (blockCount % BLOCKS_PER_TICK == 0) {
                                try {
                                    Thread.sleep(1);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void placePlaneRegion(BlockRegion region, Location origin, World world, int rotation) {
        Material material = Material.valueOf(region.getMaterial());
        int[] start = region.getStart();
        int[] end = region.getEnd();

        if (start[1] == end[1]) {
            for (int x = start[0]; x <= end[0]; x++) {
                for (int z = start[2]; z <= end[2]; z++) {
                    int[] rotatedPos = rotatePosition(x, start[1], z, rotation);
                    Location blockLoc = origin.clone().add(rotatedPos[0], rotatedPos[1], rotatedPos[2]);

                    if (isValidLocation(blockLoc)) {
                        Block block = world.getBlockAt(blockLoc);
                        block.setType(material, false);

                        if (region.getProperties() != null) {
                            applyBlockProperties(block, region.getProperties(), rotation);
                        }
                    }
                }
            }
        }
    }

    private void placeLineRegion(BlockRegion region, Location origin, World world, int rotation) {
        Material material = Material.valueOf(region.getMaterial());
        int[] start = region.getStart();
        int[] end = region.getEnd();

        if (start[0] != end[0]) {
            for (int x = start[0]; x <= end[0]; x++) {
                int[] rotatedPos = rotatePosition(x, start[1], start[2], rotation);
                Location blockLoc = origin.clone().add(rotatedPos[0], rotatedPos[1], rotatedPos[2]);

                if (isValidLocation(blockLoc)) {
                    Block block = world.getBlockAt(blockLoc);
                    block.setType(material, false);

                    if (region.getProperties() != null) {
                        applyBlockProperties(block, region.getProperties(), rotation);
                    }
                }
            }
        } else if (start[1] != end[1]) {
            for (int y = start[1]; y <= end[1]; y++) {
                int[] rotatedPos = rotatePosition(start[0], y, start[2], rotation);
                Location blockLoc = origin.clone().add(rotatedPos[0], rotatedPos[1], rotatedPos[2]);

                if (isValidLocation(blockLoc)) {
                    Block block = world.getBlockAt(blockLoc);
                    block.setType(material, false);

                    if (region.getProperties() != null) {
                        applyBlockProperties(block, region.getProperties(), rotation);
                    }
                }
            }
        } else if (start[2] != end[2]) {
            for (int z = start[2]; z <= end[2]; z++) {
                int[] rotatedPos = rotatePosition(start[0], start[1], z, rotation);
                Location blockLoc = origin.clone().add(rotatedPos[0], rotatedPos[1], rotatedPos[2]);

                if (isValidLocation(blockLoc)) {
                    Block block = world.getBlockAt(blockLoc);
                    block.setType(material, false);

                    if (region.getProperties() != null) {
                        applyBlockProperties(block, region.getProperties(), rotation);
                    }
                }
            }
        }
    }

    private void placeIndividualBlocks(BlockRegion region, Location origin, World world, int rotation) {
        if (region.getBlocks() == null) return;

        Map<String, List<BlockData>> doorPairs = new HashMap<>();
        List<BlockData> otherBlocks = new ArrayList<>();

        for (BlockData blockData : region.getBlocks()) {
            Material material = Material.valueOf(blockData.getMaterial());
            if (isDoorMaterial(material) && blockData.getProperties() != null) {
                String facing = (String) blockData.getProperties().get("facing");
                String half = (String) blockData.getProperties().get("half");

                if (facing != null && half != null) {
                    String doorKey = blockData.getPos()[0] + "," + blockData.getPos()[2] + "," + facing;
                    doorPairs.computeIfAbsent(doorKey, k -> new ArrayList<>()).add(blockData);
                } else {
                    otherBlocks.add(blockData);
                }
            } else {
                otherBlocks.add(blockData);
            }
        }

        for (List<BlockData> doorBlocks : doorPairs.values()) {
            if (doorBlocks.size() == 2) {
                placeDoorPair(doorBlocks, origin, world, rotation);
            } else {
                otherBlocks.addAll(doorBlocks);
            }
        }

        int blockCount = 0;
        for (BlockData blockData : otherBlocks) {
            int[] pos = blockData.getPos();
            int[] rotatedPos = rotatePosition(pos[0], pos[1], pos[2], rotation);
            Location blockLoc = origin.clone().add(rotatedPos[0], rotatedPos[1], rotatedPos[2]);

            if (!isValidLocation(blockLoc)) continue;

            Block block = world.getBlockAt(blockLoc);
            Material material = Material.valueOf(blockData.getMaterial());

            try {
                if (isLogMaterial(material)) {
                    block.setType(material, false);
                    placeLog(block, blockData.getProperties(), rotation);
                } else {
                    block.setType(material, false);
                    if (blockData.getProperties() != null) {
                        applyBlockProperties(block, blockData.getProperties(), rotation);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to place block " + material + " at " + blockLoc + ": " + e.getMessage());
            }

            blockCount++;
            if (blockCount % BLOCKS_PER_TICK == 0) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void placeDoorPair(List<BlockData> doorBlocks, Location origin, World world, int rotation) {
        doorBlocks.sort((a, b) -> Integer.compare(a.getPos()[1], b.getPos()[1]));

        BlockData lowerBlock = doorBlocks.get(0);
        BlockData upperBlock = doorBlocks.get(1);

        Material material = Material.valueOf(lowerBlock.getMaterial());

        int[] lowerPos = lowerBlock.getPos();
        int[] rotatedLowerPos = rotatePosition(lowerPos[0], lowerPos[1], lowerPos[2], rotation);
        Location lowerLoc = origin.clone().add(rotatedLowerPos[0], rotatedLowerPos[1], rotatedLowerPos[2]);

        int[] upperPos = upperBlock.getPos();
        int[] rotatedUpperPos = rotatePosition(upperPos[0], upperPos[1], upperPos[2], rotation);
        Location upperLoc = origin.clone().add(rotatedUpperPos[0], rotatedUpperPos[1], rotatedUpperPos[2]);

        if (isValidLocation(lowerLoc) && isValidLocation(upperLoc)) {
            Block lowerBlockObj = world.getBlockAt(lowerLoc);
            Block upperBlockObj = world.getBlockAt(upperLoc);

            lowerBlockObj.setType(material, false);
            upperBlockObj.setType(material, false);

            if (lowerBlock.getProperties() != null) {
                applyBlockProperties(lowerBlockObj, lowerBlock.getProperties(), rotation);
            }
            if (upperBlock.getProperties() != null) {
                applyBlockProperties(upperBlockObj, upperBlock.getProperties(), rotation);
            }
        }
    }

    private void placeBlockEntities(Structure structure, Location origin, World world, int rotation, LootProcessor lootProcessor) {
        plugin.getLogger().info("Placing " + structure.getBlockEntities().size() + " block entities");

        for (BlockEntity blockEntity : structure.getBlockEntities()) {
            try {
                int[] pos = blockEntity.getPos();
                int[] rotatedPos = rotatePosition(pos[0], pos[1], pos[2], rotation);
                Location blockLoc = origin.clone().add(rotatedPos[0], rotatedPos[1], rotatedPos[2]);

                if (!isValidLocation(blockLoc)) continue;

                Block block = world.getBlockAt(blockLoc);
                String blockType = block.getType().toString().toLowerCase();

                if (lootProcessor != null &&
                        isContainerBlock(block) &&
                        lootProcessor.hasLootFor(getContainerType(blockType))) {

                    pendingContainers.add(new ContainerInfo(blockLoc.clone(), getContainerType(blockType)));
                    continue;
                }

                if (blockEntity.getData().containsKey("inventory")) {
                    Map<String, Map<String, Object>> inventoryData =
                            (Map<String, Map<String, Object>>) blockEntity.getData().get("inventory");

                    fillContainer(blockLoc, inventoryData);
                }

                if (blockEntity.getData().containsKey("burnTime")) {
                    BlockState state = block.getState();
                    if (state instanceof Furnace furnace) {
                        furnace.setBurnTime(((Number) blockEntity.getData().get("burnTime")).shortValue());
                        furnace.setCookTime(((Number) blockEntity.getData().get("cookTime")).shortValue());
                        furnace.update();
                    }
                }

            } catch (Exception e) {
                plugin.getLogger().severe("Failed to place block entity: " + e.getMessage());
            }
        }
    }

    private void processLootContainers(LootProcessor lootProcessor) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (ContainerInfo containerInfo : pendingContainers) {
                Block block = containerInfo.location.getBlock();
                lootProcessor.processContainer(block, containerInfo.type);
            }
            plugin.getLogger().info("Processed " + pendingContainers.size() + " loot containers");
        }, 3L);
    }

    private boolean isContainerBlock(Block block) {
        BlockState state = block.getState();
        return state instanceof Chest ||
                state instanceof Barrel ||
                state instanceof ShulkerBox ||
                state instanceof Dispenser ||
                state instanceof Dropper ||
                state instanceof Hopper ||
                state instanceof Furnace ||
                state instanceof BlastFurnace ||
                state instanceof Smoker;
    }

    private String getContainerType(String blockType) {
        if (blockType.contains("chest")) return "chest";
        if (blockType.contains("barrel")) return "barrel";
        if (blockType.contains("shulker")) return "shulker_box";
        if (blockType.contains("dispenser")) return "dispenser";
        if (blockType.contains("dropper")) return "dropper";
        if (blockType.contains("hopper")) return "hopper";
        if (blockType.contains("furnace")) return "furnace";
        if (blockType.contains("blast_furnace")) return "blast_furnace";
        if (blockType.contains("smoker")) return "smoker";
        return "container";
    }

    private void fillContainer(Location location, Map<String, Map<String, Object>> inventoryData) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> fillContainer(location, inventoryData));
            return;
        }

        try {
            World world = location.getWorld();
            assert world != null;
            Block block = world.getBlockAt(location);

            BlockState state = block.getState();

            plugin.getLogger().info("Filling container at " + location + " with " + inventoryData.size() + " items");

            Map<Integer, ItemStack> slotItems = new HashMap<>();
            for (Map.Entry<String, Map<String, Object>> entry : inventoryData.entrySet()) {
                try {
                    int slot = Integer.parseInt(entry.getKey());
                    Map<String, Object> itemData = entry.getValue();

                    String materialName = (String) itemData.get("material");
                    int amount = ((Number) itemData.get("amount")).intValue();

                    Material material = Material.valueOf(materialName);
                    ItemStack item = new ItemStack(material, amount);

                    slotItems.put(slot, item);
                    plugin.getLogger().info("Prepared item: " + material + " x" + amount + " for slot " + slot);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to parse item: " + e.getMessage());
                }
            }

            if (slotItems.isEmpty()) {
                plugin.getLogger().warning("No valid items to add");
                return;
            }

            boolean success = false;

            if (state instanceof Chest) {
                success = fillChestInventory((Chest) state, slotItems);
            } else if (state instanceof Barrel) {
                success = fillBarrelInventory((Barrel) state, slotItems);
            } else if (state instanceof Furnace) {
                success = fillFurnaceInventory((Furnace) state, slotItems);
            } else if (state instanceof BlastFurnace) {
                success = fillBlastFurnaceInventory((BlastFurnace) state, slotItems);
            } else if (state instanceof Smoker) {
                success = fillSmokerInventory((Smoker) state, slotItems);
            } else if (state instanceof Dispenser) {
                success = fillDispenserInventory((Dispenser) state, slotItems);
            } else if (state instanceof Dropper) {
                success = fillDropperInventory((Dropper) state, slotItems);
            } else if (state instanceof Hopper) {
                success = fillHopperInventory((Hopper) state, slotItems);
            } else if (state instanceof ShulkerBox) {
                success = fillShulkerInventory((ShulkerBox) state, slotItems);
            }

            if (success) {
                plugin.getLogger().info("Successfully filled container!");
            } else {
                plugin.getLogger().warning("Failed to fill container");
            }

        } catch (Exception e) {
            plugin.getLogger().severe("Error filling container: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean fillChestInventory(Chest chest, Map<Integer, ItemStack> slotItems) {
        try {
            chest.update(true);

            Inventory inv = chest.getInventory();
            inv.clear();

            for (Map.Entry<Integer, ItemStack> entry : slotItems.entrySet()) {
                int slot = entry.getKey();
                ItemStack item = entry.getValue();

                if (slot >= 0 && slot < inv.getSize()) {
                    inv.setItem(slot, item);
                    plugin.getLogger().info("Set chest slot " + slot + " to " + item.getType() + " x" + item.getAmount());
                }
            }

            int count = 0;
            for (ItemStack item : inv.getContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    count++;
                    plugin.getLogger().info("Found item in chest: " + item.getType() + " x" + item.getAmount());
                }
            }

            plugin.getLogger().info("Chest verification: " + count + " items found");
            return count > 0;

        } catch (Exception e) {
            plugin.getLogger().warning("Chest fill failed: " + e.getMessage());
            return false;
        }
    }

    private boolean fillFurnaceInventory(Furnace furnace, Map<Integer, ItemStack> slotItems) {
        try {
            furnace.update(true);

            Inventory inv = furnace.getInventory();
            inv.clear();

            for (Map.Entry<Integer, ItemStack> entry : slotItems.entrySet()) {
                int slot = entry.getKey();
                ItemStack item = entry.getValue();

                if (slot >= 0 && slot < 3) { // Furnace only has 3 slots
                    inv.setItem(slot, item);
                    plugin.getLogger().info("Set furnace slot " + slot + " to " + item.getType() + " x" + item.getAmount());
                }
            }

            int count = 0;
            for (int i = 0; i < 3; i++) {
                ItemStack item = inv.getItem(i);
                if (item != null && item.getType() != Material.AIR) {
                    count++;
                    plugin.getLogger().info("Found item in furnace slot " + i + ": " + item.getType() + " x" + item.getAmount());
                }
            }

            plugin.getLogger().info("Furnace verification: " + count + " items found");
            return count > 0;

        } catch (Exception e) {
            plugin.getLogger().warning("Furnace fill failed: " + e.getMessage());
            return false;
        }
    }

    private boolean fillBarrelInventory(Barrel barrel, Map<Integer, ItemStack> slotItems) {
        try {
            barrel.update(true);

            Inventory inv = barrel.getInventory();
            inv.clear();

            for (Map.Entry<Integer, ItemStack> entry : slotItems.entrySet()) {
                int slot = entry.getKey();
                ItemStack item = entry.getValue();

                if (slot >= 0 && slot < inv.getSize()) {
                    inv.setItem(slot, item);
                }
            }

            int count = 0;
            for (ItemStack item : inv.getContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    count++;
                }
            }

            return count > 0;

        } catch (Exception e) {
            plugin.getLogger().warning("Barrel fill failed: " + e.getMessage());
            return false;
        }
    }

    private boolean fillBlastFurnaceInventory(BlastFurnace blastFurnace, Map<Integer, ItemStack> slotItems) {
        try {
            blastFurnace.update(true);

            Inventory inv = blastFurnace.getInventory();
            inv.clear();

            for (Map.Entry<Integer, ItemStack> entry : slotItems.entrySet()) {
                int slot = entry.getKey();
                ItemStack item = entry.getValue();

                if (slot >= 0 && slot < 3) {
                    inv.setItem(slot, item);
                }
            }

            int count = 0;
            for (ItemStack item : inv.getContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    count++;
                }
            }

            return count > 0;

        } catch (Exception e) {
            plugin.getLogger().warning("BlastFurnace fill failed: " + e.getMessage());
            return false;
        }
    }

    private boolean fillSmokerInventory(Smoker smoker, Map<Integer, ItemStack> slotItems) {
        try {
            smoker.update(true);

            Inventory inv = smoker.getInventory();
            inv.clear();

            for (Map.Entry<Integer, ItemStack> entry : slotItems.entrySet()) {
                int slot = entry.getKey();
                ItemStack item = entry.getValue();

                if (slot >= 0 && slot < 3) {
                    inv.setItem(slot, item);
                }
            }

            int count = 0;
            for (ItemStack item : inv.getContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    count++;
                }
            }

            return count > 0;

        } catch (Exception e) {
            plugin.getLogger().warning("Smoker fill failed: " + e.getMessage());
            return false;
        }
    }

    private boolean fillDispenserInventory(Dispenser dispenser, Map<Integer, ItemStack> slotItems) {
        try {
            dispenser.update(true);

            Inventory inv = dispenser.getInventory();
            inv.clear();

            for (Map.Entry<Integer, ItemStack> entry : slotItems.entrySet()) {
                int slot = entry.getKey();
                ItemStack item = entry.getValue();

                if (slot >= 0 && slot < inv.getSize()) {
                    inv.setItem(slot, item);
                }
            }

            int count = 0;
            for (ItemStack item : inv.getContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    count++;
                }
            }

            return count > 0;

        } catch (Exception e) {
            plugin.getLogger().warning("Dispenser fill failed: " + e.getMessage());
            return false;
        }
    }

    private boolean fillDropperInventory(Dropper dropper, Map<Integer, ItemStack> slotItems) {
        try {
            dropper.update(true);

            Inventory inv = dropper.getInventory();
            inv.clear();

            for (Map.Entry<Integer, ItemStack> entry : slotItems.entrySet()) {
                int slot = entry.getKey();
                ItemStack item = entry.getValue();

                if (slot >= 0 && slot < inv.getSize()) {
                    inv.setItem(slot, item);
                }
            }

            int count = 0;
            for (ItemStack item : inv.getContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    count++;
                }
            }

            return count > 0;

        } catch (Exception e) {
            plugin.getLogger().warning("Dropper fill failed: " + e.getMessage());
            return false;
        }
    }

    private boolean fillHopperInventory(Hopper hopper, Map<Integer, ItemStack> slotItems) {
        try {
            hopper.update(true);

            Inventory inv = hopper.getInventory();
            inv.clear();

            for (Map.Entry<Integer, ItemStack> entry : slotItems.entrySet()) {
                int slot = entry.getKey();
                ItemStack item = entry.getValue();

                if (slot >= 0 && slot < inv.getSize()) {
                    inv.setItem(slot, item);
                }
            }

            int count = 0;
            for (ItemStack item : inv.getContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    count++;
                }
            }

            return count > 0;

        } catch (Exception e) {
            plugin.getLogger().warning("Hopper fill failed: " + e.getMessage());
            return false;
        }
    }

    private boolean fillShulkerInventory(ShulkerBox shulkerBox, Map<Integer, ItemStack> slotItems) {
        try {
            shulkerBox.update(true);

            Inventory inv = shulkerBox.getInventory();
            inv.clear();

            for (Map.Entry<Integer, ItemStack> entry : slotItems.entrySet()) {
                int slot = entry.getKey();
                ItemStack item = entry.getValue();

                if (slot >= 0 && slot < inv.getSize()) {
                    inv.setItem(slot, item);
                }
            }

            int count = 0;
            for (ItemStack item : inv.getContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    count++;
                }
            }

            return count > 0;

        } catch (Exception e) {
            plugin.getLogger().warning("ShulkerBox fill failed: " + e.getMessage());
            return false;
        }
    }

    private void placeEntities(Structure structure, Location origin, World world, int rotation) {
        for (EntityData entityData : structure.getEntities()) {
            try {
                double[] pos = entityData.getPos();
                double[] rotatedPos = rotatePosition(pos, rotation);
                Location spawnLoc = origin.clone().add(rotatedPos[0], rotatedPos[1], rotatedPos[2]);

                if (!isValidLocation(spawnLoc)) continue;

                EntityType type = EntityType.valueOf(entityData.getType());
                Entity entity = world.spawnEntity(spawnLoc, type);

                Map<String, Object> data = entityData.getData();

                if (data.containsKey("yaw") || data.containsKey("pitch")) {
                    float yaw = data.containsKey("yaw") ? ((Number) data.get("yaw")).floatValue() : 0;
                    float pitch = data.containsKey("pitch") ? ((Number) data.get("pitch")).floatValue() : 0;

                    yaw = rotateYaw(yaw, rotation);

                    Location newLoc = entity.getLocation();
                    newLoc.setYaw(yaw);
                    newLoc.setPitch(pitch);
                    entity.teleport(newLoc);
                }

                if (data.containsKey("health") && entity instanceof LivingEntity) {
                    double health = ((Number) data.get("health")).doubleValue();
                    ((LivingEntity) entity).setHealth(health);
                }

                if (entity instanceof ItemFrame frame && data.containsKey("item")) {
                    Map<String, Object> itemData = (Map<String, Object>) data.get("item");

                    Material material = Material.valueOf((String) itemData.get("material"));
                    int amount = ((Number) itemData.get("amount")).intValue();
                    ItemStack item = new ItemStack(material, amount);

                    frame.setItem(item);

                    if (data.containsKey("rotation")) {
                        int rotationValue = ((Number) data.get("rotation")).intValue();
                        frame.setRotation(org.bukkit.Rotation.values()[rotationValue]);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to spawn entity: " + e.getMessage());
            }
        }
    }

    private boolean isValidLocation(Location location) {
        if (location.getWorld() == null) return false;

        int y = location.getBlockY();
        return y >= location.getWorld().getMinHeight() && y <= location.getWorld().getMaxHeight();
    }

    private void placeLog(Block block, Map<String, Object> properties, int rotation) {
        if (properties == null) return;

        org.bukkit.block.data.BlockData data = block.getBlockData();

        if (data instanceof Orientable orientable) {
            String axis = (String) properties.get("axis");

            if (axis != null) {
                try {
                    Axis originalAxis = Axis.valueOf(axis);
                    Axis rotatedAxis = rotateAxis(originalAxis, rotation);
                    orientable.setAxis(rotatedAxis);
                    block.setBlockData(orientable, false);
                } catch (IllegalArgumentException e) {
                    // Invalid axis, skip
                }
            }
        }
    }

    private int[] rotatePosition(int x, int y, int z, int rotation) {
        switch (rotation) {
            case 90:
                return new int[]{-z, y, x};
            case 180:
                return new int[]{-x, y, -z};
            case 270:
                return new int[]{z, y, -x};
            default:
                return new int[]{x, y, z};
        }
    }

    private void applyBlockProperties(Block block, Map<String, Object> properties, int rotation) {
        org.bukkit.block.data.BlockData data = block.getBlockData();

        try {
            if (properties.containsKey("facing") && data instanceof Directional) {
                org.bukkit.block.BlockFace facing = org.bukkit.block.BlockFace.valueOf((String) properties.get("facing"));
                org.bukkit.block.BlockFace rotatedFacing = rotateFacing(facing, rotation);
                ((Directional) data).setFacing(rotatedFacing);
            }

            if (properties.containsKey("half") && data instanceof Bisected) {
                Bisected.Half half = Bisected.Half.valueOf((String) properties.get("half"));
                ((Bisected) data).setHalf(half);
            }

            if (properties.containsKey("open") && data instanceof Openable) {
                ((Openable) data).setOpen((Boolean) properties.get("open"));
            }

            if (properties.containsKey("powered") && data instanceof Powerable) {
                ((Powerable) data).setPowered((Boolean) properties.get("powered"));
            }

            if (properties.containsKey("waterlogged") && data instanceof Waterlogged) {
                ((Waterlogged) data).setWaterlogged((Boolean) properties.get("waterlogged"));
            }

            if (properties.containsKey("axis") && data instanceof Orientable) {
                String axis = (String) properties.get("axis");
                if (axis != null) {
                    Axis originalAxis = Axis.valueOf(axis);
                    Axis rotatedAxis = rotateAxis(originalAxis, rotation);
                    ((Orientable) data).setAxis(rotatedAxis);
                }
            }

            if (properties.containsKey("faces") && data instanceof MultipleFacing multipleFacing) {
                Map<String, Boolean> savedFaces = (Map<String, Boolean>) properties.get("faces");
                Set<String> allowedFaceNames = (Set<String>) properties.get("allowedFaces");

                for (BlockFace face : multipleFacing.getAllowedFaces()) {
                    multipleFacing.setFace(face, false);
                }

                for (Map.Entry<String, Boolean> entry : savedFaces.entrySet()) {
                    try {
                        BlockFace originalFace = BlockFace.valueOf(entry.getKey());
                        BlockFace rotatedFace = rotateFaceForConnection(originalFace, rotation);
                        Boolean connected = entry.getValue();

                        if (multipleFacing.getAllowedFaces().contains(rotatedFace)) {
                            multipleFacing.setFace(rotatedFace, connected);
                        }
                    } catch (IllegalArgumentException e) {
                        // Ignore
                    }
                }
            }

            if (properties.containsKey("in_wall") && data instanceof Gate) {
                ((Gate) data).setInWall((Boolean) properties.get("in_wall"));
            }

            block.setBlockData(data, false);

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to apply properties to " + block.getType() + ": " + e.getMessage());
        }
    }

    private BlockFace rotateFaceForConnection(BlockFace face, int rotation) {
        switch (face) {
            case NORTH:
            case SOUTH:
            case EAST:
            case WEST:
                return rotateFacing(face, rotation);
            case UP:
            case DOWN:
            default:
                return face;
        }
    }

    private org.bukkit.block.BlockFace rotateFacing(org.bukkit.block.BlockFace facing, int rotation) {
        switch (rotation) {
            case 90:
                switch (facing) {
                    case NORTH:
                        return org.bukkit.block.BlockFace.EAST;
                    case EAST:
                        return org.bukkit.block.BlockFace.SOUTH;
                    case SOUTH:
                        return org.bukkit.block.BlockFace.WEST;
                    case WEST:
                        return org.bukkit.block.BlockFace.NORTH;
                    default:
                        return facing;
                }
            case 180:
                switch (facing) {
                    case NORTH:
                        return org.bukkit.block.BlockFace.SOUTH;
                    case EAST:
                        return org.bukkit.block.BlockFace.WEST;
                    case SOUTH:
                        return org.bukkit.block.BlockFace.NORTH;
                    case WEST:
                        return org.bukkit.block.BlockFace.EAST;
                    default:
                        return facing;
                }
            case 270:
                switch (facing) {
                    case NORTH:
                        return org.bukkit.block.BlockFace.WEST;
                    case EAST:
                        return org.bukkit.block.BlockFace.NORTH;
                    case SOUTH:
                        return org.bukkit.block.BlockFace.EAST;
                    case WEST:
                        return org.bukkit.block.BlockFace.SOUTH;
                    default:
                        return facing;
                }
            default:
                return facing;
        }
    }

    private Axis rotateAxis(Axis axis, int rotation) {
        if (rotation == 0 || rotation == 180) {
            return axis;
        }

        switch (axis) {
            case X:
                return Axis.Z;
            case Z:
                return Axis.X;
            default:
                return axis;
        }
    }

    private boolean isDoorMaterial(Material material) {
        return material.toString().contains("DOOR") && !material.toString().contains("TRAPDOOR");
    }

    private boolean isLogMaterial(Material material) {
        return material.toString().contains("LOG") ||
                material.toString().contains("WOOD") ||
                material == Material.HAY_BLOCK ||
                material == Material.BONE_BLOCK ||
                material.toString().contains("PILLAR");
    }

    private double[] rotatePosition(double[] pos, int rotation) {
        double x = pos[0];
        double y = pos[1];
        double z = pos[2];

        switch (rotation) {
            case 90:
                return new double[]{-z, y, x};
            case 180:
                return new double[]{-x, y, -z};
            case 270:
                return new double[]{z, y, -x};
            default:
                return new double[]{x, y, z};
        }
    }

    private float rotateYaw(float yaw, int rotation) {
        return (yaw + rotation) % 360;
    }

    public static void fillChest(Location location, ItemStack... items) {
        try {
            World world = location.getWorld();
            assert world != null;
            Block block = world.getBlockAt(location);
            BlockState state = block.getState();

            if (state instanceof Chest chest) {
                for (ItemStack item : items) {
                    if (item != null) {
                        chest.getInventory().addItem(item);
                    }
                }
                chest.update();
            } else if (state instanceof Barrel barrel) {
                for (ItemStack item : items) {
                    if (item != null) {
                        barrel.getInventory().addItem(item);
                    }
                }
                barrel.update();
            } else if (state instanceof ShulkerBox shulkerBox) {
                for (ItemStack item : items) {
                    if (item != null) {
                        shulkerBox.getInventory().addItem(item);
                    }
                }
                shulkerBox.update();
            } else if (state instanceof Dispenser dispenser) {
                for (ItemStack item : items) {
                    if (item != null) {
                        dispenser.getInventory().addItem(item);
                    }
                }
                dispenser.update();
            } else if (state instanceof Dropper dropper) {
                for (ItemStack item : items) {
                    if (item != null) {
                        dropper.getInventory().addItem(item);
                    }
                }
                dropper.update();
            } else if (state instanceof Hopper hopper) {
                for (ItemStack item : items) {
                    if (item != null) {
                        hopper.getInventory().addItem(item);
                    }
                }
                hopper.update();
            } else if (state instanceof Furnace furnace) {
                for (ItemStack item : items) {
                    if (item != null) {
                        furnace.getInventory().addItem(item);
                    }
                }
                furnace.update();
            } else if (state instanceof BlastFurnace blastFurnace) {
                for (ItemStack item : items) {
                    if (item != null) {
                        blastFurnace.getInventory().addItem(item);
                    }
                }
                blastFurnace.update();
            } else if (state instanceof Smoker smoker) {
                for (ItemStack item : items) {
                    if (item != null) {
                        smoker.getInventory().addItem(item);
                    }
                }
                smoker.update();
            }
        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Was not able to fill container at " + location + "!");
        }
    }
}