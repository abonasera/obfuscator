# Obfuscator

## Java Bytecode & Decompilation

When a Java project is built, the Java compiler parses each file and compiles and links them into .class files inside of a jar archive. These .class files contain **bytecode**, which the Java Virtual Machine interprets at runtime.

Using decompilers, a jar file can have its bytecode reinterpreted back into human readable code, allowing anyone to take your compiled projects and recover the (nearly) complete source code.

For example, when a simple Hello World program is decompiled using JD-GUI, the following source code is recovered:

![unobf](https://i.imgur.com/x5GmFQ4.png) ![helloworld](https://i.imgur.com/P7LKzfD.png)

For projects that contain sensitive information, such as links to hidden web pages or secret keys, having the program's full source code available could pose as a security risk.

## Post Obfuscation

When the same Hello World program is obfuscated and decompiled using the same program, the following source code is recovered:

![obf](https://i.imgur.com/N925MyF.png) ![helloworldobf](https://i.imgur.com/gjg6t0V.png)

All of the string values stored in the bytecode are scrambled to become unreadable, and the decryption class *$* descrambles them at runtime.

$.class:

![decryptclass](https://i.imgur.com/xyfjONW.png)

## Output

When the obfuscated Hello World program is run, the output is still as expected:

![result](https://i.imgur.com/5ZZVwLY.png)

Notice how the obfuscation of the string values does not change the intended output or semantics of the program.

## Usage

This program runs on the command line.

Open cmd/terminal and run `java -jar Obfuscator.jar` to get started - the program will display the rest of the instructions.

The program can be either downloaded from the releases tab or compiled yourself (see below).

## Building

This project uses Gradle 7.1.1 in order to compile. The steps for compiling are as follows:

- Download the source code
- Open cmd/terminal
- Change directories to the project
- Run `./gradlew build`

The finished archive can then be found in `./build/libs`.
