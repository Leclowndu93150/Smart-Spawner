package com.leclowndu93150.smartspawner;

import com.leclowndu93150.smartspawner.data.SpawnerDataManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Smartspawner implements ModInitializer {
    public static final String MOD_ID = "smartspawner";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final int MAX_STACK_SIZE = 1000;
    public static final int MAX_STORED_EXP = 10000;
    public static final int SPAWN_DELAY_TICKS = 200;
    public static final int MIN_MOBS = 1;
    public static final int MAX_MOBS = 4;
    public static final int MAX_INVENTORY_SLOTS = 54;
    public static final int HOPPER_TRANSFER_AMOUNT = 64;

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            SpawnerDataManager.init(server);
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            SpawnerDataManager.save();
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            SpawnerDataManager.tick(server);
        });

        LOGGER.info("SmartSpawner initialized");
    }
}
