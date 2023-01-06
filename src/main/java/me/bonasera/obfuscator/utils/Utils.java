package me.bonasera.obfuscator.utils;

import java.io.*;

/**
 * @author Andrew Bonasera
 */

public final class Utils
{
    /**
     * Reads the first byte value of the archive.
     * All valid <code>.jar</code> archives begin with <code>0x504B0304</code>.
     */
    public static boolean isJarArchive(File file) throws IOException
    {
        FileInputStream fos = new FileInputStream(file);
        BufferedInputStream bos = new BufferedInputStream(fos);
        DataInputStream dos = new DataInputStream(bos);

        int magicValue = dos.readInt();

        dos.close();
        bos.close();
        fos.close();

        return magicValue == 0x504B0304;
    }

    /**
     * Uses an {@link ByteArrayOutputStream} to convert the contents of an InputStream to an array of bytes.
     */
    public static byte[] readBytes(InputStream is) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        byte[] buf = new byte[is.available()];
        int len;
        while ((len = is.read(buf, 0, buf.length)) != -1)
        {
            baos.write(buf, 0, len);
        }

        baos.close();

        return baos.toByteArray();
    }
}