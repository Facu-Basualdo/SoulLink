# Mod (`src/main`)

Java 25 · Fabric API · Mixins sobre Minecraft 1.21.1 con nombres oficiales de Mojang.

- Índice del área: [claude/mod/INDICE.md](../../claude/mod/INDICE.md)
- Hub del proyecto: [CLAUDE.md](../../CLAUDE.md)

**Regla absoluta:** el inventario compartido funciona porque los jugadores de un grupo apuntan al **mismo objeto** de lista. Nunca reemplaces ni copies `items`, `armor` ni `offhand` (en 1.21.1 la armadura y la mano secundaria son listas del `Inventory`): mutalos. Detalle en [inventario-compartido](../../claude/mod/inventario-compartido.md).

No agregues documentación a este archivo: cada tema va como una nota.
