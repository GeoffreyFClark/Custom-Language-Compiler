# Custom Language Compiler

A complete compiler and interpreter for a custom statically-typed programming language, featuring lexical analysis, recursive descent parsing, semantic analysis, interpretation, and code generation to Java.

## Compiler Pipeline

```
Source Code → Lexer → Parser → Analyzer → Interpreter (execute)
                                       └→ Generator (→ Java)
```

## Language Features

- **Types:** Integer, Decimal, Boolean, Character, String, Nil
- **Variables:** `LET name: Integer = 10;` / `LET CONST PI = 3.14;`
- **Functions:** `DEF add(a: Integer, b: Integer): Integer DO ... END`
- **Control Flow:** `IF/ELSE`, `WHILE`, `FOR` loops
- **Operators:** Arithmetic (`+`, `-`, `*`, `/`), Comparison (`<`, `>`, `==`, `!=`), Logical (`&&`, `||`)

## Example

**Source:**
```
DEF main(): Integer DO
    print("Hello, World!");
    RETURN 0;
END
```

**Generated Java:**
```java
public class Main {
    public static void main(String[] args) {
        System.exit(new Main().main());
    }

    int main() {
        System.out.println("Hello, World!");
        return 0;
    }
}
```

## Project Structure

```
src/main/java/plc/project/
├── Lexer.java        # Tokenization
├── Parser.java       # Recursive descent parser → AST
├── Ast.java          # Abstract Syntax Tree definitions
├── Analyzer.java     # Semantic analysis & type checking
├── Interpreter.java  # Tree-walking interpreter
├── Generator.java    # Java code generator
├── Environment.java  # Type system & runtime
└── Scope.java        # Variable/function scope management
```

## Build & Test

```bash
./gradlew build    # Build project
./gradlew test     # Run test suite
```

**Requirements:** Java 11+, Gradle

## Key Concepts Demonstrated

- Lexical analysis & tokenization
- LL(k) recursive descent parsing
- Abstract Syntax Tree design
- Static type checking
- Visitor design pattern
- Code generation / transpilation
