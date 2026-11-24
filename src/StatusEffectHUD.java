import java.util.HashMap;
import java.util.Map;

import bspkrs.seh.util.Log;

/**
 * Backport of StatusEffectHUD 1.7.10 (v1.27) to Minecraft 1.6.4 / Forge 9.11.1.932+.
 *
 * <p>This class lives in the default package on purpose: Minecraft 1.6.4 ships its
 * (obfuscated) game classes in the default package, so the HUD code must share that package
 * in order to reference them. Forge 1.6.4 does not reobfuscate mod jars -- FML remaps
 * mod class references at runtime -- so the obfuscated 1.6.4 names may be used directly.
 * The equivalent readable (SRG/MCP) name of every game member used here is given in a
 * comment so the code stays reviewable.</p>
 */
public class StatusEffectHUD
{
    /* ------------------------------------------------------------------ *
     *  Configuration state                                                *
     * ------------------------------------------------------------------ */

    public static final String VERSION_NUMBER = "v1.27.1(1.6.4)";

    /** internal z-level used for the HUD (SRG: zLevel) */
    protected static float zLevel = -150.0F;

    /** true while the HUD should be drawn (SRG: enabled) */
    public static boolean enabled = true;

    /** topleft..bottomright (SRG: alignMode) */
    public static String alignMode = "middleright";

    /** hide the vanilla potion list in the inventory screen (SRG: hideInventoryEffectList) */
    public static boolean hideInventoryEffectList = false;

    /** draw the effect background box (SRG: enableBackground) */
    public static boolean enableBackground = false;

    /** draw effect names (SRG: enableEffectName) */
    public static boolean enableEffectName = true;

    /** blink the icon when an effect is about to expire (SRG: enableIconBlink) */
    public static boolean enableIconBlink = true;

    /** seconds left before the timer starts blinking, -1 disables (SRG: durationBlinkSeconds) */
    public static int durationBlinkSeconds = 10;

    /** colour code for the effect name, 0-9a-f (SRG: effectNameColor) */
    public static String effectNameColor = "f";

    /** colour code for the timer, 0-9a-f (SRG: durationColor) */
    public static String durationColor = "f";

    /** horizontal offset from the screen edge (SRG: xOffset) */
    public static int xOffset = 2;

    /** vertical offset from the screen edge (SRG: yOffset) */
    public static int yOffset = 2;

    /** vertical offset used by the bottomcenter alignment (SRG: yOffsetBottomCenter) */
    public static int yOffsetBottomCenter = 41;

    /** apply xOffset to centre alignments as well (SRG: applyXOffsetToCenter) */
    public static boolean applyXOffsetToCenter = false;

    /** apply yOffset to middle alignments as well (SRG: applyYOffsetToMiddle) */
    public static boolean applyYOffsetToMiddle = false;

    /** keep the HUD visible while chat is open (SRG: showInChat) */
    public static boolean showInChat = true;

    /* ------------------------------------------------------------------ *
     *  Runtime state                                                      *
     * ------------------------------------------------------------------ */

    /** net.minecraft.client.gui.ScaledResolution, obf: awf */
    private static awf scaledResolution;

    /** highest duration seen for each active effect, used for the blink threshold */
    private static final Map<nj, Integer> potionMaxDurationMap = new HashMap<nj, Integer>();

    private StatusEffectHUD()
    {
    }

    /* ------------------------------------------------------------------ *
     *  Rendering                                                          *
     * ------------------------------------------------------------------ */

    /**
     * Called from the client tick handler every tick.
     */
    public static void onClientTick(atv mc)
    {
        potionMaxDurationMap.size();
    }

