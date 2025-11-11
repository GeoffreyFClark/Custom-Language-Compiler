package plc.project;

import java.io.PrintWriter;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {
        print("public class Main {");
        newline(0);

        int old = indent;
        indent = 1;

        if (!ast.getFields().isEmpty()) {
            for (Ast.Field f : ast.getFields()) {
                newline(indent);
                visit(f);
            }
            newline(0);
        }

        newline(indent);
        print("public static void main(String[] args) {");
        newline(indent + 1);
        print("System.exit(new Main().main());");
        newline(indent);
        print("}");
        newline(0);

        for (int i = 0; i < ast.getMethods().size(); i++) {
            newline(indent);
            visit(ast.getMethods().get(i));
            newline(0);
        }

        indent = old;
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Method ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.For ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        throw new UnsupportedOperationException(); //TODO
    }
}
