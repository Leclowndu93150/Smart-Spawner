package com.leclowndu93150.smartspawner.loot;

import net.minecraft.world.item.ItemStack;

import java.util.List;

public record LootResult(List<ItemStack> items, int experience) {
}