    /**
     * Called from the render tick handler; draws the HUD when appropriate.
     *
     * @return true, kept for parity with the 1.7.10 implementation
     */
    public static boolean onRenderTick(atv mc)
    {
        if (mc == null || mc.h == null)
        {
            return true;
        }

        // mc.A                     -> field_71415_G  inGameHasFocus
        // mc.n                     -> field_71462_r  currentScreen
        // mc.u.ab                  -> field_74330_P  pauseGame
        // mc.u.T.e                 -> field_74513_e  KeyBinding.pressed (third person)
        // mc.u.Z                   -> field_74319_N  hideGUI (F1)
        //
        // The hideGUI check matters for more than tidiness: EntityRenderer only calls
        // renderGameOverlay when "!gameSettings.hideGUI || currentScreen != null", and that call is
        // what sets up the 2D orthographic projection. Drawing from the render tick while the GUI is
        // hidden therefore happens with leftover 3D state, which made the HUD flicker as the camera
        // moved. Skipping the draw when F1 is active is both correct and what stops the flicker.
        // The condition mirrors vanilla's, so F1 + an open chat screen still draws.
        if (enabled
                && (!mc.u.Z || mc.n != null)
                && (mc.A || mc.n == null || (mc.n instanceof auw && showInChat))
                && !mc.u.ab
                && !mc.u.T.e)
        {
            org.lwjgl.opengl.GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            scaledResolution = new awf(mc.u, mc.d, mc.e);
            displayStatusEffects(mc);
            org.lwjgl.opengl.GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        }

        return true;
    }

    /**
     * Horizontal position of the HUD for the current alignment.
     */
    public static int getX(int width)
    {
        if (alignMode.equalsIgnoreCase("topcenter") || alignMode.equalsIgnoreCase("middlecenter")
                || alignMode.equalsIgnoreCase("bottomcenter"))
        {
            return scaledResolution.a() / 2 - width / 2 + (applyXOffsetToCenter ? xOffset : 0);
        }
        if (alignMode.equalsIgnoreCase("topright") || alignMode.equalsIgnoreCase("middleright")
                || alignMode.equalsIgnoreCase("bottomright"))
        {
            return scaledResolution.a() - width - xOffset;
        }
        return xOffset;
    }

    /**
     * Vertical position of the HUD for the current alignment.
     */
    public static int getY(int rowCount, int height)
    {
        if (alignMode.equalsIgnoreCase("middleleft") || alignMode.equalsIgnoreCase("middlecenter")
                || alignMode.equalsIgnoreCase("middleright"))
        {
            return scaledResolution.b() / 2 - rowCount * height / 2 + (applyYOffsetToMiddle ? yOffset : 0);
        }
        if (alignMode.equalsIgnoreCase("bottomleft") || alignMode.equalsIgnoreCase("bottomright"))
        {
            return scaledResolution.b() - rowCount * height - yOffset;
        }
        if (alignMode.equalsIgnoreCase("bottomcenter"))
        {
            return scaledResolution.b() - rowCount * height - yOffsetBottomCenter;
        }
        return yOffset;
    }

    public static int getScaledWidth()
    {
        return scaledResolution != null ? scaledResolution.a() : 0;
    }

    public static int getScaledHeight()
    {
        return scaledResolution != null ? scaledResolution.b() : 0;
    }

    /**
     * Recomputes the scaled resolution. Called by the config GUI, because the HUD itself is not
     * drawn while a screen is open and would therefore not have refreshed it.
     */
    public static void refreshScaledResolution()
    {
        atv mc = atv.w();
        if (mc != null && mc.u != null)
        {
            scaledResolution = new awf(mc.u, mc.d, mc.e);
        }
    }

    /**
     * True while the given effect should be visible; used to blink near the end of an effect.
     */
    private static boolean shouldRender(nj pe, int ticksLeft, int thresholdSeconds)
    {
        Integer max = potionMaxDurationMap.get(pe);
        if (max != null && max.intValue() > 400 && ticksLeft / 20 <= thresholdSeconds)
        {
            return ticksLeft % 20 < 10;
        }
        return true;
    }

