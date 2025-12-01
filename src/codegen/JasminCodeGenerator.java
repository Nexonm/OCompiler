package codegen;

import parser.ast.ASTVisitor;
import parser.ast.declarations.*;
import parser.ast.expressions.*;
import parser.ast.statements.*;
import semantic.types.*;
import semantic.stdlib.StandardLibrary;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Generates Jasmin assembly code from O language AST.
 *
 * Produces one .j file per class, which can be assembled to .class files
 * using the Jasmin assembler, then executed on the JVM.
 *
 * Architecture:
 * - Uses InstructionEmitter to build Jasmin code
 * - Uses MethodContext to track method-level state
 * - Implements visitor pattern to traverse AST
 */
public class JasminCodeGenerator implements ASTVisitor<Void> {

    private final String outputDir;
    private final boolean DEBUG;
    private InstructionEmitter emitter;
    private MethodContext currentContext;
    private String currentClassName;

    /**
     * Creates a code generator.
     *
     * @param outputDir Directory to write .j files
     */
    public JasminCodeGenerator(String outputDir) {
        this.outputDir = outputDir;
        this.DEBUG = outputDir == null || outputDir.isEmpty();
    }

    /**
     * Generate Jasmin code for entire program.
     * Produces one .j file per class.
     *
     * @param program The program AST
     */
    public void generate(Program program) {
        try {
            // Create output directory if needed
            Files.createDirectories(Paths.get(outputDir));

            // Generate code for each class
            for (ClassDecl classDecl : program.getClasses()) {
                generateClass(classDecl);
            }

            if (DEBUG) {
                return;
            }
            System.out.println("Code generation complete!");
            System.out.println("Generated " + program.getClasses().size() +
                    " .j file(s) in " + outputDir);

        } catch (IOException e) {
            throw new RuntimeException("Failed to create output directory: " + e.getMessage());
        }
    }

    /**
     * Generate Jasmin code for a single class.
     */
    private void generateClass(ClassDecl classDecl) {
        emitter = new InstructionEmitter();
        currentClassName = classDecl.getName();

        // 1. Class header
        String parentName = classDecl.getBaseClassName().isPresent() ? classDecl.getBaseClassName().get() : null;
        emitter.emitClassHeader(classDecl.getName(), parentName);

        // 2. Fields
        for (MemberDecl member : classDecl.getMembers()) {
            if (member instanceof VariableDecl) {
                VariableDecl field = (VariableDecl) member;
                String descriptor = getTypeDescriptor(field.getDeclaredType());
                emitter.emitField(field.getName(), descriptor);
            }
        }

        emitter.emitBlank();

        // 3. Constructor
        for (MemberDecl member : classDecl.getMembers()) {
            if (member instanceof ConstructorDecl) {
                generateConstructor((ConstructorDecl) member, classDecl);
            }
        }

        // 4. Methods
        for (MemberDecl member : classDecl.getMembers()) {
            if (member instanceof MethodDecl) {
                generateMethod((MethodDecl) member, classDecl);
            }
        }

        // 5. Write to file
        if (DEBUG) {
            return;
        }
        writeToFile(classDecl.getName() + ".j", emitter.getCode());
    }

