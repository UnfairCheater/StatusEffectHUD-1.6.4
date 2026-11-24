import java.util.ArrayList;
import java.util.List;

import bspkrs.seh.util.Log;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * In-game configuration screen for StatusEffectHUD (Minecraft 1.6.4).
 *
 * <p>1.7.10 shipped this screen through {@code cpw.mods.fml.client.config.GuiConfig} and
 * {@code IModGuiFactory}, neither of which exists in Forge 1.6.4, so the screen is hand drawn
 * here. Open it with {@code /statuseffect config}.</p>
 *
 * <p>The class lives in the default package because it interacts with 1.6.4's (obfuscated) game
 * classes, which are only reachable from the default package.</p>
 */
public class SEHConfigGui extends awe
{
    /* ------------------------------------------------------------------ *
     *  Layout constants                                                   *
     * ------------------------------------------------------------------ */

    private static final int PANEL_WIDTH = 420;
    private static final int COLUMN_WIDTH = 200;
    private static final int COLUMN_GAP = 20;
    private static final int ROW_HEIGHT = 22;
    private static final int TOP_MARGIN = 46;
    private static final int HEADER_HEIGHT = 38;
    private static final int PREVIEW_WIDTH = 132;
    private static final int PREVIEW_HEIGHT = 74;

    /** Credit line shown under the title. */
    public static final String EDIT_BY = "edit by yuzhipang&deepseek";

    /* button ids */
    private static final int ID_DONE = 0;
    private static final int ID_ENABLED = 1;
    private static final int ID_ALIGN = 2;
    private static final int ID_X_OFFSET = 3;
    private static final int ID_Y_OFFSET = 4;
    private static final int ID_Y_OFFSET_BOTTOM = 5;
    private static final int ID_APPLY_X = 6;
    private static final int ID_APPLY_Y = 7;
    private static final int ID_BACKGROUND = 8;
    private static final int ID_EFFECT_NAME = 9;
    private static final int ID_NAME_COLOR = 10;
    private static final int ID_DURATION_COLOR = 11;
    private static final int ID_ICON_BLINK = 12;
    private static final int ID_BLINK_SECONDS = 13;
    private static final int ID_INV_LIST = 14;
    private static final int ID_SHOW_IN_CHAT = 15;

    private static final int ID_X_DOWN = 20;
    private static final int ID_X_UP = 21;
    private static final int ID_X_RESET = 22;
    private static final int ID_Y_DOWN = 23;
    private static final int ID_Y_UP = 24;
    private static final int ID_Y_RESET = 25;

    /** Rows of components, each entry is {buttonId, labelIndex}. */
    private final List<int[]> componentRows = new ArrayList<int[]>();
    private final List<String> labels = new ArrayList<String>();
    private final List<String> descriptions = new ArrayList<String>();

    private final awe parentScreen;

    /** Diagnostics: proves whether initGui ran, how often we render and that clicks arrive. */
    private boolean loggedInit;
    private boolean loggedFirstDraw;
    private boolean loggedWidgets;
    private boolean loggedWidgetError;
    private boolean loggedClickMethod;
    private boolean loggedHandleMouse;
    private boolean loggedUpdateScreen;
    private int drawCount;

    public SEHConfigGui(awe parent)
    {
        this.parentScreen = parent;
        Log.debug("SEHConfigGui instance created, parent=" + parent);
    }

    /* ------------------------------------------------------------------ *
     *  Layout                                                             *
     * ------------------------------------------------------------------ */

