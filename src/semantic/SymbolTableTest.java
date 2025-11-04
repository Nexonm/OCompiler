package semantic;

import semantic.symbols.*;
import semantic.exception.*;

/**
 * Simple test to verify symbol table functionality.
 */
public class SymbolTableTest {
    public static void main(String[] args) {
        SymbolTable symbolTable = new SymbolTable();

        System.out.println("=== Symbol Table Test ===\n");

        // Simulate: class Dog is ...
        System.out.println("1. Entering class Dog");
        symbolTable.pushScope("class:Dog");

        ClassSymbol dogClass = new ClassSymbol("Dog", null, null);
        symbolTable.define(dogClass);
        System.out.println("   Defined: " + dogClass);

        // Simulate: var name : String
        System.out.println("\n2. Adding field 'name'");
        VariableSymbol nameField = new VariableSymbol("name", "String", false, null);
        symbolTable.define(nameField);
        dogClass.addField(nameField);
        System.out.println("   Defined: " + nameField);

        // Simulate: method bark(volume : Integer) is ...
        System.out.println("\n3. Entering method bark");
        symbolTable.pushScope("method:bark");

        MethodSymbol barkMethod = new MethodSymbol(
                "bark", null, java.util.List.of("Integer"), false, null
        );
        System.out.println("   Defined: " + barkMethod);

        // Simulate: parameter volume
        System.out.println("\n4. Adding parameter 'volume'");
        VariableSymbol volumeParam = new VariableSymbol("volume", "Integer", true, null);
        symbolTable.define(volumeParam);
        System.out.println("   Defined: " + volumeParam);

        // Test lookups
        System.out.println("\n5. Testing lookups from inside bark():");
        System.out.println("   Looking up 'volume': " + symbolTable.lookup("volume"));
        System.out.println("   Looking up 'name': " + symbolTable.lookup("name"));
        System.out.println("   Looking up 'Dog': " + symbolTable.lookup("Dog"));
        System.out.println("   Looking up 'unknown': " + symbolTable.lookup("unknown"));

        // Exit scopes
        System.out.println("\n6. Exiting method bark");
        symbolTable.popScope();

        System.out.println("\n7. Exiting class Dog");
        symbolTable.popScope();

        System.out.println("\n8. Back in global scope");
        System.out.println("   Looking up 'Dog': " + symbolTable.lookup("Dog"));
        System.out.println("   Looking up 'name': " + symbolTable.lookup("name"));

        System.out.println("\n=== Test Complete ===");
    }
}