    private static void displayStatusEffects(atv mc)
    {
        // mc.h          -> field_71439_g  thePlayer
        // .aL()         -> func_70651_bq  EntityLivingBase.getActivePotionEffects()
        java.util.Collection<nj> activeEffects = mc.h.aL();

        if (activeEffects.isEmpty())
        {
            return;
        }

        int rowHeight = enableBackground ? 33 : (enableEffectName ? 20 : 18);
        if (activeEffects.size() > 5 && enableBackground)
        {
            rowHeight = 132 / (activeEffects.size() - 1);
        }

        int yBase = getY(activeEffects.size(), rowHeight);

        for (nj potionEffect : activeEffects)
        {
            Integer knownMax = potionMaxDurationMap.get(potionEffect);
            if (knownMax == null || knownMax.intValue() < potionEffect.b())
            {
                potionMaxDurationMap.put(potionEffect, Integer.valueOf(potionEffect.b()));
            }

            // ni.a          -> Potion.potionTypes
            // .a()          -> func_76456_a  PotionEffect.getPotionID()
            ni potion = ni.a[potionEffect.a()];

            org.lwjgl.opengl.GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

            // mc.J()        -> func_110434_K  getTextureManager()
            // .a(bjo)       -> func_110577_a  bindTexture()
            mc.J().a(new bjo("textures/gui/container/inventory.png"));

            // mc.l          -> field_71466_p  fontRenderer
            // .a(String)    -> func_78256_a   getStringWidth()
            int xBase = getX(enableBackground ? 120 : 22 + mc.l.a("0:00"));

            String potionName = "";
            if (enableEffectName)
            {
                // potion.a() -> func_76393_a  getName()
                // bu.a(s)    -> func_74838_a  StatCollector.translateToLocal()
                potionName = bu.a(potion.a());
                int amplifier = potionEffect.c(); // func_76458_c getAmplifier()
                if (amplifier == 1)
                {
                    potionName = potionName + " II";
                }
                else if (amplifier == 2)
                {
                    potionName = potionName + " III";
                }
                else if (amplifier == 3)
                {
                    potionName = potionName + " IV";
                }
                xBase = getX(enableBackground ? 120 : 22 + mc.l.a(potionName));
            }

            // ni.a(nj) -> func_76389_a  Potion.getDurationString()
            String effectDuration = ni.a(potionEffect);

            if (enableBackground)
            {
                drawTexturedRect(xBase, yBase, 0, 166, 140, 32);
            }

            if (alignMode.toLowerCase().contains("right"))
            {
                xBase = getX(0);

                // potion.d() -> func_76400_d  hasStatusIcon()
                // potion.e() -> func_76392_e  getStatusIconIndex()
                if (potion.d()
                        && (!enableIconBlink
                                || shouldRender(potionEffect, potionEffect.b(), durationBlinkSeconds)))
                {
                    drawStatusIcon(potion.e(),
                            xBase + (enableBackground ? -24 : -18),
                            yBase + (enableBackground ? 7 : 0));
                }

                int stringWidth = mc.l.a(potionName);
                // mc.l.a(String,int,int,int) -> func_78261_a drawStringWithShadow
                mc.l.a("\u00a7" + effectNameColor + potionName + "\u00a7r",
                        xBase + (enableBackground ? -10 : -4) - 18 - stringWidth,
                        yBase + (enableBackground ? 6 : 0), 0xFFFFFF);

                stringWidth = mc.l.a(effectDuration);
                if (shouldRender(potionEffect, potionEffect.b(), durationBlinkSeconds))
                {
                    mc.l.a("\u00a7" + durationColor + effectDuration + "\u00a7r",
                            xBase + (enableBackground ? -10 : -4) - 18 - stringWidth,
                            yBase + (enableBackground ? 6 : 0) + (enableEffectName ? 10 : 5), 0xFFFFFF);
                }
            }
            else
            {
                if (potion.d())
                {
                    drawStatusIcon(potion.e(),
                            xBase + (enableBackground ? 6 : 0),
                            yBase + (enableBackground ? 7 : 0));
                }

                mc.l.a("\u00a7" + effectNameColor + potionName + "\u00a7r",
                        xBase + (enableBackground ? 10 : 4) + 18,
                        yBase + (enableBackground ? 6 : 0), 0xFFFFFF);

                if (shouldRender(potionEffect, potionEffect.b(), durationBlinkSeconds))
                {
                    mc.l.a("\u00a7" + durationColor + effectDuration + "\u00a7r",
                            xBase + (enableBackground ? 10 : 4) + 18,
                            yBase + (enableBackground ? 6 : 0) + (enableEffectName ? 10 : 5), 0xFFFFFF);
                }
            }

            yBase += rowHeight;
        }

        // drop entries for effects that are no longer active
        java.util.Iterator<nj> it = potionMaxDurationMap.keySet().iterator();
        while (it.hasNext())
        {
            if (!activeEffects.contains(it.next()))
            {
                it.remove();
            }
        }
    }

