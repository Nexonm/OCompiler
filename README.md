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


