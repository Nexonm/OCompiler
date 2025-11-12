## 1. Language Overview

**O** is a pure object-oriented programming language where everything is based on classes. It features single inheritance, polymorphism, and a simplified syntax where all operations are method calls rather than traditional infix operators.

### Key Characteristics

- **Pure Object-Oriented**: Classes are the only way to define types
- **Single Inheritance**: Classes can extend one base class
- **Polymorphism**: Method overriding based on dynamic type
- **Method-Based Operations**: No infix operators; all operations are method calls
- **Type Inference**: Variable types inferred from initialization expressions
- **Forward Declarations**: Methods can be declared before implementation


## 2. Program Structure

### Entry Point

An O program consists of a sequence of class declarations. The entry point is determined at runtime by specifying which class constructor to execute with optional arguments.

```
Program : { ClassDeclaration }
```

**Execution Model**: The specified class constructor is invoked with command-line arguments, creating an unnamed object. Program execution begins with the constructor body and terminates when it completes.

## 3. Syntax and Grammar

### 3.1 Class Declaration

```
ClassDeclaration : class ClassName [ extends ClassName ] 
                   is { MemberDeclaration } end

ClassName : Identifier
```

**Example**:

```o
class Animal is
    var name : String("Default")
    
    method makeSound() : Integer is
        return Integer(0)
    end
    
    this() is
    end
end

class Dog extends Animal is
    method makeSound() : Integer is
        return Integer(1)
    end
    
    this() is
    end
end
```


### 3.2 Class Members

```
MemberDeclaration : VariableDeclaration
                  | MethodDeclaration  
                  | ConstructorDeclaration
```


#### Variable Declaration

```
VariableDeclaration : var Identifier : Expression
```

**Semantics**:

- Variables are **read-only** from outside the class ~!~ (Да, согласно описанию)
- Type is inferred from the initialization expression
- Must be initialized at declaration ~!~ (Да, согласно описанию)
- Access to member variables is allowed only from class methods (or from methods of derived classes)  ~!~ (Да, согласно описанию)

**Example**:

```o
var counter : Integer(0)
var isReady : Boolean(true)
var items : Array[Integer](10)
```


#### Method Declaration

```
MethodDeclaration : MethodHeader [ MethodBody ]

MethodHeader : method Identifier [ Parameters ] [ : Identifier ]

MethodBody : is Body end
           | => Expression

Parameters : ( ParameterDeclaration { , ParameterDeclaration } )
ParameterDeclaration : Identifier : ClassName
```

**Features**:

- **Forward Declarations**: Method header without body ~!~ (Да, согласно описанию)
- **Method Overloading**: Multiple methods with same name but different parameter signatures
- **Short Form**: Use `=> Expression` for single-expression methods
- **Return Type**: Optional return type specification

**Examples**:

```o
// Forward declaration
method process(x : Integer) : Integer

// Full method
method process(x : Integer) : Integer is
    return x.Plus(Integer(1))
end

// Short form method
method getValue() : Integer => storedValue

// Method overloading
method process(x : Real) : Real is
    return x.Plus(Real(1.0))
end
```


#### Constructor Declaration

```
ConstructorDeclaration : this [ Parameters ] is Body end
```

**Semantics**:

- Special method for object creation
- No return type
- Can have multiple constructors with different parameters
- Called during object instantiation

**Example**:

```o
this() is
    counter := Integer(0)
end

this(initialValue : Integer) is
    counter := initialValue
end
```


### 3.3 Statements

```
Statement : Assignment
          | WhileLoop
          | IfStatement
          | ReturnStatement

Body : { VariableDeclaration | Statement }
```


#### Assignment

```
Assignment : Identifier := Expression
```


#### Control Flow

```
WhileLoop : while Expression loop Body end

IfStatement : if Expression then Body [ else Body ] end

ReturnStatement : return [ Expression ]
```

**Examples**:

```o
// Assignment
result := a.Plus(b)

// While loop
while i.LessEqual(n) loop
    result := result.Mult(i)
    i := i.Plus(Integer(1))
end

// If-else
if x.Greater(Integer(5)) then
    return Integer(1)
else
    return Integer(0)
end

// Return
return result
```