    @Override
    public void A_()
    {
        this.i.clear();
        this.componentRows.clear();

        int left = (this.g - PANEL_WIDTH) / 2;
        int right = left + COLUMN_WIDTH + COLUMN_GAP;
        int row = 0;

        addButton(left, row, ID_ENABLED, 0);
        addButton(right, row, ID_ALIGN, 1);
        row++;

        addOffsetRow(left, row, ID_X_DOWN, ID_X_UP, ID_X_RESET, ID_X_OFFSET, 2);
        row++;

        addOffsetRow(left, row, ID_Y_DOWN, ID_Y_UP, ID_Y_RESET, ID_Y_OFFSET, 3);
        row++;

        addButton(left, row, ID_Y_OFFSET_BOTTOM, 4);
        addButton(right, row, ID_APPLY_X, 5);
        row++;

        addButton(left, row, ID_APPLY_Y, 6);
        addButton(right, row, ID_BACKGROUND, 7);
        row++;

        addButton(left, row, ID_EFFECT_NAME, 8);
        addButton(right, row, ID_NAME_COLOR, 9);
        row++;

        addButton(left, row, ID_DURATION_COLOR, 10);
        addButton(right, row, ID_ICON_BLINK, 11);
        row++;

        addButton(left, row, ID_BLINK_SECONDS, 12);
        addButton(right, row, ID_INV_LIST, 13);
        row++;

        addButton(left, row, ID_SHOW_IN_CHAT, 14);
        row++;

        this.i.add(new aut(ID_DONE, this.g / 2 - 80, TOP_MARGIN + row * ROW_HEIGHT + 10, 160, 20, "Done"));

        // the HUD preview goes in the space left free under the left column
        int previewY = TOP_MARGIN + row * ROW_HEIGHT + 10;
        if (previewY + PREVIEW_HEIGHT + 4 > this.h)
        {
            previewY = Math.max(TOP_MARGIN, this.h - PREVIEW_HEIGHT - 4);
        }
        this.previewX = left;
        this.previewY = previewY;

        refreshLabels();

        if (!loggedInit)
        {
            loggedInit = true;
            Log.debug("initGui ran: size=" + this.g + "x" + this.h
                    + ", buttons=" + this.i.size()
                    + ", fontRenderer=" + (this.o == null ? "null" : "ok")
                    + ", mc=" + (this.f == null ? "null" : "ok"));

            StringBuilder sb = new StringBuilder("components: ");
            int shown = 0;
            for (aut b : buttons())
            {
                if (shown++ > 0)
                {
                    sb.append(" | ");
                }
                sb.append(b.g).append('@').append(b.d).append(',').append(b.e).append("=\"").append(b.f).append('"');
                if (shown >= 24)
                {
                    break;
                }
            }
            Log.debug(sb.toString());
        }
    }

    /** Where the HUD preview is drawn, decided during layout. */
    private int previewX;
    private int previewY;

    private void addButton(int x, int row, int id, int labelIndex)
    {
        ensureLabel(labelIndex);
        int y = TOP_MARGIN + row * ROW_HEIGHT;
        this.i.add(new aut(id, x, y, COLUMN_WIDTH, 20, ""));
        this.componentRows.add(new int[] { id, labelIndex });
    }

    /**
     * Offset rows get a value button plus {@code -}, {@code +} and {@code Reset} buttons, since
     * the 1.6.4 GuiButton API has no slider widget. All five buttons span exactly one column.
     */
    private void addOffsetRow(int x, int row, int idDown, int idUp, int idReset, int idValue, int labelIndex)
    {
        ensureLabel(labelIndex);
        int y = TOP_MARGIN + row * ROW_HEIGHT;

        this.i.add(new aut(idValue, x, y, 120, 20, ""));
        this.i.add(new aut(idDown, x + 122, y, 20, 20, "-"));
        this.i.add(new aut(idUp, x + 144, y, 20, 20, "+"));
        this.i.add(new aut(idReset, x + 168, y, 32, 20, "Rst"));

        this.componentRows.add(new int[] { idValue, labelIndex });
    }

    private void ensureLabel(int index)
    {
        while (this.labels.size() <= index)
        {
            this.labels.add("");
            this.descriptions.add("");
        }
    }

    /* ------------------------------------------------------------------ *
     *  Labels                                                             *
     * ------------------------------------------------------------------ */

    @SuppressWarnings("unchecked")
    private List<aut> buttons()
    {
        return (List<aut>) (List<?>) this.i;
    }

    private int labelIndexOf(int buttonId)
    {
        for (int i = 0; i < this.componentRows.size(); i++)
        {
            if (this.componentRows.get(i)[0] == buttonId)
            {
                return this.componentRows.get(i)[1];
            }
        }
        return -1;
    }

