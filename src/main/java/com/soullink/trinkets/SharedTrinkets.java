package com.soullink.trinkets;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.soullink.LinkGroup;
import com.soullink.LinkGroups;
import com.soullink.SharedInventoryState;
import com.soullink.TrinketSlotLists;
import com.soullink.mixin.trinkets.TrinketInventoryAccessor;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/**
 * Los slots de Trinkets (Trinkets Updated) se comparten como el inventario: cada {@link TrinketInventory} de un jugador
 * del grupo apunta a la misma lista de stacks ({@link TrinketSlotLists} del {@link LinkGroup}). Esta clase sólo se
 * carga si Trinkets está instalado (ver TrinketsMixinPlugin y {@link #init()}).
 *
 * <p>En 1.21.x Trinkets es la API vieja ({@code dev.emi.trinkets}): una sola lista por slot, sin la cosmética de la
 * versión de 26.x. La lista cosmética de {@link TrinketSlotLists} queda vacía, así el formato guardado es el mismo.
 */
public final class SharedTrinkets {
	/** Marca en el archivo del jugador: los trinkets guardados son una copia de los compartidos. */
	public static final String SHARED_MARKER = "SoulLinkTrinkets";

	private SharedTrinkets() {
	}

	/** Lo llama SoulLink.onInitialize sólo con Trinkets instalado. */
	public static void init() {
		// Después de rescatar los ítems del primero que entra: desde acá los trinkets guardados de cada uno se descartan
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> SharedInventoryState.get(server).markTrinketsInitialized());
		LinkGroups.onChange(SharedTrinkets::bindAll);
	}

	/**
	 * Engancha un slot de Trinkets del jugador a las listas de su grupo. Si el grupo todavía no tiene ese slot, las
	 * listas del jugador pasan a ser las compartidas. Corre al crear el inventario, cada vez que Trinkets lo
	 * redimensiona ({@code update}) y al terminar de construir el ServerPlayer.
	 */
	public static void bind(TrinketInventory inventory) {
		TrinketComponent component = inventory.getComponent();
		// En el constructor de LivingEntity el jugador todavía no tiene perfil: se engancha al final del de ServerPlayer
		if (component == null || !(component.getEntity() instanceof ServerPlayer player) || player.getGameProfile() == null) {
			return;
		}
		LinkGroup group = SharedInventoryState.groupFor(player);
		if (group == null) {
			return;
		}
		TrinketInventoryAccessor accessor = (TrinketInventoryAccessor) (Object) inventory;
		String id = inventory.getSlotType().getId();
		TrinketSlotLists shared = group.trinketSlot(id);
		if (shared == null) {
			SharedInventoryState state = SharedInventoryState.forPlayer(player);
			if (state != null && isSharedByAnyGroup(state, accessor.soullink$getStacks())) {
				// Viene de otro grupo (/soullink grupo): el nuevo arranca vacío, lo de antes se queda con el grupo viejo
				shared = new TrinketSlotLists(NonNullList.withSize(accessor.soullink$getStacks().size(), ItemStack.EMPTY), NonNullList.create());
				group.putTrinketSlot(id, shared);
			} else {
				group.putTrinketSlot(id, new TrinketSlotLists(accessor.soullink$getStacks(), NonNullList.create()));
				return;
			}
		}
		if (accessor.soullink$getStacks() == shared.stacks()) {
			return;
		}
		int size = accessor.soullink$getStacks().size();
		if (shared.stacks().size() != size) {
			// Cambió la cantidad de lugares del slot (datapack o modificadores): se rearma con el tamaño nuevo y lo que
			// no entra cae a los pies del jugador
			shared = new TrinketSlotLists(resize(shared.stacks(), size, player), shared.cosmetic());
			group.putTrinketSlot(id, shared);
		}
		accessor.soullink$setStacks(shared.stacks());
	}

	/** Todos los slots de Trinkets del jugador: al terminar de construirlo y al cambiarlo de grupo. */
	public static void bindAll(ServerPlayer player) {
		for (TrinketInventory inventory : inventories(player)) {
			bind(inventory);
		}
	}

	/** Todos los slots, de todos los grupos de Trinkets (mano, pecho, piernas…). Vacío si el jugador no tiene el componente. */
	private static List<TrinketInventory> inventories(ServerPlayer player) {
		List<TrinketInventory> inventories = new ArrayList<>();
		TrinketsApi.getTrinketComponent(player).ifPresent(component -> component.getInventory().values().forEach(slots -> inventories.addAll(slots.values())));
		return inventories;
	}

	/** Lo que había en las listas compartidas antes de cargar el archivo del jugador (ver TrinketsLoadMixin). */
	public record LoadSnapshot(ServerPlayer player, boolean marked, Map<NonNullList<ItemStack>, List<ItemStack>> lists) {
	}

	/**
	 * Antes de que el jugador cargue su archivo: Trinkets lee sus trinkets guardados (o vacía los slots si no hay)
	 * directo sobre las listas, que son las del grupo. Se guarda qué había para devolverlo después.
	 */
	public static @Nullable LoadSnapshot beforeLoad(Entity entity, CompoundTag input) {
		if (!(entity instanceof ServerPlayer player) || SharedInventoryState.groupFor(player) == null) {
			return null;
		}
		bindAll(player);
		Map<NonNullList<ItemStack>, List<ItemStack>> lists = new IdentityHashMap<>();
		for (TrinketInventory inventory : inventories(player)) {
			NonNullList<ItemStack> stacks = ((TrinketInventoryAccessor) inventory).soullink$getStacks();
			lists.put(stacks, new ArrayList<>(stacks));
		}
		return new LoadSnapshot(player, input.getBoolean(SHARED_MARKER), lists);
	}

	/**
	 * Después de cargar: se devuelve lo compartido a como estaba. Si el archivo es de antes de compartir los trinkets
	 * (sin la marca), lo que traía es del jugador y va al cofre de ítems rescatados, como el inventario. Con el estado
	 * todavía sin inicializar (el primero que entra desde que se comparten), lo suyo queda como lo compartido.
	 */
	public static void afterLoad(LoadSnapshot snapshot) {
		SharedInventoryState state = SharedInventoryState.forPlayer(snapshot.player());
		if (state == null || !state.isTrinketsInitialized()) {
			return;
		}
		List<ItemStack> rescued = new ArrayList<>();
		snapshot.lists().forEach((list, before) -> {
			for (int slot = 0; slot < Math.min(list.size(), before.size()); slot++) {
				ItemStack loaded = list.get(slot);
				ItemStack shared = before.get(slot);
				if (loaded == shared) {
					continue;
				}
				// Una copia igual a lo compartido no es del jugador: Trinkets copia los ítems al reconstruir los slots
				if (!snapshot.marked() && !loaded.isEmpty() && !ItemStack.matches(loaded, shared)) {
					rescued.add(loaded);
				}
				list.set(slot, shared);
			}
		});
		if (!rescued.isEmpty()) {
			state.addRescuedItems(snapshot.player().getGameProfile().getId(), rescued);
		}
	}

	private static boolean isSharedByAnyGroup(SharedInventoryState state, Object list) {
		for (LinkGroup group : state.groups().values()) {
			if (group.ownsTrinketList(list)) {
				return true;
			}
		}
		return false;
	}

	private static NonNullList<ItemStack> resize(NonNullList<ItemStack> list, int size, ServerPlayer player) {
		NonNullList<ItemStack> resized = NonNullList.withSize(size, ItemStack.EMPTY);
		for (int slot = 0; slot < list.size(); slot++) {
			if (slot < size) {
				resized.set(slot, list.get(slot));
			} else if (!list.get(slot).isEmpty()) {
				Containers.dropItemStack(player.level(), player.getX(), player.getY(), player.getZ(), list.get(slot));
			}
		}
		return resized;
	}
}
