package me.bonasera.obfuscator.implementation;

import java.util.Map;

/**
 * @author Andrew Bonasera
 */

public interface IProcessor
{
    void process(Map<String, byte[]> bytecode);
}