    private void setLabel(int buttonId, String text, String description)
    {
        for (aut button : buttons())
        {
            if (button.g == buttonId)
            {
                button.f = text;
            }
        }

        int index = labelIndexOf(buttonId);
        if (index >= 0)
        {
            this.labels.set(index, text);
            this.descriptions.set(index, description);
        }
    }

    private void refreshLabels()
    {
        setLabel(ID_ENABLED, onOff("Enabled", StatusEffectHUD.enabled),
                "Enables or disables the Status Effect HUD display.");
        setLabel(ID_ALIGN, "Position: \u00a7e" + StatusEffectHUD.alignMode,
                "Where the HUD is anchored on the screen (right click to cycle backwards).");
        setLabel(ID_X_OFFSET, "X Offset: \u00a7e" + StatusEffectHUD.xOffset,
                "Horizontal distance from the screen edge; for right alignments it counts from the right edge.");
        setLabel(ID_Y_OFFSET, "Y Offset: \u00a7e" + StatusEffectHUD.yOffset,
                "Vertical distance from the screen edge; for bottom alignments it counts from the bottom edge.");
        setLabel(ID_Y_OFFSET_BOTTOM, "Bottom Center Y: \u00a7e" + StatusEffectHUD.yOffsetBottomCenter,
                "Vertical offset used only by the bottomcenter alignment, so the HUD clears the vanilla one.");
        setLabel(ID_APPLY_X, onOff("Apply X to Center", StatusEffectHUD.applyXOffsetToCenter),
                "Apply the X offset to center alignments as well.");
        setLabel(ID_APPLY_Y, onOff("Apply Y to Middle", StatusEffectHUD.applyYOffsetToMiddle),
                "Apply the Y offset to middle alignments as well.");
        setLabel(ID_BACKGROUND, onOff("Background Box", StatusEffectHUD.enableBackground),
                "Draw the effect background box behind each entry.");
        setLabel(ID_EFFECT_NAME, onOff("Show Effect Name", StatusEffectHUD.enableEffectName),
                "Show the name of each active effect.");
        setLabel(ID_NAME_COLOR, "Name Color: \u00a7" + StatusEffectHUD.effectNameColor + "Example",
                "Colour code (0-9, a-f) used for effect names.");
        setLabel(ID_DURATION_COLOR, "Timer Color: \u00a7" + StatusEffectHUD.durationColor + "0:00",
                "Colour code (0-9, a-f) used for the timer text.");
        setLabel(ID_ICON_BLINK, onOff("Blink Icon", StatusEffectHUD.enableIconBlink),
                "Blink the icon when an effect is about to run out.");
        setLabel(ID_BLINK_SECONDS, "Blink Seconds: \u00a7e" + StatusEffectHUD.durationBlinkSeconds,
                "Seconds left before the timer starts blinking. -1 disables blinking.");
        setLabel(ID_INV_LIST, onOff("Hide Inv. Effect List", StatusEffectHUD.hideInventoryEffectList),
                "Hide the vanilla potion list shown next to the inventory screen.");
        setLabel(ID_SHOW_IN_CHAT, onOff("Show In Chat", StatusEffectHUD.showInChat),
                "Keep the HUD visible while the chat screen is open.");
    }

    private static String onOff(String label, boolean value)
    {
        return label + ": " + (value ? "\u00a7aON" : "\u00a7cOFF");
    }

    /* ------------------------------------------------------------------ *
     *  Drawing                                                            *
     * ------------------------------------------------------------------ */

