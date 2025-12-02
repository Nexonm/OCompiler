# OCompiler
Simple compiler project for Compiler Construction course at Innopolis University Fall 2025

## Compilation

```txt
O Code (.o files)
    ↓
O Compiler (generates .j files)
    ↓
Jasmin Assembler (converts .j → .class)
    ↓
JVM (executes .class files)
```

## Compilation guide

First, create required directories. They are gitIgnored.
```bash
mkdir src/outcode
mkdir src/outcode/app
mkdir src/outcode/src
```

Compiling the O Compiler

For example, you can use:
```bash
javac -d out $(find src -path src/outcode -prune -o -name "*.java" -print)
```

To run test code like testTempo.o:
```bash
java -cp out LexerExample src/tests/testTempo.o
```

`LexerExample` automatically cleans `src/outcode/src` and `src/outcode/app` at the start of every run, so you always generate fresh `.j` and `.class` files without manually deleting old artifacts.

Whenever your program declares a `class Start` that contains:

- a parameterless constructor `this()`
- a method `start()` with no parameters that returns `Void`

the code generator emits a synthetic `Main` entry point in `src/outcode/src/Main.j`:

```java
public class Main {
    public static void main(String[] args) {
        Start start = new Start();
        start.start();
    }
}
```

This means you just assemble the generated Jasmin files and run `Main`.

Compile the Jasmin code into `.class` files and run it:
```bash
# Compile our jasmin code (includes Main.j)
java -jar src/tools/jasmin.jar src/outcode/src/*.j -d src/outcode/app
# Run the generated entrypoint
java -cp src/outcode/app Main
```