package com.soullink.test.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/**
 * Sólo en los tests. En 1.21.x Trinkets guarda sus slots con Cardinal Components, que echa a un jugador si su cliente no
 * puede recibir el paquete de sync. Los jugadores de prueba están conectados a un EmbeddedChannel sin canales
 * registrados: sin esto, se desconectan al entrar. Todos los jugadores del servidor de tests son falsos.
 */
@Mixin(value = ServerPlayNetworking.class, remap = false)
public abstract class CanalesDeJugadoresDePruebaMixin {
	@Inject(method = "canSend(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload$Type;)Z",
			at = @At("HEAD"), cancellable = true)
	private static void soullink$todosLosCanales(CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(true);
	}
}
