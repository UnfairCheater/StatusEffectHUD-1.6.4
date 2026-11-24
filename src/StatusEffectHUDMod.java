import java.util.EnumSet;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.ModMetadata;
import cpw.mods.fml.common.TickType;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;
import net.minecraftforge.client.ClientCommandHandler;

/**
 * StatusEffectHUD for Minecraft 1.6.4 / Forge 9.11.1.932+.
 *
 * <p>A backport of the 1.7.10 release (v1.27) that keeps its full configuration surface and adds
 * an in-game configuration screen. Unlike the original 1.6.4 build it does not require
 * bspkrsCore: configuration is read through Forge's own {@code Configuration} class and the
 * HUD is drawn with a small local helper instead of bspkrsCore's HUDUtils.</p>
 *
 * <p>This class is in the default package because 1.6.4's game classes are only reachable from
 * there (see {@link SEHTicker}).</p>
 */
@Mod(
        modid = "StatusEffectHUD",
        name = "StatusEffectHUD",
        version = StatusEffectHUDMod.VERSION,
        useMetadata = true)
public class StatusEffectHUDMod
{
    public static final String VERSION = "v1.27.1(1.6.4)";

    @Mod.Metadata("StatusEffectHUD")
    public static ModMetadata metadata;

    /**
     * FML injects the mod instance by looking the field up under the literal name
     * {@code instance} (see FMLModContainer.parseSimpleFieldAnnotation), so the field must keep
     * that name.
     */
    @Mod.Instance("StatusEffectHUD")
    public static StatusEffectHUDMod instance;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        metadata = event.getModMetadata();
        metadata.modId = "StatusEffectHUD";
        metadata.name = "StatusEffectHUD";
        metadata.version = VERSION;
        metadata.authorList = java.util.Arrays.asList(new String[] { "bspkrs", "yuzhipang", "deepseek" });
        metadata.credits = "Original mod by bspkrs. 1.7.10 -> 1.6.4 backport and in-game config screen: "
                + "edit by yuzhipang&deepseek.";
        metadata.description = "Shows active player effects (potions, etc) without opening your inventory!\n"
                + "1.6.4 backport of the 1.7.10 release, with a full in-game config screen.\n"
                + "edit by yuzhipang&deepseek";
        metadata.url = "http://www.minecraftforum.net/topic/1114612-";

        SEHConfig.init(event.getSuggestedConfigurationFile());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event)
    {
        // RENDER tick draws the HUD and hides the vanilla inventory effect list,
        // CLIENT tick opens the config screen requested by '/statuseffect config'.
        TickRegistry.registerTickHandler(
                new SEHTicker(EnumSet.of(TickType.RENDER, TickType.CLIENT)), Side.CLIENT);

        // '/statuseffect config' (ClientCommandHandler extends the vanilla CommandHandler)
        ClientCommandHandler.instance.a(new CommandStatusEffect());
    }
}
