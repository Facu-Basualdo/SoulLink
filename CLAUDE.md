# Soul Link — mod de Minecraft

Mod de Fabric para **Minecraft Java 1.21.1** (backport de la rama `main`, que es 26.3) que hace que los jugadores vinculados compartan en tiempo real un único inventario (inventario, barra rápida, armadura y mano secundaria), una sola barra de hambre, una sola barra de vida y una sola barra de experiencia. Por defecto todos están vinculados; con `/soullink grupo` se arman grupos separados (2 y 2, o como se quiera), y cada grupo comparte lo suyo. Java 21 (Gradle corre con el 25), Gradle 9.7 + Fabric Loom Remap 1.18.4 con los mappings oficiales de Mojang, Fabric Loader 0.18.4, Fabric API 0.116.18+1.21.1. Toda la lógica corre en el servidor; `src/client` es sólo la pantalla de configuración de Mod Menu, opcional.

**El id del mod es `soullink` desde la 2.0.0 (antes `inventariocompartido`).** Lo guardado con el id viejo se pasa solo al arrancar: ver [migración del id](claude/mod/migracion-de-id.md). No lo vuelvas a cambiar sin una migración, y no renombres la marca `InventarioCompartido` del archivo de cada jugador.

## Diferencias con 26.3 (rama `main`)

Para portar un cambio de `main`, aplicalo, compilá y corré `runGameTest`. Lo que cambia en esta rama:

- **Build:** `net.fabricmc.fabric-loom-remap` con `mappings loom.officialMojangMappings()` (1.21.1 viene ofuscado), dependencias con `modImplementation`/`modCompileOnly`, Java 21, `compatibilityLevel` `JAVA_21` en los mixins y JSpecify como `compileOnly`. Gradle sigue corriendo con el JDK 25.
- **Todo lo de la rama `mc/1.21.11`**, más: `ResourceLocation` en vez de `Identifier`, `MobSpawnType` en vez de `EntitySpawnReason`, `hurt` en vez de `hurtServer`, `serverLevel()`, `FastColor.ARGB32` en vez de `ARGB`, los efectos con nombre viejo (`MobEffects.HEAL`), permisos de comandos con `hasPermission(n)`.
- **SavedData:** sin `SavedDataType`: `SavedData.Factory` y `save(CompoundTag, Provider)` a mano con el mismo codec. Id como nombre de archivo, en el overworld.
- **Pantalla de configuración:** sin `ScrollableLayout`: `ListaDesplazable` (rueda y barra arrastrable sobre `AbstractContainerWidget`).
- **Tests:** API vieja de gametest (`FabricGameTest`, `@GameTest(template = EMPTY_STRUCTURE)` de vanilla, mensajes `String`, guardado con `CompoundTag`). Fabric API para 1.21.1 no tiene gametests de cliente: lo del cliente se prueba a mano.
- **Inventario:** en 1.21.1 `Inventory` tiene tres listas finales (`items`, `armor`, `offhand`) y `compartments`, que las agrupa; no hay `EntityEquipment`. `LinkGroup` guarda armadura y mano secundaria como listas y `LinkGroup.bind` engancha las tres y rearma `compartments`. El invariante es el mismo: **mismo objeto de lista, nunca copias**.
- **Dónde se engancha:** el `Inventory` se crea en un inicializador de campo, antes de que `Player` tenga perfil: el enganche va al final del constructor de `Player` (`PlayerMixin`), no en el de `Inventory`.
- **Guardado:** con `CompoundTag`; en el archivo del jugador la armadura y la mano secundaria van en la misma lista `Inventory` (slots 100 a 103 y 150). `ItemStackWithSlot` propio (llegó en 1.21.5), con el mismo formato.
- **Red:** 11 handlers en vez de 14: el ataque pasa por `handleInteract`, el pick block es `handlePickItem` y no hay selección de bundle.
- **Perfiles:** `GameProfile.getId()`/`getName()` (authlib vieja).
- **Trinkets:** el Trinkets original 3.10.0, con la misma API vieja que la rama 1.21.11 y Cardinal Components 6.1.3.
- **Pendiente:** en unas 2 de 20 corridas el servidor de tests se colgó al cerrar (loop en `ChunkMap.processUnloads`), con todos los tests en verde. No pasó sin el tramo de Trinkets del test ni en la rama 1.21.11. Hay que probar a mano que un servidor real cierre bien con Trinkets instalado.