    private void generateConstructor(ConstructorDecl constructor, ClassDecl classDecl) {
        // 1. Setup
        StringBuilder paramDesc = new StringBuilder();
        for (Parameter param : constructor.getParameters()) {
            paramDesc.append(getTypeDescriptor(param.getResolvedType()));
        }

        currentContext = new MethodContext(classDecl, "<init>", false);

        for (Parameter param : constructor.getParameters()) {
            boolean isWide = isWideType(param.getResolvedType());
            currentContext.addParameter(param.getName(), isWide);
        }

        // 2. Emit header
        emitter.emitConstructorHeader(paramDesc.toString());

        // 3. Use temp emitter to generate body and compute stack
        InstructionEmitter tempEmitter = new InstructionEmitter();
        tempEmitter.increaseIndent();  // ← IMPORTANT: Match indent level
        InstructionEmitter savedEmitter = emitter;
        emitter = tempEmitter;

        // 4. Generate constructor body
        String parentName = classDecl.getBaseClassName().isPresent() ?
                classDecl.getBaseClassName().get() : "java/lang/Object";

        emitter.emit("aload_0");
        currentContext.pushStack();
        emitter.emitInvoke(parentName, "<init>", "()V", "special");
        currentContext.popStack();

        // Initialize fields
        // Initialize fields
        for (MemberDecl member : classDecl.getMembers()) {
            if (member instanceof VariableDecl field) {
                if (field.getInitializer() != null) {
                    generateFieldInitializer(field);
                }
            }
        }


        // Constructor body
        if (constructor.getBody() != null) {
            for (Statement stmt : constructor.getBody()) {
                stmt.accept(this);
            }
        }

        // Return
        emitter.emitReturn('v');

        // 5. Now emit limits to real emitter (we know max stack/locals)
        emitter = savedEmitter;
        emitter.emitLimits(currentContext.getMaxStackDepth(),
                currentContext.getMaxLocals());
        emitter.emitBlank();

        // 6. Emit the body we generated
        emitter.emitRaw(tempEmitter.getCode());

        // 7. Footer
        emitter.emitMethodFooter();

        currentContext = null;
    }

    private void generateMethod(MethodDecl method, ClassDecl classDecl) {
        // Build descriptor
        StringBuilder descriptor = new StringBuilder("(");
        for (Parameter param : method.getParameters()) {
            descriptor.append(getTypeDescriptor(param.getResolvedType()));
        }
        descriptor.append(")");
        descriptor.append(getTypeDescriptor(method.getReturnType()));

        // Create context
        currentContext = new MethodContext(classDecl, method.getName(), false);

        for (Parameter param : method.getParameters()) {
            boolean isWide = isWideType(param.getResolvedType());
            currentContext.addParameter(param.getName(), isWide);
        }

        // Emit header
        emitter.emitMethodHeader(method.getName(), descriptor.toString());

        // Use temp emitter
        InstructionEmitter tempEmitter = new InstructionEmitter();
        tempEmitter.increaseIndent();  // ← IMPORTANT
        InstructionEmitter savedEmitter = emitter;
        emitter = tempEmitter;

        // Generate body
        if (method.getBody().isPresent()) {
            for (Statement stmt : method.getBody().get()) {
                stmt.accept(this);
            }
        }

        // Implicit return for void
        Type returnType = method.getReturnType();
        if (returnType instanceof VoidType) {
            emitter.emitReturn('v');
        }

        // Emit limits and body
        emitter = savedEmitter;
        emitter.emitLimits(currentContext.getMaxStackDepth(),
                currentContext.getMaxLocals());
        emitter.emitBlank();
        emitter.emitRaw(tempEmitter.getCode());

        // Footer
        emitter.emitMethodFooter();

        currentContext = null;
    }

    /**
     * Generate field initialization (special handling for built-in types).
     */
    private void generateFieldInitializer(VariableDecl field) {
        emitter.emit("aload_0");  // Load 'this'
        currentContext.pushStack();

        Expression initializer = field.getInitializer();

        // Check if this is a built-in type constructor with literal
        if (initializer instanceof ConstructorCall) {
            ConstructorCall ctorCall = (ConstructorCall) initializer;
            String className = ctorCall.getClassName();

            // Optimize primitive wrappers: Integer, Boolean, Real
            // Do NOT optimize Array[...] here, it must go through accept()
            boolean isPrimitiveWrapper = className.equals("Integer") || 
                                         className.equals("Boolean") || 
                                         className.equals("Real");

            if (isPrimitiveWrapper && ctorCall.getArguments().size() == 1) {

                Expression arg = ctorCall.getArguments().get(0);

                // Just push the literal value, don't create object
                if (arg instanceof IntegerLiteral) {
                    emitter.emitPushInt(((IntegerLiteral) arg).getValue());
                    currentContext.pushStack();
                } else if (arg instanceof BooleanLiteral) {
                    emitter.emitPushBoolean(((BooleanLiteral) arg).getValue());
                    currentContext.pushStack();
                } else if (arg instanceof RealLiteral) {
                    emitter.emitPushDouble(((RealLiteral) arg).getValue());
                    currentContext.pushStack(2);
                } else {
                    // Complex initializer, evaluate it
                    arg.accept(this);
                }
            } else {
                // User-defined class or Array or complex initialization
                initializer.accept(this);
            }
        } else {
            // Regular expression
            initializer.accept(this);
        }

        String descriptor = getTypeDescriptor(field.getDeclaredType());
        emitter.emitFieldAccess(currentClassName, field.getName(),
                descriptor, false);
        currentContext.popStack(2);
    }



