package bspkrs.seh.util;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Small logging helper for the StatusEffectHUD 1.6.4 backport.
 *
 * <p>1.6.4 FML has no mod-specific logger on the 1.7.10 style, so this wraps the JDK logger under
 * the {@code StatusEffectHUD} channel, which FML already configures. Set the system property
 * {@code seh.debug=false} to silence it.</p>
 */
public final class Log
{
    private static final Logger LOGGER = Logger.getLogger("StatusEffectHUD");
    private static final boolean DEBUG = !"false".equalsIgnoreCase(System.getProperty("seh.debug", "true"));

    private Log()
    {
    }

    public static void debug(String message)
    {
        if (DEBUG)
        {
            LOGGER.log(Level.INFO, "[SEH] " + message);
        }
    }

    public static void warn(String message)
    {
        LOGGER.log(Level.WARNING, "[SEH] " + message);
    }

    public static void error(String message, Throwable t)
    {
        LOGGER.log(Level.SEVERE, "[SEH] " + message, t);
    }
}