## Cómo obtener contexto

1. Ubicá el **área** en la tabla de abajo.
2. Abrí su `INDICE.md` y leé las descripciones: muchas veces la respuesta ya está ahí.
3. Abrí **sólo** la nota que corresponde. No leas un área entera.

Si no sabés en qué nota está algo, buscá por contenido antes de abrir archivos:

```bash
grep -ril "<palabra clave>" claude/
```

El mismo tema en distintas áreas usa el mismo nombre de archivo (`claude/mod/inventario-compartido.md` ↔ `claude/tests/inventario-compartido.md`), así que el grep devuelve el clúster completo.

## Áreas

| Área | Carpeta | Stack | Índice |
|---|---|---|---|
| Mod | `src/main/` | Java 25 · Fabric API · Mixins (SpongePowered) · nombres oficiales de Mojang | [claude/mod/INDICE.md](claude/mod/INDICE.md) |
| Tests | `src/gametest/` | Fabric GameTest API · servidor sin ventana | [claude/tests/INDICE.md](claude/tests/INDICE.md) |

## Notas transversales

| Nota | De qué trata |
|---|---|
| [Entorno Java y Gradle](claude/general/entorno-java-gradle.md) | `~/.gradle/gradle.properties` fuerza JDK 17 (React Native) y gana sobre el proyecto · Java 25 se fija con `gradle/gradle-daemon-jvm.properties` + `org.gradle.java.installations.paths` · sin eso Loom falla con "requires JVM runtime version 25" |
| [Minecraft 26.x sin ofuscar](claude/general/minecraft-26-sin-ofuscar.md) | Desde 26.1 se usan los nombres de Mojang (Yarn está muerto: los tutoriales viejos tienen otros nombres) · antes de escribir un mixin, verificá firmas con `javap` sobre el jar de `.gradle/loom-cache` · `ResourceLocation` ahora es `Identifier` |
| [Probar con dos jugadores](claude/general/probar-con-dos-jugadores.md) | `runServer` necesita `eula=true` y `online-mode=false` en `run/` · `runClient2` entra como "Jugador2" desde `run-client2/` · los dos clientes comparten build: hay que reiniciarlos después de cambiar código |

## Features que cruzan áreas