    private static void drawStatusIcon(int iconIndex, int x, int y)
    {
        drawTexturedRect(x, y, iconIndex % 8 * 18, 198 + iconIndex / 8 * 18, 18, 18);
    }

    /**
     * Small helper that avoids pulling in bspkrsCore's HUDUtils.
     */
    private static void drawTexturedRect(int x, int y, int u, int v, int width, int height)
    {
        float f = 0.00390625F;
        bfq tessellator = bfq.a;
        tessellator.b();
        tessellator.a(x + 0, y + height, zLevel, (u + 0) * f, (v + height) * f);
        tessellator.a(x + width, y + height, zLevel, (u + width) * f, (v + height) * f);
        tessellator.a(x + width, y + 0, zLevel, (u + width) * f, (v + 0) * f);
        tessellator.a(x + 0, y + 0, zLevel, (u + 0) * f, (v + 0) * f);
        tessellator.a();
    }

    /* ------------------------------------------------------------------ *
     *  Vanilla inventory effect list suppression                          *
     * ------------------------------------------------------------------ */

    /**
     * Class that actually declares the "show the effect list" flag
     * ({@code net.minecraft.client.renderer.InventoryEffectRenderer}, obf: {@code axp}).
     *
     * <p>Important: the screen the player sees is a <em>subclass</em> such as
     * {@code GuiInventory} ({@code axv}), and the flag is declared on {@code axp}. A plain
     * {@code getDeclaredField} on {@code renderer.getClass()} therefore never finds it - the
     * superclass chain has to be walked. That mistake is exactly why the inventory used to stay
     * shifted to the right whenever the player had a potion effect.</p>
     */
    private static Class<?> inventoryEffectListOwner;

    /** The flag itself, resolved once. */
    private static java.lang.reflect.Field inventoryEffectListField;

    private static boolean inventoryEffectListFieldResolved = false;

    private static boolean inventoryEffectListResolveLogged = false;

    /** Set when the flag was actually changed, so it is only logged once per screen. */
    private static boolean inventoryEffectListChangeLogged = false;

    /**
     * Toggles the private "show the effect list" flag of
     * {@link net.minecraft.client.renderer.InventoryEffectRenderer} (obf: axp).
     *
     * <p>That single flag drives both the vanilla effect list <em>and</em> the 40px rightward shift
     * of the container, so it is only touched when the user asked for the list to be hidden
     * ({@code hideInventoryEffectList}). By default the list stays visible - see
     * {@link #centreContainer}, which pins the container in place instead.</p>
     *
     * @return true when the flag currently says "do not show the list"
     */
    public static boolean setInventoryEffectListVisible(Object renderer, boolean visible)
    {
        if (renderer == null)
        {
            return !visible;
        }

        try
        {
            if (!inventoryEffectListFieldResolved)
            {
                resolveInventoryEffectListField(renderer.getClass());
            }

            if (inventoryEffectListField == null)
            {
                return false;
            }

            boolean current = ((Boolean) inventoryEffectListField.get(renderer)).booleanValue();
            if (current != visible)
            {
                inventoryEffectListField.set(renderer, Boolean.valueOf(visible));
                if (visible)
                {
                    inventoryEffectListChangeLogged = false;
                }
                else if (!inventoryEffectListChangeLogged)
                {
                    inventoryEffectListChangeLogged = true;
                    Log.debug("hid the vanilla inventory effect list via "
                            + inventoryEffectListOwner.getName() + "." + inventoryEffectListField.getName());
                }
            }

            return !visible;
        }
        catch (Throwable t)
        {
            // never let a cosmetic feature break the game, but say so once
            if (!inventoryEffectListResolveLogged)
            {
                inventoryEffectListResolveLogged = true;
                Log.warn("inventory effect list suppression failed: " + t);
            }
            return false;
        }
    }

