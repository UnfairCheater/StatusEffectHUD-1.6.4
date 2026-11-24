# Changelog

## v1.27.1

Backport of **StatusEffectHUD v1.27** (1.7.10) to **Minecraft 1.6.4 / Forge 9.11.1.932+**.

### Added

- Full in-game configuration screen (`/statuseffect config`) with hover descriptions and a live HUD
  position preview. 1.6.4's Forge has no config-GUI framework, so the screen is written from scratch
  on `GuiScreen` + `GuiButton`.
- All 15 options restored, key names identical to 1.7.10 so existing config files carry over.
- `enabled` master switch.
- `/statuseffect config` command.
- Live HUD position preview while adjusting alignment and offsets.
- Right-click any numeric button to step it backwards.

### Changed

- No longer requires `bspkrsCore`; the two helpers it used are reimplemented locally.
- Uses Forge 1.6.4's `net.minecraftforge.common.Configuration` (`Property` style) instead of the 1.7
  config API.
- `ITickHandler` / `TickRegistry` instead of the 1.7 `TickEvent` bus.

### Fixed

- **Inventory shifted 40 px right while the player had potion effects.** The container is now pinned
  to `guiLeft = (width - xSize) / 2` every frame without hiding the vanilla effect list. Fixing this
  required walking the superclass chain for the effect-list flag (`axv.t` is a `float` and `axm.t` is
  an `int`, both shadowing `axp.t`), using the correct 1.6.4 mapping names (`field_74198_m` guiLeft,
  `field_74194_b` xSize — the 1.7.10 names `field_147003_i` / `field_146999_f` do not exist here), and
  no longer swallowing the exception.
- **Creative-mode inventory still off-centre.** `xSize` is now read from the screen instead of being
  hardcoded to 176; `GuiContainerCreative` sets it to 195.
- **Inventory Tweaks sorting/settings buttons left behind.** `guiLeft` is corrected during the client
  tick start (before any mod's tick end), and every existing control in the button list is shifted by
  the same delta so both layout orderings line up.
- **HUD flickered while F1 hid the game GUI.** Both `renderGameOverlay` and the 2D orthographic
  projection it sets up are skipped when `GameSettings.hideGUI` is set, so drawing from the render
  tick landed in world space. The HUD is now gated on the same condition
  (`GameSettings.field_74319_N`, obfuscated `aul.Z`).

### Verification

- 93 API reflection assertions, run against both Forge 9.11.1.1345 and Forge 9.11.1.916 (0 failures
  on each).
- ASM bytecode walk: no dangling type references, no SRG/MCP name leaks.
- `InvShiftFake` drives the real centring code against a hierarchy that reproduces the field collisions,
  for a 176-wide and a 195-wide container, asserting the buttons follow the container.
- `HideGuiCheck` asserts at the bytecode level that the F1 flag is read.
