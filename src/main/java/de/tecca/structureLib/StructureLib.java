package de.tecca.structureLib;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

public class StructureLib extends JavaPlugin {
    private StructureAPI structureAPI;
    private File structuresFolder;
    private static StructureLib plugin;

    public static StructureLib getPlugin() {
        return plugin;
    }

    @Override
    public void onEnable() {
        plugin = this;
        this.structureAPI = new StructureAPI();
        this.structuresFolder = new File(getDataFolder(), "structures");

        if (!structuresFolder.exists()) {
            structuresFolder.mkdirs();
        }

        StructureCommand structureCommand = new StructureCommand(this);
        Objects.requireNonNull(getCommand("struct")).setExecutor(structureCommand);
        Objects.requireNonNull(getCommand("struct")).setTabCompleter(structureCommand);
    }

    public StructureAPI getStructureAPI() {
        return structureAPI;
    }

    public File getStructuresFolder() {
        return structuresFolder;
    }
}