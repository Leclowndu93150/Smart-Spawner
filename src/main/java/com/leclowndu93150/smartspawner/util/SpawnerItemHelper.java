package com.leclowndu93150.smartspawner.util;

import com.leclowndu93150.smartspawner.data.SpawnerData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class SpawnerItemHelper {
    public static final String SMART_SPAWNER_TAG = "SmartSpawner";

    public static ItemStack createSpawnerItem(SpawnerData data) {
        return createSpawnerItem(data.getEntityType(), data.getStackSize());
    }

    public static ItemStack createSpawnerItem(EntityType<?> entityType, int stackSize) {
        ItemStack stack = new ItemStack(Items.SPAWNER);

        CompoundTag tag = new CompoundTag();
        tag.putString("EntityType", BuiltInRegistries.ENTITY_TYPE.getKey(entityType).toString());
        tag.putInt("StackSize", stackSize);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(wrapTag(tag)));

        CompoundTag blockEntityTag = new CompoundTag();
        CompoundTag spawnData = new CompoundTag();
        CompoundTag entity = new CompoundTag();
        entity.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(entityType).toString());
        spawnData.put("entity", entity);
        blockEntityTag.put("SpawnData", spawnData);
        stack.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(BlockEntityType.MOB_SPAWNER, blockEntityTag));

        return stack;
    }

    public static boolean isSmartSpawner(ItemStack stack) {
        if (!stack.is(Items.SPAWNER)) return false;

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return false;

        return customData.copyTag().contains(SMART_SPAWNER_TAG);
    }

    public static CompoundTag getSmartSpawnerTag(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return null;

        CompoundTag tag = customData.copyTag();
        if (!tag.contains(SMART_SPAWNER_TAG)) return null;

        return tag.getCompound(SMART_SPAWNER_TAG).get();
    }

    public static EntityType<?> getEntityType(ItemStack stack) {
        CompoundTag tag = getSmartSpawnerTag(stack);
        if (tag == null || !tag.contains("EntityType")) {
            return EntityType.PIG;
        }

        ResourceLocation entityId = ResourceLocation.parse(tag.getString("EntityType").get());
        var optional = BuiltInRegistries.ENTITY_TYPE.get(entityId);
        if (optional.isPresent()) {
            return optional.get().value();
        }
        return EntityType.PIG;
    }

    public static int getStackSize(ItemStack stack) {
        CompoundTag tag = getSmartSpawnerTag(stack);
        if (tag == null || !tag.contains("StackSize")) {
            return 1;
        }

        return tag.getInt("StackSize").get();
    }

    private static CompoundTag wrapTag(CompoundTag inner) {
        CompoundTag outer = new CompoundTag();
        outer.put(SMART_SPAWNER_TAG, inner);
        return outer;
    }
}
