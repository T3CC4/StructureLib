package de.tecca.structureLib;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.*;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.List;

class LootProcessor {
    private final Map<String, String> blockTypeLootTables = new HashMap<>();
    private final Map<String, List<ItemStack>> customLoot = new HashMap<>();
    private final Random random = new Random();

    public void addLootTable(String blockType, String lootTableKey) {
        blockTypeLootTables.put(blockType.toLowerCase(), lootTableKey);
    }

    public void addCustomLoot(String blockType, List<ItemStack> items) {
        customLoot.put(blockType.toLowerCase(), new ArrayList<>(items));
    }

    public void addCustomLoot(String blockType, ItemStack item) {
        customLoot.computeIfAbsent(blockType.toLowerCase(), k -> new ArrayList<>()).add(item);
    }

    public void processContainer(Block block, String containerType) {
        BlockState state = block.getState();
        String type = containerType.toLowerCase();

        if (blockTypeLootTables.containsKey(type)) {
            String lootTableKey = blockTypeLootTables.get(type);
            applyLootTable(state, lootTableKey);
        }

        if (customLoot.containsKey(type)) {
            List<ItemStack> items = customLoot.get(type);
            addItemsToContainer(state, items);
        }

        state.update();
    }

    private void addItemsToContainer(BlockState state, List<ItemStack> items) {
        for (ItemStack item : items) {
            if (item != null) {
                addSingleItemToContainer(state, item.clone());
            }
        }
    }

    private void addSingleItemToContainer(BlockState state, ItemStack item) {
        if (state instanceof Chest chest) {
            chest.getInventory().addItem(item);
        } else if (state instanceof Barrel barrel) {
            barrel.getInventory().addItem(item);
        } else if (state instanceof ShulkerBox shulkerBox) {
            shulkerBox.getInventory().addItem(item);
        }
    }

    private void applyLootTable(BlockState state, String lootTableKey) {
        try {
            org.bukkit.loot.LootTable lootTable = Bukkit.getLootTable(NamespacedKey.minecraft(lootTableKey));
            if (lootTable != null) {
                Collection<ItemStack> loot = lootTable.populateLoot(random,
                        new org.bukkit.loot.LootContext.Builder(state.getLocation()).build());

                for (ItemStack item : loot) {
                    if (item != null) {
                        addSingleItemToContainer(state, item);
                    }
                }
            }
        } catch (Exception e) {
            applyDefaultLoot(state, lootTableKey);
        }
    }

    private void applyDefaultLoot(BlockState state, String lootTableKey) {
        List<ItemStack> defaultItems = new ArrayList<>();

        switch (lootTableKey.toLowerCase()) {
            case "village_house":
                defaultItems.add(new ItemStack(Material.BREAD, random.nextInt(3) + 1));
                if (random.nextBoolean()) {
                    defaultItems.add(new ItemStack(Material.WHEAT, random.nextInt(5) + 1));
                }
                break;
            case "dungeon":
                defaultItems.add(new ItemStack(Material.IRON_INGOT, random.nextInt(3) + 1));
                if (random.nextFloat() < 0.3f) {
                    defaultItems.add(new ItemStack(Material.DIAMOND, 1));
                }
                break;
            case "treasure":
                defaultItems.add(new ItemStack(Material.GOLD_INGOT, random.nextInt(5) + 2));
                defaultItems.add(new ItemStack(Material.EMERALD, random.nextInt(3) + 1));
                break;
            default:
                defaultItems.add(new ItemStack(Material.COBBLESTONE, random.nextInt(10) + 1));
                break;
        }

        addItemsToContainer(state, defaultItems);
    }

    public boolean hasLootFor(String blockType) {
        String type = blockType.toLowerCase();
        return blockTypeLootTables.containsKey(type) || customLoot.containsKey(type);
    }
}