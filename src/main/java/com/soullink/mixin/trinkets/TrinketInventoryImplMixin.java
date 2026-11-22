package com.soullink.mixin.trinkets;

import com.soullink.trinkets.SharedTrinkets;
import dev.emi.trinkets.api.TrinketInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cada slot de Trinkets de un jugador vinculado usa las listas de su grupo. Trinkets crea las listas en el constructor y
 * las reemplaza en update() cuando cambia la cantidad de lugares: en los dos casos se vuelven a enganchar.
 */
@Mixin(TrinketInventory.class)
public abstract class TrinketInventoryImplMixin {
	@Inject(method = "<init>", at = @At("RETURN"))
	private void soullink$shareOnCreate(CallbackInfo ci) {
		SharedTrinkets.bind((TrinketInventory) (Object) this);
	}

	@Inject(method = "update", at = @At("RETURN"))
	private void soullink$shareOnResize(CallbackInfo ci) {
		SharedTrinkets.bind((TrinketInventory) (Object) this);
	}
}
