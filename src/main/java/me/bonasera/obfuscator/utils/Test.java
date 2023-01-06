package me.bonasera.obfuscator.utils;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public final class Test
{

    public static final String test = getstring("test");

    public static String getstring(String s)
    {
        return s;
    }

    public static void main(String[] args) throws Throwable
    {
        Set<Integer> a = new HashSet<>();
        a.add(4);
        a.add(3);
        a.add(8);
        a.add(7);


        Set<Integer> b = new HashSet<>();
        b.add(7);
        b.add(3);
        b.add(4);
        b.add(8);

        Pattern pattern = Pattern.compile("[0-9]{3}-[0-9]{2}-[0-9]{3}");
        System.out.println(pattern.asPredicate().test("744-24-9123"));

        System.out.println(eq(a, b));

        int i = 0b0100100;
        int i2 = 0b01001;
        System.out.println(i2);
        System.out.println(String.format("bit 2=%s", (i >> 2) & 1));
    }

    static boolean eq(Set<Integer> a, Set<Integer> b)
    {
        if (a.size() == b.size())
        {
            for (int element : a)
            {
                if (!b.contains(element))
                {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public static <T extends Throwable> T loadexception(Class<T> clazz) throws Throwable
    {
        return clazz.newInstance();
    }

    public static int test(int i, int pos)
    {
        return ((i & ~(1 << pos)) | (1 << pos)) & ~(1 << pos);
    }

    public static String decrypt(char[] c) throws Throwable
    {
        StackTraceElement s = Thread.currentThread().getStackTrace()[2];
        for (int i = 0; i < c.length; i++)
        {
            String cn = s.getClassName();
            String dot = ".";
            String mn = s.getMethodName();
            c[i] ^= (cn + dot + mn).hashCode();
        }
        return new String(c);
    }

    static final class Bootstrapper
    {

    }
}
