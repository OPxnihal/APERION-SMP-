<div align="center">

# ⚔️ APERION SMP — Wanted & Bounty Hunter

**A PvP reputation, wanted-player detection, tracking and bounty system for Paper servers.**

[![Build APERION Wanted](https://github.com/OPxnihal/APERION-SMP-/actions/workflows/build-aperion-wanted.yml/badge.svg)](https://github.com/OPxnihal/APERION-SMP-/actions/workflows/build-aperion-wanted.yml)
![Minecraft](https://img.shields.io/badge/Minecraft-26.3-2ea44f)
![Paper](https://img.shields.io/badge/Server-Paper-blue)
![Java](https://img.shields.io/badge/Java-25-orange)
![License](https://img.shields.io/badge/License-Not%20specified-lightgrey)

**Make dangerous players known. Give hunters a reason to pursue them.**

[Latest release](https://github.com/OPxnihal/APERION-SMP-/releases/latest) · [Report a bug](https://github.com/OPxnihal/APERION-SMP-/issues) · [Source code](AperionWanted-1.0.0-source/aperion-wanted/)

</div>

---

## What is APERION Wanted?

APERION Wanted is a server-side Paper plugin that gives PvP a reputation system. Player kills increase a player's Wanted score and kill streak. High-profile targets can trigger nearby-player warnings, other players can temporarily track visible targets, and hunters can earn configurable item rewards for taking down wanted players.

It is designed for **APERION SMP** and aims to work for Java and Bedrock players on a Java server using Geyser.

## Features

| Feature | How it works |
|---|---|
| Wanted ranks | Six configurable ranks, from **Unknown** to **Most Wanted** |
| Kill statistics | Stores total PvP kills, Wanted score and current streak |
| Streak bonus | Consecutive kills add extra Wanted score |
| Two-way proximity alerts | Hunters are alerted when a wanted target is close; the wanted player is also warned when another player enters their radius |
| Elder Guardian-style warning | Title, chat warning and Elder Guardian sound |
| Temporary tracking | Compass tracking for a limited time, in the same world and within range |
| Rank-based loot | One randomized vanilla-item reward from the target's rank pool; higher ranks unlock Ancient Debris, Netherite loot and rare items |
| Tab List rank tags | Wanted players show a rank tag beside their name; no reward amount is displayed |
| Anti-farming | A configurable cooldown for repeat claims by the same killer/target pair |
| Persistent data | SQLite database keyed by player UUID |
| Bedrock support | Core server-side mechanics also apply to Bedrock players joining through Geyser |

### Wanted ranks (default)

| Rank | Minimum Wanted score |
|---|---:|
| Unknown | 0 |
| Notorious | 100 |
| Wanted | 250 |
| Dangerous | 500 |
| Outlaw | 1,000 |
| Most Wanted | 2,000 |

Ranks and score values can be changed in `config.yml`.

## Compatibility

- Minecraft / Paper API target: **26.3**
- Java: **25**
- Server software: **Paper**
- Geyser: not a required dependency; Bedrock players are handled as online server players.
- Floodgate: not a required dependency.
- No mandatory Vault, EssentialsX, BetterTeams, Skript or economy plugin integration.

Use the build produced by this repository's GitHub Actions workflow. A successful compile confirms that the source builds; it does not replace testing on a staging server.

## Installation

1. **Back up your server** and its world/plugin data.
2. Stop the Paper server.
3. Upload `AperionWanted-1.0.0.jar` into the server's `plugins/` folder.
4. Start the server once so the plugin creates its files.
5. Stop the server and edit `plugins/AperionWanted/config.yml`.
6. Start the server again.
7. Run `/plugins` and `/wanted` to confirm it loaded.

Player data is stored in `plugins/AperionWanted/wanted.sqlite`. Back up this file along with your server backups.

## Commands

### Player commands

| Command | Description |
|---|---|
| `/wanted` | View your Wanted score, rank and statistics |
| `/wanted <player>` | View an online player's Wanted status |
| `/wanted top` | Show the online top list |
| `/track <player>` | Temporarily track an eligible online target |
| `/bounty <player>` | View an online player's bounty |

### Admin commands

| Command | Description |
|---|---|
| `/bounty set <player> <amount>` | Add extra configured bounty items to an online target |
| `/bounty remove <player>` | Remove the target's extra bounty |
| `/wanted reset <player>` | Reset an online player's score, kills and streak |
| `/wanted reload` | Reload configuration |
| `/wanted enable` | Enable the system |
| `/wanted disable` | Disable the system |

### Permissions

| Permission | Default | Purpose |
|---|---|---|
| `aperionwanted.admin` | OP | Admin commands |
| `aperionwanted.check` | Everyone | Check Wanted profiles |
| `aperionwanted.track` | Everyone | Use tracking |

## Configuration

Edit `plugins/AperionWanted/config.yml`. Common settings:

```yaml
system:
  enabled: true
  detection-interval-ticks: 20
  detection-radius: 48
  alert-cooldown-seconds: 45
  alert-minimum-score: 1000

tracking:
  enabled: true
  maximum-distance: 256
  duration-seconds: 60
  update-interval-ticks: 20
  require-line-of-sight: true

bounty:
  same-pair-cooldown-hours: 24
```

The full generated configuration includes rank thresholds, Tab List formatting, separate hunter/target alert text, detection radius and cooldown, and randomized reward pools for each rank. Normal players keep a clean Tab List name; wanted players show only their rank, never a reward amount. Change settings carefully and keep a backup of your config.

## Important behaviour and current limitations

- Proximity alerts notify both the nearby hunter and the wanted target with a title, chat message and Elder Guardian sound. A normal title/chat notification cannot show a live 3D player head; this version does not display a player-head icon.
- The Tab List adds a wanted-rank tag beside wanted players' names. It does not show bounty amounts or reward items.
- A bounty claim selects one random item entry from the target's rank reward pool. The reward pools are configurable; the highest ranks can include Ancient Debris, Netherite items, Elytra, Mace and other rare loot.
- Tracking is temporary, same-world, range-limited and can require line of sight. It does not reveal target coordinates.
- Bounties are paid as vanilla items; there is no built-in Vault/economy currency integration in this version.
- The repeat-claim cooldown applies to the same killer/target pair. The BetterTeams option is reserved for a future integration; this version does not read BetterTeams team membership.
- Admin bounty set/remove and profile inspection require the target to be online.
- Player-funded bounty placement and offline-player admin management are not included in this initial version.
- Geyser/Floodgate are not required dependencies. Test Java and Bedrock clients separately on your own server before public release.
- The plugin is intended not to modify world generation, datapacks, Chunky, spawn/teleport plugins or Skript files. Always test on a backup first.

## Build from source

The Gradle project is in `AperionWanted-1.0.0-source/aperion-wanted/`.

The included GitHub Actions workflow builds with Java 25. To build locally, install JDK 25 and Gradle 9, open a terminal in that project directory, then run:

```bash
gradle clean build
```

The shaded JAR is generated in `build/libs/`.

## Testing checklist

- [ ] Plugin loads without errors on a staging Paper 26.3 server.
- [ ] Two Java test players can generate and receive PvP kill statistics.
- [ ] Kill streak increases Wanted score; death resets the streak.
- [ ] Hunter and wanted-target proximity warnings both trigger within the configured radius.
- [ ] Wanted rank tags appear in the Tab List, while normal player names remain unchanged.
- [ ] Rank reward pools give a randomized reward appropriate to the target's rank.
- [ ] Tracking expires, stops at range/dimension boundaries and respects line of sight.
- [ ] Rank and extra bounty items are paid correctly.
- [ ] Same killer/target pair cannot repeatedly claim before the cooldown expires.
- [ ] UUID records persist after a server restart.
- [ ] Repeat the gameplay checks with a Bedrock player joining through Geyser/Floodgate.

## Contributing and support

Found a bug or have an idea? [Open an issue](https://github.com/OPxnihal/APERION-SMP-/issues) and include:
- Minecraft and Paper versions
- Java version
- Relevant server console error
- Steps to reproduce the issue

---

<div align="center">

**Built for APERION SMP**  
*Hunt smart. Stay wanted.*

</div>
