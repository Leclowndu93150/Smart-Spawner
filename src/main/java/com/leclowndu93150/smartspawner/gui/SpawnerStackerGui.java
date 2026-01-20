package com.leclowndu93150.smartspawner.gui;

import com.leclowndu93150.smartspawner.Smartspawner;
import com.leclowndu93150.smartspawner.data.SpawnerData;
import com.leclowndu93150.smartspawner.util.SpawnerItemHelper;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class SpawnerStackerGui extends SimpleGui {
    private final BlockPos pos;
    private final SpawnerData data;
    private final SpawnerMainGui parentGui;

    public SpawnerStackerGui(ServerPlayer player, BlockPos pos, SpawnerData data, SpawnerMainGui parentGui) {
        super(MenuType.GENERIC_9x3, player, false);
        this.pos = pos;
        this.data = data;
        this.parentGui = parentGui;

        setTitle(Component.literal("Spawner Stacker"));
        setupGui();
    }

    private void setupGui() {
        for (int i = 0; i < 27; i++) {
            setSlot(i, new GuiElementBuilder(Items.GRAY_STAINED_GLASS_PANE)
                .setName(Component.empty())
                .hideDefaultTooltip()
                .build());
        }

        setSlot(4, new GuiElementBuilder(Items.SPAWNER)
            .setName(Component.literal("Current Stack").withStyle(ChatFormatting.GOLD))
            .setCount(Math.min(data.getStackSize(), 64))
            .addLoreLine(Component.literal("Stack Size: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(data.getStackSize())).withStyle(ChatFormatting.WHITE)))
            .addLoreLine(Component.literal("Maximum: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(Smartspawner.MAX_STACK_SIZE)).withStyle(ChatFormatting.WHITE)))
            .build());

        setSlot(10, new GuiElementBuilder(Items.RED_CONCRETE)
            .setName(Component.literal("-1 Spawner").withStyle(ChatFormatting.RED))
            .addLoreLine(Component.literal("Remove 1 spawner from stack").withStyle(ChatFormatting.GRAY))
            .addLoreLine(Component.literal("Requires empty inventory slot").withStyle(ChatFormatting.YELLOW))
            .setCallback((index, type, action) -> {
                removeSpawners(1);
            })
            .build());

        setSlot(11, new GuiElementBuilder(Items.RED_CONCRETE)
            .setName(Component.literal("-10 Spawners").withStyle(ChatFormatting.RED))
            .addLoreLine(Component.literal("Remove 10 spawners from stack").withStyle(ChatFormatting.GRAY))
            .addLoreLine(Component.literal("Requires empty inventory slots").withStyle(ChatFormatting.YELLOW))
            .setCallback((index, type, action) -> {
                removeSpawners(10);
            })
            .build());

        setSlot(15, new GuiElementBuilder(Items.LIME_CONCRETE)
            .setName(Component.literal("+1 Spawner").withStyle(ChatFormatting.GREEN))
            .addLoreLine(Component.literal("Add 1 spawner to stack").withStyle(ChatFormatting.GRAY))
            .addLoreLine(Component.literal("Requires spawner in inventory").withStyle(ChatFormatting.YELLOW))
            .setCallback((index, type, action) -> {
                addSpawners(1);
            })
            .build());

        setSlot(16, new GuiElementBuilder(Items.LIME_CONCRETE)
            .setName(Component.literal("+10 Spawners").withStyle(ChatFormatting.GREEN))
            .addLoreLine(Component.literal("Add 10 spawners to stack").withStyle(ChatFormatting.GRAY))
            .addLoreLine(Component.literal("Requires spawners in inventory").withStyle(ChatFormatting.YELLOW))
            .setCallback((index, type, action) -> {
                addSpawners(10);
            })
            .build());

        setSlot(22, new GuiElementBuilder(Items.BARRIER)
            .setName(Component.literal("Back").withStyle(ChatFormatting.RED))
            .setCallback((index, type, action) -> {
                parentGui.refresh();
                parentGui.open();
            })
            .build());
    }

    private void removeSpawners(int amount) {
        if (data.getStackSize() <= 1) {
            player.displayClientMessage(
                Component.literal("Cannot remove last spawner from stack!").withStyle(ChatFormatting.RED),
                true
            );
            return;
        }

        int toRemove = Math.min(amount, data.getStackSize() - 1);
        int given = 0;

        for (int i = 0; i < toRemove; i++) {
            ItemStack spawnerItem = SpawnerItemHelper.createSpawnerItem(data.getEntityType(), 1);
            if (player.getInventory().add(spawnerItem)) {
                given++;
            } else {
                player.drop(spawnerItem, false);
                given++;
            }
        }

        if (given > 0) {
            data.removeFromStack(given);
            player.displayClientMessage(
                Component.literal("Removed " + given + " spawner(s) from stack").withStyle(ChatFormatting.GREEN),
                true
            );
            setupGui();
        }
    }

    private void addSpawners(int amount) {
        if (data.getStackSize() >= Smartspawner.MAX_STACK_SIZE) {
            player.displayClientMessage(
                Component.literal("Stack is at maximum size!").withStyle(ChatFormatting.RED),
                true
            );
            return;
        }

        int added = 0;
        int maxToAdd = Math.min(amount, Smartspawner.MAX_STACK_SIZE - data.getStackSize());

        for (int i = 0; i < player.getInventory().getContainerSize() && added < maxToAdd; i++) {
            ItemStack stack = player.getInventory().getItem(i);

            if (stack.is(Items.SPAWNER)) {
                if (SpawnerItemHelper.isSmartSpawner(stack)) {
                    if (SpawnerItemHelper.getEntityType(stack) == data.getEntityType()) {
                        int stackAmount = SpawnerItemHelper.getStackSize(stack);
                        int toAdd = Math.min(stackAmount, maxToAdd - added);

                        if (toAdd == stackAmount) {
                            player.getInventory().removeItem(i, 1);
                            i--;
                        } else {
                            continue;
                        }

                        added += toAdd;
                    }
                } else {
                    int toTake = Math.min(stack.getCount(), maxToAdd - added);
                    player.getInventory().removeItem(i, toTake);
                    added += toTake;
                    if (player.getInventory().getItem(i).isEmpty()) {
                        i--;
                    }
                }
            }
        }

        if (added > 0) {
            data.addToStack(added);
            player.displayClientMessage(
                Component.literal("Added " + added + " spawner(s) to stack").withStyle(ChatFormatting.GREEN),
                true
            );
            setupGui();
        } else {
            player.displayClientMessage(
                Component.literal("No compatible spawners found in inventory!").withStyle(ChatFormatting.RED),
                true
            );
        }
    }
}
