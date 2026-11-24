import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Verifies StatusEffectHUD's container-centring behaviour against a fake hierarchy that mirrors the
 * real 1.6.4 structure, including the field-name collisions that caused the original bugs.
 *
 * Real 1.6.4 shape (from Forge's deobfuscation table):
 *
 * <pre>
 *   avk  Gui             - n (float), k/l/m (ResourceLocation)
 *   awe  GuiScreen       - g = width, h = height, f = mc, i = buttonList (List), o = font
 *   awy  GuiContainer    - c = xSize, d = ySize, p = guiLeft, q = guiTop
 *   axp  InventoryEffect - t = boolean  &lt;-- "show effect list" AND the 40px container shift
 *   axm  GuiContainerCr. - own int fields t, u, v, w ...   &lt;-- "t" here is an int, not the flag!
 *   axv  GuiInventory    - private float t                 &lt;-- shadows the flag with another type
 * </pre>
 *
 * Three behaviours are checked:
 *   A. a normal inventory (xSize 176) is pinned back to the centre
 *   B. the creative inventory (xSize 195) is pinned using its OWN width, not a hardcoded 176
 *   C. buttons that were laid out for the shifted position move with the container
 *
 *   java -cp build/classes;build/verify;build/lib-unsigned/* InvShiftFake
 */
public class InvShiftFake
{
    /* ---- fake game hierarchy, mirroring the collisions exactly ---- */

    static class Gui
    {
        float n;
    }

    static class Screen extends Gui
    {
        int g = 320;                              // width
        int h = 240;                              // height
        boolean j = true;
        protected List<Object> i = new ArrayList<Object>();   // buttonList
    }

    static class Container extends Screen
    {
        int c = 176;                              // xSize
        int d = 166;                              // ySize
        int p = 0;                                // guiLeft
        int q = 0;                                // guiTop
        boolean v;
        boolean s;
    }

    static class EffectRenderer extends Container
    {
        boolean t = true;                         // the flag: list visible + container shifted

        boolean flag()
        {
            return this.t;
        }
    }

    /** Mirrors axv: same name "t", different type. */
    static class InventoryScreen extends EffectRenderer
    {
        private float t = 0.0F;

        float shadow()
        {
            return this.t;
        }
    }

    /** Mirrors axm: declares its own int fields including "t", and a 195-wide container. */
    static class CreativeScreen extends EffectRenderer
    {
        int t = 7;
        int u = 8;
        int v = 9;

        CreativeScreen()
        {
            // exactly what GuiContainerCreative's constructor does
            this.d = 136;
            this.c = 195;
        }
    }

    /** the id range Inventory Tweaks uses for the buttons it adds to containers */
    private static final int[] INVTWEAKS_IDS = { 54696386, 54696387, 54696388, 54696389 };

    public static void main(String[] args) throws Exception
    {
        Class<?> hud = Class.forName("StatusEffectHUD");
        java.lang.reflect.Method hide =
                hud.getMethod("setInventoryEffectListVisible", Object.class, Boolean.TYPE);
        java.lang.reflect.Method centre = hud.getMethod("centreContainer", Object.class);

        System.out.println("=== field collisions present in the fake hierarchy ===");
        System.out.println("    InventoryScreen.t = "
                + InventoryScreen.class.getDeclaredField("t").getType() + "   (float, shadows the flag)");
        System.out.println("    CreativeScreen.t  = "
                + CreativeScreen.class.getDeclaredField("t").getType() + "   (int, shadows the flag)");
        System.out.println("    EffectRenderer.t  = "
                + EffectRenderer.class.getDeclaredField("t").getType() + "   (the flag that must be reached)");

        boolean a = run(hide, centre, new InventoryScreen(), 176, "A. GuiInventory-shaped screen");
        System.out.println();
        boolean b = run(hide, centre, new CreativeScreen(), 195, "B. GuiContainerCreative-shaped screen (xSize 195)");

        System.out.println();
        System.out.println("OVERALL: " + ((a && b) ? "PASSED" : "FAILED"));
        System.exit((a && b) ? 0 : 1);
    }

    private static boolean run(java.lang.reflect.Method hide, java.lang.reflect.Method centre,
            Object screen, int expectedXSize, String title) throws Exception
    {
        Field flag = EffectRenderer.class.getDeclaredField("t");
        flag.setAccessible(true);
        Field guiLeft = Container.class.getDeclaredField("p");
        guiLeft.setAccessible(true);
        Field width = Screen.class.getDeclaredField("g");
        width.setAccessible(true);
        Field xSizeField = Container.class.getDeclaredField("c");
        xSizeField.setAccessible(true);
        Field buttonList = Screen.class.getDeclaredField("i");
        buttonList.setAccessible(true);

        System.out.println("### " + title + " ###");

        int w = width.getInt(screen);
        int xSize = xSizeField.getInt(screen);
        int centred = (w - xSize) / 2;

        // vanilla: flag true, container pushed 40px right
        flag.setBoolean(screen, true);
        guiLeft.setInt(screen, centred + 40);

        // four Inventory-Tweaks-shaped buttons, laid out against the shifted container
        List<Object> buttons = new ArrayList<Object>();
        for (int id : INVTWEAKS_IDS)
        {
            // real GuiButton instances so the mod's field lookup works on them
            Object button = Class.forName("aut")
                    .getConstructor(Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, String.class)
                    .newInstance(Integer.valueOf(id), Integer.valueOf(guiLeft.getInt(screen) + xSize - 15),
                            Integer.valueOf(5), Integer.valueOf(10), Integer.valueOf(10), "...");
            buttons.add(button);
        }
        buttonList.set(screen, buttons);

        int[] beforeX = new int[buttons.size()];
        for (int k = 0; k < buttons.size(); k++)
        {
            beforeX[k] = xOf(buttons.get(k));
        }

        System.out.println("    screen class      : " + screen.getClass().getSimpleName());
        System.out.println("    width / xSize     : " + w + " / " + xSize
                + (xSize == expectedXSize ? "  (as expected)" : "  <-- UNEXPECTED"));
        System.out.println("    before            : guiLeft=" + guiLeft.getInt(screen)
                + " (centred=" + centred + ")  button x=" + beforeX[0]);

        // what the mod does by default: keep the list, pin the container
        Object centredOk = centre.invoke(null, screen);
        System.out.println("    after centre      : guiLeft=" + guiLeft.getInt(screen)
                + "   [centreContainer returned " + centredOk + "]");

        boolean pinned = guiLeft.getInt(screen) == centred;
        boolean listVisible = flag.getBoolean(screen);

        // the buttons must have travelled with the container
        int delta = guiLeft.getInt(screen) - (centred + 40);
        boolean buttonsMoved = true;
        for (int k = 0; k < buttons.size(); k++)
        {
            int expected = beforeX[k] + delta;
            if (xOf(buttons.get(k)) != expected)
            {
                buttonsMoved = false;
                System.out.println("    button " + k + " x=" + xOf(buttons.get(k))
                        + " expected " + expected);
            }
        }
        System.out.println("    button x after    : " + xOf(buttons.get(0))
                + "  (delta " + delta + ")");
        System.out.println("    list still visible: " + listVisible);
        System.out.println("    container centred : " + (pinned ? "YES" : "NO"));
        System.out.println("    buttons followed  : " + (buttonsMoved ? "YES" : "NO"));

        // still need the opt-in hiding to work
        guiLeft.setInt(screen, centred + 40);
        flag.setBoolean(screen, true);
        hide.invoke(null, screen, Boolean.FALSE);
        boolean listHidden = !flag.getBoolean(screen);
        System.out.println("    opt-in hiding     : " + (listHidden ? "OK" : "WRONG"));

        boolean ok = pinned && listVisible && buttonsMoved && listHidden && xSize == expectedXSize;
        System.out.println("    RESULT            : " + (ok ? "PASSED" : "FAILED"));
        return ok;
    }

    private static int xOf(Object button) throws Exception
    {
        Class<?> guiButton = Class.forName("aut");
        Field x;
        try
        {
            x = guiButton.getDeclaredField("field_73746_c");
        }
        catch (NoSuchFieldException e)
        {
            x = guiButton.getDeclaredField("d");
        }
        x.setAccessible(true);
        return x.getInt(button);
    }
}