| Feature | Mod | Tests |
|---|---|---|
| Inventario compartido (listas, armadura, sync) | [inventario-compartido](claude/mod/inventario-compartido.md) | [inventario-compartido](claude/tests/inventario-compartido.md) |
| Vida y hambre compartidas | [vida-y-hambre](claude/mod/vida-y-hambre.md) | [inventario-compartido](claude/tests/inventario-compartido.md) · [jugadores-de-prueba](claude/tests/jugadores-de-prueba.md) |
| Carga de datos del jugador sin pisar lo compartido, y cofre con los ítems de antes del mod | [carga-de-datos-del-jugador](claude/mod/carga-de-datos-del-jugador.md) | [inventario-compartido](claude/tests/inventario-compartido.md) · [jugadores-de-prueba](claude/tests/jugadores-de-prueba.md) |
| Experiencia compartida | [experiencia](claude/mod/experiencia.md) | [inventario-compartido](claude/tests/inventario-compartido.md) |
| Muerte grupal y respawn | [muerte-y-respawn](claude/mod/muerte-y-respawn.md) | [jugadores-de-prueba](claude/tests/jugadores-de-prueba.md) |
| Migración del id (`inventariocompartido` → `soullink`) | [migracion-de-id](claude/mod/migracion-de-id.md) | [inventario-compartido](claude/tests/inventario-compartido.md) |
| Grupos vinculados (`/soullink grupo`, `SoulLinkApi`) | [grupos](claude/mod/grupos.md) | [inventario-compartido](claude/tests/inventario-compartido.md) |
| Estadísticas (`/soullink`, scoreboard) | [estadisticas](claude/mod/estadisticas.md) | [inventario-compartido](claude/tests/inventario-compartido.md) |
| Idioma de los mensajes (`Lang`, comandos en inglés) | [idioma](claude/mod/idioma.md) | [configuracion](claude/tests/configuracion.md) |
| Configuración (`config/soullink.json`) | [configuracion](claude/mod/configuracion.md) | [configuracion](claude/tests/configuracion.md) |
| Intercambiar lugar (`/intercambiar`) | [intercambiar-lugar](claude/mod/intercambiar-lugar.md) | [inventario-compartido](claude/tests/inventario-compartido.md) · [jugadores-de-prueba](claude/tests/jugadores-de-prueba.md) |
| Slots de Trinkets compartidos, con Trinkets Updated opcional (3.3.0) | [trinkets](claude/mod/trinkets.md) | [inventario-compartido](claude/tests/inventario-compartido.md) |
| Pantalla de configuración con Mod Menu, opcional (3.2.0) | [pantalla-de-configuracion](claude/mod/pantalla-de-configuracion.md) | [pantalla-de-configuracion](claude/tests/pantalla-de-configuracion.md) |

## Reglas de workflow

- **Después de cada feature corré `/docs`** para actualizar las notas según el diff, y `node scripts/verificar-docs.mjs` tiene que dar OK.
- **Si la feature se nota en el juego, actualizá el `README.md`** (la lista de qué se comparte, la tabla "Cómo funciona en el juego" y lo que verifican las pruebas): es lo que lee quien instala el mod, y `/docs` sólo toca `claude/`.
- **Antes de dar algo por terminado corré `gradlew runGameTest`**: compila no alcanza, los mixins recién se aplican en runtime.
- **Con cada cambio que se pueda notar en el juego, subí `version` en `gradle.properties`** (sale en el nombre del jar y en `fabric.mod.json`): minor (`1.1.0` → `1.2.0`) para una feature nueva, patch (`1.1.0` → `1.1.1`) para un arreglo. Si es otra versión, el jar viejo y el nuevo no se confunden en la carpeta `mods/`.
- **Con cada versión nueva, agregá su sección `## <versión>` en `CHANGELOG.md`**, en inglés y pensada para quien juega: es el changelog que `gradlew publishMods` sube a CurseForge, y sin ella la publicación falla. Subir es sólo `gradlew publishMods` con el token de CurseForge en `~/.gradle/gradle.properties` (`curseforgeToken`) o en `CURSEFORGE_TOKEN`; sin token corre en seco y no sube nada. Publicá sólo si te lo piden.
- **No commitees ni pushees salvo que te lo pidan.**
- Mensajes, comentarios y docs en **español rioplatense** (vos), como el resto del proyecto.

## Convenciones de código

- Paquete `com.soullink`; mixins en `com.soullink.mixin`, registrados en `src/main/resources/soullink.mixins.json`.
- **Métodos y accessors agregados por mixins llevan prefijo `soullink$`** para no chocar con otros mods.
- Indentación con tabs, imports agrupados: java/terceros → `net.fabricmc` → `net.minecraft`.
- Comentarios sólo para el porqué (qué rompería vanilla si no estuviera), no para parafrasear.

## Qué NO va en el grafo

Planes, investigaciones, auditorías y borradores van en `docs/` y se leen a demanda; `claude/` es sólo contexto permanente. Hoy está sólo `docs/curseforge.md`, la descripción de la página de CurseForge (en inglés): actualizala cuando cambie algo que se ve en el juego.
