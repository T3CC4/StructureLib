package de.tecca.structureLib;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

class StructureCommand implements CommandExecutor, TabCompleter {
    private final StructureLib plugin;
    private final StructureAPI api;
    private final StructureMetadataGUI gui;

    public StructureCommand(StructureLib plugin) {
        this.plugin = plugin;
        this.api = plugin.getStructureAPI();
        this.gui = plugin.getMetadataGUI();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§cUsage: /struct <save|place|info|edit|list|enable-spawning> [args...]");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "save":
                return handleSave(player, args);
            case "place":
                return handlePlace(player, args);
            case "info":
                return handleInfo(player, args);
            case "edit":
                return handleEdit(player, args);
            case "list":
                return handleList(player, args);
            case "enable-spawning":
                return handleEnableSpawning(player, args);
            default:
                player.sendMessage("§cUsage: /struct <save|place|info|edit|list|enable-spawning> [args...]");
                return true;
        }
    }

    private boolean handleSave(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /struct save <id>");
            return true;
        }

        String id = args[1];

        try {
            BukkitPlayer bukkitPlayer = BukkitAdapter.adapt(player);
            LocalSession session = WorldEdit.getInstance().getSessionManager().get(bukkitPlayer);

            Region selection = session.getSelection(bukkitPlayer.getWorld());
            if (selection == null) {
                player.sendMessage("§cPlease select an area with //pos1 and //pos2 first!");
                return true;
            }

            player.sendMessage("§7Analyzing structure...");

            com.sk89q.worldedit.world.World weWorld = bukkitPlayer.getWorld();
            World bukkitWorld = BukkitAdapter.adapt(weWorld);

            Structure structure = api.captureStructure(selection, bukkitWorld, id);
            structure.setAuthor(player.getName());

            File file = new File(plugin.getStructuresFolder(), id + ".json");
            api.saveStructure(structure, file);

            int[] size = structure.getSize();
            int totalBlocks = size[0] * size[1] * size[2];
            int regionCount = structure.getRegions().size();

            player.sendMessage("§a✓ Structure saved:");
            player.sendMessage("§7  ID: §f" + id);
            player.sendMessage("§7  Size: §f" + size[0] + "x" + size[1] + "x" + size[2]);
            player.sendMessage("§7  Blocks: §f" + totalBlocks);
            player.sendMessage("§7  Regions: §f" + regionCount);

            StructureMetadata metadata = structure.getMetadata();
            if (metadata != null) {
                player.sendMessage("§7  Source Dimension: §f" + metadata.getSourceDimension());
                player.sendMessage("§7  Source Biome: §f" + metadata.getSourceBiome());
                player.sendMessage("§a  Use '/struct edit " + id + "' to configure spawn settings");
            }

        } catch (IncompleteRegionException e) {
            player.sendMessage("§cPlease select an area with //pos1 and //pos2 first!");
        } catch (Exception e) {
            player.sendMessage("§cError while saving: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }

    private boolean handlePlace(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /struct place <id> [rotation|random]");
            return true;
        }

        String id = args[1];
        File file = new File(plugin.getStructuresFolder(), id + ".json");

        if (!file.exists()) {
            player.sendMessage("§cStructure '" + id + "' not found!");
            return true;
        }

        int rotation = 0;
        boolean randomRotation = false;

        if (args.length > 2) {
            String rotationArg = args[2];
            if (rotationArg.equalsIgnoreCase("random")) {
                randomRotation = true;
            } else {
                try {
                    rotation = Integer.parseInt(rotationArg);
                    if (rotation % 90 != 0) {
                        player.sendMessage("§cRotation must be a multiple of 90 (0, 90, 180, 270)!");
                        return true;
                    }
                    rotation = rotation % 360;
                } catch (NumberFormatException e) {
                    player.sendMessage("§cInvalid rotation. Use a number or 'random'!");
                    return true;
                }
            }
        }

        try {
            Structure structure = api.loadStructure(file);

            Location location = player.getLocation().getBlock().getLocation();

            if (randomRotation) {
                player.sendMessage("§7Placing structure with random rotation...");
            } else if (rotation != 0) {
                player.sendMessage("§7Placing structure with " + rotation + "° rotation...");
            } else {
                player.sendMessage("§7Placing structure...");
            }

            try {
                api.placeStructure(structure, location, rotation, randomRotation);

                if (randomRotation) {
                    player.sendMessage("§a✓ Structure '" + id + "' placed with random rotation!");
                } else if (rotation != 0) {
                    player.sendMessage("§a✓ Structure '" + id + "' placed with " + rotation + "° rotation!");
                } else {
                    player.sendMessage("§a✓ Structure '" + id + "' placed!");
                }

            } catch (Exception e) {
                player.sendMessage("§cError while placing: " + e.getMessage());
            }

        } catch (Exception e) {
            player.sendMessage("§cError while loading: " + e.getMessage());
        }

        return true;
    }

    private boolean handleInfo(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /struct info <structure_id>");
            return true;
        }

        String structureId = args[1];
        File structureFile = new File(plugin.getStructuresFolder(), structureId + ".json");

        if (!structureFile.exists()) {
            player.sendMessage("§cStructure '" + structureId + "' not found!");
            return true;
        }

        try {
            Structure structure = api.loadStructure(structureFile);
            StructureMetadata metadata = structure.getMetadata();

            player.sendMessage("§6=== Structure Info: " + structureId + " ===");
            player.sendMessage("§7Author: §f" + structure.getAuthor());
            player.sendMessage("§7Created: §f" + new Date(structure.getCreated()));
            player.sendMessage("§7Size: §f" + Arrays.toString(structure.getSize()));
            player.sendMessage("§7Regions: §f" + structure.getRegions().size());
            player.sendMessage("§7Block Entities: §f" + structure.getBlockEntities().size());
            player.sendMessage("§7Entities: §f" + structure.getEntities().size());
            player.sendMessage("");

            if (metadata != null) {
                player.sendMessage("§6=== Spawn Settings ===");
                player.sendMessage("§7Source: §f" + metadata.getSourceDimension() + " - " + metadata.getSourceBiome() + " (Y: " + metadata.getSourceY() + ")");
                player.sendMessage("§7Dimensions: §a" + String.join(", ", metadata.getAllowedDimensions()));
                player.sendMessage("§7Biomes: §a" + metadata.getAllowedBiomes().size() + " allowed, §c" + metadata.getForbiddenBiomes().size() + " forbidden");
                player.sendMessage("§7Height: §f" + metadata.getSpawnHeightRange().getMin() + " to " + metadata.getSpawnHeightRange().getMax());
                player.sendMessage("§7Spawn Chance: §e" + String.format("%.2f", metadata.getSpawnChance() * 100) + "%");
                player.sendMessage("§7Min Distance (Same): §e" + metadata.getMinDistanceFromSame() + " blocks");
                player.sendMessage("§7Min Distance (Any): §e" + metadata.getMinDistanceFromAny() + " blocks");
                player.sendMessage("§7Natural Spawning: " + (metadata.isNaturalSpawning() ? "§aEnabled" : "§cDisabled"));
                player.sendMessage("§7Category: §f" + metadata.getCategory());
                if (!metadata.getTags().isEmpty()) {
                    player.sendMessage("§7Tags: §f" + String.join(", ", metadata.getTags()));
                }
            } else {
                player.sendMessage("§7No metadata found - use §e/struct edit " + structureId + "§7 to add spawn settings");
            }

        } catch (IOException e) {
            player.sendMessage("§cError loading structure: " + e.getMessage());
        }

        return true;
    }

    private boolean handleEdit(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /struct edit <structure_id>");
            return true;
        }

        String structureId = args[1];
        File structureFile = new File(plugin.getStructuresFolder(), structureId + ".json");

        if (!structureFile.exists()) {
            player.sendMessage("§cStructure '" + structureId + "' not found!");
            return true;
        }

        gui.openMainMetadataGUI(player, structureId);
        return true;
    }

    private boolean handleList(Player player, String[] args) {
        String filterTag = args.length > 1 ? args[1] : null;
        String filterDimension = args.length > 2 ? args[2] : null;

        List<Structure> structures = getAllStructures();

        List<Structure> filtered = structures.stream()
                .filter(s -> s.getMetadata() != null)
                .filter(s -> filterTag == null || s.getMetadata().getTags().contains(filterTag))
                .filter(s -> filterDimension == null || s.getMetadata().getAllowedDimensions().contains(filterDimension))
                .toList();

        player.sendMessage("§6=== Structures ===");
        if (filterTag != null || filterDimension != null) {
            player.sendMessage("§7Filters: " +
                    (filterTag != null ? "Tag: §a" + filterTag + " " : "") +
                    (filterDimension != null ? "Dimension: §a" + filterDimension : ""));
        }

        if (filtered.isEmpty()) {
            player.sendMessage("§7No structures found with the specified filters");
        } else {
            for (Structure structure : filtered) {
                StructureMetadata meta = structure.getMetadata();
                String spawning = meta.isNaturalSpawning() ? " §a[Natural]" : "";
                String tags = meta.getTags().isEmpty() ? "" : " §7(" + String.join(", ", meta.getTags()) + ")";
                player.sendMessage("§a" + structure.getId() + spawning + tags);
            }
            player.sendMessage("§7Total: " + filtered.size() + " structures");
        }

        return true;
    }

    private boolean handleEnableSpawning(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /struct enable-spawning <structure_id>");
            return true;
        }

        String structureId = args[1];
        File structureFile = new File(plugin.getStructuresFolder(), structureId + ".json");

        if (!structureFile.exists()) {
            player.sendMessage("§cStructure '" + structureId + "' not found!");
            return true;
        }

        try {
            Structure structure = api.loadStructure(structureFile);
            StructureMetadata metadata = structure.getMetadata();

            if (metadata == null) {
                metadata = new StructureMetadata();
                structure.setMetadata(metadata);
            }

            metadata.setNaturalSpawning(true);
            api.saveStructure(structure, structureFile);

            plugin.getNaturalSpawner().registerStructure(structureId, metadata);

            player.sendMessage("§aNatural spawning enabled for: " + structureId);
            player.sendMessage("§7Use §e/struct edit " + structureId + "§7 to configure spawn settings");

        } catch (IOException e) {
            player.sendMessage("§cError updating structure: " + e.getMessage());
        }

        return true;
    }

    private List<Structure> getAllStructures() {
        List<Structure> structures = new ArrayList<>();
        File[] files = plugin.getStructuresFolder().listFiles((dir, name) -> name.endsWith(".json"));

        if (files != null) {
            for (File file : files) {
                try {
                    Structure structure = api.loadStructure(file);
                    structures.add(structure);
                } catch (IOException e) {
                    // Skip invalid files
                }
            }
        }

        return structures;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            List<String> commands = Arrays.asList("save", "place", "info", "edit", "list", "enable-spawning");
            return commands.stream()
                    .filter(command -> command.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("place") || subCommand.equals("info") ||
                    subCommand.equals("edit") || subCommand.equals("enable-spawning")) {
                File[] files = plugin.getStructuresFolder().listFiles((dir, name) -> name.endsWith(".json"));
                if (files != null) {
                    return Arrays.stream(files)
                            .map(f -> f.getName().replace(".json", ""))
                            .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                }
            }

            if (subCommand.equals("list")) {
                List<String> tags = getAllStructures().stream()
                        .filter(s -> s.getMetadata() != null)
                        .flatMap(s -> s.getMetadata().getTags().stream())
                        .distinct()
                        .toList();

                return tags.stream()
                        .filter(tag -> tag.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("place")) {
                List<String> rotations = Arrays.asList("0", "90", "180", "270", "random");
                return rotations.stream()
                        .filter(rot -> rot.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }

            if (args[0].equalsIgnoreCase("list")) {
                List<String> dimensions = Arrays.asList("overworld", "nether", "end");
                return dimensions.stream()
                        .filter(dim -> dim.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return new ArrayList<>();
    }
}