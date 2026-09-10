# Invasion Mod

**So you think your base is tough, do you?**

Invasion Mod turns a defended Minecraft base into the objective of a sustained siege. Build and activate a Nexus, prepare the surrounding terrain, and survive increasingly dangerous attack phases. Invaders do more than walk at the player: they pursue the Nexus, mine obstacles, build bridges and ladder towers, use equipment, and adapt their roles to the battlefield.

This repository continues the community ports of the original Invasion Mod. It restores legacy mechanics, adds new mobs and integrations, and focuses on reliable large invasions across several Minecraft loaders and versions.

## Supported versions

| Minecraft | Loader | Branch |
| --- | --- | --- |
| 26.2 | NeoForge | `26.2-neo` |
| 26.2 | Fabric and Quilt | `26.2` |
| 1.21.1 | NeoForge | `1.21.1-neo` |
| 1.20.1 | NeoForge and Forge | `1.20.1-neo` |

Use a build made for your exact Minecraft version and loader. Optional integrations remain optional and are loaded only when their corresponding mods are present.

## Core gameplay

- Start, pause, continue, stop, inspect, or change an invasion through the Nexus and `/invasion` commands.
- Fight persistent, budget-driven phases with themed groups, scaling equipment, bosses, support mobs, and a compact Nexus HUD.
- Defend against invaders that mine walls, climb or place ladders, build bridges and towers, cross fluids, and recover from blocked paths.
- Use the restored Nexus, catalysts, traps, Strange Bone, engineer hammer, infused sword, searing weapons, spawn eggs, recipes, sounds, drops, and progression systems.
- Keep invasion progress through reconnects and world reloads. Legacy Nexus data and blocks migrate to the current format.
- Configure mob health, damage, equipment, wave participation, block strength and mining behavior, Nexus behavior, and other invasion rules.

## Expanded invasion roster

The original zombie, zombie brute, pigman engineer, spider, skeleton, imp, thrower, creeper, enderman, burrower, wolf, and trap mechanics have been restored and expanded.

New and extended invasion variants include:

- baby zombies and baby skeleton variants;
- Tar, Speedy, Miner, Builder, mystery, and growing Fat Zombies;
- Husks, Drowned, zombie villagers, zombified piglins, Bogged, Strays, Wither Skeletons, and aquatic Guardians;
- Phantoms, Blazes, Silverfish, Endermites, Slimes, Magma Cubes, Breezes, Witches, Ghasts, Zoglins, Wardens, Elder Guardians, and a scaling Wither boss;
- biome-aware and fluid-aware wave selection, civilian infection and conversion, replacement of matching vanilla spawns, mounted-mob support, and specialized Nexus attacks.

Mobs retain distinct abilities: engineers construct routes, burrowers excavate three-dimensional tunnels, ranged mobs attack the Nexus, support casters aid allies, aquatic mobs dive and surface intelligently, and special units teleport, infect, absorb items or experience, charge, ignite blocks, or merge into stronger encounters.

## Configuration and mod support

In-game option screens replace raw text editing for the main invasion settings. Searchable mob and block selectors make larger modpacks manageable.

Any registered hostile mob can be added to invasion themes through the mod-mob configuration. Configurable behavior includes:

- wave cost, weight, health, damage, equipment, and ranged attacks;
- ground, jumping, flying, swimming, and slime-style Nexus movement;
- ladder climbing, block mining, bridge building, block placement, and fluid traversal;
- daylight protection, civilian targeting and conversion, friendly-fire rules, attack modes, free bosses, and special abilities.

Tested defaults are supplied for supported third-party mobs. Dedicated optional integrations add deeper behavior for:

- **JEI** — Nexus flux generation recipe information;
- **Infernal Mobs** — wave-scaled modifiers without duplicate normal invasion rolls;
- **Mutant Monsters** — dedicated invasion entities, themed waves, boss handling, and Nexus attack animations;
- **Friends & Foes** — Wildfire waves, barrages, movement, conversion, and resurrection behavior;
- **Tiny Skeletons** — baby skeleton models, equipment, projectiles, and special abilities;
- **Hunter's Return** and other configured mob mods — ready-to-use defaults through the generic mod-mob system.