    /* ------------------------------------------------------------------ *
     *  Keeping the container in place when the player has effects         *
     * ------------------------------------------------------------------ */

    /**
     * {@code GuiContainer.field_74198_m} (obf: {@code p}) - the container's left edge. The
     * container is laid out as {@code guiLeft = (width - xSize) / 2}, then pushed 40px right by
     * {@code InventoryEffectRenderer} when the player has active effects.
     */
    private static java.lang.reflect.Field guiLeftField;
    private static Class<?> guiLeftOwner;

    /** {@code GuiScreen.field_73880_f} (obf: {@code g}) - the scaled screen width. */
    private static java.lang.reflect.Field screenWidthField;

    /**
     * {@code GuiContainer.field_74194_b} (obf: {@code c}) - the container width, read instead of
     * assumed: the creative inventory uses 195 while a normal inventory uses 176, so a hardcoded
     * value left the creative screen a few pixels off centre.
     */
    private static java.lang.reflect.Field xSizeField;

    private static boolean containerFieldsResolved = false;
    private static boolean containerFieldsLogged = false;
    private static boolean containerShiftLogged = false;

    /** {@code GuiScreen.field_73887_h} (obf: {@code i}) - the screen's control list. */
    private static java.lang.reflect.Field buttonListField;

    /** {@code GuiButton.field_73746_c} (obf: {@code d}) - a button's x position. */
    private static java.lang.reflect.Field buttonXField;

    private static boolean buttonFieldsLogged = false;

    /**
     * Pins the inventory container at its centred position.
     *
     * <p>This is what keeps the UI still while the player has potion effects. The flag that
     * {@code InventoryEffectRenderer} uses also gates the vanilla effect list, so it cannot simply
     * be cleared without hiding the list - re-centring the container every frame instead leaves the
     * list visible and the inventory in place.</p>
     *
     * @return true when the container was checked (possibly corrected)
     */
    public static boolean centreContainer(Object screen)
    {
        if (screen == null)
        {
            return false;
        }

        try
        {
            if (!containerFieldsResolved)
            {
                resolveContainerFields(screen.getClass());
            }

            if (guiLeftField == null || screenWidthField == null || xSizeField == null)
            {
                return false;
            }

            int width = screenWidthField.getInt(screen);
            int xSize = xSizeField.getInt(screen);
            int guiLeft = guiLeftField.getInt(screen);
            int centred = (width - xSize) / 2;

            if (guiLeft != centred)
            {
                int delta = centred - guiLeft;
                guiLeftField.setInt(screen, centred);

                // Buttons are laid out relative to guiLeft during initGui, so anything added before
                // this correction is still anchored to the shifted position. Moving them by the same
                // delta keeps them attached to the container. This is what fixes the Inventory
                // Tweaks sorting/settings buttons, which are created once and never repositioned.
                int moved = shiftButtons(screen, delta);

                if (!containerShiftLogged)
                {
                    containerShiftLogged = true;
                    Log.debug("held the inventory container in place: " + guiLeftOwner.getName() + "."
                            + guiLeftField.getName() + " " + guiLeft + " -> " + centred
                            + " (xSize=" + xSize + ", width=" + width + ", buttons moved=" + moved + ")");
                }
            }

            return true;
        }
        catch (Throwable t)
        {
            if (!containerFieldsLogged)
            {
                containerFieldsLogged = true;
                Log.warn("could not pin the inventory container: " + t);
            }
            return false;
        }
    }

