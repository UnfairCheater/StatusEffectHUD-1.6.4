/**
 * Implements {@code /statuseffect config}, the command that opens the in-game configuration
 * screen. Command registration goes through Forge's {@code ClientCommandHandler}, so the command
 * works on any server without being installed there (client side only mod).
 *
 * <p>1.7.10 did the same through {@code CommandBase} + a delayed GUI ticker; 1.6.4 has no
 * {@code DelayedGuiDisplayTicker}, so {@link SEHTicker} opens the screen a couple of ticks
 * later, once the chat screen has closed.</p>
 */
public class CommandStatusEffect extends z
{
    @Override
    public String c()
    {
        // func_71517_b -> getCommandName
        return "statuseffect";
    }

    @Override
    public String c(ad sender)
    {
        // func_71518_a -> getCommandUsage
        return "commands.statuseffect.usage";
    }

    @Override
    public boolean a(ad sender)
    {
        // func_71519_b -> canCommandSenderUseCommand
        return true;
    }

    @Override
    public int a()
    {
        // func_82362_a -> getRequiredPermissionLevel
        return 0;
    }

    @Override
    public void b(ad sender, String[] args)
    {
        // func_71515_b -> processCommand
        bspkrs.seh.util.Log.debug("/statuseffect invoked with " + args.length + " argument(s)");
        SEHTicker.requestConfigGui();
    }

    /**
     * Offers {@code config} as the sub-command, so {@code /statuseffect c<TAB>} completes
     * (func_71516_a -> addTabCompletionOptions).
     */
    @Override
    public java.util.List a(ad sender, String[] args)
    {
        if (args.length <= 1)
        {
            return filterCandidates(new String[] { "config" }, args);
        }
        return null;
    }

    /**
     * Filters the candidates by the partially typed argument. Named differently from the inherited
     * {@code CommandBase.a(String[], String...)} helper to avoid a name clash.
     */
    private static java.util.List<String> filterCandidates(String[] candidates, String[] args)
    {
        String last = args[args.length - 1];
        java.util.List<String> out = new java.util.ArrayList<String>();
        for (String candidate : candidates)
        {
            if (candidate.toLowerCase().startsWith(last.toLowerCase()))
            {
                out.add(candidate);
            }
        }
        return out;
    }

    /**
     * Required by the 1.6.4 ICommand interface (SRG name func_71525_a on CommandBase).
     */
    public int compareTo(Object other)
    {
        if (other instanceof z)
        {
            return this.c().compareTo(((z) other).c());
        }
        return 0;
    }
}