    // ========== STATEMENTS ==========

    @Override
    public Void visit(VariableDeclStatement node) {
        node.getVariableDecl().accept(this);
        return null;
    }

    @Override
    public Void visit(ExpressionStatement node) {
        node.getExpression().accept(this);
        
        Type type = node.getExpression().getInferredType();
        if (type != null && !(type instanceof VoidType)) {
            // Expression statement result is unused, pop it off the stack
            int slots = isWideType(type) ? 2 : 1;
            currentContext.popStack(slots);
        }
        
        return null;
    }

    @Override
    public Void visit(VariableDecl node) {
        // Allocate slot
        boolean isWide = isWideType(node.getDeclaredType());
        int slot = currentContext.allocateLocal(node.getName(), isWide);

        // Evaluate initializer
        if (node.getInitializer() != null) {
            node.getInitializer().accept(this);

            // Store to local
            char typeChar = getTypeChar(node.getDeclaredType());
            emitter.emitStore(slot, typeChar);
            currentContext.recordStore(isWide);
        }

        return null;
    }

    @Override
    public Void visit(Assignment node) {

        // Check if this is a field assignment (including inherited fields)
        FieldInfo fieldInfo = resolveFieldInfo(node.getTargetName(), currentContext.getClassDecl());
        if (fieldInfo != null) {
            // Load 'this'
            emitter.emitLoad(0, 'a');  // Load 'this'
            currentContext.pushStack(1);
            // Evaluate value
            node.getValue().accept(this);

            // Store value to field
            Type fieldType = fieldInfo.field.getDeclaredType();
            String descriptor = getTypeDescriptor(fieldType);
            emitter.emitFieldAccess(fieldInfo.declaringClass.getName(), node.getTargetName(), descriptor, false);
            int valueSlots = isWideType(fieldType) ? 2 : 1;
            currentContext.popStack(1 + valueSlots); // value + reference
        } else {
            // Local variable assignment
            int slot = currentContext.getSlot(node.getTargetName());
            if (slot == -1) {
                throw new RuntimeException("Undefined stack variable: " + node.getTargetName());
            }
            // evaluate value
            node.getValue().accept(this);

            char typeChar = getTypeChar(node.getValue().getInferredType());
            boolean isWide = isWideType(node.getValue().getInferredType());
            emitter.emitStore(slot, typeChar);
            currentContext.recordStore(isWide);
        }

        // Evaluate value
//        node.getValue().accept(this);
        return null;
    }

    @Override
    public Void visit(IfStatement node) {
        String elseLabel = currentContext.ifElse();
        String endLabel = currentContext.ifEnd();

        // Evaluate condition
        node.getCondition().accept(this);

        // if == 0 (false), jump to else
        emitter.emitIf("eq", elseLabel);
        currentContext.popStack();

        // Then branch
        for (Statement stmt : node.getThenBranch()) {
            stmt.accept(this);
        }
        
        // Jump to end (optimization: skip if last statement is return)
        boolean thenReturns = !node.getThenBranch().isEmpty() && 
                node.getThenBranch().get(node.getThenBranch().size() - 1) instanceof ReturnStatement;
        
        if (!thenReturns) {
            emitter.emitGoto(endLabel);
        }

        // Else branch
        emitter.emitLabel(elseLabel);
        if (node.getElseBranch() != null) {
            for (Statement stmt : node.getElseBranch()) {
                stmt.accept(this);
            }
        }

        // End label
        emitter.emitLabel(endLabel);
        emitter.emit("nop"); // Ensure label targets a valid instruction
        return null;
    }

