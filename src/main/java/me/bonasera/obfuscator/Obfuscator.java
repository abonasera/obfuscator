package me.bonasera.obfuscator;

import me.bonasera.Main;
import me.bonasera.bytecode.ClassBytecodeProvider;
import me.bonasera.obfuscator.processors.StringProcessor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * @author Andrew Bonasera
 */

public final class Obfuscator
{
    private final Map<String, byte[]> processed = new HashMap<>();
    private final Map<String, byte[]> unprocessed = new HashMap<>();

    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument") // For future processors
    private final List<IProcessor> processors = Arrays.asList(
            new StringProcessor()
    );

    /**
     * Reads the byte content of the archive.
     */
    public Obfuscator(File archive) throws IOException
    {
        Map<String, byte[]> classes = ClassBytecodeProvider.readClasses(archive);
        Map<String, byte[]> resources = ClassBytecodeProvider.readResources(archive);

        this.unprocessed.putAll(classes);
        this.unprocessed.putAll(resources);
    }

    /**
     * Pre-processing, filters which files need to be modified and which ones can be skipped.
     */
    public void preprocess(String obfuscationPrefix)
    {
        Iterator<Map.Entry<String, byte[]>> iterator = this.unprocessed.entrySet().iterator();
        while (iterator.hasNext())
        {
            Map.Entry<String, byte[]> next = iterator.next();
            if (!(next.getKey().endsWith(".class") && next.getKey().startsWith(obfuscationPrefix)))
            {
                this.processed.put(next.getKey(), next.getValue());
                iterator.remove();
            }
        }

        Main.logger().info("Pre-processing complete, prepared {} files for obfuscation.\n", this.unprocessed.size());
    }

    /**
     * The main process of the obfuscator.
     */
    public void process()
    {
        this.processors.forEach(
                p -> p.process(this.unprocessed)
        );

        this.processed.putAll(this.unprocessed);
    }

    /**
     * Write the processed classes to an output jar archive.
     */
    public void postprocess(File outputDir, String outputName, long oldSize) throws IOException
    {
        File output = new File(outputDir, outputName);

        FileOutputStream fos = new FileOutputStream(output);
        JarOutputStream jos = new JarOutputStream(fos);

        for (Iterator<Map.Entry<String, byte[]>> iterator = this.processed.entrySet().iterator(); iterator.hasNext();)
        {
           Map.Entry<String, byte[]> next = iterator.next();
            JarEntry jarEntry = new JarEntry(next.getKey());
            jos.putNextEntry(jarEntry);
            jos.write(next.getValue());
            jos.closeEntry();

            iterator.remove();
        }

        jos.close();
        fos.close();

        Main.logger().info(
                "Successfully wrote output {}, size=[{}] (+{} bytes)",
                output.getName(),
                output.length(),
                output.length() - oldSize
        );
    }
}