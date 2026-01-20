package com.leclowndu93150.smartspawner.loot;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LootGenerator {

    public static LootResult generateLoot(EntityType<?> entityType, ServerLevel level, BlockPos pos, int minMobs, int maxMobs) {
        List<ItemStack> items = new ArrayList<>();
        int totalExp = 0;

        int mobCount = minMobs + level.getRandom().nextInt(Math.max(1, maxMobs - minMobs + 1));

        Optional<ResourceKey<LootTable>> lootTableKey = entityType.getDefaultLootTable();

        if (lootTableKey.isEmpty()) {
            return new LootResult(items, getExperienceForMobs(entityType, mobCount, level));
        }

        LootTable lootTable = level.getServer().reloadableRegistries().getLootTable(lootTableKey.get());

        Entity tempEntity = entityType.create(level, EntitySpawnReason.SPAWNER);
        if (tempEntity == null) {
            return new LootResult(items, getExperienceForMobs(entityType, mobCount, level));
        }

        tempEntity.setPos(Vec3.atCenterOf(pos));

        try {
            for (int i = 0; i < mobCount; i++) {
                LootParams.Builder builder = new LootParams.Builder(level)
                    .withParameter(LootContextParams.THIS_ENTITY, tempEntity)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                    .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().generic());

                LootParams lootParams = builder.create(LootContextParamSets.ENTITY);

                lootTable.getRandomItems(lootParams, level.getRandom().nextLong(), items::add);

                totalExp += getBaseExperience(entityType);
            }
        } finally {
            tempEntity.discard();
        }

        return new LootResult(mergeStacks(items), totalExp);
    }

    private static int getBaseExperience(EntityType<?> entityType) {
        if (entityType == EntityType.ENDER_DRAGON) return 500;
        if (entityType == EntityType.WITHER) return 50;
        if (entityType == EntityType.ELDER_GUARDIAN) return 10;
        if (entityType == EntityType.RAVAGER) return 20;
        if (entityType == EntityType.PIGLIN_BRUTE) return 20;
        if (entityType == EntityType.EVOKER) return 10;
        if (entityType == EntityType.VINDICATOR) return 5;
        if (entityType == EntityType.BLAZE) return 10;
        if (entityType == EntityType.GUARDIAN) return 10;
        if (entityType == EntityType.MAGMA_CUBE) return 4;
        if (entityType == EntityType.SLIME) return 4;
        return 5;
    }

    private static int getExperienceForMobs(EntityType<?> entityType, int count, ServerLevel level) {
        int base = getBaseExperience(entityType);
        int total = 0;
        for (int i = 0; i < count; i++) {
            total += base + level.getRandom().nextInt(Math.max(1, base / 2));
        }
        return total;
    }

    private static List<ItemStack> mergeStacks(List<ItemStack> items) {
        List<ItemStack> merged = new ArrayList<>();

        for (ItemStack stack : items) {
            if (stack.isEmpty()) continue;

            boolean found = false;
            for (ItemStack existing : merged) {
                if (ItemStack.isSameItemSameComponents(existing, stack)) {
                    int maxSize = existing.getMaxStackSize();
                    int toAdd = Math.min(stack.getCount(), maxSize - existing.getCount());
                    existing.grow(toAdd);
                    stack.shrink(toAdd);

                    if (stack.isEmpty()) {
                        found = true;
                        break;
                    }
                }
            }

            if (!found && !stack.isEmpty()) {
                merged.add(stack.copy());
            }
        }

        return merged;
    }
}
