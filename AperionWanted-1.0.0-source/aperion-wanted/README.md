# APERION Wanted 1.0.0

A standalone Paper plugin project for wanted scores, kill streaks, proximity alerts, short-lived compass tracking, and configurable item bounties. It uses UUID keys and SQLite persistence. Geyser/Floodgate are not required dependencies: Bedrock players appear to Paper as normal online players, so the core mechanics work for them too.

## Compatibility target

- Minecraft/Paper API: 26.3 (`26.3-R0.1-SNAPSHOT`)
- Java toolchain/runtime: 25
- No required Vault, Geyser, Floodgate, Skript, BetterTeams, or economy dependency.
- SQLite JDBC is shaded into the plugin JAR.

The source project was prepared for this target, but **a compiled JAR has not been produced or tested in this environment**: this build environment only has JDK 21 and no network access to retrieve the Paper 26.3 API or JDK 25. Do not upload the source ZIP to `plugins/`; build the JAR first using the steps below.

## Build on a Java 25 computer

1. Install a JDK 25 distribution and Gradle 9.1 or newer.
2. Open a terminal in this project folder.
3. Run `gradle clean build`.
4. The installable shaded JAR should be created at `build/libs/AperionWanted-1.0.0.jar`.
5. Check the build output for errors before installing.

Paper's Maven repository must be reachable during the first build. The project downloads Paper API and SQLite JDBC automatically.

## Install

1. Stop the Paper server and make a backup.
2. Upload `AperionWanted-1.0.0.jar` to the server's `plugins/` directory.
3. Start the server once, then stop it.
4. Edit `plugins/AperionWanted/config.yml` as desired.
5. Start the server and run `/plugins` and `/wanted` to check it loaded.
6. SQLite data is stored at `plugins/AperionWanted/wanted.sqlite`.

## Commands

- `/wanted` — your own statistics.
- `/wanted <online-player>` — inspect an online player.
- `/wanted top` — online top list.
- `/track <online-player>` — acquire a wanted target temporarily.
- `/bounty <online-player>` — view wanted rank and bounty amount.
- `/bounty set <online-player> <amount>` — admin-set extra bounty item count.
- `/bounty remove <online-player>` — clear the extra bounty.
- `/wanted reset <online-player>` — reset score, kills and streak.
- `/wanted reload|enable|disable` — admin controls.

Permissions: `aperionwanted.admin` (op), `aperionwanted.check` (true), `aperionwanted.track` (true).

## Mechanics and known limits

- A PvP kill increases kills, streak and score. A death resets the victim's streak.
- Streak bonus is added to the base score and capped by config.
- Detection is server-side, same-world only, and uses per-observer/per-target cooldowns. The alert is a title, chat warning and Elder Guardian sound. Minecraft's normal title/chat APIs cannot embed a live 3D player head in the notification; no fake head is shown.
- Tracking is temporary, same-world, distance-limited, and stops if line of sight is lost. It updates the player's compass target; it does not print coordinates.
- Rewards are vanilla items. The rank reward is configured by rank name. `/bounty set` adds that many `custom-bounty-material` items when the target is killed. A killer/target pair can claim again after the configured cooldown.
- The `bounty.block-same-team` option is reserved for a future team-plugin adapter; this build does not introspect BetterTeams teams. Keep it false and use the pair cooldown.
- Admin bounty commands currently require the target to be online. Player-funded/self-service bounty placement and offline admin lookup are not implemented in this initial build.
- No interaction with Skript, exile, datapacks, world generation, Chunky, spawn, or teleport plugins is intentionally registered.

## Test checklist

1. Start server and confirm `AperionWanted` appears in `/plugins`.
2. Join with two test accounts. Kill one with the other; check `/wanted` for the killer and victim.
3. Kill again and confirm streak bonus increases; have the killer die and confirm streak returns to zero.
4. Temporarily set `alert-minimum-score: 1` and `detection-radius: 48`; move the test players together and confirm title, sound and chat warning. Restore the desired threshold afterward.
5. Use `/track <player>` while in clear line of sight. Confirm compass updates; move behind a wall and confirm tracking ends.
6. Set a bounty with `/bounty set <target> 2`; eliminate the target and confirm rank reward plus two configured custom bounty items. Repeat the same pair before cooldown and confirm no second payout.
7. Restart server and confirm records persist.
8. Repeat with one Bedrock account through Geyser/Floodgate.

## Important pre-release note

This is an initial source implementation, not a verified release binary. Compile against the exact Paper API used by the server, resolve any API changes reported by the compiler, and test on a staging copy before production use. In particular, verify the configured Paper API snapshot exists in the Paper Maven repository at build time.