    @Override
    public Void visit(WhileLoop node) {
        String startLabel = currentContext.whileStart();
        String endLabel = currentContext.whileEnd();

        // Loop start
        emitter.emitLabel(startLabel);

        // Evaluate condition
        node.getCondition().accept(this);

        // Exit if false
        emitter.emitIf("eq", endLabel);
        currentContext.popStack();

        // Loop body
        for (Statement stmt : node.getBody()) {
            stmt.accept(this);
        }

        // Jump back to start
        emitter.emitGoto(startLabel);

        // Loop end
        emitter.emitLabel(endLabel);
        emitter.emit("nop"); // Ensure label targets a valid instruction
        return null;
    }

    @Override
    public Void visit(ReturnStatement node) {
        if (node.getValue().isPresent()) {
            // Evaluate return value
            node.getValue().get().accept(this);

            // Return with value
            Type returnType = node.getValue().get().getInferredType();
            char typeChar = getTypeChar(returnType);
            emitter.emitReturn(typeChar);
            currentContext.popStack(isWideType(returnType) ? 2 : 1);
        } else {
            // Void return
            emitter.emitReturn('v');
        }

        return null;
    }

    @Override
    public Void visit(UnknownStatement node) {
        throw new RuntimeException("Unknown statement type");
    }

    // ========== EXPRESSIONS ==========

    @Override
    public Void visit(IntegerLiteral node) {
        emitter.emitPushInt(node.getValue());
        currentContext.recordPushConstant(false);
        return null;
    }

    @Override
    public Void visit(BooleanLiteral node) {
        emitter.emitPushBoolean(node.getValue());
        currentContext.recordPushConstant(false);
        return null;
    }

    @Override
    public Void visit(RealLiteral node) {
        emitter.emitPushDouble(node.getValue());
        currentContext.recordPushConstant(true);  // double is wide
        return null;
    }

    @Override
    public Void visit(IdentifierExpr node) {
        FieldInfo fieldInfo = resolveFieldInfo(node.getName(), currentContext.getClassDecl());
        if (fieldInfo != null) {
            // Field access
            emitter.emitLoad(0, 'a');  // Load 'this'
            currentContext.pushStack();

            Type fieldType = node.getInferredType();
            boolean isWide = isWideType(fieldType);

            String descriptor = getTypeDescriptor(fieldType);
            emitter.emitFieldAccess(fieldInfo.declaringClass.getName(), node.getName(), descriptor, true);

            currentContext.popStack();
            currentContext.pushStack(isWide ? 2 : 1);
        } else {
            // Local variable access
            int slot = currentContext.getSlot(node.getName());
            if (slot == -1) {
                throw new RuntimeException("Undefined variable: " + node.getName());
            }

            Type type = node.getResolvedDecl().getDeclaredType();
            char typeChar = getTypeChar(type);
            boolean isWide = isWideType(type);

            emitter.emitLoad(slot, typeChar);
            currentContext.recordLoad(isWide);
        }
        return null;
    }


    @Override
    public Void visit(ThisExpr node) {
        emitter.emitLoad(0, 'a');
        currentContext.pushStack();
        return null;
    }

    @Override
    public Void visit(ConstructorCall node) {
        // Handle Array instantiation
        if (node.getClassName().startsWith("Array[")) {
             generateArrayInstantiation(node);
             return null;
        }

        // Handle Printer instantiation - it's a virtual object
        if (node.getClassName().equals("Printer")) {
            emitter.emit("aconst_null"); // Push a null reference
            currentContext.pushStack();
            return null;
        }

        // For built-in types in expression context, just push argument value
        if (StandardLibrary.isBuiltInType(node.getClassName()) &&
                node.getArguments().size() == 1) {

            Expression arg = node.getArguments().get(0);
            arg.accept(this);
        } else {
            // For user-defined types or complex constructors, create object
            emitter.emitNew(node.getClassName());
            currentContext.pushStack(2);  // new + dup pushes 2 references

            // Evaluate arguments
            for (Expression arg : node.getArguments()) {
                arg.accept(this);
            }

            // Build constructor descriptor
            StringBuilder descriptor = new StringBuilder("(");
            for (Expression arg : node.getArguments()) {
                descriptor.append(getTypeDescriptor(arg.getInferredType()));
            }
            descriptor.append(")V");

            // Call constructor
            emitter.emitInvoke(node.getClassName(), "<init>", descriptor.toString(), "special");

            // invokespecial pops: reference + all arguments
            int argCount = node.getArguments().size();
            for (Expression arg : node.getArguments()) {
                if (isWideType(arg.getInferredType())) {
                    argCount++;  // Wide types count as 2
                }
            }
            currentContext.popStack(1 + argCount);  // 1 for reference, rest for args
        }
        return null;
    }


