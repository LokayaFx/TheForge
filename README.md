# TheForge

Real-time forging plugin for **Paper 1.18.2 through 26.2**.

Author: **Lokayaa** · https://cwresports.lk

Forge items over real time, even while offline. Slots, levels and
custom recipes/categories, all through one GUI.

## Project layout

This is a Maven multi-module build:

| Module | Target | Java | Output |
| --- | --- | --- | --- |
| `common` | Shared plugin logic, compiled against Paper 1.18.2 | 17 | `common/target/TheForge-Common-26.2.jar` |
| `v1_18` | Paper 1.18.2 | 17 | `v1_18/target/TheForge-1.18.2.jar` |
| `v1_19` | Paper 1.19.x | 17 | `v1_19/target/TheForge-1.19.4.jar` |
| `v1_20` | Paper 1.20.x | 17 | `v1_20/target/TheForge-1.20.4.jar` |
| `v1_21` | Paper 1.21.x | 21 | `v1_21/target/TheForge-1.21.jar` |
| `v26` | Paper 26.2 | 25 | `v26/target/TheForge-26.2.jar` |

`common` holds the code and default configs. Each version module only adds
a small platform adapter (`GlowPlatform`) and its `plugin.yml`, then shades
`common` into the final jar. `common` targets the oldest supported API so
the shared code compiles for every server version. Only the version jars are
installable; `TheForge-Common` is a library.

The version modules are compiled against the oldest API of their line
(1.19.4, 1.20.4 and 1.21) so each jar runs across its whole series. The
`v1_18` jar still works as a fallback on 1.19-1.21, but the dedicated jars
are recommended. The `v26` jar is only for **26.2+**.

## Install

1. Build all jars: `mvn -DskipTests package`.
   On Windows without Maven on the PATH, use
   `.build-tools/apache-maven-3.9.11/bin/mvn.cmd -DskipTests package`.
2. Pick the jar for your server:
   * Paper 1.18.2 → `v1_18/target/TheForge-1.18.2.jar`
   * Paper 1.19.x → `v1_19/target/TheForge-1.19.4.jar`
   * Paper 1.20.x → `v1_20/target/TheForge-1.20.4.jar`
   * Paper 1.21.x → `v1_21/target/TheForge-1.21.jar`
   * Paper 26.2+ → `v26/target/TheForge-26.2.jar`
3. Drop that jar into your server's `plugins/` folder.
4. Start the server (Java 17 for 1.18.2-1.20.4, Java 21 for 1.20.5-1.20.6
   and 1.21.x, Java 25 for 26.2).
5. Edit `plugins/TheForge/config.yml`, `categories.yml`, `messages.yml`,
   `slots.yml` and the files in `plugins/TheForge/gui/`, then run `/forge reload`.

## GUI customization

The plugin creates `gui/main.yml`, `gui/categories.yml` and `gui/recipes.yml`
in its data folder. These files let you customize menu titles, button slot
numbers, materials, names and lore. Category icons and names remain
configurable in `categories.yml`. GUI slot lists must keep the same number of
entries as the defaults, and button slot numbers must be unique within a menu.

GUI buttons can use `PLAYER_HEAD`. Add `owner: PlayerName` for a player's skin,
or `texture: <Base64 texture value>` for a custom head. A direct
`https://textures.minecraft.net/texture/...` URL is also accepted.

MMOItems, EcoItems, AuraSkills/AureliumSkills and EcoSkills support is optional.
The plugin loads fine without them; recipes that use their items stay in the
config and become craftable once the plugin is installed.

## categories.yml

Categories and their recipes live in one file:

```yaml
recipe_categories:
  parts:
    display_name: Parts
    required-level: 1
    icon:
      material: IRON_INGOT
      name: '&7Parts'
    recipes:
      iron_pickaxe_head:
        result:
          material: IRON_INGOT
          name: '&fIron Pickaxe Head'
          amount: 1
          custom_model_data: 10001
          lore:
          - '&7A forged pickaxe head'
        time_in_seconds: 3600
        experience: 3600
        required-level: 1
        ingredients:
          iron_ingot:
            material: IRON_INGOT
            amount: 3
```

* `time_in_seconds` controls the job duration.
* `experience` is the Forge XP granted when the job completes. If omitted,
  it defaults to `time_in_seconds`.
* `required-level` on a recipe or category locks it until the player reaches
  that Forge level. Locked entries stay visible in the GUI with their requirement.