    /**
     * Finds {@code guiLeft}, {@code xSize} and the screen width by walking the superclass chain.
     *
     * <p>Uses the <b>1.6.4</b> SRG names. Note that the names widely quoted for 1.7.10
     * ({@code field_147003_i} for guiLeft, {@code field_146999_f} for xSize) do <b>not exist</b> in
     * the 1.6.4 mapping table - using them silently finds nothing, which is exactly why the
     * inventory kept shifting. The 1.6.4 names are:</p>
     *
     * <pre>
     *   GuiContainer.field_74198_m (obf: p)  guiLeft
     *   GuiContainer.field_74194_b (obf: c)  xSize
     *   GuiScreen.field_73880_f    (obf: g)  width
     * </pre>
     */
    private static void resolveContainerFields(Class<?> type)
    {
        containerFieldsResolved = true;

        // screen width: GuiScreen.field_73880_f (obf: g)
        screenWidthField = findIntFieldInHierarchy(type, "field_73880_f", "g");

        // guiLeft: GuiContainer.field_74198_m (obf: p)
        guiLeftField = findIntFieldInHierarchy(type, "field_74198_m", "p");
        if (guiLeftField != null)
        {
            guiLeftOwner = guiLeftField.getDeclaringClass();
        }

        // xSize: GuiContainer.field_74194_b (obf: c)
        xSizeField = findIntFieldInHierarchy(type, "field_74194_b", "c");

        if (!containerFieldsLogged)
        {
            containerFieldsLogged = true;
            Log.debug("container fields resolved on " + type.getName() + ": guiLeft="
                    + (guiLeftField == null ? "not found" : guiLeftOwner.getName() + "." + guiLeftField.getName())
                    + ", xSize="
                    + (xSizeField == null ? "not found" : xSizeField.getName())
                    + ", screenWidth="
                    + (screenWidthField == null ? "not found"
                            : screenWidthField.getDeclaringClass().getName() + "." + screenWidthField.getName()));

            if (guiLeftField == null)
            {
                // last resort diagnostics: show what int fields the hierarchy actually offers
                StringBuilder sb = new StringBuilder("int fields in the hierarchy:");
                for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass())
                {
                    for (java.lang.reflect.Field f : c.getDeclaredFields())
                    {
                        if (f.getType() == Integer.TYPE)
                        {
                            sb.append(' ').append(c.getSimpleName()).append('.').append(f.getName());
                        }
                    }
                }
                Log.warn(sb.toString());
            }
        }
    }

    /**
     * Moves every button in the screen's control list horizontally by {@code delta}.
     *
     * <p>Needed because a container's buttons are positioned during {@code initGui} relative to
     * {@code guiLeft}; after the container is pinned back to the centre, those buttons would
     * otherwise stay at the old, shifted position. Inventory Tweaks is the visible case: it creates
     * its settings and sorting buttons once and never repositions them.</p>
     *
     * @return how many buttons were moved
     */
    private static int shiftButtons(Object screen, int delta)
    {
        if (buttonListField == null || buttonXField == null)
        {
            resolveButtonFields(screen.getClass());
        }
        if (buttonListField == null || buttonXField == null)
        {
            return 0;
        }

        int moved = 0;
        try
        {
            Object list = buttonListField.get(screen);
            if (!(list instanceof java.util.List))
            {
                return 0;
            }

            for (Object button : (java.util.List<?>) list)
            {
                if (button == null)
                {
                    continue;
                }
                try
                {
                    buttonXField.setInt(button, buttonXField.getInt(button) + delta);
                    moved++;
                }
                catch (Throwable ignored)
                {
                    // a control without the expected field: leave it alone
                }
            }
        }
        catch (Throwable t)
        {
            if (!buttonFieldsLogged)
            {
                buttonFieldsLogged = true;
                Log.warn("could not move the container buttons: " + t);
            }
        }
        return moved;
    }

    private static void resolveButtonFields(Class<?> type)
    {
        // GuiScreen.field_73887_h (obf: i) - the control list
        for (Class<?> c = type; c != null && c != Object.class && buttonListField == null; c = c.getSuperclass())
        {
            for (String name : new String[] { "field_73887_h", "i" })
            {
                try
                {
                    java.lang.reflect.Field f = c.getDeclaredField(name);
                    if (java.util.List.class.isAssignableFrom(f.getType()))
                    {
                        f.setAccessible(true);
                        buttonListField = f;
                        break;
                    }
                }
                catch (NoSuchFieldException ignored)
                {
                    // next candidate
                }
            }
        }

        // GuiButton.field_73746_c (obf: d) - xPosition
        for (String name : new String[] { "field_73746_c", "d" })
        {
            try
            {
                java.lang.reflect.Field f = aut.class.getDeclaredField(name);
                if (f.getType() == Integer.TYPE)
                {
                    f.setAccessible(true);
                    buttonXField = f;
                    break;
                }
            }
            catch (NoSuchFieldException ignored)
            {
                // next candidate
            }
        }

        if (!buttonFieldsLogged)
        {
            buttonFieldsLogged = true;
            Log.debug("button fields resolved: list="
                    + (buttonListField == null ? "not found" : buttonListField.getName())
                    + ", x=" + (buttonXField == null ? "not found" : buttonXField.getName()));
        }
    }

    /** Finds an int field by SRG name, then by obfuscated name, on exactly this class. */
    private static java.lang.reflect.Field findIntField(Class<?> type, String srgName, String obfName)
    {
        for (String name : new String[] { srgName, obfName })
        {
            try
            {
                java.lang.reflect.Field f = type.getDeclaredField(name);
                if (f.getType() == Integer.TYPE)
                {
                    f.setAccessible(true);
                    return f;
                }
            }
            catch (NoSuchFieldException ignored)
            {
                // try the next name
            }
        }
        return null;
    }

    private static java.lang.reflect.Field findIntFieldInHierarchy(Class<?> type, String srgName, String obfName)
    {
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass())
        {
            java.lang.reflect.Field f = findIntField(c, srgName, obfName);
            if (f != null)
            {
                return f;
            }
        }
        return null;
    }

    /**
     * Walks up the superclass chain looking for the boolean flag and remembers which class declared
     * it. Both the SRG name and the obfuscated name are accepted.
     *
     * <p>The walk is essential: {@code GuiInventory} (obf: {@code axv}) declares its own
     * {@code private float t}, which shadows the {@code private boolean t} on
     * {@code InventoryEffectRenderer} (obf: {@code axp}). Resolving the name on the concrete screen
     * class therefore yields a field of the wrong type, and the flag is never found.</p>
     */
    private static void resolveInventoryEffectListField(Class<?> type)
    {
        inventoryEffectListFieldResolved = true;

        String[] candidates = new String[] { "t", "field_74222_o" };

        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass())
        {
            for (String name : candidates)
            {
                try
                {
                    java.lang.reflect.Field f = c.getDeclaredField(name);
                    if (f.getType() != Boolean.TYPE)
                    {
                        // a same-named field of another type shadows the real flag: keep walking
                        continue;
                    }
                    f.setAccessible(true);
                    inventoryEffectListOwner = c;
                    inventoryEffectListField = f;
                    Log.debug("resolved inventory effect list flag: " + c.getName() + "." + name
                            + " (screen class " + type.getName() + ")");
                    return;
                }
                catch (NoSuchFieldException ignored)
                {
                    // not on this class, try the next candidate name
                }
            }
        }

        Log.warn("could not find the inventory effect list flag on " + type.getName()
                + " or its superclasses; the inventory may still shift when effects are active");
    }
}
