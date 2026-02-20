# Tests Documentation

This document describes the test suite for the RuneLite Home Assistant Data Exporter plugin.
The tests ensure correctness of the data models, events, enums, and utility classes that form the
core of the plugin's data pipeline.

## Running Tests

```bash
./gradlew test
```

## Test Structure

Tests are located under `src/test/java/haexporterplugin/` and mirror the main source structure.

---

### Data Model Tests (`data/`)

These tests verify that the data transfer objects used to serialize game state to JSON
behave correctly. Since these objects are sent to Home Assistant, their correctness is critical.

#### `RootTest`
**Goal:** Validate the top-level `Root` container that holds all player data and events.
- Verifies the constructor initializes an empty events list
- Tests `addEvent` adds events correctly
- Tests `resetEvents` clears all events
- Tests player getter/setter functionality
- Tests event lifecycle (add → reset → add again)

#### `PlayerTest`
**Goal:** Validate the `Player` model which contains all per-player game state.
- Verifies the default constructor leaves all fields null
- Tests getters and setters for all fields (name, accountType, world, location, health, prayer, spellbook, stats, inventory, equipment)
- Tests the `Object`-parameter overloads of `setStats`, `setInventory`, and `setEquipment` with valid types
- Tests that invalid casts throw `ClassCastException`

#### `HealthDataTest`
**Goal:** Validate `HealthData` which stores current and max hitpoints.
- Uses Gson serialization to verify field values since the class has no public getters
- Tests normal, full, zero, and boosted health scenarios

#### `PrayerDataTest`
**Goal:** Validate `PrayerData` which stores current and max prayer points.
- Uses Gson serialization to verify field values since the class has no public getters
- Tests normal, full, zero, and boosted prayer scenarios

#### `SpellbookDataTest`
**Goal:** Validate `SpellbookData` and the static `getSpellbookName` mapping.
- Tests all four known spellbooks: standard (0), ancient (1), lunar (2), arceuus (3)
- Tests that unknown IDs return "unknown"
- Verifies constructor correctly sets both `id` and `name` fields via Gson serialization

#### `StatsTest`
**Goal:** Validate the `Stats` container which maps skill names to `SkillInfo` objects.
- Tests default and parameterized constructors
- Tests skill map getter/setter
- Verifies skills can be stored and retrieved by name

#### `SkillInfoTest`
**Goal:** Validate `SkillInfo` which stores XP and level for a single skill.
- Tests default constructor returns null for both fields
- Tests parameterized constructor
- Tests individual getters and setters
- Tests edge cases (max XP of 200M, zero values)

#### `ItemDataTest`
**Goal:** Validate `ItemData` which represents a single game item.
- Tests that the default constructor initializes quantity to 1
- Tests parameterized constructor with all fields
- Tests stackable items with high quantities
- Tests equipment slot setter
- Tests all individual setters

#### `InventoryTest`
**Goal:** Validate `Inventory` which holds a list of inventory items.
- Tests default and parameterized constructors
- Tests item list getter/setter
- Tests empty inventory
- Tests full inventory (28 slots)
- Tests replacing items

#### `EquipmentTest`
**Goal:** Validate `Equipment` which holds a list of equipped items.
- Tests default and parameterized constructors
- Tests equipment slot assignments
- Tests empty equipment and multiple equipment slots

#### `HAConnectionTest`
**Goal:** Validate `HAConnection` which stores Home Assistant connection details.
- Tests constructor and getters with various URL formats
- Tests with HTTPS, IP addresses, empty values, and null values

---

### Event Tests (`events/`)

#### `DeathEventTest`
**Goal:** Validate the `DeathEvent` class which captures death information for Home Assistant.
- Verifies `DeathEvent` implements `HAExporterEvent` interface
- Tests PvP death events with killer name and items
- Tests NPC death events with NPC killer ID
- Tests safe death events with no value lost
- Tests location serialization (WorldPoint coordinates)
- Tests kept and lost item collections

---

### Enum Tests (`enums/`)

#### `AccountTypeTest`
**Goal:** Validate the `AccountType` enum and its utility methods.
- Tests `get()` returns the correct enum for each valid varbit value (0–6)
- Tests `get()` returns null for out-of-bounds and negative values
- Tests `isHardcore()` returns true only for `HARDCORE_IRONMAN` and `HARDCORE_GROUP_IRONMAN`
- Verifies total enum count matches expected

#### `ExceptionalDeathTest`
**Goal:** Validate `ExceptionalDeath` display names and the `Danger` enum.
- Tests `toString()` returns the correct display name for each exceptional death location
- Verifies `Danger` enum contains exactly three values: SAFE, DANGEROUS, EXCEPTIONAL

---

### Utility Tests (`utils/`)

#### `MessageBuilderTest`
**Goal:** Validate the `MessageBuilder` which assembles game data into JSON payloads.
- Tests constructor initializes a valid `Root` object
- Tests `getPlayer()` creates a `Player` if none exists and returns the same instance
- Tests `setData()` for all supported categories: name, accounttype, health, prayer, spellbook, world, location, stats, inventory, equipment
- Tests category name case insensitivity
- Tests event management (add and reset)
- Tests `resetData()` clears all accumulated state
- Tests `build()` produces valid JSON with correct structure
- Tests JSON output with events and full player data
