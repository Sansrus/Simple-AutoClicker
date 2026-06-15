# Simple AutoClicker

A configurable client-side autoclicker mod for Minecraft Fabric. Supports delayed, timed, and spam-based execution of various in-game actions.

**[Русская версия](README.ru.md)**

---

## Features

- **Multiple independent profiles** — each autoclick has its own name, action, timing, and toggles
- **Flexible timing** — separate interval (pause) and use duration (hold time) in ticks
- **Spam mode** — repeats every tick with no delay (`interval = 0`)
- **Alternative actions** — bind autoclickers to any vanilla keybinding (jump, inventory, F-keys, etc.) ⚠ does not work with all actions
- **Graphical interface** — open via a configurable hotkey or through ModMenu; toggle on/off with a keybind
- **HUD overlay** — shows when the autoclicker is enabled/disabled

## Available Actions

| Action                                 | Description |
|----------------------------------------|---|
| Walk Forward / Backward / Left / Right | Hold movement keys |
| Sprint                                 | Hold sprint |
| Jump                                   | Jump (safe: ground, water, lava, climbable, riding) |
| Sneak                                  | Hold sneak |
| Use                                    | Right-click: items (food, bow, crossbow, etc.), blocks, entities |
| Attack                                 | Left-click: entity or block |
| Drop                                   | Throw out held item |
| Pick Block                             | Pick block/entity to hotbar (middle-click) |
| Swap Hands                             | Swap main and offhand items |
| Toggle Perspective                     | Cycle camera perspective |
| Screenshot                             | Take a screenshot |
| Hotbar 1–9                             | Select hotbar slot |
| Alternative                            | Trigger any vanilla keybinding via the picker |

## How to Use

1. Press the configured hotkey to open the GUI, or open it via ModMenu
2. Add an autoclick profile, choose an action, set timings
3. Press the toggle keybind (default unbound — set in vanilla Controls) to enable/disable the autoclicker

### Tips

- **For blocks (Use action):** set `useDuration > 1` for continuous placement; `useDuration = 1` places exactly one block per cycle
- **For items (Use action):** the mod automatically holds right-click for consumables (food, bow); release happens after `useDuration` ticks
- **Alternative action:** click the "Select" button to open the keybind picker; all vanilla keybindings are grouped by category with search. ⚠ Some actions may not work correctly!
- **Global toggle:** bind a key in Vanilla Controls → Simple AutoClicker category → "Toggle"

## Timing Model

The mod uses **client ticks** (20 ticks = 1 second at 20 TPS).

- **Interval (Wait):** pause between action cycles in ticks
- **Use Duration:** how many ticks the action stays active per cycle
- **Spam mode:** interval = 0 — the action fires every tick continuously

## Requirements

- Minecraft 26.1 (1.21.5)
- Fabric Loader ≥0.16.0
- Fabric API (recommended)

## Dependencies (included)

- [Cloth Config](https://modrinth.com/mod/cloth-config) — GUI configuration
- [ModMenu](https://modrinth.com/mod/modmenu) — integration into the mods screen
