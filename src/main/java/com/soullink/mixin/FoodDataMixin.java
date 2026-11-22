package com.soullink.mixin;

import com.soullink.SharedInventoryState;
import com.soullink.SharedVitals;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;

@Mixin(FoodData.class)
public abstract class FoodDataMixin {
	@Unique
	private int soullink$lastTick = -1;

	/**
	 * Cada jugador llama a tick() desde su doTick(). Con el FoodData compartido eso lo correría una vez
	 * por jugador del grupo y por tick: la regeneración natural y el daño por inanición irían al doble de velocidad.
	 */
	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void soullink$tickOncePerServerTick(Player jugador, CallbackInfo ci) {
		SharedInventoryState state = SharedInventoryState.active();
		// En 1.21.1 tick recibe un Player: el del cliente también pasa por acá
		if (!(jugador instanceof ServerPlayer player) || state == null || !state.isShared(this)) {
			return;
		}
		// Un jugador en creativo no gasta hambre; si se quedara con el tick, la regeneración lo curaría a él
		// y no al grupo.
		if (!SharedVitals.participates(player)) {
			ci.cancel();
			return;
		}
		int now = player.level().getServer().getTickCount();
		if (this.soullink$lastTick == now) {
			ci.cancel();
			return;
		}
		this.soullink$lastTick = now;
	}

	// readAdditionalSaveData() pisa el hambre compartida con la que el jugador tenía guardada.
	@Inject(method = "readAdditionalSaveData", at = @At("HEAD"), cancellable = true)
	private void soullink$keepSharedFood(CompoundTag input, CallbackInfo ci) {
		if (SharedInventoryState.shouldBlockLoadInto(this)) {
			ci.cancel();
		}
	}
}
