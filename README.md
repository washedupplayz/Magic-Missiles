# Magic Missiles

A tech/military mod for Minecraft focused on launchable missiles with target
tracking, radar, and missile interception.

- Minecraft: 1.21.1
- Loader: NeoForge 21.1.242
- Mod version: 0.1.0

## Requirements

- A full JDK 21 (with `javac`). NeoForge/Gradle 8.12 do not support newer JDKs.

Gradle itself is pinned to JDK 21 via `org.gradle.java.home` in `gradle.properties`
(a machine-specific path — change it if your JDK 21 lives elsewhere). No `JAVA_HOME`
export is needed.

## Build

```bash
./gradlew build
```

The mod jar is written to `build/libs/magicmissiles-0.1.0.jar`.

## Test

```bash
./gradlew test               # run unit tests
./gradlew test --rerun-tasks -i   # force re-run and print each test
```

Reports: `build/reports/tests/test/index.html`.

## Run in a dev environment

```bash
./gradlew runClient          # launch Minecraft with the mod loaded
./gradlew runServer          # launch a dedicated server with the mod loaded
```

No Mojang login is required for the dev client.

## Install in a regular Minecraft instance

1. Install the NeoForge 21.1.242 loader for Minecraft 1.21.1.
2. Run `./gradlew build`.
3. Copy `build/libs/magicmissiles-0.1.0.jar` into that instance's `mods/` folder.

## Content

- Missile Launcher (item) - fires a missile in the direction you look.
- Missile Silo (block) - fires a missile upward; if a Radar Station is within
  8 blocks, it designates a target for the missile.
- Radar Station (block) - scans a 32-block radius and tracks living targets.
- Missile - a server-managed, ghost-rendered projectile (not a world entity). It
  lofts to a cruise altitude, homes toward a locked target with a limited turn rate
  (or acquires one with its onboard seeker), and detonates on impact or fuel-out.
  Because it is decoupled from the entity system it can cross unloaded chunks without
  forcing chunk loads and is visible far beyond entity range. See `DESIGN.md`.

## Project layout

```
src/main/java/net/washedupplayz/magicmissiles/
  MagicMissiles.java        mod entrypoint (registers network + level-tick sim)
  registry/                 items, blocks, block entities, creative tab
  missile/                  MissileManager (server sim, SavedData), MissileState, tick hook
  network/                  MissileSpawn/Update/Remove payloads + registration
  item/                     MissileLauncherItem
  block/                    MissileSiloBlock, RadarBlock (+ entity/RadarBlockEntity)
  client/                   ghost render (GhostRenderer, ClientMissileManager, MissileGhost)
  util/                     GuidanceMath (steering + lofted trajectory)
src/test/java/              unit tests
src/main/resources/         assets, models, lang, mod metadata template
```
