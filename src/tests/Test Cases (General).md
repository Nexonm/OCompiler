# Basic Test Cases for O Language

## Test 1: Basic Class Declaration
```o
class SimpleClass is
    var value: Integer(42)

    this() is
    end
end
```

## Test 2: Basic Inheritance
```o
class Base is
    var x: Integer(10)

    method getValue() : Integer is
        return x
    end

    this() is
    end
end

class Derived extends Base is
    var y: Integer(20)

    this() is
    end
end

class Test is
    this is
        var d: Derived()
        var p: Printer()
        p.print(d.y)
    end
end
```

## Test 3: Basic Polymorphism
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
        var d: Animal()
        var p: Printer()
        p.print(d.makeSound())
    end
end
```

## Test 4: If-Else
```o
class ConditionalTest is
    method test(x: Integer) : Integer is
        if x.Greater(Integer(5)) then
            return Integer(1)
        else
            return Integer(0)
        end
    end

    this() is
        var result: test(Integer(10))
    end
end
```

## Test 5: Loop
```o
class LoopTest is
    method factorial(n: Integer) : Integer is
        var result: Integer(1)
        var i: Integer(1)
        while i.LessEqual(n) loop
            result := result.Mult(i)
            i := i.Plus(Integer(1))
        end
        return result
    end

    this() is
    end
end
```

## Test 6: Assignment
```o
class ExpressionTest is
    method calculate() : Integer is
        var a: Integer(10)
        var b: Integer(5)
        var sum: a.Plus(b)
        var diff: a.Minus(b)
        return sum.Mult(diff)
    end

    this() is
    end
end
```

## Test 7: Integer Class Test
```o
class IntegerTest is
    this() is
        var a: Integer(15)
        var b: Integer(7)

        var sum: a.Plus(b)
        var product: a.Mult(b)
        var quotient: a.Div(b)
        var remainder: a.Rem(b)
        var negation: a.UnaryMinus()
    end
end
```

## Test 8: Boolean Class
```o
class BooleanTest is
    this() is
        var t: Boolean(true)
        var f: Boolean(false)

        var andResult: t.And(f)
        var orResult: t.Or(f)
        var xorResult: t.Xor(f)
        var notResult: t.Not()
    end
end
```

## Test 9: Real Class
```o
class RealTest is
    this() is
        var x: Real(3.14)
        var y: Real(2.71)
        var t: Real(3.15)

        var sum: x.Plus(y)
        var comparison: x.Greater(y)
        var comparison2: x.Equal(t)
    end
end
```

## Test 10: Method Overloading
```o
class OverloadTest is
    method process(x: Integer) : Integer is
        return x.Plus(Integer(1))
    end

    method process(x: Real) : Real is
        return x.Plus(Real(1.0))
    end

    this() is
        var intResult: process(Integer(5))
        var realResult: process(Real(5.5))
    end
end
```

## Test 11: Declaration sequence does not affect
```o
class ForwardTest is
    method second() is
        first()
    end

    method first() is
        return
    end

    this() is
    end
end
```

## Error Test 12: Wrong Return Type
```o
class TypeErrorTest is
    method getNumber() : Integer is
        return Boolean(true) // return Boolean instead of Integer
    end

    this() is
    end
end
```

## Error Test 13: Basic Program
```o
class Calculator is
    var lastResult: Integer(0)

    method add(a: Integer, b: Integer) : Integer is
        var result: a.Plus(b)
        lastResult := result
        return result
    end

    method getLastResult() : Integer is
        return lastResult
    end

    this(initialValue: Integer) is
        lastResult := initialValue
        var sum: add(Integer(5), Integer(3))
        var final: getLastResult()
    end
end
```

## Error Test 14: Basic Print
```o
class ConsolePrinter is
    method hello is
        print("Hellow World!")
    end

    this() is
    end
end

class Test is
    this is
        var p: ConsolePrinter()
        p.hello()
    end
end
```

## Error Test 15: Integer Print
```o
class Printer is
    method print(num: Integer) is
        do_magic_to_print(num)
    end

    this() is
    end
end

class Test is
    this is
        var p: Printer()
        p.print(Integer(100))
    end
end
```
