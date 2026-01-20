package com.leclowndu93150.smartspawner.mixin;

import com.leclowndu93150.smartspawner.data.SpawnerData;
import com.leclowndu93150.smartspawner.data.SpawnerDataManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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
    private void smartspawner$loadAdditional(CompoundTag tag, HolderLookup.Provider provider, CallbackInfo ci) {
        if (tag.contains("SmartSpawner")) {
            if (this.level instanceof ServerLevel serverLevel) {
                SpawnerData data = SpawnerData.fromNbt(this.worldPosition, tag.getCompound("SmartSpawner"), provider);
                SpawnerDataManager.put(serverLevel, this.worldPosition, data);
            }
        }
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void smartspawner$saveAdditional(CompoundTag tag, HolderLookup.Provider provider, CallbackInfo ci) {
        if (this.level instanceof ServerLevel serverLevel) {
            SpawnerData data = SpawnerDataManager.get(serverLevel, this.worldPosition);
            if (data != null) {
                tag.put("SmartSpawner", data.toNbt(provider));
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