### 3.4 Expressions

```
Expression : Primary
           | ConstructorInvocation
           | FunctionCall
           | Expression { . Expression }

ConstructorInvocation : ClassName [ Arguments ]
FunctionCall : Expression [ Arguments ]
Arguments : ( ) | ( Expression { , Expression } )

Primary : IntegerLiteral | RealLiteral | BooleanLiteral | this
```

**Expression Types**:

- **Member Access**: `object.member`
- **Method Calls**: `object.method(args)`
- **Constructor Calls**: `ClassName(args)`
- **Chained Calls**: `object.method1().method2()`

**Examples**:

```o
// Constructor invocation
var num : Integer(42)
var arr : Array[Integer](10)

// Method calls
var sum : a.Plus(b)
var isGreater : x.Greater(y)

// Member access and chaining
var value : obj.getValue().Plus(Integer(1))
```


## 4. Type System and Inheritance

### 4.1 Type Hierarchy

The O language defines a complete type hierarchy with library classes:
~!~ String
```
Class
├── AnyValue
│   ├── Integer
│   ├── Real
│   └── Boolean
└── AnyRef
    ├── Array[T]
    └── List[T]
```


### 4.2 Inheritance Rules

- **Single Inheritance**: A class can extend at most one base class
- **Transitive**: If A extends B and B extends C, then A inherits from C
- **Polymorphism**: Derived class methods override base class methods with matching signatures
- **Dynamic Dispatch**: Method resolution based on runtime type


### 4.3 Access Control

- **Class Members**: Read-only from outside the class ~!~ (Да, согласно описанию)
- **Method Access**: Class methods can modify member variables
- **Inheritance Access**: Derived classes can access base class members


## 5. Standard Library Classes

~!~ откуда нахер взялись AnyValue and AnyRef? (They are automatically initialized when their concrete subclasses are created)

### 5.1 Integer Class

```o
class Integer extends AnyValue is
    // Constructors
    this(p: Integer)
    this(p: Real)
    
    // Constants
    var Min : Integer
    var Max : Integer
    
    // Conversions
    method toReal : Real
    method toBoolean : Boolean
    
    // Arithmetic Operations
    method UnaryMinus : Integer
    method Plus(p: Integer) : Integer
    method Plus(p: Real) : Real
    method Minus(p: Integer) : Integer
    method Minus(p: Real) : Real
    method Mult(p: Integer) : Integer
    method Mult(p: Real) : Real
    method Div(p: Integer) : Integer
    method Div(p: Real) : Real
    method Rem(p: Integer) : Integer
    
    // Comparison Operations
    method Less(p: Integer) : Boolean
    method Less(p: Real) : Boolean
    method LessEqual(p: Integer) : Boolean
    method LessEqual(p: Real) : Boolean
    method Greater(p: Integer) : Boolean
    method Greater(p: Real) : Boolean
    method GreaterEqual(p: Integer) : Boolean
    method GreaterEqual(p: Real) : Boolean
    method Equal(p: Integer) : Boolean
    method Equal(p: Real) : Boolean
end
```


### 5.2 Real Class

```o
class Real extends AnyValue is
    // Constructors
    this(p: Real)
    this(p: Integer)
    
    // Constants
    var Min : Real
    var Max : Real
    var Epsilon : Real
    
    // Conversions
    method toInteger : Integer
    
    // Arithmetic and Comparison Operations
    // (Similar to Integer with Real/Integer parameter variations)
end
```


### 5.3 Boolean Class

```o
class Boolean extends AnyValue is
    // Constructor
    this(Boolean)
    
    // Conversion
    method toInteger() : Integer
    
    // Logical Operations
    method Or(p: Boolean) : Boolean
    method And(p: Boolean) : Boolean
    method Xor(p: Boolean) : Boolean
    method Not : Boolean
end
```


### 5.4 Generic Classes

#### Array Class

```o
class Array[T] extends AnyRef is
    // Constructor
    this(l: Integer)
    
    // Conversion
    method toList : List
    
    // Properties
    method Length : Integer
    
    // Element Access
    method get(i: Integer) : T
    method set(i: Integer, v: T)
end
```


