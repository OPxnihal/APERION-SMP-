# APERION Wanted 1.0.0 — Source Project

This folder contains the Gradle source project for the **APERION SMP Wanted & Bounty Hunter** Paper plugin.

For the public-facing overview, feature list, commands, permissions, installation guide, configuration notes, limitations and test checklist, see the [repository README](../../../README.md).

## Build target

- Minecraft / Paper API: 26.3
- Java toolchain and runtime: 25
- Gradle: 9.x
- SQLite JDBC is bundled into the shaded plugin JAR.
- Geyser and Floodgate are optional; neither is a hard dependency.

## Build locally

From this directory, with JDK 25 and Gradle 9 installed:

```bash
gradle clean build
```

The shaded plugin JAR is produced in:

```
build/libs/AperionWanted-1.0.0.jar
```

The GitHub Actions workflow at the repository root builds this project automatically on pushes affecting this source folder and can also be run manually from the Actions tab.

## Install

1. Back up the Paper server and stop it.
2. Copy `AperionWanted-1.0.0.jar` into `plugins/`.
3. Start the server once to generate the plugin folder.
4. Edit `plugins/AperionWanted/config.yml`.
5. Restart and verify with `/plugins` and `/wanted`.

Player records are stored in `plugins/AperionWanted/wanted.sqlite`.

## Current implementation notes

- PvP kills add Wanted score and streak bonuses; death resets the victim's streak.
- Proximity alerts notify both the hunter and the wanted target using titles, chat warnings and the Elder Guardian sound. A live 3D player head is not embedded in the title.
- Wanted players receive a rank tag in the Tab List; normal players keep their clean name. No bounty amount or reward item is shown beside the name.
- Bounty claims select one random item from the target rank's configurable reward pool. Higher ranks can reward Ancient Debris, Netherite items and rare gear.
- Tracking is temporary, same-world, range-limited and can require line of sight; no coordinates are shown.
- Bounty rewards are randomized vanilla items, not Vault currency. Reward pools are configurable by rank.
- Repeat bounty claims are limited by a configurable killer/target pair cooldown.
- BetterTeams team detection is not implemented in this version; its config flag is reserved for a future adapter.
- Player-funded bounty placement and offline target administration are not included.
- Geyser/Floodgate are not required dependencies. Test Bedrock behaviour on your own server before production use.

Always test on a staging server before installing on a live world. A successful build is not a substitute for in-game feature testing.
