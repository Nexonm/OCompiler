TODO:
Make MethodCall recognition in variable

class ConditionalTest is
    method test(x: Integer) : Integer is
        if x.Greater(Integer(5)) then
            return Integer(1)
        else
            return Integer(0)
        end
    end

    this() is
        var result: test(Integer(10)) <-- Variable with method call
    end
end


=== Parsing stage ===
Program
└─ ClassDecl: ConditionalTest
   ├─ MethodDecl: test(x:Integer) : Integer
   │  └─ IfStatement
   │     ├─ condition:
   │     │  └─ MethodCall: Greater(1 args)
   │     │     ├─ target:
   │     │     │  └─ IdentifierExpr: x
   │     │     └─ arguments:
   │     │        └─ ConstructorCall: Integer(1 args)
   │     │           └─ IntegerLiteral: 5
   │     ├─ then
   │     │  └─ ReturnStatement
   │     │     └─ ConstructorCall: Integer(1 args)
   │     │        └─ IntegerLiteral: 1
   │     └─ else:
   │        └─ ReturnStatement
   │           └─ ConstructorCall: Integer(1 args)
   │              └─ IntegerLiteral: 0
   └─ ConstructorDecl()
      └─ VariableDecl: result
         └─ ConstructorCall: test(1 args) <-- Should be MethodCall
            └─ ConstructorCall: Integer(1 args)
               └─ IntegerLiteral: 10