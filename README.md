# OCompiler
Simple compiler project for Compiler Construction course at Innopolis University Fall 2025


## SymbolTableBuilder Validation Checks

### Pass 1: Class Registration & Inheritance

#### Class Declaration Checks
- ✓ **Duplicate Class Names** - Ensures no two classes have the same name
- ✓ **Self-Inheritance** - Prevents a class from extending itself (`class A extends A`)
- ✓ **Circular Inheritance** - Detects inheritance cycles (e.g., `A extends B`, `B extends A`)
- ✓ **Parent Class Existence** - Verifies that parent class exists before allowing inheritance
- ✓ **Built-in Type Extension** - Prevents extending built-in types like `Integer`, `Boolean`, `Real`

### Pass 2: Class Member Validation

#### Field Checks
- ✓ **Duplicate Field Names** - Ensures no duplicate field declarations within a class

#### Method Checks
- ✓ **Duplicate Method Signatures** - Prevents multiple methods with identical signature (`name(Type1,Type2)`)
- ✓ **Parameter Type Validation** - Verifies all parameter types exist (checks against global scope)
- ✓ **Return Type Validation** - Verifies return type exists (checks against global scope)
- ✓ **Duplicate Parameter Names** - Ensures no duplicate parameters in same method

#### Constructor Checks
- ✓ **Duplicate Constructor Signatures** - Prevents multiple constructors with identical parameter types
- ✓ **Parameter Type Validation** - Verifies all constructor parameter types exist
- ✓ **Duplicate Parameter Names** - Ensures no duplicate parameters in same constructor

### Pass 3: Name Resolution in Method Bodies

#### Variable Resolution
- ✓ **Undefined Variable Usage** - Ensures variables are declared before use in expressions
- ✓ **Variable Redeclaration** - Prevents declaring same variable twice in same scope
- ✓ **Assignment Target Resolution** - Verifies assignment target variable exists

#### Expression Resolution
- ✓ **Identifier Resolution** - Links all identifier uses to their declarations
- ✓ **Constructor Call Validation** - Verifies class exists for constructor calls
- ✓ **'this' Context Validation** - Ensures 'this' is only used inside methods/constructors

### Summary Statistics
- **Total Checks**: 17 validation rules
- **Critical Checks**: 5 (duplicate classes, inheritance issues)
- **Important Checks**: 8 (type validation, duplicate members)
- **Name Resolution**: 4 (variable/identifier lookups)

## TypeChecker Validation Checks

### Type Inference

- ✓ **Expression Type Inference** - Determines type for every expression in the program
- ✓ **Identifier Type Lookup** - Resolves identifier to its declared type from variable declaration
- ✓ **Literal Type Inference** - Assigns built-in types to literals (Integer, Boolean, Real)
- ✓ **Constructor Call Type** - Infers object type from constructor call
- ✓ **Method Call Return Type** - Infers type from method's declared return type
- ✓ **This Expression Type** - Resolves 'this' to current class type

### Type Compatibility Validation

- ✓ **Assignment Type Matching** - Ensures value type is compatible with target variable type
- ✓ **Built-in Type Incompatibility** - Rejects assignments between Integer, Boolean, and Real
- ✓ **Inheritance-based Compatibility** - Allows subclass-to-parent type assignments
- ✓ **Constructor Argument Types** - Validates constructor call arguments match expected types

### Method Call Validation

- ✓ **Method Existence Check** - Verifies method exists in target class or built-in type
- ✓ **Method Signature Resolution** - Matches method call to correct overloaded method
- ✓ **Argument Count Validation** - Ensures correct number of arguments
- ✓ **Argument Type Compatibility** - Checks each argument type matches parameter type
- ✓ **Built-in Method Lookup** - Validates calls to standard library methods (Plus, Greater, etc.)
- ✓ **Method Call on Built-in Types** - Ensures valid method calls on Integer, Boolean, Real

### Constructor Validation

- ✓ **Built-in Constructor Arguments** - Validates Integer(Integer), Boolean(Boolean), Real(Real)
- ✓ **Constructor Type Mismatch** - Rejects Integer(Boolean) or other incompatible constructor calls
- ✓ **User Constructor Resolution** - Finds matching constructor in user-defined classes
- ✓ **Constructor Parameter Count** - Ensures correct number of constructor arguments

