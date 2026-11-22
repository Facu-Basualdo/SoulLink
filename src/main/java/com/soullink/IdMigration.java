package com.soullink;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;


/**
 * Hasta la 1.2.1 el id del mod era {@code inventariocompartido}, y el estado compartido se guardaba en
 * {@code world/data/inventariocompartido/shared_inventory.dat}. Con el id nuevo, {@link SharedInventoryState} lo busca
 * en {@code world/data/soullink/}: sin mover el archivo, un mundo existente arrancaría con el inventario vacío.
 */
public final class IdMigration {
	static final String OLD_ID = "inventariocompartido";

	private IdMigration() {
	}

	/**
	 * Mueve el archivo sólo si el nuevo no existe: si ya hay uno con el id nuevo, es el que vale y el viejo queda
	 * como estaba. Devuelve si movió.
	 */
	public static boolean moveIfMissing(Path oldFile, Path newFile) {
		if (!Files.exists(oldFile) || Files.exists(newFile)) {
			return false;
		}
		try {
			Files.createDirectories(newFile.getParent());
			Files.move(oldFile, newFile);
			return true;
		} catch (IOException e) {
			// Mejor no arrancar que arrancar con el estado vacío y pisar el de verdad al guardar
			throw new UncheckedIOException("No se pudo mover " + oldFile + " a " + newFile, e);
		}
	}
}
