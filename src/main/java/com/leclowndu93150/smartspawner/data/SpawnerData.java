package com.leclowndu93150.smartspawner.data;

import com.leclowndu93150.smartspawner.Smartspawner;
import com.leclowndu93150.smartspawner.loot.LootGenerator;
import com.leclowndu93150.smartspawner.loot.LootResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.List;

public class SpawnerData {
    private EntityType<?> entityType;
    private int stackSize;
    private int storedExp;
    private VirtualInventory inventory;
    private int spawnDelay;
    private long lastSpawnTime;
    private BlockPos pos;
    private boolean dirty;

    public SpawnerData(BlockPos pos) {
        this.pos = pos;
        this.entityType = EntityType.PIG;
        this.stackSize = 1;
        this.storedExp = 0;
        this.inventory = new VirtualInventory();
        this.spawnDelay = Smartspawner.SPAWN_DELAY_TICKS;
        this.lastSpawnTime = 0;
        this.dirty = true;
    }

    public SpawnerData(BlockPos pos, EntityType<?> entityType, int stackSize) {
        this(pos);
        this.entityType = entityType;
        this.stackSize = stackSize;
    }

    public void tick(ServerLevel level) {
        long gameTime = level.getGameTime();

        if (!level.hasNearbyAlivePlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 16)) {
            return;
        }

        if (gameTime - lastSpawnTime >= spawnDelay) {
            generateLoot(level);
            lastSpawnTime = gameTime;
            markDirty();
        }
    }

    private void generateLoot(ServerLevel level) {
        int effectiveMinMobs = Smartspawner.MIN_MOBS * stackSize;
        int effectiveMaxMobs = Smartspawner.MAX_MOBS * stackSize;

        LootResult result = LootGenerator.generateLoot(entityType, level, pos, effectiveMinMobs, effectiveMaxMobs);

        for (ItemStack stack : result.items()) {
            if (inventory.getUsedSlots() < getMaxInventorySlots()) {
                inventory.addItem(stack);
            }
        }

        int newExp = storedExp + result.experience();
        storedExp = Math.min(newExp, getMaxStoredExp());
        markDirty();
    }

    public void addItems(List<ItemStack> items) {
        inventory.addItems(items);
        markDirty();
    }

    public int claimExperience() {
        int exp = storedExp;
        storedExp = 0;
        markDirty();
        return exp;
    }

    public EntityType<?> getEntityType() {
        return entityType;
    }

    public void setEntityType(EntityType<?> entityType) {
        this.entityType = entityType;
        markDirty();
    }

    public int getStackSize() {
        return stackSize;
    }

    public void setStackSize(int stackSize) {
        this.stackSize = Math.max(1, Math.min(stackSize, Smartspawner.MAX_STACK_SIZE));
        markDirty();
    }

    public void addToStack(int amount) {
        setStackSize(stackSize + amount);
    }

    public void removeFromStack(int amount) {
        setStackSize(stackSize - amount);
    }

    public int getStoredExp() {
        return storedExp;
    }

    public int getMaxStoredExp() {
        return Smartspawner.MAX_STORED_EXP * stackSize;
    }

    public int getMaxInventorySlots() {
        return Smartspawner.MAX_INVENTORY_SLOTS * stackSize;
    }

    public VirtualInventory getInventory() {
        return inventory;
    }

    public BlockPos getPos() {
        return pos;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markDirty() {
        dirty = true;
    }

    public void clearDirty() {
        dirty = false;
    }

    public CompoundTag toNbt(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putString("EntityType", BuiltInRegistries.ENTITY_TYPE.getKey(entityType).toString());
        tag.putInt("StackSize", stackSize);
        tag.putInt("StoredExp", storedExp);
        tag.putInt("SpawnDelay", spawnDelay);
        tag.putLong("LastSpawnTime", lastSpawnTime);
        tag.put("Inventory", inventory.toNbt(provider));
        return tag;
    }

    public void toValueOutput(ValueOutput output) {
        output.putString("EntityType", BuiltInRegistries.ENTITY_TYPE.getKey(entityType).toString());
        output.putInt("StackSize", stackSize);
        output.putInt("StoredExp", storedExp);
        output.putInt("SpawnDelay", spawnDelay);
        output.putLong("LastSpawnTime", lastSpawnTime);
        inventory.toValueOutput(output.child("Inventory"));
    }

    private static EntityType<?> parseEntityType(String id) {
        ResourceLocation entityId = ResourceLocation.parse(id);
        var optional = BuiltInRegistries.ENTITY_TYPE.get(entityId);
        if (optional.isPresent()) {
            return optional.get().value();
        }
        return EntityType.PIG;
    }

    public static SpawnerData fromNbt(BlockPos pos, CompoundTag tag, HolderLookup.Provider provider) {
        SpawnerData data = new SpawnerData(pos);

        if (tag.contains("EntityType")) {
            data.entityType = parseEntityType(tag.getString("EntityType").get());
        }

        if (tag.contains("StackSize")) {
            data.stackSize = tag.getInt("StackSize").get();
        }

        if (tag.contains("StoredExp")) {
            data.storedExp = tag.getInt("StoredExp").get();
        }

        if (tag.contains("SpawnDelay")) {
            data.spawnDelay = tag.getInt("SpawnDelay").get();
        }

        if (tag.contains("LastSpawnTime")) {
            data.lastSpawnTime = tag.getLong("LastSpawnTime").get();
        }

        if (tag.contains("Inventory")) {
            data.inventory = VirtualInventory.fromNbt(tag.getCompound("Inventory").get(), provider);
        }

        data.dirty = false;
        return data;
    }

    public static SpawnerData fromValueInput(BlockPos pos, ValueInput input) {
        SpawnerData data = new SpawnerData(pos);

        input.getString("EntityType").ifPresent(id -> data.entityType = parseEntityType(id));
        input.getInt("StackSize").ifPresent(v -> data.stackSize = v);
        input.getInt("StoredExp").ifPresent(v -> data.storedExp = v);
        input.getInt("SpawnDelay").ifPresent(v -> data.spawnDelay = v);
        input.getLong("LastSpawnTime").ifPresent(v -> data.lastSpawnTime = v);
        input.child("Inventory").ifPresent(child -> data.inventory = VirtualInventory.fromValueInput(child));

        data.dirty = false;
        return data;
    }

    public CompoundTag toItemNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putString("EntityType", BuiltInRegistries.ENTITY_TYPE.getKey(entityType).toString());
        tag.putInt("StackSize", stackSize);
        return tag;
    }

    public static SpawnerData fromItemNbt(BlockPos pos, CompoundTag tag) {
        SpawnerData data = new SpawnerData(pos);

        if (tag.contains("EntityType")) {
            data.entityType = parseEntityType(tag.getString("EntityType").get());
        }

        if (tag.contains("StackSize")) {
            data.stackSize = tag.getInt("StackSize").get();
        }

        return data;
    }
}