    @Override
    public void a(int mouseX, int mouseY, float partialTicks)
    {
        // make sure the HUD helper knows the current screen size, so the preview is accurate
        StatusEffectHUD.refreshScaledResolution();

        if (!loggedFirstDraw)
        {
            loggedFirstDraw = true;
            Log.debug("first drawScreen: mouse=" + mouseX + "," + mouseY
                    + " size=" + this.g + "x" + this.h
                    + " buttons=" + this.i.size()
                    + " font=" + (this.o == null ? "null" : "ok"));
        }

        // frame frequency probe: once is a red flag, hundreds per second is normal
        drawCount++;
        if (drawCount == 2 || drawCount == 10 || drawCount == 100 || drawCount == 1000)
        {
            Log.debug("drawScreen frame #" + drawCount + " (mouse " + mouseX + "," + mouseY + ")");
        }

        drawRect(0, 0, this.g, this.h, 0xC0101010);
        drawRect(0, 0, this.g, HEADER_HEIGHT, 0x80000000);

        String title = "StatusEffectHUD " + StatusEffectHUD.VERSION_NUMBER;
        this.b(this.o, "\u00a7f" + title, (this.g - this.o.a(title)) / 2, 12, 0xFFFFFF);

        // credit line, requested by the pack author
        String editedBy = EDIT_BY;
        this.b(this.o, "\u00a77" + editedBy, (this.g - this.o.a(editedBy)) / 2, 24, 0xFFFFFF);

        drawRect(0, HEADER_HEIGHT - 1, this.g, HEADER_HEIGHT, 0x40FFFFFF);

        int left = (this.g - PANEL_WIDTH) / 2;
        if (this.previewX == 0)
        {
            this.previewX = left;
            this.previewY = this.h - PREVIEW_HEIGHT - 4;
        }
        drawPreview(this.previewX, this.previewY, PREVIEW_WIDTH, PREVIEW_HEIGHT);

        String description = descriptionAt(mouseX, mouseY);
        if (description != null)
        {
            this.b(this.o, "\u00a77" + description, left + PREVIEW_WIDTH + 16, this.previewY + 4, 0xFFFFFF);
        }
        else
        {
            this.b(this.o, "\u00a78/statuseffect config", left + PREVIEW_WIDTH + 16, this.previewY + 4, 0xFFFFFF);
            this.b(this.o, "\u00a78Settings are written when you press Done, Esc also saves.",
                    left + PREVIEW_WIDTH + 16, this.previewY + 16, 0xFFFFFF);
        }

        // Draw the widgets ourselves (identical to GuiScreen.drawScreen) so that a failure in the
        // parent implementation cannot silently swallow them.
        try
        {
            int drawn = 0;
            for (aut button : buttons())
            {
                button.a(this.f, mouseX, mouseY);
                drawn++;
            }
            if (!loggedWidgets)
            {
                loggedWidgets = true;
                Log.debug("drew " + drawn + " widgets directly (parent drawScreen is NOT used)");
            }
        }
        catch (Throwable t)
        {
            if (!loggedWidgetError)
            {
                loggedWidgetError = true;
                Log.error("drawing the widgets failed", t);
            }
        }
    }

    /** Simple filled rectangle helper (Gui.func_73734_a, obf: avk.a). */
    private void drawRect(int x1, int y1, int x2, int y2, int colour)
    {
        avk.a(x1, y1, x2, y2, colour);
    }

    private String descriptionAt(int mouseX, int mouseY)
    {
        for (aut button : buttons())
        {
            if (mouseX >= button.d && mouseY >= button.e
                    && mouseX < button.d + button.b && mouseY < button.e + button.c)
            {
                int index = labelIndexOf(button.g);
                if (index >= 0)
                {
                    return this.descriptions.get(index) + "   (" + this.labels.get(index) + "\u00a77)";
                }
            }
        }
        return null;
    }

    /**
     * Draws a miniature screen with a box marking where the HUD will appear.
     */
    private void drawPreview(int x, int y, int width, int height)
    {
        drawRect(x, y - 12, x + width, y, 0x80000000);
        this.b(this.o, "Preview", x + 2, y - 10, 0xA0A0A0);

        drawRect(x, y, x + width, y + height, 0x60000000);
        drawRect(x, y, x + width, y + 1, 0xFFA0A0A0);
        drawRect(x, y + height - 1, x + width, y + height, 0xFFA0A0A0);
        drawRect(x, y, x + 1, y + height, 0xFFA0A0A0);
        drawRect(x + width - 1, y, x + width, y + height, 0xFFA0A0A0);

        int rows = 2;
        int rowHeight = StatusEffectHUD.enableBackground ? 33
                : (StatusEffectHUD.enableEffectName ? 20 : 18);
        int boxWidth = 120;
        int boxHeight = rows * rowHeight;

        int screenWidth = Math.max(1, StatusEffectHUD.getScaledWidth());
        int screenHeight = Math.max(1, StatusEffectHUD.getScaledHeight());

        int hudX = StatusEffectHUD.getX(boxWidth);
        int hudY = StatusEffectHUD.getY(rows, rowHeight);

        int usableWidth = Math.max(1, width - boxWidth);
        int usableHeight = Math.max(1, height - boxHeight);

        int px = x + clamp((int) ((float) hudX / screenWidth * usableWidth), 0, usableWidth);
        int py = y + clamp((int) ((float) hudY / screenHeight * usableHeight), 0, usableHeight);

        drawRect(px, py, px + boxWidth, py + boxHeight, 0x90FFAA00);
    }

