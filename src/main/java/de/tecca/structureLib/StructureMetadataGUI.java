package de.tecca.structureLib;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;

class StructureMetadataGUI implements Listener {
    private final StructureLib plugin;
    private final Map<String, StructureMetadata> tempMetadata;
    private final Map<String, Player> activeGUIPlayers;
    private final Map<String, String> pendingAnvilInputs;

    public StructureMetadataGUI(StructureLib plugin) {
        this.plugin = plugin;
        this.tempMetadata = new HashMap<>();
        this.activeGUIPlayers = new HashMap<>();
        this.pendingAnvilInputs = new HashMap<>();
    }

    public void openMainMetadataGUI(Player player, String structureId) {
        try {
            String tempKey = player.getName() + ":" + structureId;
            StructureMetadata metadata = tempMetadata.get(tempKey);

            if (metadata == null) {
                Structure structure = plugin.getStructureAPI().loadStructure(new File(plugin.getStructuresFolder(), structureId + ".json"));
                metadata = structure.getMetadata();

                if (metadata == null) {
                    metadata = new StructureMetadata();
                }

                tempMetadata.put(tempKey, metadata);
            }

            activeGUIPlayers.put(player.getName(), player);

            Inventory gui = Bukkit.createInventory(null, 54, "§6Structure Metadata: " + structureId);

            Structure structure = plugin.getStructureAPI().loadStructure(new File(plugin.getStructuresFolder(), structureId + ".json"));
            gui.setItem(4, createInfoItem(structure));

            gui.setItem(19, createDimensionItem(metadata));
            gui.setItem(20, createBiomeItem(metadata));
            gui.setItem(21, createHeightItem(metadata));
            gui.setItem(22, createSpawnChanceItem(metadata));
            gui.setItem(23, createDistanceItem(metadata));
            gui.setItem(24, createConditionsItem(metadata));
            gui.setItem(25, createCategoryItem(metadata));

            gui.setItem(28, createTagsItem(metadata));
            gui.setItem(29, createNaturalSpawningItem(metadata));

            gui.setItem(45, createSaveItem());
            gui.setItem(53, createCancelItem());

            player.openInventory(gui);

        } catch (Exception e) {
            player.sendMessage("§cFailed to load structure: " + e.getMessage());
        }
    }

    private ItemStack createInfoItem(Structure structure) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6Structure Information");

