package com.leclowndu93150.smartspawner.mixin;

import com.leclowndu93150.smartspawner.data.SpawnerData;
import com.leclowndu93150.smartspawner.data.SpawnerDataManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpawnerBlockEntity.class)
public abstract class SpawnerBlockEntityMixin extends BlockEntity {

    public SpawnerBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void smartspawner$loadAdditional(ValueInput input, CallbackInfo ci) {
        input.child("SmartSpawner").ifPresent(child -> {
            if (this.level instanceof ServerLevel serverLevel) {
                SpawnerData data = SpawnerData.fromValueInput(this.worldPosition, child);
                SpawnerDataManager.put(serverLevel, this.worldPosition, data);
            }
        });
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void smartspawner$saveAdditional(ValueOutput output, CallbackInfo ci) {
        if (this.level instanceof ServerLevel serverLevel) {
            SpawnerData data = SpawnerDataManager.get(serverLevel, this.worldPosition);
            if (data != null) {
                data.toValueOutput(output.child("SmartSpawner"));
            }
        }
    }

    @Inject(method = "serverTick", at = @At("HEAD"), cancellable = true)
    private static void smartspawner$serverTick(Level level, BlockPos pos, BlockState state, SpawnerBlockEntity blockEntity, CallbackInfo ci) {
        if (level instanceof ServerLevel serverLevel) {
            SpawnerData data = SpawnerDataManager.get(serverLevel, pos);
            if (data != null) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "setEntityId", at = @At("TAIL"))
    private void smartspawner$setEntityId(EntityType<?> entityType, RandomSource random, CallbackInfo ci) {
        if (this.level instanceof ServerLevel serverLevel) {
            SpawnerData data = SpawnerDataManager.get(serverLevel, this.worldPosition);
            if (data != null) {
                data.setEntityType(entityType);
            }
        }
    }
}
