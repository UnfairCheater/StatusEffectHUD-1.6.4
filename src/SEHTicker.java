import java.util.EnumSet;

import bspkrs.seh.util.Log;

import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;

/**
 * Client tick handler for the 1.6.4 backport.
 *
 * <p>Forge 1.6.4 has neither the {@code TickEvent} bus nor {@code @SubscribeEvent}, so the legacy
 * {@link ITickHandler} API is used. The RENDER tick drives the HUD, the CLIENT tick handles the
 * deferred opening of the config screen requested by {@code /statuseffect config}.</p>
 *
 * <p>This class lives in the default package because 1.6.4's (obfuscated) game classes are only
 * reachable from there. {@link Log} is the one helper kept in a named package: it must never
 * reference a game class, so it does not need default-package access.</p>
 */
public class SEHTicker implements ITickHandler
{
    private final EnumSet<TickType> tickTypes;
    private final atv mc;

    /** Countdown used to open the config GUI once the chat screen has closed. */
    private static int pendingConfigGuiTicks = 0;

    /* diagnostics, so a GUI that never opened can be told apart from a GUI that opened blank */
    private static boolean loggedClientTick = false;
    private static boolean loggedRenderTick = false;
    private static boolean loggedHudState = false;

    public SEHTicker(EnumSet<TickType> tickTypes)
    {
        this.tickTypes = tickTypes;
        this.mc = atv.w();
        Log.debug("SEHTicker constructed: tickTypes=" + tickTypes + " mc=" + (this.mc != null));
    }

    /**
     * Requests that the config GUI be opened as soon as the chat screen is gone.
     */
    public static void requestConfigGui()
    {
        pendingConfigGuiTicks = 2;
        Log.debug("requestConfigGui(): queued, opening in " + pendingConfigGuiTicks + " client ticks");
    }

    public void tickStart(EnumSet<TickType> tickTypes, Object... tickData)
    {
        this.tick(tickTypes, true);
    }

    public void tickEnd(EnumSet<TickType> tickTypes, Object... tickData)
    {
        this.tick(tickTypes, false);
    }

    private void tick(EnumSet<TickType> tickTypes, boolean isStart)
    {
        for (TickType tickType : tickTypes)
        {
            this.onTick(tickType, isStart);
        }
    }

    public boolean onTick(TickType tick, boolean isStart)
    {
        if (this.mc == null)
        {
            return true;
        }

        if (tick == TickType.CLIENT && isStart)
        {
            if (!loggedClientTick)
            {
                loggedClientTick = true;
                Log.debug("CLIENT tick observed (isStart=" + isStart + ")");
                this.logEnvironment();
            }
            this.handlePendingConfigGui();

            // Correct the container position before any mod's CLIENT/RENDER tickEnd runs, so mods
            // that lay out their own controls against guiLeft (Inventory Tweaks does) see the
            // centred value. FML runs every tickStart before any tickEnd, so this is early enough.
            this.suppressVanillaInventoryEffectList();
        }

        if (tick == TickType.RENDER && !isStart)
        {
            if (!loggedRenderTick)
            {
                loggedRenderTick = true;
                Log.debug("RENDER tick End observed");
            }
            if (!loggedHudState)
            {
                loggedHudState = true;
                this.logHudState();
            }
            this.suppressVanillaInventoryEffectList();
            StatusEffectHUD.onRenderTick(this.mc);
        }

        return true;
    }

    /** One-shot dump of the facts the config GUI depends on. */
    private void logEnvironment()
    {
        Log.debug("env: screen=" + (this.mc.n == null ? "null" : this.mc.n.getClass().getName())
                + " inGameHasFocus=" + this.mc.A
                + " display=" + this.mc.d + "x" + this.mc.e
                + " fontRenderer=" + (this.mc.l == null ? "null" : "ok")
                + " player=" + (this.mc.h == null ? "null" : "ok")
                + " gameSettings=" + (this.mc.u == null ? "null" : "ok"));
    }

    private void logHudState()
    {
        StatusEffectHUD.refreshScaledResolution();
        Log.debug("hud: scaled=" + StatusEffectHUD.getScaledWidth() + "x" + StatusEffectHUD.getScaledHeight()
                + " enabled=" + StatusEffectHUD.enabled
                + " align=" + StatusEffectHUD.alignMode
                + " xOffset=" + StatusEffectHUD.xOffset
                + " yOffset=" + StatusEffectHUD.yOffset
                + " hideInvList=" + StatusEffectHUD.hideInventoryEffectList);
    }

    private void handlePendingConfigGui()
    {
        if (pendingConfigGuiTicks <= 0)
        {
            return;
        }

        pendingConfigGuiTicks--;
        Log.debug("handlePendingConfigGui(): countdown=" + pendingConfigGuiTicks
                + " currentScreen=" + (this.mc.n == null ? "null" : this.mc.n.getClass().getName()));

        if (pendingConfigGuiTicks != 0)
        {
            return;
        }

        if (this.mc.n != null)
        {
            Log.warn("could not open the config GUI: a screen is still open ("
                    + this.mc.n.getClass().getName() + ")");
            return;
        }

        try
        {
            SEHConfigGui gui = new SEHConfigGui(null);
            Log.debug("opening SEHConfigGui: instance created, calling displayGuiScreen");
            this.mc.a(gui);
            Log.debug("displayGuiScreen returned: currentScreen="
                    + (this.mc.n == null ? "null" : this.mc.n.getClass().getName()));
        }
        catch (Throwable t)
        {
            Log.error("failed to open the config GUI", t);
        }
    }

    /**
     * Keeps the vanilla inventory screen from shifting right when the player has potion effects.
     *
     * <p>{@code InventoryEffectRenderer} (obf: {@code axp}) widens the whole container by 40px and
     * anchors it 40px further right whenever active effects exist. That same flag also gates the
     * vanilla effect list, so by default the list is left alone and only the container position is
     * pinned ({@code centreContainer}). Setting {@code hideInventoryEffectList} additionally
     * clears the flag, which hides the list.</p>
     */
    private void suppressVanillaInventoryEffectList()
    {
        if (this.mc.n == null)
        {
            return;
        }

        // axp -> net.minecraft.client.renderer.InventoryEffectRenderer
        if (this.mc.n instanceof axp)
        {
            if (StatusEffectHUD.hideInventoryEffectList)
            {
                StatusEffectHUD.setInventoryEffectListVisible(this.mc.n, false);
            }
            StatusEffectHUD.centreContainer(this.mc.n);
        }
    }

    public EnumSet<TickType> ticks()
    {
        return this.tickTypes;
    }

    public String getLabel()
    {
        return "SEHTicker";
    }
}
