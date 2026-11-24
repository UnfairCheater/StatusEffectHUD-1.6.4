# StatusEffectHUD for Minecraft 1.6.4

[![Minecraft](https://img.shields.io/badge/Minecraft-1.6.4-informational)](https://www.minecraft.net/)
[![Forge](https://img.shields.io/badge/Forge-9.11.1.932%2B-orange)](https://files.minecraftforge.net/)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

> **edit by yuzhipang & deepseek**
> Original mod by **bspkrs**. This repository is a 1.7.10 → 1.6.4 backport with a full in-game
> config screen and several bug fixes.

**Potion / status effect HUD for Minecraft 1.6.4** — the complete, fully configurable 1.7.10
`StatusEffectHUD v1.27` feature set brought down to Forge 9.11.1.932+.

**Download:** grab the jar from [Releases](../../releases/latest) — `StatusEffectHUD-1.6.4-v1.27.1.jar`

---

## Why this backport exists

The 1.6.4 build (v1.19) renders the HUD fine — its drawing code is nearly line-for-line identical to
1.7.10. What it lacks is the **in-game config screen**, so the position is effectively hardcoded for a
normal player even though it does read `config/StatusEffectHUD.cfg`.

| | 1.6.4 v1.19 | 1.7.10 v1.27 | this port |
|---|---|---|---|
| HUD rendering (align, offset, blink, colours, background) | yes | yes | yes |
| Config file keys | yes (via bspkrsCore) | yes | yes (Forge `Configuration`) |
| **In-game config screen** | **missing** | yes | **yes, hand-written** |
| `enabled` master switch | missing | yes | yes |
| `/statuseffect config` command | missing | yes | yes |
| Requires `bspkrsCore` | **yes** | yes | **no** |

## Features

- **In-game config screen** — `/statuseffect config`, with hover descriptions and a live HUD position
  preview. `Done` and `Esc` both save.
- **All 15 options**, key names identical to 1.7.10 so an existing config file carries over:
  `enabled`, `alignMode`, `hideInventoryEffectList`, `enableBackground`, `enableEffectName`,
  `durationBlinkSeconds`, `enableIconBlink`, `effectNameColor`, `durationColor`, `xOffset`, `yOffset`,
  `yOffsetBottomCenter`, `applyXOffsetToCenter`, `applyYOffsetToMiddle`, `showInChat`
- **9 anchors** — `topleft`/`topcenter`/`topright` × top/middle/bottom
- **Adjustable offsets** — X, Y and bottom-center Y, each with `-` `+` `Rst`; right-click any numeric
  button to step backwards
- **Optional hiding of the vanilla inventory effect list** (`hideInventoryEffectList`, default
  `false`)
- **No bspkrsCore dependency**

## Install

1. Install **Forge 9.11.1.932** or any later 1.6.4 build (e.g. 9.11.1.1345).
2. Drop `StatusEffectHUD-1.6.4-v1.27.1.jar` into `.minecraft/mods/`.
3. In game, run `/statuseffect config`, or edit `config/StatusEffectHUD.cfg` by hand.

> Do not install alongside the original `[1.6.4]StatusEffectHUD` — both use
> `@Mod(modid="StatusEffectHUD")` and FML will refuse to start with a duplicate mod id.
> `bspkrsCore` may stay installed if another bspkrs mod needs it; this port does not.

## Build from source

Requires **JDK 7 or 8** (the output must be Java 6 bytecode, major 50).

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk1.8.0_202'
.\fetch-deps.ps1     # downloads MC 1.6.4 client, Forge universal, LWJGL, libs, ASM
.\build.ps1          # -> build\StatusEffectHUD-1.6.4-v1.27.1.jar
.\verify.ps1         # ASM bytecode checks + API reflection probe on two Forge builds
```

`fetch-deps.ps1` also fetches the ASM 9.7 jars that `verify.ps1` needs.

> `verify.ps1` first writes signature-stripped copies of the game jars into `build/lib-unsigned/`.
> The Mojang-signed client and Forge both live in the default package, and the JVM refuses to load
> them together — stripping is exactly what Forge's own runtime class loader does.

## Fixes beyond the straight port

### Container stays centred when the player has effects

**Symptom:** the whole inventory jumps 40 px to the right as soon as the player has a potion effect.

`InventoryEffectRenderer` widens the container by 40 px and anchors it 40 px further right. The same
private boolean also gates the vanilla effect list, so clearing the flag to stop the shift would hide
the list too. The port instead pins the container back to `guiLeft = (width - xSize) / 2` every frame
and leaves the flag alone.

Three separate bugs had to be fixed to get there:

1. **The field is shadowed by same-named fields of other types.**
   ```
   axv (GuiInventory)           .t = float     <-- shadows
   axm (GuiContainerCreative)   .t = int       <-- shadows too
   axp (InventoryEffectRenderer).t = boolean   <-- the real one
   ```
   `getDeclaredField` does not search superclasses, so the original lookup returned the float/int and
   bailed out silently.
2. **The exception was swallowed** by `catch (Throwable ignored)`, so the feature never worked and
   never logged anything.
3. **`guiLeft` was read with 1.7.10 field names.** `field_147003_i` / `field_146999_f` do not exist
   in the 1.6.4 mapping; the correct names are `field_74198_m` (`guiLeft`) and `field_74194_b`
   (`xSize`).

### Creative mode and Inventory Tweaks buttons

Two more causes of "still off-centre":

- **`xSize` was hardcoded to 176.** A normal inventory is 176 wide but `GuiContainerCreative` sets
  `xSize = 195`, so centring used the wrong width and left the creative screen ~10 px right. The port
  now reads `GuiContainer.field_74194_b` from the screen.
- **Inventory Tweaks lays its buttons out once**, from `guiLeft`, and never repositions them. The port
  corrects `guiLeft` in the **client tick start** (before any mod's tick end, so mods that lay out in
  tick end see the corrected value) and also shifts every existing control in the button list by the
  same delta, so both orderings end up aligned.

### No more flicker while F1 hides the HUD

**Symptom:** with F1 held, the potion HUD flickers as the camera moves.

Because this mod draws from the render tick rather than from `RenderGameOverlayEvent`, vanilla's own
guard does not cover it:

```java
if (!gameSettings.hideGUI || currentScreen != null) {
    minecraft.ingameGUI.renderGameOverlay(...);   // <- also sets up the 2D orthographic projection
}
```

With F1 held, `renderGameOverlay` is skipped, so the projection is still 3D and the HUD gets drawn into
world space. The port now gates its drawing on the same condition
(`GameSettings.field_74319_N`, obfuscated `aul.Z`).

## Verification

`verify.ps1` runs four checks:

| Check | What it does |
|---|---|
| 1 | ASM walk of every class: no dangling type references, no SRG/MCP name leaks, every reflection target name present in the 1.6.4 mapping table |
| 2 | **93 API reflection assertions** against the real 1.6.4 + Forge 9.11.1.1345 jars |
| 2b | `tools/InvShiftFake.java` — fake hierarchy mirroring the real `axv`/`axm`/`axp` field collisions, driving the real `centreContainer` for both a 176-wide and a 195-wide container, asserting the container centres **and** the Inventory-Tweaks-shaped buttons follow |
| 2c | `tools/HideGuiCheck.java` — ASM assertion that `onRenderTick` actually reads `aul.Z`, so the F1 fix cannot silently regress |
| 3 | repeats check 2 against **Forge 9.11.1.916** |

All checks pass on both Forge builds. The jar's bytecode is Java 6 (major 50).

## Source layout

| File | Purpose |
|---|---|
| `src/StatusEffectHUD.java` | HUD rendering, all config fields, container centring, effect-list toggle |
| `src/SEHConfig.java` | Forge `Configuration` read/write, key names matching 1.7.10 |
| `src/SEHConfigGui.java` | Hand-written in-game config screen (1.6.4 has no Forge config-GUI framework) |
| `src/SEHTicker.java` | `ITickHandler`: drives the HUD, pins the container, opens the config screen |
| `src/StatusEffectHUDMod.java` | `@Mod` entry point |
| `src/CommandStatusEffect.java` | `/statuseffect config` |
| `tools/*.java` | Verification harnesses |

**Why the sources are in the default package:** Minecraft 1.6.4's game classes (`atv`, `awe`, `ni`, …)
themselves live in the default package, and their package-private members are only reachable from
there. 1.7.10 has no such problem because it uses the fully qualified MCP names.

**Why Java 6 bytecode:** 1.6.4-era toolchains emitted major version 50; `-source 1.6 -target 1.6`
matches what ForgeGradle 1.2 produced.

## Credits & license

MIT. Original **StatusEffectHUD** by **bspkrs**; this 1.6.4 backport, config screen and fixes by
**yuzhipang & deepseek**. See [LICENSE](LICENSE).
