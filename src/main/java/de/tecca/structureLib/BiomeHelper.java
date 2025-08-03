package de.tecca.structureLib;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Biome;

import java.util.*;
import java.util.stream.Collectors;

class BiomeHelper {
    private static final Map<String, String> BIOME_DISPLAY_NAMES = new HashMap<>();
    private static List<Biome> allBiomes = null;

    static {
        BIOME_DISPLAY_NAMES.put("OCEAN", "Ocean");
        BIOME_DISPLAY_NAMES.put("PLAINS", "Plains");
        BIOME_DISPLAY_NAMES.put("DESERT", "Desert");
        BIOME_DISPLAY_NAMES.put("MOUNTAINS", "Mountains");
        BIOME_DISPLAY_NAMES.put("FOREST", "Forest");
        BIOME_DISPLAY_NAMES.put("TAIGA", "Taiga");
        BIOME_DISPLAY_NAMES.put("SWAMP", "Swamp");
        BIOME_DISPLAY_NAMES.put("NETHER_WASTES", "Nether Wastes");
        BIOME_DISPLAY_NAMES.put("THE_END", "The End");
        BIOME_DISPLAY_NAMES.put("FROZEN_OCEAN", "Frozen Ocean");
        BIOME_DISPLAY_NAMES.put("FROZEN_RIVER", "Frozen River");
        BIOME_DISPLAY_NAMES.put("SNOWY_TUNDRA", "Snowy Tundra");
        BIOME_DISPLAY_NAMES.put("SNOWY_MOUNTAINS", "Snowy Mountains");
        BIOME_DISPLAY_NAMES.put("MUSHROOM_FIELDS", "Mushroom Fields");
        BIOME_DISPLAY_NAMES.put("MUSHROOM_FIELD_SHORE", "Mushroom Field Shore");
        BIOME_DISPLAY_NAMES.put("BEACH", "Beach");
        BIOME_DISPLAY_NAMES.put("DESERT_HILLS", "Desert Hills");
        BIOME_DISPLAY_NAMES.put("WOODED_HILLS", "Wooded Hills");
        BIOME_DISPLAY_NAMES.put("TAIGA_HILLS", "Taiga Hills");
        BIOME_DISPLAY_NAMES.put("MOUNTAIN_EDGE", "Mountain Edge");
        BIOME_DISPLAY_NAMES.put("JUNGLE", "Jungle");
        BIOME_DISPLAY_NAMES.put("JUNGLE_HILLS", "Jungle Hills");
        BIOME_DISPLAY_NAMES.put("JUNGLE_EDGE", "Jungle Edge");
        BIOME_DISPLAY_NAMES.put("DEEP_OCEAN", "Deep Ocean");
        BIOME_DISPLAY_NAMES.put("STONE_SHORE", "Stone Shore");
        BIOME_DISPLAY_NAMES.put("SNOWY_BEACH", "Snowy Beach");
        BIOME_DISPLAY_NAMES.put("BIRCH_FOREST", "Birch Forest");
        BIOME_DISPLAY_NAMES.put("BIRCH_FOREST_HILLS", "Birch Forest Hills");
        BIOME_DISPLAY_NAMES.put("DARK_FOREST", "Dark Forest");
        BIOME_DISPLAY_NAMES.put("SNOWY_TAIGA", "Snowy Taiga");
        BIOME_DISPLAY_NAMES.put("SNOWY_TAIGA_HILLS", "Snowy Taiga Hills");
        BIOME_DISPLAY_NAMES.put("GIANT_TREE_TAIGA", "Giant Tree Taiga");
        BIOME_DISPLAY_NAMES.put("GIANT_TREE_TAIGA_HILLS", "Giant Tree Taiga Hills");
        BIOME_DISPLAY_NAMES.put("WOODED_MOUNTAINS", "Wooded Mountains");
        BIOME_DISPLAY_NAMES.put("SAVANNA", "Savanna");
        BIOME_DISPLAY_NAMES.put("SAVANNA_PLATEAU", "Savanna Plateau");
        BIOME_DISPLAY_NAMES.put("BADLANDS", "Badlands");
        BIOME_DISPLAY_NAMES.put("WOODED_BADLANDS_PLATEAU", "Wooded Badlands Plateau");
        BIOME_DISPLAY_NAMES.put("BADLANDS_PLATEAU", "Badlands Plateau");
    }

    public static List<Biome> getAllBiomes() {
        if (allBiomes == null) {
            loadAllBiomes();
        }
        return new ArrayList<>(allBiomes);
    }

