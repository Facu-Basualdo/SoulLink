package com.soullink.mixin.trinkets;

import com.soullink.SharedInventoryState;
import com.soullink.trinkets.SharedTrinkets;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/** Marca que los trinkets guardados en el archivo del jugador son una copia de los compartidos (ver SharedTrinkets.afterLoad). */
@Mixin(Player.class)
public abstract class TrinketsSaveMixin {
	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void soullink$markSharedTrinkets(CompoundTag output, CallbackInfo ci) {
		if (SharedInventoryState.forPlayer((Player) (Object) this) != null) {
			output.putBoolean(SharedTrinkets.SHARED_MARKER, true);
		}
	}
}