    private void generateArrayInstantiation(ConstructorCall node) {
        // Expect 1 argument (size)
        Expression sizeArg = node.getArguments().get(0);
        sizeArg.accept(this); // Pushes size (int)

        Type type = node.getResolvedType();
        // Fallback if resolvedType not set (e.g. if semantic analysis failed but we proceeded)
        if (type == null) {
             // We could try to parse it again, but better to fail or assume object
             throw new RuntimeException("Array type not resolved for " + node.getClassName());
        }
        
        if (type instanceof ArrayType) {
             Type elemType = ((ArrayType) type).getElementType();
             String name = elemType.getName();
             
             if (name.equals("Integer")) {
                 emitter.emit("newarray int");
             } else if (name.equals("Boolean")) {
                 emitter.emit("newarray int"); // Boolean is int
             } else if (name.equals("Real")) {
                 emitter.emit("newarray double");
             } else {
                 // Object array
                 // anewarray takes class name
                 emitter.emit("anewarray " + name);
             }
        }
        
        currentContext.popStack(); // pop size
        currentContext.pushStack(); // push array ref
    }

    @Override
    public Void visit(MethodCall node) {
        Type targetType = node.getTarget().getInferredType();

        if (targetType instanceof ArrayType) {
            generateArrayMethodCall(node, (ArrayType) targetType);
            return null;
        }

        // Check if built-in method
        if (StandardLibrary.isBuiltInType(targetType.getName())) {
            generateBuiltInMethodCall(node);
        } else {
            generateUserMethodCall(node);
        }

        return null;
    }

    private void generateArrayMethodCall(MethodCall node, ArrayType arrayType) {
        String methodName = node.getMethodName();
        
        // Evaluate target (array ref)
        node.getTarget().accept(this);
        
        // Evaluate arguments
        for (Expression arg : node.getArguments()) {
            arg.accept(this);
        }
        
        if (methodName.equals("get")) {
            // Stack: array_ref, index
            // Op: xaload
            Type elemType = arrayType.getElementType();
            String name = elemType.getName();
            
            if (name.equals("Integer") || name.equals("Boolean")) {
                emitter.emit("iaload");
            } else if (name.equals("Real")) {
                emitter.emit("daload");
            } else {
                emitter.emit("aaload");
            }
            
            // Stack update
            currentContext.popStack(2); // array + index
            currentContext.pushStack(isWideType(elemType) ? 2 : 1);
            
        } else if (methodName.equals("set")) {
             // Stack: array_ref, index, value
             // Op: xastore
            Type elemType = arrayType.getElementType();
            String name = elemType.getName();
            
            if (name.equals("Integer") || name.equals("Boolean")) {
                emitter.emit("iastore");
            } else if (name.equals("Real")) {
                emitter.emit("dastore");
            } else {
                emitter.emit("aastore");
            }
            
            // Stack update
            int valueSlots = isWideType(elemType) ? 2 : 1;
            currentContext.popStack(2 + valueSlots); // array + index + value
            
        } else if (methodName.equals("Length")) {
            // Stack: array_ref
            emitter.emit("arraylength");
            currentContext.popStack();
            currentContext.pushStack(); // int
        }
    }

