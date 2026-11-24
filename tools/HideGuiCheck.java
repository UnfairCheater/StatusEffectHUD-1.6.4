import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Asserts that the HUD mods respect the F1 "hide GUI" flag.
 *
 * <p>Why this needs a test at all: both mods draw from the render tick, not from
 * {@code RenderGameOverlayEvent}, so vanilla's own guard does not protect them. Vanilla only calls
 * {@code renderGameOverlay} when {@code !gameSettings.hideGUI || currentScreen != null}; that call is
 * also what switches the GL state to a 2D orthographic projection. Drawing from the render tick with
 * F1 held therefore happens in leftover 3D state and the HUD flickers.</p>
 *
 * <p>The guard is expressed as a read of {@code GameSettings.field_74319_N} (obfuscated
 * {@code aul.Z}), so this check looks for exactly that: a GETFIELD of {@code Z} on {@code aul}
 * inside the method that renders the HUD.</p>
 *
 *   java -cp tools/verify;tools/asm*.jar HideGuiCheck &lt;jar&gt; &lt;class&gt; &lt;method&gt;
 */
public class HideGuiCheck
{
    public static void main(String[] args) throws Exception
    {
        if (args.length < 3)
        {
            System.out.println("usage: HideGuiCheck <jar> <class> <methodName>");
            System.exit(2);
        }

        String jarPath = args[0];
        String className = args[1];
        String methodName = args[2];

        byte[] bytes = readEntry(jarPath, className + ".class");
        if (bytes == null)
        {
            System.out.println("  FAIL " + className + " not found in " + jarPath);
            System.exit(1);
        }

        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);

        MethodNode target = null;
        for (MethodNode m : node.methods)
        {
            if (m.name.equals(methodName))
            {
                target = m;
                break;
            }
        }

        if (target == null)
        {
            System.out.println("  FAIL " + className + "." + methodName + " not found");
            System.exit(1);
        }

        System.out.println("  " + className + "." + target.name + target.desc);

        // collect every field read in the method, so a failure can show what WAS read
        List<String> fieldsRead = new ArrayList<String>();
        boolean hideGuiRead = false;
        for (AbstractInsnNode insn = target.instructions.getFirst(); insn != null; insn = insn.getNext())
        {
            if (insn instanceof FieldInsnNode)
            {
                FieldInsnNode f = (FieldInsnNode) insn;
                fieldsRead.add(f.owner + "." + f.name);
                if (f.owner.equals("aul") && f.name.equals("Z"))
                {
                    hideGuiRead = true;
                }
            }
        }

        System.out.println("    field reads in this method: " + fieldsRead);
        System.out.println("    reads aul.Z (hideGUI, F1):  " + (hideGuiRead ? "YES" : "NO"));

        if (!hideGuiRead)
        {
            System.out.println("  FAIL the HUD would keep drawing while F1 hides the game GUI,"
                    + " which makes it flicker (no 2D projection is set up in that state)");
            System.exit(1);
        }

        System.out.println("  OK   the F1 hide-GUI flag gates the HUD");
    }

    private static byte[] readEntry(String jarPath, String entryName) throws Exception
    {
        ZipFile zip = new ZipFile(new File(jarPath));
        try
        {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements())
            {
                ZipEntry e = entries.nextElement();
                if (e.getName().equals(entryName))
                {
                    java.io.InputStream in = zip.getInputStream(e);
                    java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) > 0)
                    {
                        out.write(buffer, 0, read);
                    }
                    in.close();
                    return out.toByteArray();
                }
            }
        }
        finally
        {
            zip.close();
        }
        return null;
    }
}
