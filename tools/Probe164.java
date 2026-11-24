import java.util.ArrayList;
import java.util.List;

/**
 * Resolves every game/Forge member the ported StatusEffectHUD uses, through reflection against
 * the real Minecraft 1.6.4 client jar and the Forge 1.6.4 universal jar.
 *
 * The jars must be signature-stripped first (lib_unsigned), because the Mojang-signed client and
 * Forge live in the same default package and the JVM refuses to mix their signers.
 *
 * usage: java -cp tools/probe;lib_unsigned/* Probe164
 */
public class Probe164
{
    private static final List<String> failures = new ArrayList<String>();
    private static int checks = 0;

    public static void main(String[] args) throws Exception
    {
        /* ---- Minecraft ------------------------------------------------------ */
        clazz("atv");
        method("atv", "w", "()Latv;");
        method("atv", "J", "()Lbim;");
        method("atv", "a", "(Lawe;)V");
        field("atv", "n");
        field("atv", "h");
        field("atv", "l");
        field("atv", "u");
        field("atv", "A");
        field("atv", "d");
        field("atv", "e");

        constructor("awf", "(Laul;II)V");
        method("awf", "a", "()I");
        method("awf", "b", "()I");

        field("aul", "ab");
        field("aul", "Z");           // hideGUI (F1)
        field("aul", "T");
        field("ats", "e");

        method("bim", "a", "(Lbjo;)V");
        constructor("bjo", "(Ljava/lang/String;)V");

        method("ni", "d", "()Z");
        method("ni", "e", "()I");
        method("ni", "a", "()Ljava/lang/String;");
        method("ni", "a", "(Lnj;)Ljava/lang/String;");
        field("ni", "a");

        method("nj", "a", "()I");
        method("nj", "b", "()I");
        method("nj", "c", "()I");

        method("of", "aL", "()Ljava/util/Collection;");
        method("bu", "a", "(Ljava/lang/String;)Ljava/lang/String;");
        method("avi", "a", "(Ljava/lang/String;)I");
        method("avi", "a", "(Ljava/lang/String;III)I");

        method("bfq", "b", "()V");
        method("bfq", "a", "()I");
        method("bfq", "a", "(DDDDD)Lbfq;");
        field("bfq", "a");

        /* ---- GUI ------------------------------------------------------------ */
        constructor("awe", "()V");
        method("awe", "a", "(IIF)V");
        method("awe", "a", "(III)V");
        method("awe", "a", "(CI)V");
        method("awe", "a", "(Laut;)V");
        method("awe", "A_", "()V");
        method("awe", "d", "()V");
        field("awe", "i");
        field("awe", "o");
        field("awe", "g");
        field("awe", "h");
        field("awe", "f");

        constructor("aut", "(IIIIILjava/lang/String;)V");
        field("aut", "g");
        field("aut", "d");
        field("aut", "e");
        field("aut", "b");
        field("aut", "c");
        field("aut", "f");

        method("avk", "a", "(IIIII)V");

        /* ---- inventory effect list ------------------------------------------ */
        field("axp", "t");

        /* ---- command interface ---------------------------------------------- */
        method("aa", "a", "(Lab;)Lab;");
        field("net.minecraftforge.client.ClientCommandHandler", "instance");
        method("net.minecraftforge.client.ClientCommandHandler", "a", "(Lad;Ljava/lang/String;)I");
        clazz("ab");
        clazz("ad");

        /* ---- Forge configuration -------------------------------------------- */
        constructor("net.minecraftforge.common.Configuration", "(Ljava/io/File;)V");
        method("net.minecraftforge.common.Configuration", "load", "()V");
        method("net.minecraftforge.common.Configuration", "save", "()V");
        method("net.minecraftforge.common.Configuration", "addCustomCategoryComment", "(Ljava/lang/String;Ljava/lang/String;)V");
        method("net.minecraftforge.common.Configuration", "get", "(Ljava/lang/String;Ljava/lang/String;Z)Lnet/minecraftforge/common/Property;");
        method("net.minecraftforge.common.Configuration", "get", "(Ljava/lang/String;Ljava/lang/String;I)Lnet/minecraftforge/common/Property;");
        method("net.minecraftforge.common.Configuration", "get", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Lnet/minecraftforge/common/Property;");
        method("net.minecraftforge.common.Configuration", "get", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Lnet/minecraftforge/common/Property;");
        method("net.minecraftforge.common.Property", "getBoolean", "(Z)Z");
        method("net.minecraftforge.common.Property", "getInt", "(I)I");
        method("net.minecraftforge.common.Property", "getString", "()Ljava/lang/String;");
        method("net.minecraftforge.common.Property", "set", "(Z)V");
        method("net.minecraftforge.common.Property", "set", "(I)V");
        method("net.minecraftforge.common.Property", "set", "(Ljava/lang/String;)V");

        /* ---- FML ------------------------------------------------------------ */
        method("cpw.mods.fml.common.registry.TickRegistry", "registerTickHandler", "(Lcpw/mods/fml/common/ITickHandler;Lcpw/mods/fml/relauncher/Side;)V");
        method("cpw.mods.fml.common.FMLCommonHandler", "instance", "()Lcpw/mods/fml/common/FMLCommonHandler;");
        clazz("cpw.mods.fml.common.Mod");
        clazz("cpw.mods.fml.common.Mod$EventHandler");
        clazz("cpw.mods.fml.common.Mod$Instance");
        clazz("cpw.mods.fml.common.Mod$Metadata");
        clazz("cpw.mods.fml.common.ModMetadata");
        clazz("cpw.mods.fml.common.ITickHandler");
        clazz("cpw.mods.fml.common.TickType");
        clazz("cpw.mods.fml.common.event.FMLPreInitializationEvent");
        clazz("cpw.mods.fml.common.event.FMLInitializationEvent");
        clazz("cpw.mods.fml.relauncher.Side");

        /* ---- annotations we rely on ----------------------------------------- */
        method("cpw.mods.fml.common.event.FMLPreInitializationEvent", "getSuggestedConfigurationFile", "()Ljava/io/File;");
        method("cpw.mods.fml.common.event.FMLPreInitializationEvent", "getModMetadata", "()Lcpw/mods/fml/common/ModMetadata;");

        /* ---- TickType constants --------------------------------------------- */
        Enum<?>[] tickTypes = (Enum<?>[]) Class.forName("cpw.mods.fml.common.TickType").getEnumConstants();
        List<String> names = new ArrayList<String>();
        for (Enum<?> e : tickTypes)
        {
            names.add(e.name());
        }
        check(names.contains("RENDER"), "TickType.RENDER exists");
        check(names.contains("CLIENT"), "TickType.CLIENT exists");

        /* ---- tick types accepted by TickRegistry ---------------------------- */
        clazz("cpw.mods.fml.common.TickType");

        System.out.println();
        System.out.println("checks run : " + checks);
        System.out.println("failures   : " + failures.size());
        for (String f : failures)
        {
            System.out.println("  FAIL " + f);
        }
        if (failures.isEmpty())
        {
            System.out.println("OK - every member used by the 1.6.4 port resolves on the real classpath");
        }
        System.exit(failures.isEmpty() ? 0 : 1);
    }

    private static void clazz(String name)
    {
        checks++;
        try
        {
            Class.forName(name, false, Probe164.class.getClassLoader());
        }
        catch (Throwable t)
        {
            failures.add("class " + name + " (" + t + ")");
        }
    }

    private static void constructor(String name, String desc)
    {
        checks++;
        try
        {
            Class<?> cls = Class.forName(name, false, Probe164.class.getClassLoader());
            cls.getDeclaredConstructor(types(desc));
        }
        catch (Throwable t)
        {
            failures.add(name + ".<init>" + desc + " (" + t + ")");
        }
    }

    private static void method(String name, String methodName, String desc)
    {
        checks++;
        try
        {
            Class<?> cls = Class.forName(name, false, Probe164.class.getClassLoader());
            cls.getDeclaredMethod(methodName, types(desc));
        }
        catch (Throwable t)
        {
            failures.add(name + "." + methodName + desc + " (" + t + ")");
        }
    }

    private static void field(String name, String fieldName)
    {
        checks++;
        try
        {
            Class<?> cls = Class.forName(name, false, Probe164.class.getClassLoader());
            cls.getDeclaredField(fieldName);
        }
        catch (Throwable t)
        {
            failures.add(name + "." + fieldName + " (" + t + ")");
        }
    }

    private static void check(boolean ok, String what)
    {
        checks++;
        if (!ok)
        {
            failures.add(what);
        }
    }

    /** Minimal descriptor parser (enough for the members probed above). */
    private static Class<?>[] types(String desc) throws ClassNotFoundException
    {
        List<Class<?>> out = new ArrayList<Class<?>>();
        int i = 1;
        while (desc.charAt(i) != ')')
        {
            switch (desc.charAt(i))
            {
                case 'Z': out.add(Boolean.TYPE); i++; break;
                case 'B': out.add(Byte.TYPE); i++; break;
                case 'C': out.add(Character.TYPE); i++; break;
                case 'S': out.add(Short.TYPE); i++; break;
                case 'I': out.add(Integer.TYPE); i++; break;
                case 'J': out.add(Long.TYPE); i++; break;
                case 'F': out.add(Float.TYPE); i++; break;
                case 'D': out.add(Double.TYPE); i++; break;
                case 'V': out.add(Void.TYPE); i++; break;
                case 'L':
                {
                    int end = desc.indexOf(';', i);
                    out.add(Class.forName(desc.substring(i + 1, end).replace('/', '.'), false, Probe164.class.getClassLoader()));
                    i = end + 1;
                    break;
                }
                case '[':
                {
                    int start = i;
                    while (desc.charAt(i) == '[')
                    {
                        i++;
                    }
                    if (desc.charAt(i) == 'L')
                    {
                        i = desc.indexOf(';', i) + 1;
                    }
                    else
                    {
                        i++;
                    }
                    out.add(Class.forName(desc.substring(start, i).replace('/', '.'), false, Probe164.class.getClassLoader()));
                    break;
                }
                default:
                    i++;
                    break;
            }
        }
        return out.toArray(new Class<?>[out.size()]);
    }
}
