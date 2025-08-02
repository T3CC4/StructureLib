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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class StructureCommand implements CommandExecutor, TabCompleter {
    private final StructureLib plugin;
    private final StructureAPI api;

    public StructureCommand(StructureLib plugin) {
        this.plugin = plugin;
        this.api = plugin.getStructureAPI();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§cUsage: /struct <save|place> <id> [rotation]");
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "save":
                return handleSave(player, args);
            case "place":
                return handlePlace(player, args);
            default:
                player.sendMessage("§cUsage: /struct <save|place> <id> [rotation]");
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

        } catch (IncompleteRegionException e) {
            player.sendMessage("§cPlease select an area with //pos1 and //pos2 first!");
        } catch (Exception e) {
            player.sendMessage("§cError while saving: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            List<String> commands = Arrays.asList("save", "place");
            return commands.stream()
                    .filter(command -> command.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("place")) {
            File[] files = plugin.getStructuresFolder().listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null) {
                return Arrays.stream(files)
                        .map(f -> f.getName().replace(".json", ""))
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("place")) {
            List<String> rotations = Arrays.asList("0", "90", "180", "270", "random");
            return rotations.stream()
                    .filter(rot -> rot.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
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
}