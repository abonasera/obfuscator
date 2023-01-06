package me.bonasera.obfuscator.processors;

import me.bonasera.Main;
import me.bonasera.bytecode.ClassBytecodeProvider;
import me.bonasera.obfuscator.IProcessor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Andrew Bonasera
 */

public final class StringProcessor implements IProcessor
{
    @Override
    public void process(Map<String, byte[]> bytecode)
    {
        List<LdcInfo> ldcs = new ArrayList<>();

        /*
         * Collect all ldc instructions that need to be obfuscated
         */
        for (Map.Entry<String, byte[]> next : bytecode.entrySet())
        {
            ClassNode classNode = ClassBytecodeProvider.getNewClassNode(next.getValue());

            for (MethodNode methodNode : classNode.methods)
            {
                for (AbstractInsnNode absInsn : methodNode.instructions)
                {
                    if (absInsn instanceof LdcInsnNode && ((LdcInsnNode) absInsn).cst instanceof String)
                    {
                        LdcInfo info = new LdcInfo(next.getKey(), classNode, methodNode, (LdcInsnNode) absInsn);
                        ldcs.add(info);
                    }
                }
            }
        }

        /*
         * Hash each string with its parent method's unique signature
         */
        String[] obfuscated = new String[ldcs.size()];
        AtomicInteger count = new AtomicInteger();

        ldcs.forEach(ldcInfo ->
        {
            String val = (String) ldcInfo.ldc().cst;

            String sig = ldcInfo.parentClass().name
                    .replace("/", ".")
                    + "."
                    + ldcInfo.parentMethod().name;

            StringBuilder cst = new StringBuilder();

            for (char c : val.toCharArray())
            {
                c ^= sig.hashCode();
                cst.append(c);
            }

            for (AbstractInsnNode absInsn : ldcInfo.parentMethod().instructions)
            {
                if (absInsn.equals(ldcInfo.ldc()))
                {
                    /*
                     * Replace ldc instructions with a call to the decryption method
                     */
                    FieldInsnNode fin = new FieldInsnNode(
                            Opcodes.GETSTATIC,
                            "$",
                            ".",
                            "[Ljava/lang/String;"
                    );

                    ldcInfo.parentMethod().instructions.insertBefore(absInsn, fin);

                    ldcInfo.parentMethod().instructions.insert(ldcInfo.ldc(), ClassBytecodeProvider.ldcInt(count.get()));

                    ldcInfo.parentMethod().instructions.remove(ldcInfo.ldc());

                    InsnNode aaload = new InsnNode(Opcodes.AALOAD);

                    ldcInfo.parentMethod().instructions.insert(fin.getNext(), aaload);

                    MethodInsnNode toCharArray = new MethodInsnNode(
                            Opcodes.INVOKEVIRTUAL,
                            "java/lang/String",
                            "toCharArray",
                            "()[C",
                            false
                    );

                    ldcInfo.parentMethod().instructions.insert(aaload, toCharArray);

                    MethodInsnNode decrypt = new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            "$",
                            ".",
                            "([C)Ljava/lang/String;",
                            false
                    );

                    ldcInfo.parentMethod().instructions.insert(toCharArray, decrypt);
                }
            }

            obfuscated[count.getAndAdd(1)] = cst.toString();

            bytecode.put(
                    ldcInfo.name(),
                    ClassBytecodeProvider.readBytes(ldcInfo.parentClass())
            );
        });

        Main.logger().info("Successfully transformed {} string values.\n", count.get());

        /*
         * Generate dummy class with the string fields
         */
        ClassNode dummy = new ClassNode();

        dummy.name = "$";
        dummy.superName = "java/lang/Object";
        dummy.version = Opcodes.V1_8;
        dummy.access = Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL;

        /*
         * <clinit>()V
         */
        MethodNode clinit = new MethodNode(
                Opcodes.ACC_STATIC,
                "<clinit>",
                "()V",
                null,
                null
        );

        InsnList instructions = new InsnList();

        /*
         * $ = anewarray[count]
         */
        instructions.add(ClassBytecodeProvider.ldcInt(count.get()));
        instructions.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/String"));
        instructions.add(new FieldInsnNode(Opcodes.PUTSTATIC, "$", ".", "[Ljava/lang/String;"));

