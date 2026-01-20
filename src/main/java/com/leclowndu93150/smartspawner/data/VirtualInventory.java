package com.leclowndu93150.smartspawner.data;

import com.leclowndu93150.smartspawner.Smartspawner;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class VirtualInventory {
    private final Map<ItemSignature, Long> items = new HashMap<>();
    private List<ItemStack> displayCache = null;
    private boolean cacheValid = false;

    public void addItem(ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemSignature sig = new ItemSignature(stack);
        items.merge(sig, (long) stack.getCount(), Long::sum);
        invalidateCache();
    }

    public void addItems(List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            addItem(stack);
        }
    }

    public ItemStack removeItem(ItemSignature sig, int amount) {
        Long current = items.get(sig);
        if (current == null || current == 0) {
            return ItemStack.EMPTY;
        }

        int toRemove = (int) Math.min(amount, current);
        long remaining = current - toRemove;

        if (remaining <= 0) {
            items.remove(sig);
        } else {
            items.put(sig, remaining);
        }

        invalidateCache();
        return sig.toItemStack(toRemove);
    }

    public ItemStack removeFirstStack(int maxAmount) {
        if (items.isEmpty()) return ItemStack.EMPTY;

        Map.Entry<ItemSignature, Long> first = items.entrySet().iterator().next();
        return removeItem(first.getKey(), maxAmount);
    }

    public List<ItemStack> getDisplayInventory() {
        if (cacheValid && displayCache != null) {
            return displayCache;
        }

        displayCache = new ArrayList<>();
        for (Map.Entry<ItemSignature, Long> entry : items.entrySet()) {
            ItemSignature sig = entry.getKey();
            long count = entry.getValue();

            while (count > 0) {
                int stackSize = (int) Math.min(count, sig.getMaxStackSize());
                displayCache.add(sig.toItemStack(stackSize));
                count -= stackSize;
            }
        }
        cacheValid = true;
        return displayCache;
    }

    public int getUsedSlots() {
        int slots = 0;
        for (Map.Entry<ItemSignature, Long> entry : items.entrySet()) {
            long count = entry.getValue();
            int maxStack = entry.getKey().getMaxStackSize();
            slots += (int) Math.ceil((double) count / maxStack);
        }
        return slots;
    }

    public long getTotalItemCount() {
        return items.values().stream().mapToLong(Long::longValue).sum();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public void clear() {
        items.clear();
        invalidateCache();
    }

    private void invalidateCache() {
        cacheValid = false;
        displayCache = null;
    }

    public CompoundTag toNbt(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        ListTag itemsList = new ListTag();

        for (Map.Entry<ItemSignature, Long> entry : items.entrySet()) {
            CompoundTag itemTag = new CompoundTag();
            itemTag.putString("Item", entry.getKey().getItemId());

            DataComponentPatch components = entry.getKey().getComponents();
            if (!components.isEmpty()) {
                Tag componentsTag = DataComponentPatch.CODEC.encodeStart(
                    provider.createSerializationContext(NbtOps.INSTANCE),
                    components
                ).result().orElse(null);
                if (componentsTag != null) {
                    itemTag.put("Components", componentsTag);
                }
            }

            itemTag.putLong("Count", entry.getValue());
            itemsList.add(itemTag);
        }

        tag.put("Items", itemsList);
        return tag;
    }

    public static VirtualInventory fromNbt(CompoundTag tag, HolderLookup.Provider provider) {
        VirtualInventory inventory = new VirtualInventory();

        if (!tag.contains("Items")) return inventory;

        ListTag itemsList = tag.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < itemsList.size(); i++) {
            CompoundTag itemTag = itemsList.getCompound(i);
            String itemId = itemTag.getString("Item");
            long count = itemTag.getLong("Count");

            DataComponentPatch components = DataComponentPatch.EMPTY;
            if (itemTag.contains("Components")) {
                components = DataComponentPatch.CODEC.parse(
                    provider.createSerializationContext(NbtOps.INSTANCE),
                    itemTag.get("Components")
                ).result().orElse(DataComponentPatch.EMPTY);
            }

            ItemSignature sig = new ItemSignature(itemId, components);
            inventory.items.put(sig, count);
        }

        return inventory;
    }

    public static class ItemSignature {
        private final String itemId;
        private final DataComponentPatch components;
        private final int hashCode;

        public ItemSignature(ItemStack stack) {
            this.itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            this.components = stack.getComponentsPatch();
            this.hashCode = Objects.hash(itemId, components);
        }

        public ItemSignature(String itemId, DataComponentPatch components) {
            this.itemId = itemId;
            this.components = components;
            this.hashCode = Objects.hash(itemId, components);
        }

        public String getItemId() {
            return itemId;
        }

        public DataComponentPatch getComponents() {
            return components;
        }

        private Item getItem() {
            var optional = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
            if (optional.isPresent()) {
                return optional.get().value();
            }
            return Items.AIR;
        }

        public int getMaxStackSize() {
            return getItem().getDefaultMaxStackSize();
        }

        public ItemStack toItemStack(int count) {
            ItemStack stack = new ItemStack(getItem(), count);
            stack.applyComponents(components);
            return stack;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ItemSignature that = (ItemSignature) o;
            return Objects.equals(itemId, that.itemId) && Objects.equals(components, that.components);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }
}