    private static void loadAllBiomes() {
        allBiomes = new ArrayList<>();

        try {
            Registry<Biome> biomeRegistry = Bukkit.getRegistry(Biome.class);
            if (biomeRegistry != null) {
                for (Biome biome : biomeRegistry) {
                    allBiomes.add(biome);
                }
            }
        } catch (Exception e) {
            try {
                allBiomes.addAll(Arrays.asList(Biome.values()));
            } catch (Exception ex) {
                addCommonBiomes();
            }
        }

        allBiomes.sort(Comparator.comparing(biome -> getBiomeDisplayName(biome)));
    }

    private static void addCommonBiomes() {
        String[] commonBiomes = {
                "OCEAN", "PLAINS", "DESERT", "MOUNTAINS", "FOREST", "TAIGA", "SWAMP",
                "NETHER_WASTES", "THE_END", "FROZEN_OCEAN", "SNOWY_TUNDRA", "MUSHROOM_FIELDS",
                "BEACH", "JUNGLE", "DEEP_OCEAN", "STONE_SHORE", "BIRCH_FOREST", "DARK_FOREST",
                "SNOWY_TAIGA", "GIANT_TREE_TAIGA", "SAVANNA", "BADLANDS"
        };

        for (String biomeName : commonBiomes) {
            try {
                Biome biome = Biome.valueOf(biomeName);
                allBiomes.add(biome);
            } catch (IllegalArgumentException e) {
                // Biome doesn't exist in this version, skip it
            }
        }
    }

    public static String getBiomeDisplayName(Biome biome) {
        String biomeName = biome.toString();

        if (BIOME_DISPLAY_NAMES.containsKey(biomeName)) {
            return BIOME_DISPLAY_NAMES.get(biomeName);
        }

        return Arrays.stream(biomeName.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    public static List<String> getBiomeNames() {
        return getAllBiomes().stream()
                .map(Biome::toString)
                .collect(Collectors.toList());
    }

    public static List<String> getBiomeDisplayNames() {
        return getAllBiomes().stream()
                .map(BiomeHelper::getBiomeDisplayName)
                .collect(Collectors.toList());
    }

    public static Biome getBiomeByName(String name) {
        try {
            return Biome.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static List<Biome> getBiomesByCategory(BiomeCategory category) {
        return getAllBiomes().stream()
                .filter(biome -> getBiomeCategory(biome) == category)
                .collect(Collectors.toList());
    }

    public static BiomeCategory getBiomeCategory(Biome biome) {
        String name = biome.toString().toLowerCase();

        if (name.contains("ocean") || name.contains("river")) {
            return BiomeCategory.AQUATIC;
        } else if (name.contains("desert") || name.contains("badlands")) {
            return BiomeCategory.DESERT;
        } else if (name.contains("mountain") || name.contains("hills")) {
            return BiomeCategory.MOUNTAIN;
        } else if (name.contains("forest") || name.contains("jungle") || name.contains("taiga")) {
            return BiomeCategory.FOREST;
        } else if (name.contains("snowy") || name.contains("frozen") || name.contains("ice")) {
            return BiomeCategory.ICY;
        } else if (name.contains("nether")) {
            return BiomeCategory.NETHER;
        } else if (name.contains("end")) {
            return BiomeCategory.END;
        } else if (name.contains("savanna") || name.contains("plains")) {
            return BiomeCategory.SAVANNA;
        } else if (name.contains("swamp")) {
            return BiomeCategory.SWAMP;
        } else {
            return BiomeCategory.OTHER;
        }
    }

    public enum BiomeCategory {
        AQUATIC("Aquatic"),
        DESERT("Desert"),
        FOREST("Forest"),
        ICY("Icy"),
        MOUNTAIN("Mountain"),
        NETHER("Nether"),
        END("End"),
        SAVANNA("Savanna"),
        SWAMP("Swamp"),
        OTHER("Other");

        private final String displayName;

        BiomeCategory(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public static boolean isValidBiome(String biomeName) {
        return getBiomeByName(biomeName) != null;
    }

    public static List<String> searchBiomes(String query) {
        String lowerQuery = query.toLowerCase();

        return getAllBiomes().stream()
                .filter(biome ->
                        biome.toString().toLowerCase().contains(lowerQuery) ||
                                getBiomeDisplayName(biome).toLowerCase().contains(lowerQuery)
                )
                .map(Biome::toString)
                .collect(Collectors.toList());
    }
}