        List<String> lore = new ArrayList<>();
        lore.add("§7ID: §f" + structure.getId());
        lore.add("§7Author: §f" + structure.getAuthor());
        lore.add("§7Created: §f" + new Date(structure.getCreated()));
        lore.add("§7Size: §f" + structure.getSize()[0] + "x" + structure.getSize()[1] + "x" + structure.getSize()[2]);
        lore.add("§7Regions: §f" + structure.getRegions().size());
        lore.add("§7Block Entities: §f" + structure.getBlockEntities().size());
        lore.add("§7Entities: §f" + structure.getEntities().size());

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createDimensionItem(StructureMetadata metadata) {
        ItemStack item = new ItemStack(Material.ENDER_PEARL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§aDimensions");

        List<String> lore = new ArrayList<>();
        lore.add("§7Click to edit allowed dimensions");
        lore.add("");
        lore.add("§7Currently allowed:");
        for (String dim : metadata.getAllowedDimensions()) {
            lore.add("§a  ✓ " + dim);
        }
        lore.add("");
        lore.add("§eClick to open dimension selector");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBiomeItem(StructureMetadata metadata) {
        ItemStack item = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§aBiomes");

        List<String> lore = new ArrayList<>();
        lore.add("§7Click to edit biome restrictions");
        lore.add("");
        lore.add("§7Allowed biomes: §a" + metadata.getAllowedBiomes().size());
        lore.add("§7Forbidden biomes: §c" + metadata.getForbiddenBiomes().size());
        lore.add("");
        lore.add("§eClick to open biome selector");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createHeightItem(StructureMetadata metadata) {
        ItemStack item = new ItemStack(Material.LADDER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§aHeight Range");

        List<String> lore = new ArrayList<>();
        lore.add("§7Current: §e" + metadata.getSpawnHeightRange().getMin() + " to " + metadata.getSpawnHeightRange().getMax());
        lore.add("");
        lore.add("§eClick to adjust height range");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSpawnChanceItem(StructureMetadata metadata) {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§aSpawn Chance");

        List<String> lore = new ArrayList<>();
        lore.add("§7Current: §e" + String.format("%.2f", metadata.getSpawnChance() * 100) + "%");
        lore.add("");
        lore.add("§7Chance per valid chunk");
        lore.add("§eLeft-click: +0.1%");
        lore.add("§eRight-click: -0.1%");
        lore.add("§eShift+click: ±1%");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createDistanceItem(StructureMetadata metadata) {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§aDistance Settings");

        List<String> lore = new ArrayList<>();
        lore.add("§7Min distance from same: §e" + metadata.getMinDistanceFromSame());
        lore.add("§7Min distance from any: §e" + metadata.getMinDistanceFromAny());
        lore.add("");
        lore.add("§eClick to adjust distances");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createConditionsItem(StructureMetadata metadata) {
        ItemStack item = new ItemStack(Material.REDSTONE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§aSpawn Conditions");

        List<String> lore = new ArrayList<>();
        lore.add("§7Configure spawn requirements");
        lore.add("");
        SpawnConditions conditions = metadata.getSpawnConditions();
        lore.add("§7Flat Ground: " + (conditions.isRequiresFlatGround() ? "§aRequired" : "§cOptional"));
        lore.add("§7Avoid Water: " + (conditions.isAvoidWater() ? "§aYes" : "§cNo"));
        lore.add("§7Avoid Lava: " + (conditions.isAvoidLava() ? "§aYes" : "§cNo"));
        lore.add("§7Sky Access: " + (conditions.isRequiresSkyAccess() ? "§aRequired" : "§cOptional"));
        lore.add("");
        lore.add("§eClick to configure conditions");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCategoryItem(StructureMetadata metadata) {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§aCategory");

        List<String> lore = new ArrayList<>();
        lore.add("§7Current: §e" + metadata.getCategory());
        lore.add("");
        lore.add("§eClick to change category");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createTagsItem(StructureMetadata metadata) {
        ItemStack item = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§aTags");

        List<String> lore = new ArrayList<>();
        lore.add("§7Current tags:");
        for (String tag : metadata.getTags()) {
            lore.add("§a  • " + tag);
        }
        lore.add("");
        lore.add("§eClick to manage tags");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createNaturalSpawningItem(StructureMetadata metadata) {
        ItemStack item = new ItemStack(metadata.isNaturalSpawning() ? Material.GREEN_WOOL : Material.RED_WOOL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§aNatural Spawning");

        List<String> lore = new ArrayList<>();
        lore.add("§7Status: " + (metadata.isNaturalSpawning() ? "§aEnabled" : "§cDisabled"));
        lore.add("");
        lore.add("§eClick to toggle");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSaveItem() {
        ItemStack item = new ItemStack(Material.GREEN_CONCRETE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§aSave Changes");

        List<String> lore = new ArrayList<>();
        lore.add("§7Click to save metadata changes");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCancelItem() {
        ItemStack item = new ItemStack(Material.RED_CONCRETE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§cCancel");

        List<String> lore = new ArrayList<>();
        lore.add("§7Click to cancel without saving");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void openDimensionSelector(Player player, String structureId) {
        Inventory dimensionGUI = Bukkit.createInventory(null, 27, "§6Dimension Selector: " + structureId);

        String tempKey = player.getName() + ":" + structureId;
        StructureMetadata metadata = tempMetadata.get(tempKey);

        dimensionGUI.setItem(10, createDimensionToggleItem("overworld", metadata));
        dimensionGUI.setItem(12, createDimensionToggleItem("nether", metadata));
        dimensionGUI.setItem(14, createDimensionToggleItem("end", metadata));
        dimensionGUI.setItem(16, createDimensionToggleItem("custom", metadata));

        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName("§cBack to Metadata");
        backItem.setItemMeta(backMeta);
        dimensionGUI.setItem(26, backItem);

        player.openInventory(dimensionGUI);
    }

    private ItemStack createDimensionToggleItem(String dimension, StructureMetadata metadata) {
        boolean isAllowed = metadata.getAllowedDimensions().contains(dimension);

        Material material = Material.STONE;
        switch (dimension) {
            case "overworld": material = Material.GRASS_BLOCK; break;
            case "nether": material = Material.NETHERRACK; break;
            case "end": material = Material.END_STONE; break;
            case "custom": material = Material.BEDROCK; break;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String displayName = dimension.substring(0, 1).toUpperCase() + dimension.substring(1);
        meta.setDisplayName((isAllowed ? "§a✓ " : "§c✗ ") + displayName);

        List<String> lore = new ArrayList<>();
        lore.add("§7Status: " + (isAllowed ? "§aAllowed" : "§cNot Allowed"));
        lore.add("");
        lore.add("§eClick to toggle");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void openHeightSelector(Player player, String structureId) {
        Inventory heightGUI = Bukkit.createInventory(null, 27, "§6Height Range: " + structureId);

        String tempKey = player.getName() + ":" + structureId;
        StructureMetadata metadata = tempMetadata.get(tempKey);
        IntRange range = metadata.getSpawnHeightRange();

        // Min height controls
        heightGUI.setItem(9, createHeightControlItem("§cMin Height: " + range.getMin(), Material.RED_CONCRETE, "min", -10));
        heightGUI.setItem(10, createHeightControlItem("§c-1", Material.RED_TERRACOTTA, "min", -1));
        heightGUI.setItem(11, createHeightControlItem("§a+1", Material.GREEN_TERRACOTTA, "min", 1));
        heightGUI.setItem(12, createHeightControlItem("§aMin Height: +" + 10, Material.GREEN_CONCRETE, "min", 10));

        // Max height controls
        heightGUI.setItem(14, createHeightControlItem("§cMax Height: " + range.getMax(), Material.RED_CONCRETE, "max", -10));
        heightGUI.setItem(15, createHeightControlItem("§c-1", Material.RED_TERRACOTTA, "max", -1));
        heightGUI.setItem(16, createHeightControlItem("§a+1", Material.GREEN_TERRACOTTA, "max", 1));
        heightGUI.setItem(17, createHeightControlItem("§aMax Height: +" + 10, Material.GREEN_CONCRETE, "max", 10));

        // Preset buttons
        heightGUI.setItem(3, createHeightPresetItem("Underground", new IntRange(-64, 50)));
        heightGUI.setItem(4, createHeightPresetItem("Surface", new IntRange(50, 120)));
        heightGUI.setItem(5, createHeightPresetItem("Mountain", new IntRange(80, 320)));

        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName("§cBack to Metadata");
        backItem.setItemMeta(backMeta);
        heightGUI.setItem(26, backItem);

        player.openInventory(heightGUI);
    }

    private ItemStack createHeightControlItem(String name, Material material, String type, int change) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);

        List<String> lore = new ArrayList<>();
        lore.add("§7Adjust " + type + " height by " + change);
        lore.add("§eClick to apply");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createHeightPresetItem(String name, IntRange range) {
        ItemStack item = new ItemStack(Material.STONE_BUTTON);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6" + name + " Preset");

        List<String> lore = new ArrayList<>();
        lore.add("§7Height: " + range.getMin() + " to " + range.getMax());
        lore.add("§eClick to apply preset");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void openDistanceSelector(Player player, String structureId) {
        Inventory distanceGUI = Bukkit.createInventory(null, 27, "§6Distance Settings: " + structureId);

        String tempKey = player.getName() + ":" + structureId;
        StructureMetadata metadata = tempMetadata.get(tempKey);

        // Same structure distance controls
        distanceGUI.setItem(9, createDistanceControlItem("§cSame: " + metadata.getMinDistanceFromSame(), Material.RED_CONCRETE, "same", -50));
        distanceGUI.setItem(10, createDistanceControlItem("§c-10", Material.RED_TERRACOTTA, "same", -10));
        distanceGUI.setItem(11, createDistanceControlItem("§a+10", Material.GREEN_TERRACOTTA, "same", 10));
        distanceGUI.setItem(12, createDistanceControlItem("§aSame: +" + 50, Material.GREEN_CONCRETE, "same", 50));

        // Any structure distance controls
        distanceGUI.setItem(14, createDistanceControlItem("§cAny: " + metadata.getMinDistanceFromAny(), Material.RED_CONCRETE, "any", -25));
        distanceGUI.setItem(15, createDistanceControlItem("§c-5", Material.RED_TERRACOTTA, "any", -5));
        distanceGUI.setItem(16, createDistanceControlItem("§a+5", Material.GREEN_TERRACOTTA, "any", 5));
        distanceGUI.setItem(17, createDistanceControlItem("§aAny: +" + 25, Material.GREEN_CONCRETE, "any", 25));

        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName("§cBack to Metadata");
        backItem.setItemMeta(backMeta);
        distanceGUI.setItem(26, backItem);

        player.openInventory(distanceGUI);
    }

    private ItemStack createDistanceControlItem(String name, Material material, String type, int change) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);

        List<String> lore = new ArrayList<>();
        lore.add("§7Adjust distance from " + type + " by " + change);
        lore.add("§eClick to apply");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void openConditionsSelector(Player player, String structureId) {
        Inventory conditionsGUI = Bukkit.createInventory(null, 27, "§6Spawn Conditions: " + structureId);

        String tempKey = player.getName() + ":" + structureId;
        StructureMetadata metadata = tempMetadata.get(tempKey);
        SpawnConditions conditions = metadata.getSpawnConditions();

        conditionsGUI.setItem(10, createConditionToggleItem("Flat Ground", Material.SMOOTH_STONE, conditions.isRequiresFlatGround()));
        conditionsGUI.setItem(11, createConditionToggleItem("Avoid Water", Material.WATER_BUCKET, conditions.isAvoidWater()));
        conditionsGUI.setItem(12, createConditionToggleItem("Avoid Lava", Material.LAVA_BUCKET, conditions.isAvoidLava()));
        conditionsGUI.setItem(13, createConditionToggleItem("Sky Access", Material.GLASS, conditions.isRequiresSkyAccess()));

        conditionsGUI.setItem(15, createSlopeControlItem("Max Slope", conditions.getMaxSlope()));
        conditionsGUI.setItem(16, createClearHeightControlItem("Clear Height", conditions.getMinClearHeight()));

        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName("§cBack to Metadata");
        backItem.setItemMeta(backMeta);
        conditionsGUI.setItem(26, backItem);

        player.openInventory(conditionsGUI);
    }

    private ItemStack createConditionToggleItem(String name, Material material, boolean enabled) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName((enabled ? "§a✓ " : "§c✗ ") + name);

        List<String> lore = new ArrayList<>();
        lore.add("§7Status: " + (enabled ? "§aEnabled" : "§cDisabled"));
        lore.add("§eClick to toggle");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSlopeControlItem(String name, double slope) {
        ItemStack item = new ItemStack(Material.SANDSTONE_STAIRS);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6" + name + ": " + String.format("%.1f", slope));

        List<String> lore = new ArrayList<>();
        lore.add("§7Maximum terrain slope allowed");
        lore.add("§eLeft-click: +0.1");
        lore.add("§eRight-click: -0.1");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createClearHeightControlItem(String name, int height) {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6" + name + ": " + height);

        List<String> lore = new ArrayList<>();
        lore.add("§7Minimum clear space above ground");
        lore.add("§eLeft-click: +1");
        lore.add("§eRight-click: -1");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void openTagsManager(Player player, String structureId) {
        Inventory tagsGUI = Bukkit.createInventory(null, 54, "§6Tags Manager: " + structureId);

        String tempKey = player.getName() + ":" + structureId;
        StructureMetadata metadata = tempMetadata.get(tempKey);

        // Common tag presets
        String[] commonTags = {
                "village", "house", "building", "medieval", "modern", "fantasy",
                "castle", "tower", "dungeon", "farm", "temple", "ruins",
                "small", "medium", "large", "underground", "surface", "elevated"
        };

        int slot = 0;
        for (String tag : commonTags) {
            if (slot >= 45) break;
            tagsGUI.setItem(slot++, createTagToggleItem(tag, metadata.getTags().contains(tag)));
        }

        // Add custom tag button
        ItemStack addCustom = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta addMeta = addCustom.getItemMeta();
        addMeta.setDisplayName("§aAdd Custom Tag");
        List<String> addLore = new ArrayList<>();
        addLore.add("§7Click to add a custom tag");
        addMeta.setLore(addLore);
        addCustom.setItemMeta(addMeta);
        tagsGUI.setItem(49, addCustom);

        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName("§cBack to Metadata");
        backItem.setItemMeta(backMeta);
        tagsGUI.setItem(53, backItem);

        player.openInventory(tagsGUI);
    }

    private ItemStack createTagToggleItem(String tag, boolean enabled) {
        ItemStack item = new ItemStack(enabled ? Material.GREEN_DYE : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName((enabled ? "§a✓ " : "§7") + tag);

        List<String> lore = new ArrayList<>();
        lore.add("§7Status: " + (enabled ? "§aActive" : "§cInactive"));
        lore.add("§eClick to toggle");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void openCategoryAnvil(Player player, String structureId) {
        Inventory anvilGUI = Bukkit.createInventory(null, InventoryType.ANVIL, "§6Set Category");

        String tempKey = player.getName() + ":" + structureId;
        StructureMetadata metadata = tempMetadata.get(tempKey);

        ItemStack nameItem = new ItemStack(Material.NAME_TAG);
        ItemMeta nameMeta = nameItem.getItemMeta();
        nameMeta.setDisplayName(metadata.getCategory());
        nameItem.setItemMeta(nameMeta);

        anvilGUI.setItem(0, nameItem);

        pendingAnvilInputs.put(player.getName(), "category:" + structureId);
        player.openInventory(anvilGUI);
    }

    private void openBiomeSelector(Player player, String structureId) {
        Inventory biomeGUI = Bukkit.createInventory(null, 54, "§6Biome Selector: " + structureId);

        List<org.bukkit.block.Biome> biomes = BiomeHelper.getAllBiomes();
        int slot = 0;

        for (org.bukkit.block.Biome biome : biomes) {
            if (slot >= 45) break;

            ItemStack biomeItem = createBiomeSelectItem(biome, structureId, player);
            biomeGUI.setItem(slot++, biomeItem);
        }

        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName("§cBack to Metadata");
        backItem.setItemMeta(backMeta);
        biomeGUI.setItem(53, backItem);

        player.openInventory(biomeGUI);
    }

    private ItemStack createBiomeSelectItem(org.bukkit.block.Biome biome, String structureId, Player player) {
        String tempKey = player.getName() + ":" + structureId;
        StructureMetadata metadata = tempMetadata.get(tempKey);

        boolean isAllowed = metadata != null && metadata.getAllowedBiomes().contains(biome.toString());
        boolean isForbidden = metadata != null && metadata.getForbiddenBiomes().contains(biome.toString());

        Material material = Material.GRASS_BLOCK;
        if (biome.toString().contains("DESERT")) material = Material.SAND;
        else if (biome.toString().contains("OCEAN")) material = Material.WATER_BUCKET;
        else if (biome.toString().contains("SNOW")) material = Material.SNOW_BLOCK;
        else if (biome.toString().contains("NETHER")) material = Material.NETHERRACK;
        else if (biome.toString().contains("END")) material = Material.END_STONE;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String displayName = BiomeHelper.getBiomeDisplayName(biome);
        if (isAllowed) {
            meta.setDisplayName("§a" + displayName + " ✓");
        } else if (isForbidden) {
            meta.setDisplayName("§c" + displayName + " ✗");
        } else {
            meta.setDisplayName("§7" + displayName);
        }

        List<String> lore = new ArrayList<>();
        lore.add("§7Biome: §f" + biome.toString());
        lore.add("");
        lore.add("§eLeft-click: Add to allowed");
        lore.add("§eRight-click: Add to forbidden");
        lore.add("§eShift-click: Remove from lists");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();

        if (title.startsWith("§6Structure Metadata:")) {
            handleMainMetadataClick(event);
            return;
        }

        if (title.startsWith("§6Biome Selector:")) {
            handleBiomeSelectorClick(event);
            return;
        }

        if (title.startsWith("§6Dimension Selector:")) {
            handleDimensionSelectorClick(event);
            return;
        }

        if (title.startsWith("§6Height Range:")) {
            handleHeightSelectorClick(event);
            return;
        }

        if (title.startsWith("§6Distance Settings:")) {
            handleDistanceSelectorClick(event);
            return;
        }

        if (title.startsWith("§6Spawn Conditions:")) {
            handleConditionsSelectorClick(event);
            return;
        }

        if (title.startsWith("§6Tags Manager:")) {
            handleTagsManagerClick(event);
            return;
        }

        if (title.equals("§6Set Category")) {
            handleCategoryAnvilClick(event);
            return;
        }
    }

    private void handleMainMetadataClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType() == Material.AIR) return;

        String structureId = extractStructureId(event.getView().getTitle());
        String tempKey = player.getName() + ":" + structureId;
        StructureMetadata metadata = tempMetadata.get(tempKey);

        if (metadata == null) return;

        switch (event.getSlot()) {
            case 19: // Dimensions
                openDimensionSelector(player, structureId);
                break;
            case 20: // Biomes
                openBiomeSelector(player, structureId);
                break;
            case 21: // Height
                openHeightSelector(player, structureId);
                break;
            case 22: // Spawn Chance
                adjustSpawnChance(player, structureId, event.getClick(), metadata);
                break;
            case 23: // Distance
                openDistanceSelector(player, structureId);
                break;
            case 24: // Conditions
                openConditionsSelector(player, structureId);
                break;
            case 25: // Category
                openCategoryAnvil(player, structureId);
                break;
            case 28: // Tags
                openTagsManager(player, structureId);
                break;
            case 29: // Natural Spawning
                metadata.setNaturalSpawning(!metadata.isNaturalSpawning());
                tempMetadata.put(tempKey, metadata);
                openMainMetadataGUI(player, structureId);
                break;
            case 45: // Save
                saveMetadata(player, structureId, metadata);
                break;
            case 53: // Cancel
                tempMetadata.remove(tempKey);
                activeGUIPlayers.remove(player.getName());
                player.closeInventory();
                break;
        }
    }

    private void handleBiomeSelectorClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType() == Material.AIR) return;

        String structureId = extractStructureId(event.getView().getTitle());
        String tempKey = player.getName() + ":" + structureId;
        StructureMetadata metadata = tempMetadata.get(tempKey);

        if (metadata == null) return;

        if (event.getSlot() == 53) {
            openMainMetadataGUI(player, structureId);
            return;
        }

        List<org.bukkit.block.Biome> biomes = BiomeHelper.getAllBiomes();
        if (event.getSlot() >= biomes.size()) return;

        org.bukkit.block.Biome selectedBiome = biomes.get(event.getSlot());
        String biomeName = selectedBiome.toString();

        if (event.getClick().isShiftClick()) {
            metadata.getAllowedBiomes().remove(biomeName);
            metadata.getForbiddenBiomes().remove(biomeName);
        } else if (event.getClick().isLeftClick()) {
            metadata.getAllowedBiomes().add(biomeName);
            metadata.getForbiddenBiomes().remove(biomeName);
        } else if (event.getClick().isRightClick()) {
            metadata.getForbiddenBiomes().add(biomeName);
            metadata.getAllowedBiomes().remove(biomeName);
        }

        // Update temp metadata
        tempMetadata.put(tempKey, metadata);

        openBiomeSelector(player, structureId);
    }

    private void handleDimensionSelectorClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType() == Material.AIR) return;

        String structureId = extractStructureId(event.getView().getTitle());
        String tempKey = player.getName() + ":" + structureId;
        StructureMetadata metadata = tempMetadata.get(tempKey);

        if (metadata == null) return;

        if (event.getSlot() == 26) {
            openMainMetadataGUI(player, structureId);
            return;
        }

        String dimension = null;
        switch (event.getSlot()) {
            case 10: dimension = "overworld"; break;
            case 12: dimension = "nether"; break;
            case 14: dimension = "end"; break;
            case 16: dimension = "custom"; break;
        }

        if (dimension != null) {
            if (metadata.getAllowedDimensions().contains(dimension)) {
                metadata.getAllowedDimensions().remove(dimension);
            } else {
                metadata.getAllowedDimensions().add(dimension);
            }

            // Update temp metadata
            tempMetadata.put(tempKey, metadata);

            openDimensionSelector(player, structureId);
        }
    }

    private void handleHeightSelectorClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType() == Material.AIR) return;

        String structureId = extractStructureId(event.getView().getTitle());
        String tempKey = player.getName() + ":" + structureId;
        StructureMetadata metadata = tempMetadata.get(tempKey);

        if (metadata == null) return;

        if (event.getSlot() == 26) {
            openMainMetadataGUI(player, structureId);
            return;
        }

        IntRange currentRange = metadata.getSpawnHeightRange();

        // Preset buttons
        switch (event.getSlot()) {
            case 3: // Underground
                metadata.setSpawnHeightRange(new IntRange(-64, 50));
                tempMetadata.put(tempKey, metadata);
                openHeightSelector(player, structureId);
                return;
            case 4: // Surface
                metadata.setSpawnHeightRange(new IntRange(50, 120));
                tempMetadata.put(tempKey, metadata);
                openHeightSelector(player, structureId);
                return;
            case 5: // Mountain
                metadata.setSpawnHeightRange(new IntRange(80, 320));
                tempMetadata.put(tempKey, metadata);
                openHeightSelector(player, structureId);
                return;
        }

        // Min height controls
        int newMin = currentRange.getMin();
        int newMax = currentRange.getMax();

        switch (event.getSlot()) {
            case 9: newMin = Math.max(-64, newMin - 10); break;
            case 10: newMin = Math.max(-64, newMin - 1); break;
            case 11: newMin = Math.min(newMax - 1, newMin + 1); break;
            case 12: newMin = Math.min(newMax - 1, newMin + 10); break;
            case 14: newMax = Math.max(newMin + 1, newMax - 10); break;
            case 15: newMax = Math.max(newMin + 1, newMax - 1); break;
            case 16: newMax = Math.min(320, newMax + 1); break;
            case 17: newMax = Math.min(320, newMax + 10); break;
        }

        metadata.setSpawnHeightRange(new IntRange(newMin, newMax));

        tempMetadata.put(tempKey, metadata);

        openHeightSelector(player, structureId);
    }

    private void handleDistanceSelectorClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType() == Material.AIR) return;

        String structureId = extractStructureId(event.getView().getTitle());
        String tempKey = player.getName() + ":" + structureId;
        StructureMetadata metadata = tempMetadata.get(tempKey);

        if (metadata == null) return;

        if (event.getSlot() == 26) {
            openMainMetadataGUI(player, structureId);
            return;
        }

        switch (event.getSlot()) {
            case 9: // -50
                metadata.setMinDistanceFromSame(Math.max(0, metadata.getMinDistanceFromSame() - 50));
                break;
            case 10: // -10
                metadata.setMinDistanceFromSame(Math.max(0, metadata.getMinDistanceFromSame() - 10));
                break;
            case 11: // +10
                metadata.setMinDistanceFromSame(metadata.getMinDistanceFromSame() + 10);
                break;
            case 12: // +50
                metadata.setMinDistanceFromSame(metadata.getMinDistanceFromSame() + 50);
                break;
            case 14: // -25
                metadata.setMinDistanceFromAny(Math.max(0, metadata.getMinDistanceFromAny() - 25));
                break;
            case 15: // -5
                metadata.setMinDistanceFromAny(Math.max(0, metadata.getMinDistanceFromAny() - 5));
                break;
            case 16: // +5
                metadata.setMinDistanceFromAny(metadata.getMinDistanceFromAny() + 5);
                break;
            case 17: // +25
                metadata.setMinDistanceFromAny(metadata.getMinDistanceFromAny() + 25);
                break;
        }

        tempMetadata.put(tempKey, metadata);

        openDistanceSelector(player, structureId);
    }

    private void handleConditionsSelectorClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType() == Material.AIR) return;

        String structureId = extractStructureId(event.getView().getTitle());
        String tempKey = player.getName() + ":" + structureId;
        StructureMetadata metadata = tempMetadata.get(tempKey);

        if (metadata == null) return;

        if (event.getSlot() == 26) {
            openMainMetadataGUI(player, structureId);
            return;
        }

        SpawnConditions conditions = metadata.getSpawnConditions();

        switch (event.getSlot()) {
            case 10:
                conditions.setRequiresFlatGround(!conditions.isRequiresFlatGround());
                break;
            case 11:
                conditions.setAvoidWater(!conditions.isAvoidWater());
                break;
            case 12:
                conditions.setAvoidLava(!conditions.isAvoidLava());
                break;
            case 13:
                conditions.setRequiresSkyAccess(!conditions.isRequiresSkyAccess());
                break;
            case 15:
                if (event.getClick().isLeftClick()) {
                    conditions.setMaxSlope(Math.min(2.0, conditions.getMaxSlope() + 0.1));
                } else if (event.getClick().isRightClick()) {
                    conditions.setMaxSlope(Math.max(0.0, conditions.getMaxSlope() - 0.1));
                }
                break;
            case 16:
                if (event.getClick().isLeftClick()) {
                    conditions.setMinClearHeight(Math.min(20, conditions.getMinClearHeight() + 1));
                } else if (event.getClick().isRightClick()) {
                    conditions.setMinClearHeight(Math.max(1, conditions.getMinClearHeight() - 1));
                }
                break;
        }

        tempMetadata.put(tempKey, metadata);

        openConditionsSelector(player, structureId);
    }

    private void handleTagsManagerClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType() == Material.AIR) return;

        String structureId = extractStructureId(event.getView().getTitle());
        String tempKey = player.getName() + ":" + structureId;
        StructureMetadata metadata = tempMetadata.get(tempKey);

        if (metadata == null) return;

        if (event.getSlot() == 53) {
            openMainMetadataGUI(player, structureId);
            return;
        }

        if (event.getSlot() == 49) {
            player.sendMessage("§eType in chat: addtag <tagname>");
            player.closeInventory();
            return;
        }

        String[] commonTags = {
                "village", "house", "building", "medieval", "modern", "fantasy",
                "castle", "tower", "dungeon", "farm", "temple", "ruins",
                "small", "medium", "large", "underground", "surface", "elevated"
        };

        if (event.getSlot() < commonTags.length) {
            String tag = commonTags[event.getSlot()];

            if (metadata.getTags().contains(tag)) {
                metadata.getTags().remove(tag);
            } else {
                metadata.getTags().add(tag);
            }

            tempMetadata.put(tempKey, metadata);

            openTagsManager(player, structureId);
        }
    }

    private void handleCategoryAnvilClick(InventoryClickEvent event) {
        if (event.getSlot() != 2) return;

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack result = event.getCurrentItem();

        if (result == null || !result.hasItemMeta() || !Objects.requireNonNull(result.getItemMeta()).hasDisplayName()) return;

        String pendingInput = pendingAnvilInputs.get(player.getName());
        if (pendingInput == null || !pendingInput.startsWith("category:")) return;

        String structureId = pendingInput.substring("category:".length());
        String tempKey = player.getName() + ":" + structureId;
        StructureMetadata metadata = tempMetadata.get(tempKey);

        if (metadata == null) return;

        String newCategory = result.getItemMeta().getDisplayName();
        metadata.setCategory(newCategory);

        tempMetadata.put(tempKey, metadata);

        pendingAnvilInputs.remove(player.getName());
        player.sendMessage("§aCategory set to: " + newCategory);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            openMainMetadataGUI(player, structureId);
        }, 1L);
    }

    private String extractStructureId(String title) {
        if (title.startsWith("§6Structure Metadata: ")) {
            return title.substring("§6Structure Metadata: ".length());
        } else if (title.startsWith("§6Biome Selector: ")) {
            return title.substring("§6Biome Selector: ".length());
        } else if (title.startsWith("§6Dimension Selector: ")) {
            return title.substring("§6Dimension Selector: ".length());
        } else if (title.startsWith("§6Height Range: ")) {
            return title.substring("§6Height Range: ".length());
        } else if (title.startsWith("§6Distance Settings: ")) {
            return title.substring("§6Distance Settings: ".length());
        } else if (title.startsWith("§6Spawn Conditions: ")) {
            return title.substring("§6Spawn Conditions: ".length());
        } else if (title.startsWith("§6Tags Manager: ")) {
            return title.substring("§6Tags Manager: ".length());
        }
        return "";
    }

    private void adjustSpawnChance(Player player, String structureId, ClickType clickType, StructureMetadata metadata) {
        float adjustment = 0.001f;
        if (clickType.isShiftClick()) {
            adjustment = 0.01f;
        }

        if (clickType.isLeftClick()) {
            metadata.setSpawnChance(Math.min(1.0f, metadata.getSpawnChance() + adjustment));
        } else if (clickType.isRightClick()) {
            metadata.setSpawnChance(Math.max(0.0f, metadata.getSpawnChance() - adjustment));
        }

        String tempKey = player.getName() + ":" + structureId;
        tempMetadata.put(tempKey, metadata);

        openMainMetadataGUI(player, structureId);
    }

    private void saveMetadata(Player player, String structureId, StructureMetadata metadata) {
        try {
            File structureFile = new File(plugin.getStructuresFolder(), structureId + ".json");
            Structure structure = plugin.getStructureAPI().loadStructure(structureFile);
            structure.setMetadata(metadata);

            plugin.getStructureAPI().saveStructure(structure, structureFile);

            tempMetadata.remove(player.getName() + ":" + structureId);
            activeGUIPlayers.remove(player.getName());
            pendingAnvilInputs.remove(player.getName());

            player.sendMessage("§aMetadata saved for structure: " + structureId);
            player.closeInventory();

        } catch (Exception e) {
            player.sendMessage("§cFailed to save metadata: " + e.getMessage());
        }
    }
}