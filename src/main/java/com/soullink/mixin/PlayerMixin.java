package com.soullink.mixin;

import java.util.ArrayList;
import java.util.List;

import com.soullink.SharedExperience;
import com.soullink.LinkGroup;
import com.soullink.SharedInventoryState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;

@Mixin(Player.class)
public abstract class PlayerMixin {
	/**
	 * Marca que el inventario guardado en el archivo del jugador es una copia del compartido. Un archivo
	 * sin la marca es de antes de instalar el mod: ese inventario es del jugador y no se puede tirar.
	 */
	// Con el nombre de antes a propósito: ya está en el archivo de cada jugador. Si cambiara, todos parecerían de antes
	// del mod y se les devolverían otra vez sus ítems viejos en un cofre (duplicados)
	@Unique
	private static final String SHARED_MARKER = "InventarioCompartido";

	@Shadow
	protected FoodData foodData;

	/**
	 * Cada ServerPlayer que se crea (al entrar, al reaparecer tras morir, etc.) usa las listas y el FoodData de su
	 * grupo. En 1.21.1 el Inventory se crea en un inicializador de campo, antes de que Player tenga perfil (y sin
	 * perfil no se sabe el grupo): por eso el inventario también se engancha acá y no en el constructor de Inventory.
	 */
	@Inject(method = "<init>", at = @At("TAIL"))
	private void soullink$useSharedStorage(CallbackInfo ci) {
		Player self = (Player) (Object) this;
		LinkGroup group = SharedInventoryState.groupFor(self);
		if (group != null) {
			group.bind(self.getInventory());
			this.foodData = group.foodData();
		}
	}

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void soullink$markSharedSave(CompoundTag output, CallbackInfo ci) {
		if (SharedInventoryState.forPlayer((Player) (Object) this) != null) {
			output.putBoolean(SHARED_MARKER, true);
		}
	}

	/**
	 * Corre antes que la carga de vanilla, que va a descartar el inventario guardado porque ya hay uno
	 * compartido (ver InventoryMixin). Si el archivo es de antes del mod, se
	 * rescatan esos ítems para dejarlos en un cofre cuando el jugador termine de entrar.
	 */
	@Inject(method = "readAdditionalSaveData", at = @At("HEAD"))
	private void soullink$rescuePersonalItems(CompoundTag input, CallbackInfo ci) {
		Player self = (Player) (Object) this;
		SharedInventoryState state = SharedInventoryState.forPlayer(self);
		if (state == null || !state.isInitialized() || input.getBoolean(SHARED_MARKER)) {
			return;
		}
		// Su experiencia sí se carga (vanilla la lee en este mismo método); en su primer tick se suma a la compartida.
		state.addExperienceContribution(self.getGameProfile().getId());

		// En 1.21.1 la armadura y la mano secundaria van en la misma lista que el inventario (slots 100 a 103 y 150)
		List<ItemStack> items = new ArrayList<>();
		ListTag inventory = input.getList("Inventory", Tag.TAG_COMPOUND);
		for (int i = 0; i < inventory.size(); i++) {
			ItemStack.parse(self.registryAccess(), inventory.getCompound(i)).filter(stack -> !stack.isEmpty()).ifPresent(items::add);
		}
		if (!items.isEmpty()) {
			state.addRescuedItems(self.getGameProfile().getId(), items);
		}
	}

	// Sólo cuenta para los orbes que se tiran al morir: los demás del grupo ya no tienen nada que tirar.
	@Inject(method = "getBaseExperienceReward", at = @At("HEAD"), cancellable = true)
	private void soullink$dropSharedExperienceOnce(CallbackInfoReturnable<Integer> cir) {
		if ((Object) this instanceof ServerPlayer player && !SharedExperience.shouldDropExperience(player)) {
			cir.setReturnValue(0);
		}
	}
}