* `result` supports `material`, `name`, `amount`, `lore` and `custom_model_data`.
* Ingredient ids are free-form; each one needs `material` and `amount`.
* Categories without a `recipes` section (like `tools` and `items`) show up
  as empty categories.

### Custom items (EcoItems / MMOItems)

EcoItems items use their id:

```yaml
result:
  ecoitems: mithril_sword
  amount: 1
```

or inline on the material field:

```yaml
material: ecoitems:mithril_sword
```

MMOItems items accept a type and id, or just an id to search every type:

```yaml
result:
  mmoitems: SWORD:CUTLASS
```

```yaml
result:
  mmoitems: CUTLASS
```

The legacy `mmoitems_type` + `mmoitems_id` fields still work. The same keys are
used inside `ingredients` sections.

## Commands

| Command | Description |
| --- | --- |
| `/forge` | Open the forge GUI |
| `/forge help` | Show help |
| `/forge reload` | Reload all configs (admin) |
| `/forge settime <player> <seconds>` | Set total forging time stat (admin) |
| `/forge setlevel <player> <1-7>` | Set forge level (admin) |
| `/forge setxp <player> <amount>` | Set Forge XP (admin) |
| `/forge addxp <player> <amount>` | Add or remove Forge XP (admin) |
| `/forge category add <name> [display] [icon]` | Create a category (admin) |
| `/forge category remove <name>` | Delete a category and its recipes (admin) |
| `/forge category list` | List categories |
| `/forge recipe add <category> <hand\|item> <time> <materials>` | Add a recipe (admin) |
| `/forge recipe remove <category> <recipe>` | Remove a recipe (admin) |
| `/forge recipe list [category]` | List recipes |

Alias: `/tf`

### Recipe examples

```text
/forge recipe add ores DIAMOND 2h DIAMOND:2,COAL:8
/forge recipe add parts hand 1h30m IRON_INGOT:4,STICK:2
/forge recipe add armor mmoitems:ARMOR:STEEL_CHESTPLATE 6h ia:MATERIAL:STEEL_INGOT:8
/forge recipe add parts ecoitems:mithril_sword 1h ecoitems:mithril_ingot:4,COAL:8
```

* Time formats: `3600`, `30m`, `1h`, `1h30m`, `2h15m30s`
* Ingredients: `MATERIAL:amount`, `ecoitems:ID:amount`, `mmoitems:TYPE:ID:amount`
  or `mmoitems:ID:amount` (`ia:TYPE:ID:amount` is an alias for MMOItems)
* `hand` uses the item in your main hand, including custom model data / MMOItems tags.

## Progression

Completing a recipe grants **Forge XP** (recipe `experience`, defaulting to its
time in seconds). Levels are driven by cumulative XP; recipes and whole
categories can require a minimum level via `required-level`.

| Level | XP required | Time reduction | Slots |
| --- | --- | --- | --- |
| 1 | 0 | 0% | 1 |
| 2 | 1,200 | 10% | 2 |
| 3 | 2,400 | 20% | 3 |
| 4 | 3,600 | 30% | 4 |
| 5 | 7,200 | 40% | 5 |
| 6 | 10,800 | 50% | 6 |
| 7 | 14,400 | 60% | 7 |

XP thresholds, time reductions and slots are configurable in `config.yml`.

### Slot unlock requirements

`slots.yml` controls when each forge slot unlocks. Unlisted slots default to
requiring Forge Level equal to the slot number, and a slot unlocks only when
every requirement is met:

```yaml
slots:
  4:
    required-level: 4
    aureliumskills:
      farming: 10
  5:
    required-level: 5
    ecoskills:
      mining: 15
```

`aureliumskills` works with both AuraSkills and the legacy AureliumSkills API.
If the skill plugin is not installed, slots with that requirement stay locked.

## Data

Player data is stored in `plugins/TheForge/playerdata.yml`, under each player's
UUID. Jobs keep running while the player is offline and are credited on join.
Older per-player files in `playerdata/<uuid>.yml` are migrated when that player
next joins.

## Permissions

| Permission | Default | Description |
| --- | --- | --- |
| `theforge.use` | true | Open the forge and forge items |
| `theforge.admin` | op | Reload, settime, setlevel, recipe/category management |

## Notes

* XP is granted when a job finishes, even if the player collects it later.
* Running jobs keep the duration they started with; a level-up never changes
  an active job's timer.
* Older player data that only stored total forging time is migrated to XP on load.
