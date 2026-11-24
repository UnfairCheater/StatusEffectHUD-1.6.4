import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Verifies that a 1.6.4 mod jar only references types that exist on the game classpath,
 * and that it does not use MCP/SRG names that the retail client cannot resolve.
 *
 * usage: java -cp asm.jar;asm-tree.jar VerifyJar <modJar> <libDir>
 */
public class VerifyJar
{
    private static final Set<String> available = new HashSet<String>();
    private static final Set<String> missing = new TreeSet<String>();
    private static final Set<String> suspiciousNames = new TreeSet<String>();

    /** SRG names the mod passes to reflection, collected from string constants. */
    private static final Set<String> referencedSrgNames = new TreeSet<String>();

    /** SRG names that exist in the 1.6.4 mapping table (empty when no table was supplied). */
    private static final Set<String> knownSrgNames = new TreeSet<String>();

    private static boolean unknownSrgNameFound = false;

    public static void main(String[] args) throws Exception
    {
        File modJar = new File(args[0]);
        File libDir = new File(args[1]);

        PrintStream out = System.out;

        // optional third argument: Forge's deobfuscation_data-1.6.4 dump, used to check that every
        // SRG name the mod mentions actually exists in the 1.6.4 mappings
        // knownSrgNames is a static set (filled below)
        if (args.length >= 3)
        {
            File deobf = new File(args[2]);
            if (deobf.isFile())
            {
                java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(
                        new java.io.FileInputStream(deobf), "UTF-8"));
                String line;
                while ((line = r.readLine()) != null)
                {
                    // the format is "FD: <obf> <srg>" / "MD: <obf> <sig> <srg>": split on whitespace
                    // AND the colon, or p[0] comes out as "FD:" and nothing ever matches
                    String[] p = line.split("[\\s:]+", -1);
                    if (p.length >= 3 && ("FD".equals(p[0]) || "MD".equals(p[0])))
                    {
                        // every token that looks like an SRG name counts, old and new
                        for (String tok : p)
                        {
                            int slash = tok.lastIndexOf('/');
                            String simple = slash >= 0 ? tok.substring(slash + 1) : tok;
                            if (simple.startsWith("field_") || simple.startsWith("func_"))
                            {
                                knownSrgNames.add(simple);
                            }
                        }
                    }
                }
                r.close();
            }
        }

        for (File lib : libDir.listFiles())
        {
            if (!lib.getName().endsWith(".jar"))
            {
                continue;
            }
            ZipFile zf = new ZipFile(lib);
            for (Enumeration<? extends ZipEntry> e = zf.entries(); e.hasMoreElements();)
            {
                String n = e.nextElement().getName();
                if (n.endsWith(".class"))
                {
                    available.add(n.substring(0, n.length() - 6));
                }
            }
            zf.close();
        }

        Set<String> modClasses = new HashSet<String>();
        ZipFile zf = new ZipFile(modJar);
        for (Enumeration<? extends ZipEntry> e = zf.entries(); e.hasMoreElements();)
        {
            String n = e.nextElement().getName();
            if (n.endsWith(".class"))
            {
                modClasses.add(n.substring(0, n.length() - 6));
            }
        }

        out.println("classpath types: " + available.size());
        out.println("mod classes    : " + modClasses.size());