    /**
     * Generate code for built-in method call (Integer.Plus, etc.)
     */
    private void generateBuiltInMethodCall(MethodCall node) {
        String methodName = node.getMethodName();
        Type targetType = node.getTarget().getInferredType();
        String typeName = targetType.getName();

        if (typeName.equals("Printer")) {
            generatePrinterMethodCall(node);
            return;
        }

        // Evaluate target (pushes the value)
        node.getTarget().accept(this);

        // Evaluate arguments
        for (Expression arg : node.getArguments()) {
            arg.accept(this);
        }

        // Generate appropriate bytecode
        if (typeName.equals("Integer")) {
            generateIntegerOperation(methodName, node.getArguments().size());
        } else if (typeName.equals("Boolean")) {
            generateBooleanOperation(methodName, node.getArguments().size());
        } else if (typeName.equals("Real")) {
            generateRealOperation(methodName, node.getArguments().size());
        }
    }

    /**
     * Generate Integer built-in operations.
     */
    private void generateIntegerOperation(String methodName, int argCount) {
        switch (methodName) {
            case "Plus":
                emitter.emitAdd('i');
                currentContext.recordBinaryOp();
                break;
            case "Minus":
                emitter.emitSub('i');
                currentContext.recordBinaryOp();
                break;
            case "Mult":
                emitter.emitMul('i');
                currentContext.recordBinaryOp();
                break;
            case "Div":
                emitter.emitDiv('i');
                currentContext.recordBinaryOp();
                break;
            case "Rem":
                emitter.emitRem('i');
                currentContext.recordBinaryOp();
                break;
            case "UnaryMinus":
                emitter.emitNeg('i');
                currentContext.recordUnaryOp();
                break;
            case "UnaryPlus":
                // No operation needed
                break;
            case "Greater":
                generateComparison("gt");
                break;
            case "GreaterEqual":
                generateComparison("ge");
                break;
            case "Less":
                generateComparison("lt");
                break;
            case "LessEqual":
                generateComparison("le");
                break;
            case "Equal":
                generateComparison("eq");
                break;
            case "toReal":
                emitter.emit("i2d");
                currentContext.popStack();
                currentContext.pushStack(2);  // double is wide
                break;
            default:
                throw new RuntimeException("Unknown Integer method: " + methodName);
        }
    }

    private void generatePrinterMethodCall(MethodCall node) {
        // Get System.out on the stack
        emitter.emit("getstatic java/lang/System/out Ljava/io/PrintStream;");
        currentContext.pushStack();

        // Evaluate the argument and push it on the stack
        Expression arg = node.getArguments().get(0);
        arg.accept(this);

        // Determine println signature
        Type argType = arg.getInferredType();
        String descriptor;
        char typeChar = getTypeChar(argType);
        switch (typeChar) {
            case 'i' -> descriptor = "(I)V";
            case 'd' -> descriptor = "(D)V";
            default ->
                // Fallback for objects
                    descriptor = "(Ljava/lang/Object;)V";
        }

        // Invoke println
        emitter.emitInvoke("java/io/PrintStream", "println", descriptor, "virtual");
        int argSlots = isWideType(argType) ? 2 : 1;
        currentContext.popStack(1 + argSlots); // System.out + argument
    }

    /**
     * Generate Boolean built-in operations.
     */
    private void generateBooleanOperation(String methodName, int argCount) {
        switch (methodName) {
            case "And":
                emitter.emit("iand");
                currentContext.recordBinaryOp();
                break;
            case "Or":
                emitter.emit("ior");
                currentContext.recordBinaryOp();
                break;
            case "Xor":
                emitter.emit("ixor");
                currentContext.recordBinaryOp();
                break;
            case "Not":
                // XOR with 1 flips the boolean
                emitter.emitPushInt(1);
                currentContext.pushStack();
                emitter.emit("ixor");
                currentContext.recordBinaryOp();
                break;
            default:
                throw new RuntimeException("Unknown Boolean method: " + methodName);
        }
    }

