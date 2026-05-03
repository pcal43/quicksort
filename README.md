# Quicksort

Quicksort is a vanilla-friendly item sorting mod for Fabric. It does not add new blocks; a regular chest becomes a quicksort chest based on the block placed underneath it.

## Version

- Minecraft: `26.1`
- Loader: `Fabric`
- Mod version: `0.22.0+26.1`
- Java: `25`

## Dependencies

- Fabric Loader `0.19.2` or newer
- Fabric API for Minecraft `26.1`
- Cloth Config API `26.1.154`
- Mod Menu `18.0.0-alpha.8` is optional, but recommended for editing the config in-game

## Features

- Lightweight item sorting using vanilla chests.
- Configurable quicksort chest types and ranges.
- Config screen through Mod Menu and Cloth Config.
- Optional obstruction checks between the source chest and target chests.
- Transfer by individual item counts or by stack counts.
- Optional redstone activation.
- Optional processing while the source chest is open.
- Configurable sound volume, pitch, cooldown, and animation mode.

## Usage

1. Place one of the configured base blocks in your storage area.
2. Place a regular chest on top of that block. This chest becomes the quicksort chest.
3. Place matching items in nearby target chests.
4. Put items into the quicksort chest.
5. Close the quicksort chest to start sorting, or toggle redstone from off to on if redstone activation is enabled for that base block.

Items are sent to nearby configured target containers that already contain matching items or have room for a matching stack.

![](https://github.com/pcal43/quicksort/raw/main/etc/quicksort-demo2.gif)

## Default Base Blocks

The default configuration includes:

- Iron block: radius `5`
- Copper block: radius `5`
- Gold block: radius `10`
- Emerald block: radius `25`
- Diamond block: radius `50`
- Netherite block: radius `100`

These values can be edited in the config file or through Mod Menu.

## Configuration

The default config is stored at:

`src/main/resources/quicksort-default-config.json5`

At runtime, the generated config is:

`config/quicksort.json5`

Important options:

- `range`: cube radius used to find target containers.
- `cooldownTicks`: ticks between transfer attempts.
- `animationTicks`: animation duration. Use `-1` to disable animation.
- `animationMode`: `PARTICLE` or `ENTITY`. `PARTICLE` is the default and safest mode.
- `soundVolume`: transfer sound volume.
- `soundPitch`: transfer sound pitch.
- `checkObstructions`: when true, solid blocks between the quicksort chest and target container block that transfer.
- `transferMode`: `ITEMS` or `STACKS`.
- `transferAmount`: amount used by the selected transfer mode each cooldown cycle.
- `enableRedstoneActivation`: starts sorting when the base block receives a redstone rising edge.
- `continueWhileOpen`: keeps sorting while the quicksort chest is open.
- `enchantmentMatchingIds`: item ids that only match when enchantments/components also match.
- `targetContainerIds`: block ids that can receive sorted items.

Older config files can omit newly added values; missing values inherit from defaults.

## Transfer Modes

`ITEMS` sends up to `transferAmount` items per item type each cooldown cycle.

Example: with `transferAmount: 5`, ten different item types can each send five items, for a total of fifty items in that cycle.

`STACKS` sends up to `transferAmount` stacks total each cooldown cycle.

Example: with `transferAmount: 5`, only five stacks are transferred in that cycle, even if the quicksort chest contains more item types.

## Redstone Behavior

When `enableRedstoneActivation` is true, sorting starts when the base block changes from unpowered to powered.

The mod avoids creating duplicate jobs for the same quicksort chest. If redstone remains powered and no job is active, the mod can retry after a short delay. However, redstone that is already powered while the world loads is treated as the initial state and does not start sorting automatically.

## World Load Behavior

Quicksort does not resume unfinished sorting jobs automatically when a world loads.

This is intentional. Starting sorting jobs while the integrated server is still loading chunks can block spawn loading and leave Minecraft stuck at `Preparing spawn area: 16%`.

After entering the world, start sorting through normal triggers:

- close the quicksort chest, or
- toggle redstone from off to on when `enableRedstoneActivation` is enabled.

## Notes

- Target containers must be loaded and within the configured cube radius.
- If `checkObstructions` is enabled, the path is checked when a target is found and again during transfer attempts.
- Other quicksort chests are skipped as sorting targets.
- The `ENTITY` animation mode uses temporary non-saving item entities. Use `PARTICLE` if you want the safest behavior.

## Credits

Icon components courtesy of:

- [Minecraft Toolbox - Minecraft Chest PNG](https://flyclipart.com/minecraft-toolbox-minecraft-chest-png-50783)
- [transparentpng.com](https://www.transparentpng.com/download/circle-vector-2_15270.html)
- [freesvg.org](https://freesvg.org/8-directions-arrows)

## Legal

This mod is published under the [MIT License](LICENSE).

You are free to include this mod in your modpack provided you attribute it to pcal.net.

## Questions?

If you have questions about this mod, please join the Discord server:

[https://discord.pcal.net](https://discord.pcal.net)

Comments have been disabled and I will not reply to private messages on CurseForge.
