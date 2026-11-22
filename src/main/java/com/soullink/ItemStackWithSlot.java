package com.soullink;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;

/**
 * Un ítem con su lugar en una lista. 1.21.1 no tiene el de vanilla (llegó en 1.21.5): éste guarda lo mismo, con los
 * campos del ítem en línea junto a {@code Slot}, así lee los archivos que se guardaron con el de vanilla.
 */
public record ItemStackWithSlot(int slot, ItemStack stack) {
	public static final Codec<ItemStackWithSlot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.BYTE.fieldOf("Slot").forGetter(item -> (byte) item.slot()),
			ItemStack.ITEM_NON_AIR_CODEC.fieldOf("id").forGetter(item -> item.stack().getItemHolder()),
			ExtraCodecs.intRange(1, 99).fieldOf("count").orElse(1).forGetter(item -> item.stack().getCount()),
			DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(item -> item.stack().getComponentsPatch())
	).apply(instance, (slot, item, count, components) -> new ItemStackWithSlot(slot & 0xFF, new ItemStack(item, count, components))));

	public boolean isValidInContainer(int size) {
		return this.slot >= 0 && this.slot < size;
	}
}