    private static int clamp(int value, int min, int max)
    {
        return value < min ? min : (value > max ? max : value);
    }

    /* ------------------------------------------------------------------ *
     *  Input                                                              *
     * ------------------------------------------------------------------ */

    @Override
    protected void a(aut button)
    {
        Log.debug("actionPerformed: id=" + button.g + " label=\"" + button.f + "\"");
        switch (button.g)
        {
            case ID_DONE:
                saveAndClose();
                return;
            case ID_ENABLED:
                StatusEffectHUD.enabled = !StatusEffectHUD.enabled;
                break;
            case ID_ALIGN:
                cycleAlign(1);
                break;
            case ID_X_DOWN:
                StatusEffectHUD.xOffset--;
                break;
            case ID_X_UP:
                StatusEffectHUD.xOffset++;
                break;
            case ID_X_RESET:
                StatusEffectHUD.xOffset = 2;
                break;
            case ID_Y_DOWN:
                StatusEffectHUD.yOffset--;
                break;
            case ID_Y_UP:
                StatusEffectHUD.yOffset++;
                break;
            case ID_Y_RESET:
                StatusEffectHUD.yOffset = 2;
                break;
            case ID_Y_OFFSET_BOTTOM:
                StatusEffectHUD.yOffsetBottomCenter++;
                break;
            case ID_APPLY_X:
                StatusEffectHUD.applyXOffsetToCenter = !StatusEffectHUD.applyXOffsetToCenter;
                break;
            case ID_APPLY_Y:
                StatusEffectHUD.applyYOffsetToMiddle = !StatusEffectHUD.applyYOffsetToMiddle;
                break;
            case ID_BACKGROUND:
                StatusEffectHUD.enableBackground = !StatusEffectHUD.enableBackground;
                break;
            case ID_EFFECT_NAME:
                StatusEffectHUD.enableEffectName = !StatusEffectHUD.enableEffectName;
                break;
            case ID_NAME_COLOR:
                cycleColour(true, 1);
                break;
            case ID_DURATION_COLOR:
                cycleColour(false, 1);
                break;
            case ID_ICON_BLINK:
                StatusEffectHUD.enableIconBlink = !StatusEffectHUD.enableIconBlink;
                break;
            case ID_BLINK_SECONDS:
                StatusEffectHUD.durationBlinkSeconds = nextBlinkSeconds(StatusEffectHUD.durationBlinkSeconds, 1);
                break;
            case ID_INV_LIST:
                StatusEffectHUD.hideInventoryEffectList = !StatusEffectHUD.hideInventoryEffectList;
                break;
            case ID_SHOW_IN_CHAT:
                StatusEffectHUD.showInChat = !StatusEffectHUD.showInChat;
                break;
            default:
                break;
        }

        refreshLabels();
    }

    /**
     * GuiScreen.handleMouseInput (obf: d). Overridden purely as a probe, to find out whether mouse
     * events reach the screen at all - the parent then does the real dispatch.
     */
    @Override
    public void d()
    {
        if (!loggedHandleMouse)
        {
            loggedHandleMouse = true;
            Log.debug("handleMouseInput reached: eventX=" + Mouse.getEventX()
                    + " eventY=" + Mouse.getEventY()
                    + " button=" + Mouse.getEventButton()
                    + " state=" + Mouse.getEventButtonState()
                    + " | screen=" + this.g + "x" + this.h
                    + " display=" + this.f.d + "x" + this.f.e);
        }
        super.d();
    }