        for (int i = 0; i < count.get(); i++)
        {
            /*
             * $[i] = obfuscated[i]
             */
            instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, "$", ".", "[Ljava/lang/String;"));
            instructions.add(ClassBytecodeProvider.ldcInt(i));
            instructions.add(new LdcInsnNode(obfuscated[i]));
            instructions.add(new InsnNode(Opcodes.AASTORE));
        }

        instructions.add(new InsnNode(Opcodes.RETURN));

        clinit.instructions = instructions;

        dummy.methods.add(clinit);

        /*
         * <init>()V
         */
        MethodNode init = new MethodNode(
                Opcodes.ACC_PUBLIC,
                "<init>",
                "()V",
                null,
                null
        );

        instructions = new InsnList();

        /*
         * super java/lang/Object
         */
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false));

        instructions.add(new InsnNode(Opcodes.RETURN));

        init.instructions = instructions;

        dummy.methods.add(init);

        /*
         * Obfuscation string array
         */
        FieldNode array = new FieldNode(
                Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC + Opcodes.ACC_FINAL,
                ".",
                "[Ljava/lang/String;",
                null,
                null
        );

        dummy.fields.add(array);

        /*
         * decrypt([C)Ljava/lang/String;
         */
        MethodNode decrypt = new MethodNode(
                Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC,
                ".",
                "([C)Ljava/lang/String;",
                null,
                new String[] {
                        "java/lang/IllegalThreadStateException",
                        "java/lang/SecurityException",
                }
        );

        instructions = new InsnList();

        LabelNode forStart = new LabelNode();
        LabelNode forEnd = new LabelNode();

        /*
         * astore Thread.currentThread() @var1
         */
        instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;"));
        instructions.add(new VarInsnNode(Opcodes.ASTORE, 1));

        /*
         * astore var1.getStackTrace() @var2
         */
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Thread", "getStackTrace", "()[Ljava/lang/StackTraceElement;"));
        instructions.add(new VarInsnNode(Opcodes.ASTORE, 2));

        /*
         * astore var1[2] @var1
         */
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
        instructions.add(new InsnNode(Opcodes.ICONST_2));
        instructions.add(new InsnNode(Opcodes.AALOAD));
        instructions.add(new VarInsnNode(Opcodes.ASTORE, 1));

        /*
         * istore 0 @var2
         */
        instructions.add(new InsnNode(Opcodes.ICONST_0));
        instructions.add(new VarInsnNode(Opcodes.ISTORE, 2));

        /*
         * begin for loop
         */
        instructions.add(forStart);

        /*
         * if_icmpge(var2, var0.length) end for
         */
        instructions.add(new VarInsnNode(Opcodes.ILOAD, 2));
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        instructions.add(new InsnNode(Opcodes.ARRAYLENGTH));
        instructions.add(new JumpInsnNode(Opcodes.IF_ICMPGE, forEnd));

        /*
         * astore var1.getClassName() @var3
         */
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StackTraceElement", "getClassName", "()Ljava/lang/String;"));
        instructions.add(new VarInsnNode(Opcodes.ASTORE, 3));

        /*
         * istore "." @var4
         * (int)"." = 46
         */
        instructions.add(new IntInsnNode(Opcodes.BIPUSH, 46));
        instructions.add(new VarInsnNode(Opcodes.ISTORE, 4));

        /*
         * astore var1.getMethodName() @var5
         */
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StackTraceElement", "getMethodName", "()Ljava/lang/String;"));
        instructions.add(new VarInsnNode(Opcodes.ASTORE, 5));

        /*
         * caload char var0[var2]
         */
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        instructions.add(new VarInsnNode(Opcodes.ILOAD, 2));
        instructions.add(new InsnNode(Opcodes.DUP2));
        instructions.add(new InsnNode(Opcodes.CALOAD));

        /*
         * new StringBuilder()
         */
        instructions.add(new TypeInsnNode(Opcodes.NEW, "java/lang/StringBuilder"));
        instructions.add(new InsnNode(Opcodes.DUP));
        instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false));

        /*
         * StringBuilder.append(var3)
         */
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 3));
        instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));

        /*
         * StringBuilder.append(var4)
         */
        instructions.add(new VarInsnNode(Opcodes.ILOAD, 4));
        instructions.add(new InsnNode(Opcodes.I2C));
        instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(C)Ljava/lang/StringBuilder;", false));

        /*
         * StringBuilder.append(var5)
         */
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 5));
        instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));

        /*
         * var0[var2] ^= StringBuilder.toString().hashCode()
         */
        instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false));
        instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "hashCode", "()I"));
        instructions.add(new InsnNode(Opcodes.IXOR));
        instructions.add(new InsnNode(Opcodes.I2C));
        instructions.add(new InsnNode(Opcodes.CASTORE));

        /*
         * var2++; goto forStart
         */
        instructions.add(new IincInsnNode(2, 1));
        instructions.add(new JumpInsnNode(Opcodes.GOTO, forStart));

        /*
         * end of for loop
         */
        instructions.add(forEnd);

        /*
         * new String([C)
         */
        instructions.add(new TypeInsnNode(Opcodes.NEW, "java/lang/String"));
        instructions.add(new InsnNode(Opcodes.DUP));
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/String", "<init>", "([C)V"));

        /*
         * return String
         */
        instructions.add(new InsnNode(Opcodes.ARETURN));

        decrypt.instructions.add(instructions);

        dummy.methods.add(decrypt);

        bytecode.put(
                "$.class",
                ClassBytecodeProvider.readBytes(dummy)
        );
    }

    private static final class LdcInfo
    {
        private final String name;
        private final ClassNode parentClass;
        private final MethodNode parentMethod;
        private final LdcInsnNode ldc;

        public LdcInfo(String name, ClassNode parentClass, MethodNode parentMethod, LdcInsnNode ldc)
        {
            this.name = name;
            this.parentClass = parentClass;
            this.parentMethod = parentMethod;
            this.ldc = ldc;
        }

        public String name()
        {
            return this.name;
        }

        public ClassNode parentClass()
        {
            return this.parentClass;
        }

        public MethodNode parentMethod()
        {
            return this.parentMethod;
        }

        public LdcInsnNode ldc()
        {
            return this.ldc;
        }
    }
}