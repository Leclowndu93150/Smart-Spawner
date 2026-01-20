package com.leclowndu93150.smartspawner.gui;

import com.leclowndu93150.smartspawner.data.SpawnerData;
import com.leclowndu93150.smartspawner.data.VirtualInventory;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public class SpawnerStorageGui extends SimpleGui {
    private static final int ITEMS_PER_PAGE = 45;

    private final BlockPos pos;
    private final SpawnerData data;
    private int currentPage = 0;

    public SpawnerStorageGui(ServerPlayer player, BlockPos pos, SpawnerData data) {
        super(MenuType.GENERIC_9x6, player, false);
        this.pos = pos;
        this.data = data;

        updateTitle();
        setupGui();
    }

    private void updateTitle() {
        int maxPages = data.getStackSize();
        setTitle(Component.literal("Spawner Storage - Page " + (currentPage + 1) + "/" + maxPages));
    }

    private void setupGui() {
        for (int i = 0; i < 54; i++) {
            clearSlot(i);
        }

        List<ItemStack> displayItems = data.getInventory().getDisplayInventory();
        int maxPages = data.getStackSize();
        int totalItemPages = Math.max(1, (int) Math.ceil((double) displayItems.size() / ITEMS_PER_PAGE));

        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, displayItems.size());

        for (int i = startIndex; i < endIndex; i++) {
            int slot = i - startIndex;
            ItemStack stack = displayItems.get(i);

            setSlot(slot, new GuiElementBuilder(stack.getItem())
                .setCount(Math.min(stack.getCount(), 64))
                .setName(stack.getHoverName())
                .addLoreLine(Component.literal("Amount: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(String.valueOf(stack.getCount())).withStyle(ChatFormatting.WHITE)))
                .addLoreLine(Component.empty())
                .addLoreLine(Component.literal("Click to take a stack").withStyle(ChatFormatting.YELLOW))
                .setCallback((index, type, action) -> {
                    takeItem(startIndex + index);
                })
                .build());
        }

        for (int i = 45; i < 54; i++) {
            setSlot(i, new GuiElementBuilder(Items.GRAY_STAINED_GLASS_PANE)
                .setName(Component.empty())
                .hideDefaultTooltip()
                .build());
        }

        int usedSlots = data.getInventory().getUsedSlots();
        int maxSlots = data.getMaxInventorySlots();

        setSlot(47, new GuiElementBuilder(Items.PAPER)
            .setName(Component.literal("Storage Info").withStyle(ChatFormatting.AQUA))
            .addLoreLine(Component.literal("Used: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(usedSlots + "/" + maxSlots + " slots").withStyle(ChatFormatting.WHITE)))
            .addLoreLine(Component.literal("Total Items: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(data.getInventory().getTotalItemCount())).withStyle(ChatFormatting.WHITE)))
            .addLoreLine(Component.literal("Pages Available: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(maxPages)).withStyle(ChatFormatting.WHITE)))
            .build());

        if (currentPage > 0) {
            setSlot(45, new GuiElementBuilder(Items.ARROW)
                .setName(Component.literal("Previous Page").withStyle(ChatFormatting.YELLOW))
                .addLoreLine(Component.literal("Page " + currentPage + "/" + maxPages).withStyle(ChatFormatting.GRAY))
                .setCallback((index, type, action) -> {
                    currentPage--;
                    updateTitle();
                    setupGui();
                })
                .build());
        }

        setSlot(49, new GuiElementBuilder(Items.BARRIER)
            .setName(Component.literal("Back").withStyle(ChatFormatting.RED))
            .setCallback((index, type, action) -> {
                new SpawnerMainGui(player, pos, data).open();
            })
            .build());

        if (!displayItems.isEmpty()) {
            setSlot(51, new GuiElementBuilder(Items.HOPPER)
                .setName(Component.literal("Take All").withStyle(ChatFormatting.GREEN))
                .addLoreLine(Component.literal("Take all items to your inventory").withStyle(ChatFormatting.GRAY))
                .setCallback((index, type, action) -> {
                    takeAll();
                })
                .build());
        }

        if (currentPage < maxPages - 1) {
            setSlot(53, new GuiElementBuilder(Items.ARROW)
                .setName(Component.literal("Next Page").withStyle(ChatFormatting.YELLOW))
                .addLoreLine(Component.literal("Page " + (currentPage + 2) + "/" + maxPages).withStyle(ChatFormatting.GRAY))
                .setCallback((index, type, action) -> {
                    currentPage++;
                    updateTitle();
                    setupGui();
                })
                .build());
        }
    }

    private void takeItem(int displayIndex) {
        List<ItemStack> displayItems = data.getInventory().getDisplayInventory();
        if (displayIndex >= displayItems.size()) return;

        ItemStack displayStack = displayItems.get(displayIndex);
        VirtualInventory.ItemSignature sig = new VirtualInventory.ItemSignature(displayStack);

        int maxTake = Math.min(displayStack.getMaxStackSize(), displayStack.getCount());
        ItemStack taken = data.getInventory().removeItem(sig, maxTake);

        if (!taken.isEmpty()) {
            if (!player.getInventory().add(taken)) {
                player.drop(taken, false);
            }
            data.markDirty();
            setupGui();
        }
    }

    private void takeAll() {
        VirtualInventory inventory = data.getInventory();

        while (!inventory.isEmpty()) {
            ItemStack stack = inventory.removeFirstStack(64);
            if (stack.isEmpty()) break;

            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
                break;
            }
        }

        data.markDirty();
        setupGui();
    }
}
