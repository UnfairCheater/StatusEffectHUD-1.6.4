import java.io.File;

import net.minecraftforge.common.Configuration;
import net.minecraftforge.common.Property;


/**
 * Configuration model for StatusEffectHUD (Minecraft 1.6.4).
 *
 * <p>Uses Forge's own {@link Configuration} instead of bspkrsCore's copy, which removes the
 * bspkrsCore dependency that the original 1.6.4 release required. All keys match the ones used
 * by the 1.7.10 release (v1.27) so existing config files stay compatible.</p>
 */
public class SEHConfig
{
    public static final String CATEGORY_GENERAL = "general";

    /** Forge configuration instance, shared with the in-game config GUI. */
    public static Configuration config = null;

    private SEHConfig()
    {
    }

    /**
     * Binds the Forge configuration object and loads every value into the HUD.
     */
    public static void init(File file)
    {
        config = new Configuration(file);
        sync();
    }

    /**
     * Loads (or reloads) every config value. Safe to call again from the config GUI.
     */
    public static void sync()
    {
        if (config == null)
        {
            return;
        }

        config.load();
        config.addCustomCategoryComment(CATEGORY_GENERAL,
                "ATTENTION: Editing this file manually is no longer necessary.\n"
                        + "Type the command '/statuseffect config' without the quotes in-game to modify these settings.");

        Property p;

        p = config.get(CATEGORY_GENERAL, "enabled", true,
                "Enables or disables the Status Effect HUD display.");
        StatusEffectHUD.enabled = p.getBoolean(true);

        p = config.get(CATEGORY_GENERAL, "alignMode", "middleright",
                "Sets the position of the HUD on the screen. Valid alignment strings are "
                        + "topleft, topcenter, topright, middleleft, middlecenter, middleright, "
                        + "bottomleft, bottomcenter, bottomright.");
        StatusEffectHUD.alignMode = sanitizeAlignMode(p.getString());

        p = config.get(CATEGORY_GENERAL, "hideInventoryEffectList", false,
                "Set to true to hide the vanilla status effect list shown beside your inventory. "
                        + "Leave it false to keep the vanilla list; either way the HUD pins the inventory "
                        + "in place so it no longer shifts right when you have effects.");
        StatusEffectHUD.hideInventoryEffectList = p.getBoolean(false);

        p = config.get(CATEGORY_GENERAL, "enableBackground", false,
                "Set to true to see the effect background box, false to disable.");
        StatusEffectHUD.enableBackground = p.getBoolean(false);

        p = config.get(CATEGORY_GENERAL, "enableEffectName", true,
                "Set to true to show effect names, false to disable.");
        StatusEffectHUD.enableEffectName = p.getBoolean(true);

        p = config.get(CATEGORY_GENERAL, "durationBlinkSeconds", 10,
                "When a potion/effect has this many seconds remaining the timer will begin to blink. "
                        + "Set to -1 to disable blinking.");
        StatusEffectHUD.durationBlinkSeconds = clamp(p.getInt(10), -1, 60);

        p = config.get(CATEGORY_GENERAL, "enableIconBlink", true,
                "Set to true to enable blinking for the icon when a potion/effect is nearly gone, false to disable.");
        StatusEffectHUD.enableIconBlink = p.getBoolean(true);

        p = config.get(CATEGORY_GENERAL, "effectNameColor", "f",
                "Valid color values are 0-9, a-f (color values can be found here: "
                        + "http://www.minecraftwiki.net/wiki/File:Colors.png).");
        StatusEffectHUD.effectNameColor = sanitizeColor(p.getString());

        p = config.get(CATEGORY_GENERAL, "durationColor", "f",
                "Valid color values are 0-9, a-f (color values can be found here: "
                        + "http://www.minecraftwiki.net/wiki/File:Colors.png).");
        StatusEffectHUD.durationColor = sanitizeColor(p.getString());

        p = config.get(CATEGORY_GENERAL, "xOffset", 2,
                "Horizontal offset from the edge of the screen (when using right alignments the x offset "
                        + "is relative to the right edge of the screen).");
        StatusEffectHUD.xOffset = p.getInt(2);

        p = config.get(CATEGORY_GENERAL, "yOffset", 2,
                "Vertical offset from the edge of the screen (when using bottom alignments the y offset "
                        + "is relative to the bottom edge of the screen).");
        StatusEffectHUD.yOffset = p.getInt(2);

        p = config.get(CATEGORY_GENERAL, "yOffsetBottomCenter", 41,
                "Vertical offset used only for the bottomcenter alignment to avoid the vanilla HUD.");
        StatusEffectHUD.yOffsetBottomCenter = p.getInt(41);

        p = config.get(CATEGORY_GENERAL, "applyXOffsetToCenter", false,
                "Set to true if you want the xOffset value to be applied when using a center alignment.");
        StatusEffectHUD.applyXOffsetToCenter = p.getBoolean(false);

        p = config.get(CATEGORY_GENERAL, "applyYOffsetToMiddle", false,
                "Set to true if you want the yOffset value to be applied when using a middle alignment.");
        StatusEffectHUD.applyYOffsetToMiddle = p.getBoolean(false);

        p = config.get(CATEGORY_GENERAL, "showInChat", true,
                "Set to true to show info when chat is open, false to disable info when chat is open.");
        StatusEffectHUD.showInChat = p.getBoolean(true);

        config.save();
    }