#### List Class

```o
class List[T] extends AnyRef is
    // Constructors
    this()
    this(p: T)
    this(p: T, count: Integer)
    
    // List Operations
    method append(v: T) : List
    method head() : T
    method tail() : List
end
```


## 6. Implementation Guidelines

### 6.1 Target Platform

- **Implementation Language**: Java
- **Parser**: Hand-written (no parser generators)
- **Target**: Jasmin assembler for JVM bytecode generation


### 6.2 Compilation Process

1. **Lexical Analysis**: Tokenize source code
2. **Syntax Analysis**: Build Abstract Syntax Tree (AST)
3. **Semantic Analysis**: Type checking and symbol resolution
4. **Code Generation**: Generate Jasmin assembly code

### 6.3 Special Features

#### Print Functionality

The implementation includes special print capabilities through library classes:

```o
class Printer is
    method print(num : Integer) is
        // Implementation-specific printing
    end
    
    method print(text : String) is
        // Implementation-specific printing
    end
    ~!~ Real, Boolean
    
    this() is
    end
end
```


### 6.4 Error Handling

The compiler should detect and report:

- **Type Mismatches**: Wrong return types, incompatible assignments
- **Undeclared Identifiers**: References to undefined variables/methods
- **Invalid Method Calls**: Wrong parameter count or types
- **Inheritance Errors**: Invalid class hierarchies


## 7. Example Programs

### 7.1 Basic Class with Inheritance

```o
class Base is
    var x : Integer(10)
    
    method getValue() : Integer is
        return x
    end
    
    this() is
    end
end

class Derived extends Base is
    var y : Integer(20)
    
    this() is
    end
end
```


### 7.2 Polymorphism Example

```o
class Animal is
    method makeSound() : Integer is
        return Integer(0)
    end
    
    this() is
    end
end

class Dog extends Animal is
    method makeSound() : Integer is
        return Integer(1)
    end
    
    this() is
    end
end

class Test is
    this is
        var animal : Animal()
        var dog : Dog()
        var sound1 : animal.makeSound()  // Returns 0
        var sound2 : dog.makeSound()     // Returns 1
    end
end
```


### 7.3 Method Overloading

```o
class Calculator is
    method add(a : Integer, b : Integer) : Integer is
        return a.Plus(b)
    end
    
    method add(a : Real, b : Real) : Real is
        return a.Plus(b)
    end
    
    this() is
    end
end
```


### 7.4 Loop and Conditional Logic

```o
class MathUtils is
    method factorial(n : Integer) : Integer is
        var result : Integer(1)
        var i : Integer(1)
        
        while i.LessEqual(n) loop
            result := result.Mult(i)
            i := i.Plus(Integer(1))
        end
        
        return result
    end
    
    method max(a : Integer, b : Integer) : Integer is
        if a.Greater(b) then
            return a
        else
            return b
        end
    end
    
    this() is
    end
end
```


## 8. Compilation Notes

### 8.1 Type Inference

- Variable types are determined from initialization expressions
- Method return types must be explicitly declared when returning values

~!~ List of keywords

#### Class and Inheritance Keywords

- **`class`** - Declares a class

- **`extends`** - Specifies inheritance relationship

- **`is`** - Begins class body, method body, or constructor body

- **`end`** - Ends class declaration, method body, conditional statement, or loop

#### Variable and Method Declaration Keywords

- **`var`** - Declares a variable

- **`method`** - Declares a method

- **`this`** - Constructor declaration or self-reference in method bodies

#### Control Flow Keywords

- **`if`** - Conditional statement

- **`then`** - Part of if statement (after condition)

- **`else`** - Alternative branch in conditional statement

- **`while`** - Loop statement

- **`loop`** - Part of while statement (begins loop body)

- **`return`** - Returns from method

#### Literal Keywords

- **`true`** - Boolean literal

- **`false`** - Boolean literal

#### Special Operators

- **`:=`** - Assignment operator

- **`=>`** - Short method body delimiter (for single-expression methods)