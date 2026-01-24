package com.leclowndu93150.smartspawner.mixin;

import com.leclowndu93150.smartspawner.data.SpawnerData;
import com.leclowndu93150.smartspawner.data.SpawnerDataManager;
import com.leclowndu93150.smartspawner.gui.SpawnerMainGui;
import com.leclowndu93150.smartspawner.util.SpawnerItemHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SpawnerBlock;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.entity.LivingEntity;
import java.util.List;

@Mixin(SpawnerBlock.class)
public abstract class SpawnerBlockMixin extends Block {

    public SpawnerBlockMixin(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        SpawnerData data = SpawnerDataManager.get(serverLevel, pos);
        if (data == null) {
            data = SpawnerDataManager.getOrCreate(serverLevel, pos);

            if (level.getBlockEntity(pos) instanceof SpawnerBlockEntity spawnerBlockEntity) {
                EntityType<?> entityType = getSpawnerEntityType(spawnerBlockEntity);
                data.setEntityType(entityType);
            }
        }

        new SpawnerMainGui(serverPlayer, pos, data).open();
        return InteractionResult.CONSUME;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (level instanceof ServerLevel serverLevel && SpawnerItemHelper.isSmartSpawner(stack)) {
            EntityType<?> entityType = SpawnerItemHelper.getEntityType(stack);
            int stackSize = SpawnerItemHelper.getStackSize(stack);

            SpawnerData data = SpawnerDataManager.getOrCreate(serverLevel, pos);
            data.setEntityType(entityType);
            data.setStackSize(stackSize);

            if (level.getBlockEntity(pos) instanceof SpawnerBlockEntity spawnerBlockEntity) {
                spawnerBlockEntity.setEntityId(entityType, level.getRandom());
            }
        }
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        ItemStack tool = builder.getOptionalParameter(LootContextParams.TOOL);
        if (tool == null) {
            return super.getDrops(state, builder);
        }

        ServerLevel level = builder.getLevel();
        var enchantmentRegistry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var silkTouchHolder = enchantmentRegistry.getOrThrow(Enchantments.SILK_TOUCH);
        boolean hasSilkTouch = EnchantmentHelper.getItemEnchantmentLevel(silkTouchHolder, tool) > 0;

        if (hasSilkTouch) {
            BlockPos pos = BlockPos.containing(builder.getOptionalParameter(LootContextParams.ORIGIN));
            SpawnerData data = SpawnerDataManager.get(level, pos);

            ItemStack spawnerItem;
            if (data != null) {
                spawnerItem = SpawnerItemHelper.createSpawnerItem(data);
            } else {
                var blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
                if (blockEntity instanceof SpawnerBlockEntity spawnerBlockEntity) {
                    EntityType<?> entityType = getSpawnerEntityType(spawnerBlockEntity);
                    spawnerItem = SpawnerItemHelper.createSpawnerItem(entityType, 1);
                } else {
                    spawnerItem = SpawnerItemHelper.createSpawnerItem(EntityType.PIG, 1);
                }
            }

            return List.of(spawnerItem);
        }

        return super.getDrops(state, builder);
    }

    @Inject(method = "spawnAfterBreak", at = @At("TAIL"))
    private void smartspawner$spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean dropExperience, CallbackInfo ci) {
        SpawnerDataManager.remove(level, pos);
    }

    private static EntityType<?> getSpawnerEntityType(SpawnerBlockEntity blockEntity) {
        try {
            var spawner = blockEntity.getSpawner();
            var level = blockEntity.getLevel();
            var pos = blockEntity.getBlockPos();

            if (level != null) {
                var displayEntity = spawner.getOrCreateDisplayEntity(level, pos);
                if (displayEntity != null) {
                    return displayEntity.getType();
                }
            }
        } catch (Exception ignored) {
        }
        return EntityType.PIG;
    }
}
