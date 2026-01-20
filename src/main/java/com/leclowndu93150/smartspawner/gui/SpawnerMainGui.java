package com.leclowndu93150.smartspawner.gui;

import com.leclowndu93150.smartspawner.data.SpawnerData;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;

public class SpawnerMainGui extends SimpleGui {
    private final BlockPos pos;
    private final SpawnerData data;

    public SpawnerMainGui(ServerPlayer player, BlockPos pos, SpawnerData data) {
        super(MenuType.GENERIC_9x3, player, false);
        this.pos = pos;
        this.data = data;

        setTitle(Component.literal("Smart Spawner"));
        setupGui();
    }

    private void setupGui() {
        for (int i = 0; i < 27; i++) {
            setSlot(i, new GuiElementBuilder(Items.GRAY_STAINED_GLASS_PANE)
                .setName(Component.empty())
                .hideDefaultTooltip()
                .build());
        }

        String entityName = BuiltInRegistries.ENTITY_TYPE.getKey(data.getEntityType()).getPath();
        entityName = entityName.substring(0, 1).toUpperCase() + entityName.substring(1).replace("_", " ");

        int usedSlots = data.getInventory().getUsedSlots();
        int maxSlots = data.getMaxInventorySlots();
        int totalPages = (int) Math.ceil((double) maxSlots / 45);

        setSlot(4, new GuiElementBuilder(Items.SPAWNER)
            .setName(Component.literal("Spawner Info").withStyle(ChatFormatting.GOLD))
            .addLoreLine(Component.literal("Entity: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(entityName).withStyle(ChatFormatting.WHITE)))
            .addLoreLine(Component.literal("Stack Size: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(data.getStackSize())).withStyle(ChatFormatting.WHITE)))
            .addLoreLine(Component.literal("Storage: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(usedSlots + "/" + maxSlots + " slots").withStyle(ChatFormatting.WHITE)))
            .addLoreLine(Component.literal("Storage Pages: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(totalPages)).withStyle(ChatFormatting.WHITE)))
            .addLoreLine(Component.literal("Stored Items: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(data.getInventory().getTotalItemCount())).withStyle(ChatFormatting.WHITE)))
            .addLoreLine(Component.literal("Stored XP: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(data.getStoredExp() + "/" + data.getMaxStoredExp()).withStyle(ChatFormatting.GREEN)))
            .build());

        setSlot(11, new GuiElementBuilder(Items.CHEST)
            .setName(Component.literal("Storage").withStyle(ChatFormatting.YELLOW))
            .addLoreLine(Component.literal("Click to browse stored items").withStyle(ChatFormatting.GRAY))
            .addLoreLine(Component.literal("Used: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(usedSlots + "/" + maxSlots + " slots").withStyle(ChatFormatting.WHITE)))
            .addLoreLine(Component.literal("Items: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(data.getInventory().getTotalItemCount())).withStyle(ChatFormatting.WHITE)))
            .setCallback((index, type, action) -> {
                new SpawnerStorageGui(player, pos, data).open();
            })
            .build());

        int storedExp = data.getStoredExp();
        int maxExp = data.getMaxStoredExp();
        setSlot(13, new GuiElementBuilder(Items.EXPERIENCE_BOTTLE)
            .setName(Component.literal("Collect Experience").withStyle(ChatFormatting.GREEN))
            .addLoreLine(Component.literal("Stored XP: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(storedExp + "/" + maxExp).withStyle(ChatFormatting.GREEN)))
            .addLoreLine(Component.empty())
            .addLoreLine(Component.literal("Click to collect all XP").withStyle(ChatFormatting.YELLOW))
            .setCallback((index, type, action) -> {
                int exp = data.claimExperience();
                if (exp > 0) {
                    player.giveExperiencePoints(exp);
                    player.displayClientMessage(
                        Component.literal("Collected " + exp + " experience!").withStyle(ChatFormatting.GREEN),
                        true
                    );
                    setupGui();
                }
            })
            .build());

        setSlot(15, new GuiElementBuilder(Items.SPAWNER)
            .setName(Component.literal("Spawner Stacker").withStyle(ChatFormatting.LIGHT_PURPLE))
            .addLoreLine(Component.literal("Current Stack: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(data.getStackSize())).withStyle(ChatFormatting.WHITE)))
            .addLoreLine(Component.empty())
            .addLoreLine(Component.literal("Click to manage stack size").withStyle(ChatFormatting.YELLOW))
            .setCallback((index, type, action) -> {
                new SpawnerStackerGui(player, pos, data, this).open();
            })
            .build());
    }

    public void refresh() {
        setupGui();
    }
}
