package me.bonasera;

import me.bonasera.obfuscator.Obfuscator;
import me.bonasera.utils.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.Scanner;

/**
 * @author Andrew Bonasera
 */

public final class Main
{
    private static final Logger logger = LogManager.getLogger();
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) throws Throwable
    {
        logger.info("Enter path to target jar file: ");

        File file = new File(scanner.nextLine());

        if (!file.exists() || !file.canRead() || file.isDirectory() || !Utils.isJarArchive(file))
        {
            logger.error("Provide a valid .jar archive.");
            return;
        }

        Obfuscator obfuscator = new Obfuscator(file);

        long oldSize = file.length();

        logger.info(
                "Located target archive {}, size=[{}]\n",
                file.getName(),
                oldSize
        );

        logger.info("Provide an obfuscation package filter (such as 'your/package/'): ");

        String filter = scanner.nextLine();

        obfuscator.preprocess(filter);
        obfuscator.process();
        obfuscator.postprocess(file.getParentFile(), "obf-" + file.getName(), oldSize);

        scanner.close();
    }

    public static Logger logger()
    {
        return logger;
    }
}