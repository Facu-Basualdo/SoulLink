package com.soullink.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.soullink.SharedInventoryState;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

@Mixin(Inventory.class)
public abstract class InventoryMixin {
	@Shadow
	@Final
	public NonNullList<ItemStack> items;

	// load() vacía las tres listas antes de rellenarlas con lo que el jugador tenía guardado.
	@Inject(method = "load", at = @At("HEAD"), cancellable = true)
	private void soullink$keepSharedItems(ListTag input, CallbackInfo ci) {
		if (SharedInventoryState.shouldBlockLoadInto(this.items)) {
			ci.cancel();
		}
	}
}
