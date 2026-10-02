# 🏠 HA Exporter — RuneLite Home Assistant Data Exporter

[![Build](https://github.com/xXD4rkDragonXx/runelite-homeassistant-data-exporter/actions/workflows/build.yml/badge.svg)](https://github.com/xXD4rkDragonXx/runelite-homeassistant-data-exporter/actions/workflows/build.yml)
[![License: BSD-2-Clause](https://img.shields.io/badge/License-BSD--2--Clause-blue.svg)](LICENSE)
[![Java 17](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/projects/jdk/17/)

A [RuneLite](https://runelite.net/) plugin that exports real-time Old School RuneScape game data to [Home Assistant](https://www.home-assistant.io/) for smart-home automation and dashboards.

> A lot of the codebase is based on the [Dink plugin](https://github.com/pajlads/DinkPlugin) by pajlads.

---

## ✨ Features

| Category | What's Tracked                                       |
|----------|------------------------------------------------------|
| **Player Stats** | All 24 skill levels & XP                             |
| **Health & Prayer** | Current / max hitpoints, prayer points and spellbook |
| **Inventory** | Full item list with GE & HA prices                   |
| **Equipment** | Worn gear with slot information                      |
| **Loot Drops** | Configurable value & rarity filters                  |
| **Level-Ups** | Skill name and new level                             |
| **Deaths** | Killer info, kept/lost items, danger level           |
| **World & Location** | Current world, coordinates                           |

All data is pushed over HTTP(s) as JSON to your Home Assistant instance, where a companion integration turns it into entities you can use in automations, dashboards, and more.

---

## 📋 Prerequisites

| Requirement | Details |
|-------------|---------|
| **RuneLite** | Latest release — [runelite.net](https://runelite.net/) |
| **Home Assistant** | With the [**OSRS Data**](https://github.com/RedFirebreak/ha-osrs-data) custom integration installed |

---

## 🚀 Quick Start

### 1. Install the plugin

Install **HA Exporter** from the RuneLite Plugin Hub (or side-load the JAR for development).

### 2. Pair with Home Assistant

The plugin uses a **code-based pairing** flow to securely link your RuneLite client with Home Assistant.

1. In Home Assistant, open the [**OSRS Data**](https://github.com/RedFirebreak/ha-osrs-data) integration and click **Add Device** — you'll receive a **5-digit pairing code**.
2. In RuneLite, open the **HA Exporter** side panel (icon in the toolbar).
3. Click **Connect New Device**.
4. Enter the 5-digit code and, under **Endpoint URL**, your Home Assistant URL — or any compatible endpoint (e.g. `https://ha.example.com`).
5. Click **Submit**. The plugin exchanges the code for a long-lived token and stores the connection. If the endpoint sends a name, it becomes the connection's default friendly name (rename it anytime via ⚙). If pairing fails, the endpoint's own error message (e.g. "Code expired") is shown when it provides one.

You can pair **multiple** Home Assistant instances (or other compatible endpoints) — each connection is stored independently.

### 3. Play the game!

Once paired, the plugin automatically sends data on a configurable tick interval and whenever notable events occur (loot, level-ups, deaths, etc.).

---

## 🔗 Data Pairing — How It Works

Home Assistant (with the OSRS Data integration) is the primary target, but any endpoint that implements these two requests can be paired:

```text
RuneLite                                Home Assistant
   │                                          │
   │  POST /api/osrs-data/pair                │
   │  Header: X-Osrs-Exporter-Version: 1.6.0  │
   │  Body: { "code": "12345" }        ──────►│
   │                                          │
   │  Response: { "token": "abc123…" } ◄──────│
   │                                          │
   │         ── connection saved ──           │
   │                                          │
   │  POST /api/osrs-data/events              │
   │  Header: X-Osrs-Token: abc123…    ──────►│
   │  Header: X-Osrs-Exporter-Version: 1.6.0  │
   │  Body: <JSON payload>                    │
   │                                          │
```

Both requests carry an `X-Osrs-Exporter-Version` header with the plugin version (e.g. `1.6.0`).

**Pair response** — `token` is required on success; these fields are optional:

| Outcome | Example body | Optional field |
|---------|--------------|----------------|
| Success (2xx) | `{ "token": "abc123…", "name": "My Server" }` | `name` — used as the connection's default friendly name (max 64 characters) |
| Failure (non-2xx) | `{ "error": "Code expired" }` | `error` — shown to the user instead of the generic "Connection failed" message (max 200 characters) |

Each stored connection contains:

```jsonc
{
  "baseUrl": "https://ha.example.com",
  "token": "abc123def456…",
  "friendlyName": "My Server"   // optional — defaults to the pair response's "name"
}
```

---

## 📦 JSON Payload Structure

Every message sent to Home Assistant follows this structure:

```jsonc
{
  "player": {
    "name": "PlayerName",
    "accountType": "0",           // 0 = Normal, 1 = Ironman, 2 = Ultimate Ironman, 3 = Hardcore Ironman, 4 = Group Ironman, …
    "world": "302",
    "location": { "x": 3222, "y": 3218, "plane": 0, "isOnBoat": false },
    "locationTrail": [
      // Every tile visited since the previous message, oldest first. See "Location trail" below
      { "x": 3221, "y": 3218, "plane": 0, "isOnBoat": false, "timestamp": 1735689599400 },
      { "x": 3222, "y": 3218, "plane": 0, "isOnBoat": false, "timestamp": 1735689600000 }
    ],
    "health": { "current": 85, "max": 99 },
    "prayerPoints": { "current": 52, "max": 70 },
    "spellbook": { "id": 0, "name": "standard" },
    "stats": {
      "skills": {
        "Attack":    { "xp": 200000000, "level": 99 },
        "Strength":  { "xp": 200000000, "level": 99 },
        "Defence":   { "xp": 1210421, "level": 75 },
        // … every skill
      }
    },
    "inventory": {
      // Empty slots are omitted. inventorySlot is 0-27, left to right then top to bottom (row = slot / 4, column = slot % 4)
      "items": [
        { "name": "Abyssal whip", "id": 4151, "gePrice": 1650000, "haPrice": 72000, "quantity": 1, "inventorySlot": 0 }
      ]
    },
    "equipment": {
      "items": [
        { "name": "Dragon defender", "id": 12954, "gePrice": 500000, "haPrice": 68000, "quantity": 1, "equipmentSlot": "SHIELD" }
      ]
    }
  },
  "events": [
    // Empty unless something noteworthy happened since the previous message. Every event has the same envelope:
    {
      "type": "levelUp",                                   // event type, see the table below
      "data": [ { "skill": "Attack", "level": 99 } ],      // type-specific payload
      "eventId": "3f2c9a4e-8d1b-4c6e-9f0a-2b7d5e1c8a90",   // random UUID, unique per event
      "timestamp": 1735689600000                           // when the event happened (epoch millis, UTC)
    }
  ],
  "state": "LOGGED_IN",
  "tickDelay": 100,
  "timestamp": 1735689600123                               // when this message was built (epoch millis, UTC)
}
```

| Field | Description |
|-------|-------------|
| `timestamp` | When this message was built, in epoch milliseconds (UTC) |
| `player` | Full snapshot of the player's current state |
| `events` | Array of events that fired since the last message (may be empty) |
| `state` | Current `GameState` (e.g. `LOGGED_IN`, `HOPPING`, `LOGIN_SCREEN`) |
| `tickDelay` | Number of game ticks between periodic base messages |

### Events

Each event is an object `{ "type", "data", "eventId", "timestamp" }`:

| Field | Description |
|-------|-------------|
| `type` | Event type (see below) |
| `data` | Type-specific payload |
| `eventId` | Random UUID. Identical events can legitimately repeat (e.g. two diary tasks in a row), so use this to tell a genuine repeat from a duplicate delivery |
| `timestamp` | When the event happened, in epoch milliseconds (UTC). Can be earlier than the message `timestamp` when the event waited for the next periodic message |

Every event is sent exactly once. Events that trigger an immediate message go out right away; the others (`achievementDiary`, `combatTask`) are included in the next periodic message. Events that happen on a special world are dropped unless **Special world data** is enabled (see below).

| `type` | `data` |
|--------|--------|
| `levelUp` | Array of `{ "skill", "level" }` (`skill` can also be `"Combat"`) |
| `loot` | `{ "items", "highestValueItem", "totalValue", "source": { "text", "link" }, "type", "npcId", "criteria" }` — items with known drop rates also carry `rarity` |
| `pkLoot` | Same as `loot`, for PK loot chests whose total value exceeds the minimum |
| `death` | `{ "valueLost", "danger", "killerName", "killerNpcId", "keptItems", "lostItems", "location": { "x", "y", "plane" } }` |
| `achievementDiary` | `{ "region", "tier" }` |
| `combatTask` | `{ "taskName", "tier" }` |
| `superiorSpawn` | `{ "name", "npcId", "location": { "x", "y", "plane" } }` |
| `collectionLog` | `{ "itemName", "itemId", "value", "killCount" }` (`itemId` is `-1` when the name can't be matched to an item; `killCount` is the kill count of the loot drop the item came from, absent when there is none) |
| `clientShutdown` | `"Logout"`, `"Shutdown"` or `"Disabled"` (plugin turned off) |

`collectionLog` events come from the game's own new-item notification, so the in-game setting **Collection log - New addition notification** must be on. Chat and popup both work; with the setting off, the game doesn't announce new items and no event is sent.

### Location trail

`player.location` is only the tile the player stands on when a message is built. `player.locationTrail` fills in the path between two messages:

| Field | Description |
|-------|-------------|
| `x`, `y`, `plane`, `isOnBoat` | Same meaning as in `player.location` |
| `timestamp` | When the player was seen on this tile, in epoch milliseconds (UTC) |

- The position is checked every game tick, and a point is added whenever the tile, plane or boat state changed. Standing still adds nothing, so the array can be empty.
- Points are ordered oldest first and each point is sent exactly once. To draw a path, append every message's trail to the points you already have.
- After logging in or hopping worlds, the trail starts with the tile the player is on.
- Teleports are not marked, and a teleport doesn't trigger a message of its own. A player on foot moves at most 2 tiles per game tick (0.6 s), so two consecutive points that are much further apart were not walked. Don't draw a line between them. Allow a few tiles of margin, because the position can catch up several tiles at once after lag, and boats (`isOnBoat`) are faster. Such a gap can be:
  - a teleport, or a cave or dungeon entrance;
  - a room change inside an instance such as a player-owned house, where the coordinates are those of the map area each room was copied from, so neighbouring rooms can be far apart;
  - a part of the trail that was never delivered (see the last point below).
- A message holds at most 300 points; beyond that the oldest are dropped.
- The trail follows the **Share location** switches. A connection that doesn't receive `location` doesn't receive the trail either.
- While a connection is paused after a failed delivery, messages without events are dropped, so that part of the trail is lost for that connection.

### Account identity & world types

Two `player` fields help receivers tell accounts and worlds apart:

| Field | Description |
|-------|-------------|
| `player.accountHash` | Salted SHA-224 hex digest of the RuneLite account hash. Stays the same when the display name changes. Omitted when not logged in. It cannot be reversed and cannot be matched against hashes sent by other plugins. |
| `player.worldTypes` | RuneLite `WorldType` names of the current world: `[]` on free-to-play worlds, `["MEMBERS"]` on normal members worlds. |

```json
{
  "player": {
    "name": "PlayerName",
    "accountHash": "de731bc0f710567a6a0e852bbe79eb5fa8daf37d4140440a774591f0",
    "world": "302",
    "worldTypes": ["MEMBERS"]
  }
}
```

- Key accounts on `accountHash`, and fall back to `name` when it is absent.
- Special worlds (`SEASONAL`, `DEADMAN`, `TOURNAMENT_WORLD`, `BETA_WORLD`, `QUEST_SPEEDRUNNING`, `NOSAVE_MODE`, `PVP_ARENA`) use separate or temporary characters. By default the plugin sends **nothing** while you're on one, so their stats never mix with your main account. Enabling **Special world data** sends them anyway, and `worldTypes` then tells receivers which world the data came from.

---

## ⚙️ Configuration

Open **RuneLite Settings → HA Exporter** to find these options. Every event type has its own section with an on/off switch at the top. That switch applies to **all** connections, and when it's off the matching checkbox in the side panel is greyed out.

### General

| Option | Default | Description |
|--------|---------|-------------|
| **Update rate (ticks)** | `100` (~60 s) | How often a full state update is sent. Events are always sent right away |
| **Instant health / prayer updates** | `on` | Send every HP / prayer change right away instead of waiting for the next update |
| **Special world data** | `off` | Send data while on special or event worlds (Leagues, Deadman, tournament, beta, quest speedrunning, PvP Arena). When off, nothing at all is sent from those worlds — not even a logout — so receivers keep the last state from a normal world |

### Data Sharing

| Option | Default | Description |
|--------|---------|-------------|
| **Share inventory / equipment / location** | `on` | Include these in every update. Location also covers the location trail |

### Loot

| Option | Default | Description |
|--------|---------|-------------|
| **Send loot events** | `on` | Master switch for loot events |
| **Min item value (gp)** | `25000` | Only send a drop when an item stack is worth at least this much |
| **Rarer than 1 in X** | `0` (off) | Also send rare drops that are below the minimum value (NPC drops and pickpocketing) |
| **Value AND rarity** | `off` | A drop must pass both thresholds |
| **Always send for these items** | _(empty)_ | Always sent, even below the thresholds. One name per line, `*` wildcard |
| **Never send for these items** | _(empty)_ | Never sent. Wins over *Always send* |
| **Never send these sources** | `Einar` | NPC / activity names to ignore (not player names) |
| **Send PK loot** | `on` | Loot from killing other players |
| **PK chest total value** | `on` | Send PK loot chests when their combined value passes the minimum |
| **Send pickpocket loot** | `on` | Loot from pickpocketing |
| **Player lookup link** | `OSRS HiScore` | Website that player names in PK loot link to |

### Level Up

| Option | Default | Description |
|--------|---------|-------------|
| **Send level-up events** | `on` | Master switch. Skill levels are still shared in every update |
| **Minimum level** | `1` | Only send level-ups to this level or higher |
| **Send every Nth level** | `1` | Only send multiples of N. Level 99 is always sent |
| **Send virtual levels** | `on` | Levels above 99 |
| **Send combat level** | `on` | Combat level increases |

### Collection Log, Achievement Diary, Combat Tasks

| Option | Default | Description |
|--------|---------|-------------|
| **Send … events** | `on` | Master switch for each event type |
| **Collection log: Min item value (gp)** | `0` | Items without a GE price (such as pets) are always sent |
| **Diary: Minimum tier** | `Easy` | Only send diary tasks of this tier or harder |
| **Combat tasks: Minimum tier** | `Easy` | Only send combat tasks of this tier or harder |

### Deaths & Superior Spawns

| Option | Default | Description |
|--------|---------|-------------|
| **Send death events** | `on` | Master switch for death events |
| **Send superior spawn events** | `on` | Master switch for superior slayer monster spawns |

---

## 🔁 Delivery & Backoff

Every connection is handled on its own. When an endpoint (Home Assistant or any other receiver) can't be reached or asks the plugin to slow down, the plugin pauses sending to that connection for a while instead of continuing to send every update.

| Response | What the plugin does |
|----------|----------------------|
| `2xx` | Delivered — any pause is lifted and the backoff resets to 30 s |
| `401 Unauthorized` | Connection is disabled (the token may have been revoked) |
| `410 Gone` | Connection is disabled (the endpoint no longer accepts data) |
| `429` / `503` with `Retry-After` | Paused until the time the server asks for (seconds or an HTTP date), capped at 10 minutes |
| `429` / `503` without a valid `Retry-After` | Exponential backoff |
| Other `5xx`, network errors & timeouts | Exponential backoff |
| Other `4xx` | Payload is dropped and not retried — no pause |

**Exponential backoff:** the first failure pauses the connection for 30 s, and every failed retry doubles the pause (30 s → 1 min → 2 min → 4 min → 8 min) up to a maximum of 10 minutes. A successful delivery resets it.

While a connection is paused:

- Periodic snapshots **without events** are dropped — the next snapshot carries the full state anyway.
- Payloads **with events** (loot, level-ups, deaths, …) are queued: at most 50 payloads per connection and nothing older than 10 minutes, dropping the oldest first.
- When the pause ends, the queued payloads are resent one at a time, in their original order.

Pauses and queued payloads live in memory only: they are never saved to your RuneLite config, and restarting the client or turning the plugin off clears them.

> **Duplicates:** after a network error or timeout the plugin can't tell whether the endpoint already received a payload, so it sends it again. Receivers may therefore occasionally get the same event twice and should de-duplicate on each event's `eventId`.

The side panel shows a paused connection under its name, e.g. `⏸ Paused — retrying in 2m 05s (3 queued)`, counting down live until sending resumes.

---

## 🏗️ Building from Source

```bash
# Clone the repository
git clone https://github.com/xXD4rkDragonXx/runelite-homeassistant-data-exporter.git
cd runelite-homeassistant-data-exporter

# Build the plugin
./gradlew build

# Run in development mode (launches RuneLite with the plugin loaded)
./gradlew run

# Create a fat JAR with all dependencies
./gradlew shadowJar
```

> **Note:** Java 17+ is required. The Gradle wrapper (`gradlew`) will download Gradle 8.10 automatically.

---

## 🏛️ Project Structure

```
src/main/java/haexporterplugin/
├── HAExporterPlugin.java        # Main plugin — subscribes to RuneLite events
├── HAExporterConfig.java        # Configuration interface
├── HAExporterPanel.java         # Swing side-panel UI
│
├── data/                        # Data classes (serialized to JSON)
│   ├── Root.java                #   Top-level payload wrapper
│   ├── Player.java              #   Player snapshot
│   ├── HAConnection.java        #   Stored baseUrl + token pair
│   ├── Stats.java / SkillInfo   #   Skill levels & XP
│   ├── HealthData / PrayerData  #   HP & prayer points
│   ├── Inventory / Equipment    #   Item containers
│   ├── ItemData.java            #   Single item with prices
│   └── LootData.java            #   Loot event details
│
├── events/                      # Event objects added to the events array
│   ├── LevelEvent.java          #   Skill level-up
│   └── DeathEvent.java          #   Player death
│
├── notifiers/                   # Detect & fire events
│   ├── BaseNotifier.java
│   ├── LevelNotifier.java
│   ├── LootNotifier.java
│   ├── DeathNotifier.java
│   ├── ItemNotifier.java
│   └── LocationNotifier.java
│
├── utils/                       # Helpers
│   ├── HomeAssistUtils.java     #   HTTP client (OkHttp3)
│   ├── MessageBuilder.java      #   Builds the Root JSON
│   ├── TickUtils.java           #   Tick-based send scheduling
│   ├── ConfigUtils.java         #   Connection persistence
│   ├── ItemUtils.java           #   RuneLite → ItemData mapping
│   └── RarityUtils.java         #   Drop-rate evaluation
│
└── enums/                       # Enum types (AccountType, etc.)
```

---

## 📄 License

This project is licensed under the **BSD 2-Clause License** — see [LICENSE](LICENSE) for details.

## 🙏 Credits

- **[pajlads / DinkPlugin](https://github.com/pajlads/DinkPlugin)** — large portions of the codebase are based on Dink.
- **[RuneLite](https://runelite.net/)** — the open-source OSRS client that makes this possible.
- **[Home Assistant](https://www.home-assistant.io/)** — the home-automation platform on the receiving end.
- **[OSRS-Data](https://github.com/RedFirebreak/ha-osrs-data)** — the companion Home Assistant integration
