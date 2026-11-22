package com.soullink.mixin.trinkets;

import dev.emi.trinkets.api.TrinketInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

@Mixin(TrinketInventory.class)
public interface TrinketInventoryAccessor {
	@Accessor("stacks")
	NonNullList<ItemStack> soullink$getStacks();

	@Accessor("stacks")
	void soullink$setStacks(NonNullList<ItemStack> stacks);
}
