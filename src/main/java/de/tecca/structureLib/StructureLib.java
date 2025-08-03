package de.tecca.structureLib;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

public class StructureLib extends JavaPlugin {
    private StructureAPI structureAPI;
    private File structuresFolder;
    private MetadataBasedSpawner naturalSpawner;
    private StructureMetadataGUI metadataGUI;
    private static StructureLib plugin;

    public static StructureLib getPlugin() {
        return plugin;
    }

    @Override
    public void onEnable() {
        plugin = this;
        this.structureAPI = new StructureAPI();
        this.structuresFolder = new File(getDataFolder(), "structures");
        this.naturalSpawner = new MetadataBasedSpawner(this);
        this.metadataGUI = new StructureMetadataGUI(this);

        if (!structuresFolder.exists()) {
            structuresFolder.mkdirs();
        }

        StructureCommand structureCommand = new StructureCommand(this);
        Objects.requireNonNull(getCommand("struct")).setExecutor(structureCommand);
        Objects.requireNonNull(getCommand("struct")).setTabCompleter(structureCommand);

        Bukkit.getPluginManager().registerEvents(naturalSpawner, this);
        Bukkit.getPluginManager().registerEvents(metadataGUI, this);

        naturalSpawner.loadAllActiveSpawners();

        getLogger().info("StructureLib enabled with metadata system!");
    }

    @Override
    public void onDisable() {
        getLogger().info("StructureLib disabled!");
    }

    public StructureAPI getStructureAPI() {
        return structureAPI;
    }

    public File getStructuresFolder() {
        return structuresFolder;
    }

    public MetadataBasedSpawner getNaturalSpawner() {
        return naturalSpawner;
    }

    public StructureMetadataGUI getMetadataGUI() {
        return metadataGUI;
    }
}