    /**
     * Writes the current in-memory values back to the config file.
     */
    public static void save()
    {
        if (config == null)
        {
            return;
        }

        config.get(CATEGORY_GENERAL, "enabled", true).set(StatusEffectHUD.enabled);
        config.get(CATEGORY_GENERAL, "alignMode", "middleright").set(StatusEffectHUD.alignMode);
        config.get(CATEGORY_GENERAL, "hideInventoryEffectList", false)
                .set(StatusEffectHUD.hideInventoryEffectList);
        config.get(CATEGORY_GENERAL, "enableBackground", false).set(StatusEffectHUD.enableBackground);
        config.get(CATEGORY_GENERAL, "enableEffectName", true).set(StatusEffectHUD.enableEffectName);
        config.get(CATEGORY_GENERAL, "durationBlinkSeconds", 10).set(StatusEffectHUD.durationBlinkSeconds);
        config.get(CATEGORY_GENERAL, "enableIconBlink", true).set(StatusEffectHUD.enableIconBlink);
        config.get(CATEGORY_GENERAL, "effectNameColor", "f").set(StatusEffectHUD.effectNameColor);
        config.get(CATEGORY_GENERAL, "durationColor", "f").set(StatusEffectHUD.durationColor);
        config.get(CATEGORY_GENERAL, "xOffset", 2).set(StatusEffectHUD.xOffset);
        config.get(CATEGORY_GENERAL, "yOffset", 2).set(StatusEffectHUD.yOffset);
        config.get(CATEGORY_GENERAL, "yOffsetBottomCenter", 41).set(StatusEffectHUD.yOffsetBottomCenter);
        config.get(CATEGORY_GENERAL, "applyXOffsetToCenter", false).set(StatusEffectHUD.applyXOffsetToCenter);
        config.get(CATEGORY_GENERAL, "applyYOffsetToMiddle", false).set(StatusEffectHUD.applyYOffsetToMiddle);
        config.get(CATEGORY_GENERAL, "showInChat", true).set(StatusEffectHUD.showInChat);

        config.save();
    }

    /* ------------------------------------------------------------------ *
     *  Helpers                                                            *
     * ------------------------------------------------------------------ */

    public static String[] alignModes()
    {
        return new String[] {
                "topleft", "topcenter", "topright",
                "middleleft", "middlecenter", "middleright",
                "bottomleft", "bottomcenter", "bottomright" };
    }

    public static String[] colorCodes()
    {
        return new String[] { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f" };
    }

    public static String sanitizeAlignMode(String value)
    {
        if (value != null)
        {
            for (String valid : alignModes())
            {
                if (valid.equalsIgnoreCase(value.trim()))
                {
                    return valid;
                }
            }
        }
        return "middleright";
    }

    public static String sanitizeColor(String value)
    {
        if (value != null && value.trim().length() > 0)
        {
            char c = Character.toLowerCase(value.trim().charAt(0));
            if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'))
            {
                return String.valueOf(c);
            }
        }
        return "f";
    }

    public static int cycle(String[] values, String current, int step)
    {
        int index = 0;
        for (int i = 0; i < values.length; i++)
        {
            if (values[i].equalsIgnoreCase(current))
            {
                index = i;
                break;
            }
        }
        index = (index + step) % values.length;
        if (index < 0)
        {
            index += values.length;
        }
        return index;
    }

    private static int clamp(int value, int min, int max)
    {
        return value < min ? min : (value > max ? max : value);
    }
}