    /**
     * Generate Real built-in operations.
     */
    private void generateRealOperation(String methodName, int argCount) {
        switch (methodName) {
            case "Plus":
                emitter.emitAdd('d');
                currentContext.popStack(4);  // Pops 2 doubles (4 slots)
                currentContext.pushStack(2);  // Pushes 1 double (2 slots)
                break;
            case "Minus":
                emitter.emitSub('d');
                currentContext.popStack(4);
                currentContext.pushStack(2);
                break;
            case "Mult":
                emitter.emitMul('d');
                currentContext.popStack(4);
                currentContext.pushStack(2);
                break;
            case "Div":
                emitter.emitDiv('d');
                currentContext.popStack(4);
                currentContext.pushStack(2);
                break;
            case "UnaryMinus":
                emitter.emitNeg('d');
                currentContext.recordUnaryOp();
                break;
            case "UnaryPlus":
                // No operation needed
                break;
            case "Greater":
                generateDoubleComparison("gt");
                break;
            case "GreaterEqual":
                generateDoubleComparison("ge");
                break;
            case "Less":
                generateDoubleComparison("lt");
                break;
            case "LessEqual":
                generateDoubleComparison("le");
                break;
            case "Equal":
                generateDoubleComparison("eq");
                break;
            case "toInteger":
                emitter.emit("d2i");
                currentContext.popStack(2);  // Pop double
                currentContext.pushStack();   // Push int
                break;
            default:
                throw new RuntimeException("Unknown Real method: " + methodName);
        }
    }

    /**
     * Generate integer comparison (produces boolean result).
     */
    private void generateComparison(String comparison) {
        String trueLabel = currentContext.nextLabel("CmpTrue");
        String endLabel = currentContext.nextLabel("CmpEnd");

        // if_icmpXX true_label
        emitter.emitIfICmp(comparison, trueLabel);
        currentContext.recordComparison();

        // Push false (0)
        emitter.emitPushBoolean(false);
        currentContext.pushStack();
        emitter.emitGoto(endLabel);

        // Push true (1)
        emitter.emitLabel(trueLabel);
        emitter.emitPushBoolean(true);
        currentContext.pushStack();

        emitter.emitLabel(endLabel);
    }

    /**
     * Generate double comparison.
     */
    private void generateDoubleComparison(String comparison) {
        // dcmpg/dcmpl: compares two doubles, pushes -1, 0, or 1
        emitter.emit("dcmpg");
        currentContext.popStack(4);  // Pop 2 doubles
        currentContext.pushStack();   // Push int result

        String trueLabel = currentContext.nextLabel("CmpTrue");
        String endLabel = currentContext.nextLabel("CmpEnd");

        // Compare result with 0
        switch (comparison) {
            case "gt":
                emitter.emitIf("gt", trueLabel);
                break;
            case "ge":
                emitter.emitIf("ge", trueLabel);
                break;
            case "lt":
                emitter.emitIf("lt", trueLabel);
                break;
            case "le":
                emitter.emitIf("le", trueLabel);
                break;
            case "eq":
                emitter.emitIf("eq", trueLabel);
                break;
        }
        currentContext.popStack();

        // Push false
        emitter.emitPushBoolean(false);
        currentContext.pushStack();
        emitter.emitGoto(endLabel);

        // Push true
        emitter.emitLabel(trueLabel);
        emitter.emitPushBoolean(true);
        currentContext.pushStack();

        emitter.emitLabel(endLabel);
    }

    /**
     * Generate code for user-defined method call.
     */
    private void generateUserMethodCall(MethodCall node) {
        // Evaluate target (pushes object reference)
        node.getTarget().accept(this);

        // Evaluate arguments
        for (Expression arg : node.getArguments()) {
            arg.accept(this);
        }

        // Build method descriptor
        MethodDecl method = node.getResolvedMethod();
        if (method == null) {
            throw new RuntimeException("Unresolved method: " + node.getMethodName());
        }

        StringBuilder descriptor = new StringBuilder("(");
        for (Parameter param : method.getParameters()) {
            descriptor.append(getTypeDescriptor(param.getResolvedType()));
        }
        descriptor.append(")");
        descriptor.append(getTypeDescriptor(method.getReturnType()));

        // Get class name
        Type targetType = node.getTarget().getInferredType();
        String className = targetType.getName();

        // Invoke method
        emitter.emitInvoke(className, node.getMethodName(),
                descriptor.toString(), "virtual");

        // Update stack
        int poppedSlots = 1;  // target reference
        for (Expression arg : node.getArguments()) {
            poppedSlots += isWideType(arg.getInferredType()) ? 2 : 1;
        }
        currentContext.popStack(poppedSlots);

        // Push return value if any
        Type returnType = method.getReturnType();
        if (!(returnType instanceof VoidType)) {
            currentContext.pushStack(isWideType(returnType) ? 2 : 1);
        }
    }

