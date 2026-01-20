package com.leclowndu93150.smartspawner.mixin;

import com.leclowndu93150.smartspawner.Smartspawner;
import com.leclowndu93150.smartspawner.data.SpawnerData;
import com.leclowndu93150.smartspawner.data.SpawnerDataManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin {

    @Inject(method = "suckInItems", at = @At("HEAD"), cancellable = true)
    private static void smartspawner$suckInItems(Level level, Hopper hopper, CallbackInfoReturnable<Boolean> cir) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        BlockPos hopperPos = BlockPos.containing(hopper.getLevelX(), hopper.getLevelY(), hopper.getLevelZ());
        BlockPos abovePos = hopperPos.above();

        SpawnerData spawnerData = SpawnerDataManager.get(serverLevel, abovePos);
        if (spawnerData == null) return;

        if (spawnerData.getInventory().isEmpty()) return;

        if (hopper instanceof HopperBlockEntity hopperBlockEntity) {
            ItemStack extracted = spawnerData.getInventory().removeFirstStack(Smartspawner.HOPPER_TRANSFER_AMOUNT);

            if (!extracted.isEmpty()) {
                ItemStack remaining = HopperBlockEntity.addItem(null, hopperBlockEntity, extracted, null);

                if (!remaining.isEmpty()) {
                    spawnerData.getInventory().addItem(remaining);
                }

                spawnerData.markDirty();
                cir.setReturnValue(true);
                return;
            }
        }
    }
}
