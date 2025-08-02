package de.tecca.structureLib;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

class StructureAPI {
    private final Gson gson;

    public StructureAPI() {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
    }

    public Structure captureStructure(Region region, World world, String id) {
        return new StructureCapture().capture(region, world, id);
    }

    public void saveStructure(Structure structure, File file) throws IOException {
        Files.write(file.toPath(), gson.toJson(structure).getBytes());
    }

    public Structure loadStructure(File file) throws IOException {
        String json = Files.readString(file.toPath());
        return gson.fromJson(json, Structure.class);
    }

    public void placeStructure(Structure structure, Location location) {
        new StructurePlacer(StructureLib.getPlugin()).place(structure, location);
    }

    public void placeStructure(Structure structure, Location location, int rotation, boolean randomRotation) {
        new StructurePlacer(StructureLib.getPlugin()).place(structure, location, rotation, randomRotation);
    }
}