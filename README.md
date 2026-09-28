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
4. Enter the 5-digit code and your Home Assistant base URL (e.g. `https://ha.example.com`).
5. Click **Submit**. The plugin exchanges the code for a long-lived token and stores the connection.

You can pair **multiple** Home Assistant instances — each connection is stored independently.

### 3. Play the game!

Once paired, the plugin automatically sends data on a configurable tick interval and whenever notable events occur (loot, level-ups, deaths, etc.).

---

## 🔗 Data Pairing — How It Works

```text
RuneLite                                Home Assistant
   │                                          │
   │  POST /api/osrs-data/pair                │
   │  Body: { "code": "12345" }        ──────►│
   │                                          │
   │  Response: { "token": "abc123…" } ◄──────│
   │                                          │
   │         ── connection saved ──           │
   │                                          │
   │  POST /api/osrs-data/events              │
   │  Header: X-Osrs-Token: abc123…    ──────►│
   │  Body: <JSON payload>                    │
   │                                          │
```

Each stored connection contains:

```json
{
  "baseUrl": "https://ha.example.com",
  "token": "abc123def456…"
}
```

---

## 📦 JSON Payload Structure

Every message sent to Home Assistant follows this structure:

```jsonc
{
  "player": {
    "name": "PlayerName",
    "accountType": "0",           // 0 = Normal, 1 = Ironman, 2 = Group Ironman, …
    "world": "302",
    "location": { "x": 3222, "y": 3218, "plane": 0 },
    "health": { "current": 85, "max": 99 },
    "prayerPoints": { "current": 52, "max": 70 },
    "spellbook": { "id": 0 },
    "stats": {
      "skills": {
        "Attack":    { "level": 99, "xp": 200000000 },
        "Strength":  { "level": 99, "xp": 200000000 },
        "Defence":   { "level": 75, "xp": 1210421 },
        // … all 23 skills
      }
    },
    "inventory": {
      "items": [
        { "name": "Abyssal whip", "id": 4151, "gePrice": 1650000, "haPrice": 72000, "quantity": 1 }
      ]
    },
    "equipment": {
      "items": [
        { "name": "Dragon defender", "id": 12954, "gePrice": 500000, "haPrice": 68000, "quantity": 1, "equipmentSlot": "SHIELD" }
      ]
    }
  },
  "events": [
    // Only present when something noteworthy happened:
    // Level-up
    { "skill": "Attack", "level": 99 },
    // Loot drop
    { "items": [ … ], "highestValueItem": { … }, "totalValue": 150000, "source": "Zulrah", "type": "NPC" },
    // Death
    { "valueLost": 500000, "danger": "SAFE", "killerName": "Jad", "keptItems": [ … ], "lostItems": [ … ] }
  ],
  "state": "LOGGED_IN",
  "tickDelay": 100
}
```

| Field | Description |
|-------|-------------|
| `player` | Full snapshot of the player's current state |
| `events` | Array of events that fired since the last message (may be empty) |
| `state` | Current `GameState` (e.g. `LOGGED_IN`, `HOPPING`, `LOGIN_SCREEN`) |
| `tickDelay` | Number of game ticks between periodic base messages |

---

## ⚙️ Configuration

Open **RuneLite Settings → HA Exporter** to find these options. Every event type has its own section with an on/off switch at the top. That switch applies to **all** connections, and when it's off the matching checkbox in the side panel is greyed out.

### General

| Option | Default | Description |
|--------|---------|-------------|
| **Update interval (ticks)** | `100` (~60 s) | How often a full state update is sent. Events are always sent right away |
| **Send health / prayer changes instantly** | `on` | Send every HP / prayer change right away instead of waiting for the next update |

### Data Sharing

| Option | Default | Description |
|--------|---------|-------------|
| **Share inventory / equipment / location** | `on` | Include these in every update |

### Loot

| Option | Default | Description |
|--------|---------|-------------|
| **Send loot events** | `on` | Master switch for loot events |
| **Minimum item value (gp)** | `25000` | Only send a drop when an item stack is worth at least this much |
| **Also send if rarer than 1 in X** | `0` (off) | Also send rare drops that are below the minimum value (NPC drops and pickpocketing) |
| **Require value AND rarity** | `off` | A drop must pass both thresholds |
| **Always send these items** | _(empty)_ | Always sent, even below the thresholds. One name per line, `*` wildcard |
| **Never send these items** | _(empty)_ | Never sent. Wins over *Always send* |
| **Never send loot from these sources** | `Einar` | NPC / activity names to ignore (not player names) |
| **Send PK loot** | `on` | Loot from killing other players |
| **PK loot chest: use total value** | `on` | Send PK loot chests when their combined value passes the minimum |
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
| **Collection log: Minimum item value (gp)** | `0` | Items without a GE price (such as pets) are always sent |
| **Diary: Minimum tier** | `Easy` | Only send diary tasks of this tier or harder |
| **Combat tasks: Minimum tier** | `Easy` | Only send combat tasks of this tier or harder |

### Deaths & Superior Spawns

| Option | Default | Description |
|--------|---------|-------------|
| **Send death events** | `on` | Master switch for death events |
| **Send superior spawn events** | `on` | Master switch for superior slayer monster spawns |

--------|---------|-------------|
| **Send Rate** | `100` ticks (~60 s) | How often a full state snapshot is sent |

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