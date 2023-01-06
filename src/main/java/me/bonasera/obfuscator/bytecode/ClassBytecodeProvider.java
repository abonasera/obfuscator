package me.bonasera.obfuscator.bytecode;

import me.bonasera.obfuscator.utils.Utils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author Andrew Bonasera
 */

public final class ClassBytecodeProvider
{
    public static Map<String, byte[]> readClasses(File archive) throws IOException
    {
        Map<String, byte[]> classes = new HashMap<>();
        FileInputStream fis = new FileInputStream(archive);
        ZipInputStream zis = new ZipInputStream(fis);
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null)
        {
            String name = entry.getName();
            if (name.endsWith(".class"))
            {
                byte[] bytecode = Utils.readBytes(zis);
                classes.put(name, bytecode);
            }
        }

        fis.close();
        zis.close();

        return classes;
    }

    public static Map<String, byte[]> readResources(File archive) throws IOException
    {
        Map<String, byte[]> resources = new HashMap<>();
        FileInputStream fis = new FileInputStream(archive);
        ZipInputStream zis = new ZipInputStream(fis);
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null)
        {
            String name = entry.getName();
            if (!name.endsWith(".class"))
            {
                byte[] content = Utils.readBytes(zis);
                resources.put(name, content);
            }
        }

        fis.close();
        zis.close();

        return resources;
    }

    public static ClassNode getNewClassNode(byte[] bytecode)
    {
        ClassNode classNode = new ClassNode();
        new ClassReader(bytecode).accept(classNode, 0);
        return classNode;
    }

    public static byte[] readBytes(ClassNode node)
    {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        node.accept(writer);
        return writer.toByteArray();
    }

    public static AbstractInsnNode ldcInt(int val)
    {
        if (val >= -1 && val <= 5)
        {
            return new InsnNode(val + 3);
        } else if (val >= -128 && val <= 127)
        {
            return new IntInsnNode(Opcodes.BIPUSH, val);
        } else if (val >= -32768 && val <= 32767)
        {
            return new IntInsnNode(Opcodes.SIPUSH, val);
        }
        return new LdcInsnNode(val);
    }
}