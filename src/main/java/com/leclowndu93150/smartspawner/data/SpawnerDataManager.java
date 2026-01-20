package com.leclowndu93150.smartspawner.data;

import com.leclowndu93150.smartspawner.Smartspawner;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SpawnerDataManager {
    private static final Map<ResourceKey<Level>, Map<BlockPos, SpawnerData>> spawnersByWorld = new ConcurrentHashMap<>();
    private static MinecraftServer server;
    private static Path saveDir;

    public static void init(MinecraftServer mcServer) {
        server = mcServer;
        saveDir = mcServer.getWorldPath(LevelResource.ROOT).resolve("data").resolve("smartspawner");
        try {
            Files.createDirectories(saveDir);
        } catch (IOException e) {
            Smartspawner.LOGGER.error("Failed to create SmartSpawner data directory", e);
        }
        load();
    }

    public static SpawnerData getOrCreate(ServerLevel level, BlockPos pos) {
        ResourceKey<Level> worldKey = level.dimension();
        Map<BlockPos, SpawnerData> worldSpawners = spawnersByWorld.computeIfAbsent(worldKey, k -> new ConcurrentHashMap<>());

        return worldSpawners.computeIfAbsent(pos, p -> new SpawnerData(p));
    }

    public static SpawnerData get(ServerLevel level, BlockPos pos) {
        ResourceKey<Level> worldKey = level.dimension();
        Map<BlockPos, SpawnerData> worldSpawners = spawnersByWorld.get(worldKey);

        if (worldSpawners == null) return null;
        return worldSpawners.get(pos);
    }

    public static void put(ServerLevel level, BlockPos pos, SpawnerData data) {
        ResourceKey<Level> worldKey = level.dimension();
        Map<BlockPos, SpawnerData> worldSpawners = spawnersByWorld.computeIfAbsent(worldKey, k -> new ConcurrentHashMap<>());
        worldSpawners.put(pos, data);
    }

    public static void remove(ServerLevel level, BlockPos pos) {
        ResourceKey<Level> worldKey = level.dimension();
        Map<BlockPos, SpawnerData> worldSpawners = spawnersByWorld.get(worldKey);

        if (worldSpawners != null) {
            worldSpawners.remove(pos);
        }
    }

    public static boolean exists(ServerLevel level, BlockPos pos) {
        ResourceKey<Level> worldKey = level.dimension();
        Map<BlockPos, SpawnerData> worldSpawners = spawnersByWorld.get(worldKey);

        return worldSpawners != null && worldSpawners.containsKey(pos);
    }

    public static void tick(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            ResourceKey<Level> worldKey = level.dimension();
            Map<BlockPos, SpawnerData> worldSpawners = spawnersByWorld.get(worldKey);

            if (worldSpawners == null) continue;

            for (SpawnerData data : worldSpawners.values()) {
                if (level.isLoaded(data.getPos())) {
                    data.tick(level);
                }
            }
        }
    }

    public static void save() {
        if (server == null || saveDir == null) return;

        for (Map.Entry<ResourceKey<Level>, Map<BlockPos, SpawnerData>> worldEntry : spawnersByWorld.entrySet()) {
            String worldName = worldEntry.getKey().location().toString().replace(":", "_").replace("/", "_");
            Path worldFile = saveDir.resolve(worldName + ".dat");

            CompoundTag worldTag = new CompoundTag();
            ListTag spawnersList = new ListTag();

            for (Map.Entry<BlockPos, SpawnerData> spawnerEntry : worldEntry.getValue().entrySet()) {
                SpawnerData data = spawnerEntry.getValue();
                CompoundTag spawnerTag = new CompoundTag();

                BlockPos pos = spawnerEntry.getKey();
                spawnerTag.putInt("X", pos.getX());
                spawnerTag.putInt("Y", pos.getY());
                spawnerTag.putInt("Z", pos.getZ());

                spawnerTag.put("Data", data.toNbt(server.registryAccess()));
                spawnersList.add(spawnerTag);
            }

            worldTag.put("Spawners", spawnersList);

            try {
                NbtIo.writeCompressed(worldTag, worldFile);
            } catch (IOException e) {
                Smartspawner.LOGGER.error("Failed to save SmartSpawner data for world {}", worldName, e);
            }
        }
    }

    private static void load() {
        if (server == null || saveDir == null) return;

        try {
            if (!Files.exists(saveDir)) return;

            Files.list(saveDir)
                .filter(path -> path.toString().endsWith(".dat"))
                .forEach(SpawnerDataManager::loadWorldData);
        } catch (IOException e) {
            Smartspawner.LOGGER.error("Failed to load SmartSpawner data", e);
        }
    }

    private static void loadWorldData(Path worldFile) {
        String fileName = worldFile.getFileName().toString();
        String worldName = fileName.substring(0, fileName.length() - 4).replace("_", ":");

        ResourceKey<Level> worldKey = null;
        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension().location().toString().equals(worldName.replace("_", "/"))) {
                worldKey = level.dimension();
                break;
            }
        }

        if (worldKey == null) {
            if (worldName.equals("minecraft:overworld")) {
                worldKey = Level.OVERWORLD;
            } else if (worldName.equals("minecraft:the_nether")) {
                worldKey = Level.NETHER;
            } else if (worldName.equals("minecraft:the_end")) {
                worldKey = Level.END;
            } else {
                return;
            }
        }

        try {
            CompoundTag worldTag = NbtIo.readCompressed(worldFile, NbtAccounter.unlimitedHeap());
            if (!worldTag.contains("Spawners")) return;

            ListTag spawnersList = worldTag.getList("Spawners", Tag.TAG_COMPOUND);
            Map<BlockPos, SpawnerData> worldSpawners = new ConcurrentHashMap<>();

            for (int i = 0; i < spawnersList.size(); i++) {
                CompoundTag spawnerTag = spawnersList.getCompound(i);
                int x = spawnerTag.getInt("X");
                int y = spawnerTag.getInt("Y");
                int z = spawnerTag.getInt("Z");
                BlockPos pos = new BlockPos(x, y, z);

                if (spawnerTag.contains("Data")) {
                    SpawnerData data = SpawnerData.fromNbt(pos, spawnerTag.getCompound("Data"), server.registryAccess());
                    worldSpawners.put(pos, data);
                }
            }

            spawnersByWorld.put(worldKey, worldSpawners);
        } catch (IOException e) {
            Smartspawner.LOGGER.error("Failed to load SmartSpawner data from {}", worldFile, e);
        }
    }

    public static void clear() {
        spawnersByWorld.clear();
    }
}