        for (String cls : modClasses)
        {
            final String owner = cls;
            ClassReader cr = new ClassReader(zf.getInputStream(zf.getEntry(cls + ".class")));
            cr.accept(new ClassVisitor(Opcodes.ASM9)
            {
                private void check(String internal)
                {
                    if (internal == null || internal.length() == 0)
                    {
                        return;
                    }
                    if (internal.charAt(0) == '[')
                    {
                        check(Type.getType(internal).getElementType().getInternalName());
                        return;
                    }
                    if (internal.length() == 1)
                    {
                        // primitive descriptor (I, V, Z, C, D, F, J, S, B)
                        return;
                    }
                    if (internal.startsWith("java/") || internal.startsWith("javax/")
                            || internal.startsWith("sun/") || internal.startsWith("com/sun/"))
                    {
                        // provided by the JRE
                        return;
                    }
                    if (modClasses.contains(internal) || available.contains(internal))
                    {
                        return;
                    }
                    // inner classes resolve through their outer class
                    int dollar = internal.indexOf('$');
                    if (dollar > 0 && (modClasses.contains(internal.substring(0, dollar))
                            || available.contains(internal.substring(0, dollar))))
                    {
                        return;
                    }
                    missing.add(internal + "   <- " + owner);
                }

                private void checkDesc(String desc)
                {
                    if (desc == null)
                    {
                        return;
                    }
                    try
                    {
                        for (Type t : Type.getArgumentTypes(desc))
                        {
                            check(t.getInternalName());
                        }
                        check(Type.getReturnType(desc).getInternalName());
                    }
                    catch (RuntimeException ignored)
                    {
                    }
                    scanForSrg(desc, owner);
                }

                private void scanForSrg(String text, String where)
                {
                    if (text == null)
                    {
                        return;
                    }
                    // These SRG names are intentionally kept as reflection fallback names, for an
                    // environment that runs with SRG names instead of obfuscated ones. They are
                    // string constants used with Class.getDeclaredField, never class references:
                    //   field_74222_o  InventoryEffectRenderer "show effect list" flag (axp.t)
                    //   field_74198_m  GuiContainer guiLeft  (awy.p)
                    //   field_73880_f  GuiScreen width       (awe.g)
                    //
                    // NOTE: the 1.7.10 names field_147003_i / field_146999_f must NOT appear here -
                    // they do not exist in 1.6.4 and using them is the bug this check now catches.
                    if (text.contains("field_") || text.contains("func_"))
                    {
                        if (text.equals("field_74222_o")
                                || text.equals("field_74198_m")
                                || text.equals("field_73880_f"))
                        {
                            return;
                        }
                        suspiciousNames.add(text + "   <- " + where);
                    }
                }

                @Override
                public void visit(int version, int access, String name, String signature, String superName, String[] interfaces)
                {
                    check(superName);
                    if (interfaces != null)
                    {
                        for (String i : interfaces)
                        {
                            check(i);
                        }
                    }
                    scanForSrg(signature == null ? "" : signature, owner);
                    if (!"java/lang/Object".equals(superName) && superName != null && superName.length() <= 3)
                    {
                        // obfuscated game class: expected for 1.6.4, nothing to do
                    }
                }

                @Override
                public FieldVisitor visitField(int access, String name, String desc, String signature, Object value)
                {
                    checkDesc(desc);
                    scanForSrg(name, owner);
                    return null;
                }

                @Override
                public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions)
                {
                    checkDesc(desc);
                    if (exceptions != null)
                    {
                        for (String ex : exceptions)
                        {
                            check(ex);
                        }
                    }
                    return new MethodVisitor(Opcodes.ASM9)
                    {
                        @Override
                        public void visitTypeInsn(int opcode, String type)
                        {
                            check(type);
                        }

                        @Override
                        public void visitFieldInsn(int opcode, String ownerType, String name, String desc)
                        {
                            check(ownerType);
                            checkDesc(desc);
                            scanForSrg(ownerType + "." + name, owner);
                        }

                        @Override
                        public void visitMethodInsn(int opcode, String ownerType, String name, String desc, boolean itf)
                        {
                            check(ownerType);
                            checkDesc(desc);
                            scanForSrg(ownerType + "." + name, owner);
                        }

                        @Override
                        public void visitLdcInsn(Object cst)
                        {
                            if (cst instanceof String)
                            {
                                String s = (String) cst;
                                scanForSrg(s, owner);
                                // remember SRG names used as string constants (reflection targets)
                                for (String tok : s.split("[^A-Za-z0-9_]"))
                                {
                                    if (tok.startsWith("field_") || tok.startsWith("func_"))
                                    {
                                        referencedSrgNames.add(tok);
                                    }
                                }
                            }
                        }
                    };
                }
            }, ClassReader.SKIP_FRAMES);
        }

        zf.close();

        out.println();
        out.println("=== unresolved types ===");
        if (missing.isEmpty())
        {
            out.println("  none - every referenced type exists on the 1.6.4 + Forge classpath");
        }
        else
        {
            for (String m : missing)
            {
                out.println("  MISSING " + m);
            }
        }

        out.println();
        out.println("=== SRG/MCP name leaks (would fail on the retail client) ===");
        if (suspiciousNames.isEmpty())
        {
            out.println("  none");
        }
        else
        {
            for (String s : suspiciousNames)
            {
                out.println("  " + s);
            }
        }

        // This is the check that would have caught field_147003_i: an SRG name copied from a
        // 1.7.10 example that simply does not exist in the 1.6.4 mapping table.
        out.println();
        out.println("=== SRG names used as reflection targets ===");
        if (knownSrgNames.isEmpty())
        {
            out.println("  skipped (no deobfuscation table supplied as the 3rd argument)");
        }
        else if (referencedSrgNames.isEmpty())
        {
            out.println("  none");
        }
        else
        {
            boolean allKnown = true;
            for (String s : new TreeSet<String>(referencedSrgNames))
            {
                boolean known = knownSrgNames.contains(s);
                if (!known)
                {
                    allKnown = false;
                    unknownSrgNameFound = true;
                }
                out.println("  " + (known ? "OK      " : "UNKNOWN ") + s);
            }
            if (!allKnown)
            {
                out.println("  ^ an UNKNOWN name does not exist in the 1.6.4 mappings - it can never");
                out.println("    resolve at runtime, so the reflection using it is dead code");
            }
        }

        System.exit(missing.isEmpty() && !unknownSrgNameFound ? 0 : 1);
    }
}