    /**
     * A configuration screen must not pause the game: 1.7.10's {@code GuiConfig} behaves the same
     * way, and pausing makes the screen feel frozen (and skips the client tick).
     *
     * <p>GuiScreen.doesGuiPauseGame is obfuscated as {@code f()} on 1.6.4.</p>
     */
    @Override
    public boolean f()
    {
        return false;
    }

    /**
     * Right clicking a value button steps it backwards, which is handy for the offset rows.
     */
    @Override
    protected void a(int mouseX, int mouseY, int mouseButton)
    {
        Log.debug("mouseClicked received: " + mouseX + "," + mouseY + " button=" + mouseButton);

        if (mouseButton == 1)
        {
            for (aut button : buttons())
            {
                if (mouseX >= button.d && mouseY >= button.e
                        && mouseX < button.d + button.b && mouseY < button.e + button.c)
                {
                    if (cycleBackwards(button.g))
                    {
                        refreshLabels();
                        return;
                    }
                }
            }
        }

        // Dispatch directly instead of relying on the parent implementation, so a mismatch there
        // cannot silently make every widget dead. Mirrors GuiScreen.mouseClicked exactly.
        if (mouseButton == 0)
        {
            int hits = 0;
            for (aut button : buttons())
            {
                if (button.c(this.f, mouseX, mouseY))
                {
                    hits++;
                    this.a(button);
                }
            }
            Log.debug("mouseClicked: " + hits + " widget(s) matched the click position");
        }
    }

    /**
     * GuiScreen.updateScreen (obf: b()). Tracked because it is what drives the per-frame widget
     * animation; if this never runs, something is wrong with the screen's lifecycle.
     */
    @Override
    public void b()
    {
        if (!loggedUpdateScreen)
        {
            loggedUpdateScreen = true;
            Log.debug("updateScreen is running");
        }
        super.b();
    }

    private boolean cycleBackwards(int id)
    {
        switch (id)
        {
            case ID_ALIGN:
                cycleAlign(-1);
                return true;
            case ID_X_OFFSET:
            case ID_X_DOWN:
                StatusEffectHUD.xOffset--;
                return true;
            case ID_Y_OFFSET:
            case ID_Y_DOWN:
                StatusEffectHUD.yOffset--;
                return true;
            case ID_Y_OFFSET_BOTTOM:
                StatusEffectHUD.yOffsetBottomCenter--;
                return true;
            case ID_NAME_COLOR:
                cycleColour(true, -1);
                return true;
            case ID_DURATION_COLOR:
                cycleColour(false, -1);
                return true;
            case ID_BLINK_SECONDS:
                StatusEffectHUD.durationBlinkSeconds = nextBlinkSeconds(StatusEffectHUD.durationBlinkSeconds, -1);
                return true;
            default:
                return false;
        }
    }

    private void cycleAlign(int step)
    {
        String[] modes = SEHConfig.alignModes();
        StatusEffectHUD.alignMode = modes[SEHConfig.cycle(modes, StatusEffectHUD.alignMode, step)];
    }

    private void cycleColour(boolean nameColour, int step)
    {
        String[] codes = SEHConfig.colorCodes();
        if (nameColour)
        {
            StatusEffectHUD.effectNameColor = codes[SEHConfig.cycle(codes, StatusEffectHUD.effectNameColor, step)];
        }
        else
        {
            StatusEffectHUD.durationColor = codes[SEHConfig.cycle(codes, StatusEffectHUD.durationColor, step)];
        }
    }

    private static int nextBlinkSeconds(int current, int step)
    {
        int next = current + step;
        if (next > 60)
        {
            next = -1;
        }
        if (next < -1)
        {
            next = 60;
        }
        return next;
    }

    @Override
    protected void a(char typedChar, int keyCode)
    {
        if (keyCode == Keyboard.KEY_ESCAPE)
        {
            saveAndClose();
            return;
        }

        super.a(typedChar, keyCode);
    }

    /* ------------------------------------------------------------------ *
     *  Lifecycle                                                          *
     * ------------------------------------------------------------------ */

    private void saveAndClose()
    {
        SEHConfig.save();
        Log.debug("saving config and closing the screen");
        // this.f -> field_73882_e (Minecraft) ; a(awe) -> func_71405_e (displayGuiScreen)
        this.f.a(this.parentScreen);
    }
}

