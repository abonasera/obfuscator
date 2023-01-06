package me.bonasera.obfuscator;

import java.util.Map;

/**
 * @author Andrew Bonasera
 */

public interface IProcessor
{
    void process(Map<String, byte[]> bytecode);
}