## Performance and reliability improvements

Large invasions now avoid several sources of repeated world scanning and allocation:

- Nexus-bound mobs use tracked registries instead of full level scans.
- Civilian, item, Silverfish block, Witch target, and density searches are cached, indexed, sampled, or staggered across ticks.
- Wither Skeleton groups cache shared state; mining and pathfinding avoid unnecessary streams and position-list allocations.
- Persistent Nexus and bounty data are marked dirty only when state changes.
- Spawn simulation retries blocked positions without busy looping, while stopped or failed invasions discard pending work and stagger cleanup.
- Recovery logic repairs stalled navigation, mining, bridges, ladder towers, mounts, and flying or aquatic movement without continuously rebuilding paths.

Thread-safe target tracking and loader-specific synchronization also prevent async equipment, entity join, civilian conversion, and replacement crashes.

## Major fixes and enhancements since cedric2018's fork

- Restored complete wave coverage, drops, recipes, sounds, spawn eggs, special weapons, traps, wolf behavior, and legacy Nexus GUI behavior.
- Rebuilt engineer bridges and ladder towers, Burrower movement and rendering, mob mining, fluid navigation, and Nexus pursuit.
- Replaced fixed waves with persistent budget phases, themed pools, catalyst skips, reliable spawn retries, accurate kill tracking, and clearer HUD progress.
- Fixed Nexus state loss, reconnect progress, invalid spawn points, duplicate replacements, jockey crashes, recursive conversions, friendly fire, and cleanup spikes.
- Fixed equipment pickup and drops, ranged attacks, baby models, armor rendering, projectile rendering, entity names, tier health, and numerous mob-specific AI stalls.
- Added configuration validation, safe defaults, live block-override reloads, command permission checks, config search, and regression tests for commands and configuration.
- Ported the maintained feature set across NeoForge, Fabric, Quilt, and legacy Forge-compatible branches, including version-specific rendering implementations.

## Commands

`/invasion help` lists commands available in the installed build. Administrative invasion controls require game-master permission. Common controls include `start`, `stop`, `pause`, `continue`, `status`, `set`, `radius`, `destroy`, and debug tools when debug mode is enabled.

## Building from source

This is a single Gradle mod project. Select the branch for the target Minecraft version, install its required Java version, then run:

```bash
./gradlew build
```

The distributable JAR is created in `build/libs`. Development tasks such as `runClient` and `runServer` depend on the selected loader branch.

## Credits and project history

Invasion Mod exists because many maintainers kept an old and unusually ambitious idea alive:

- **Lieu** — original Invasion Mod for Minecraft Beta 1.8.1 through 1.6.2.
- **Elsee** — original project contributions.
- **UnstoppableN** — 1.7.10 update and published source for version 1.1.2.
- **XenoDarth** and **crazysnailboy** — 1.10.2 community port.
- **DerToaster98** and **durinfab** — 1.12.2 community work.
- **DolphinTechCodes** — 1.15.2 port.
- **Doenerstyle** — later porting and maintenance work.
- **Sollace** — modern Fabric port that formed the base of the 1.21 line.
- **cedric2018** — 1.21.1 fixes, AI, pathfinding, spawning, localization, and performance work used as the starting point for this fork.
- **Kevin Trini** — current maintenance, restored mechanics, new content, performance work, compatibility, configuration, and multi-version ports.

Thanks also to every tester, translator, issue reporter, and contributor whose feedback made the invasion systems more stable.

## License and support

Source code is available under the MIT License. Report reproducible problems through the [GitHub issue tracker](https://github.com/kevintrini2811/Invasion-Mod/issues), including Minecraft version, loader, mod version, logs, and reproduction steps.