    @Override
    public Void visit(MemberAccess node) {
        // Load 'this' or target object
        node.getTarget().accept(this);

        // Get field
        Type targetType = node.getTarget().getInferredType();
        String fieldName = node.getMemberName();

        ClassDecl targetClassDecl = null;
        if (targetType instanceof ClassType) {
            targetClassDecl = ((ClassType) targetType).getDeclaration();
        }

        FieldInfo fieldInfo = resolveFieldInfo(fieldName, targetClassDecl);
        if (fieldInfo == null) {
            throw new RuntimeException("Undefined field: " + fieldName + " in class " +
                    (targetClassDecl != null ? targetClassDecl.getName() : "<unknown>"));
        }

        Type fieldType = fieldInfo.field.getDeclaredType();
        String descriptor = getTypeDescriptor(fieldType);

        emitter.emitFieldAccess(fieldInfo.declaringClass.getName(), fieldName, descriptor, true);

        // Update stack: pop reference, push field value
        currentContext.popStack();
        currentContext.pushStack(isWideType(fieldType) ? 2 : 1);

        return null;
    }

    @Override
    public Void visit(UnknownExpression node) {
        throw new RuntimeException("Unknown expression type");
    }

    // ========== TYPE HELPERS ==========

    /**
     * Check if the given variable name is a field of the current class.
     */
    private FieldInfo resolveFieldInfo(String name, ClassDecl classDecl) {
        ClassDecl current = classDecl;
        while (current != null) {
            for (MemberDecl member : current.getMembers()) {
                if (member instanceof VariableDecl field && field.getName().equals(name)) {
                    return new FieldInfo(field, current);
                }
            }
            current = current.getParentClass();
        }
        return null;
    }

    private static class FieldInfo {
        final VariableDecl field;
        final ClassDecl declaringClass;

        FieldInfo(VariableDecl field, ClassDecl declaringClass) {
            this.field = field;
            this.declaringClass = declaringClass;
        }
    }


    /**
     * Get JVM type descriptor for a type.
     */
    private String getTypeDescriptor(Type type) {
        if (type == null) {
            return "V";  // void
        }
        
        if (type instanceof ArrayType) {
            return "[" + getTypeDescriptor(((ArrayType) type).getElementType());
        }

        if (type instanceof VoidType) {
            return "V";
        }

        String name = type.getName();
        switch (name) {
            case "Integer":
                return "I";
            case "Boolean":
                return "I";  // Booleans are ints in JVM
            case "Real":
                return "D";
            default:
                // User-defined class
                return "L" + name + ";";
        }
    }

    /**
     * Get type character for load/store instructions.
     */
    private char getTypeChar(Type type) {
        if (type == null) {
            return 'v';
        }

        String name = type.getName();
        switch (name) {
            case "Integer":
            case "Boolean":
                return 'i';
            case "Real":
                return 'd';
            default:
                return 'a';  // reference
        }
    }

    /**
     * Check if type is wide (takes 2 stack slots).
     */
    private boolean isWideType(Type type) {
        if (type == null) {
            return false;
        }
        return type.getName().equals("Real");
    }

    // ========== FILE I/O ==========

    /**
     * Write generated code to file.
     */
    private void writeToFile(String filename, String content) {
        try {
            Path filepath = Paths.get(outputDir, filename);
            try (FileWriter writer = new FileWriter(filepath.toFile())) {
                writer.write(content);
            }
            System.out.println("Generated: " + filepath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write file: " + filename, e);
        }
    }

    // ========== UNUSED VISITOR METHODS ==========

    @Override
    public Void visit(Program node) {
        // Not used - generate() is the entry point
        return null;
    }

    @Override
    public Void visit(ClassDecl node) {
        // Not used - generateClass() is called directly
        return null;
    }

    @Override
    public Void visit(MethodDecl node) {
        // Not used - generateMethod() is called directly
        return null;
    }

    @Override
    public Void visit(ConstructorDecl node) {
        // Not used - generateConstructor() is called directly
        return null;
    }
}

