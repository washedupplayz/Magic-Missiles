# Magic Missiles

A tech/military mod for Minecraft focused on launchable missiles with target
tracking, radar, and missile interception.

- Minecraft: 1.21.1
- Loader: NeoForge 21.1.242
- Mod version: 0.1.0

## Requirements

- A full JDK 21 (with `javac`). On this machine: `/usr/lib/jvm/java-21-openjdk-amd64`.

Set it before running Gradle:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
```

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
- Missile (entity) - homes toward a locked target with a limited turn rate, or
  acquires one with its onboard seeker; detonates on impact or when fuel runs out.

## Project layout

```
src/main/java/net/washedupplayz/magicmissiles/
  MagicMissiles.java        mod entrypoint
  registry/                 items, blocks, block entities, entities, creative tab
  entity/                   MissileEntity (guidance)
  item/                     MissileLauncherItem
  block/                    MissileSiloBlock, RadarBlock (+ entity/RadarBlockEntity)
  client/                   renderer registration
  util/                     GuidanceMath (steering)
src/test/java/              unit tests
src/main/resources/         assets, models, lang, mod metadata template
```