### Return Statement Validation

- ✓ **Return Type Matching** - Ensures return value type matches method's declared return type
- ✓ **Void Method Returns** - Validates void methods don't return values
- ✓ **Missing Return Value** - Checks non-void methods return a value
- ✓ **Return Type Compatibility** - Allows subclass return where parent expected

### Control Flow Type Checking

- ✓ **If Condition Type** - Ensures if statement condition is Boolean type
- ✓ **While Condition Type** - Ensures while loop condition is Boolean type
- ✓ **Boolean Expression Validation** - Rejects non-Boolean types in conditional contexts

### Member Access Validation

- ✓ **Field Existence Check** - Verifies field exists in target class
- ✓ **Field Type Resolution** - Determines type of accessed field
- ✓ **Member Access on Built-ins** - Prevents field access on Integer, Boolean, Real

### Parameter and Variable Type Resolution

- ✓ **Parameter Type Resolution** - Resolves parameter type names to Type objects
- ✓ **Parameter Type Assignment** - Sets declaredType on parameter VariableDecl objects
- ✓ **Variable Initialization Type** - Infers variable type from initializer expression
- ✓ **Method Return Type Resolution** - Converts return type name to Type object

### Context Validation

- ✓ **'this' Context Check** - Ensures 'this' is only used inside methods/constructors
- ✓ **Current Class Tracking** - Maintains context of which class is being type-checked
- ✓ **Current Method Tracking** - Tracks which method body is being validated

### Error Reporting

- ✓ **Type Mismatch Errors** - Reports detailed type incompatibility messages
- ✓ **Unknown Type Errors** - Reports when type name cannot be resolved
- ✓ **Method Not Found Errors** - Detailed error for missing methods
- ✓ **Source Location Tracking** - All errors include line and column information

## Constant Folder Optimizations

### What It Does
- ✓ **Evaluates constant expressions at compile time** - Computes values before runtime
- ✓ **Folds Integer arithmetic** - Plus, Minus, Mult, Div, Rem operations
- ✓ **Folds Boolean logic** - And, Or, Xor, Not operations
- ✓ **Folds Real arithmetic** - Plus, Minus, Mult, Div operations
- ✓ **Folds comparison operations** - Less, Greater, Equal, LessEqual, GreaterEqual
- ✓ **Handles unary operations** - UnaryMinus, UnaryPlus
- ✓ **Unwraps nested constructors** - Simplifies Boolean(Boolean(false)) to Boolean(false)
- ✓ **Recursively folds nested expressions** - Bottom-up evaluation of complex expressions

### Optimization Examples

#### Integer Arithmetic
- **Before**: `Integer(5).Plus(Integer(3))`
- **After**: `Integer(8)`

#### Boolean Logic
- **Before**: `Boolean(true).And(Boolean(false))`
- **After**: `Boolean(false)`

#### Comparisons
- **Before**: `Integer(10).Greater(Integer(5))`
- **After**: `Boolean(true)`

#### Nested Expressions
- **Before**: `Integer(2).Plus(Integer(3)).Mult(Integer(4))`
- **After**: `Integer(20)` (requires multiple passes)

### Implementation Details
- **Type**: AST-modifying optimization
- **Algorithm**: Bottom-up recursive traversal with expression replacement
- **Complexity**: O(n) per pass, where n = number of expressions
- **Multiple passes**: Required for nested expressions (iterative until fixed point)
- **Safe**: Yes - mathematically sound transformations only

### Operations Supported
- **Integer**: 11 operations (5 arithmetic + 5 comparisons + 2 unary)
- **Boolean**: 4 operations (3 binary + 1 unary)
- **Real**: 10 operations (5 arithmetic + 5 comparisons + 2 unary)

### Statistics Tracked
- ✓ **Expressions folded count** - Reports number of optimized expressions
- ✓ **Change detection** - Returns boolean indicating if AST was modified
- ✓ **Optimization report** - Console output showing results per pass

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