package plc.project;

import java.io.PrintWriter;
import java.util.List;
import java.math.BigInteger;
import java.math.BigDecimal;

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

    private void printForHeaderPart(Ast.Statement s) {
        if (s instanceof Ast.Statement.Assignment) {
            Ast.Statement.Assignment a = (Ast.Statement.Assignment) s;
            visit(a.getReceiver());
            print(" = ");
            visit(a.getValue());
        } else if (s instanceof Ast.Statement.Declaration) {
            Ast.Statement.Declaration d = (Ast.Statement.Declaration) s;
            Environment.Variable v = d.getVariable();
            print(v.getType().getJvmName());
            print(" ");
            print(v.getJvmName());
            if (d.getValue().isPresent()) {
                print(" = ");
                visit(d.getValue().get());
            }
        } else {
            visit(s);
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
        if (ast.getVariable().getConstant()) {
            print("final ");
        }
        print(ast.getVariable().getType().getJvmName());
        print(" ");
        print(ast.getVariable().getJvmName());
        if (ast.getValue().isPresent()) {
            print(" = ");
            visit(ast.getValue().get());
        }
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {
        Environment.Function f = ast.getFunction();
        print(f.getReturnType().getJvmName());
        print(" ");
        print(f.getJvmName());
        print("(");
        if (!ast.getParameters().isEmpty()) {
            List<String> params = ast.getParameters();
            List<Environment.Type> types = f.getParameterTypes();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < params.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(types.get(i).getJvmName()).append(" ").append(params.get(i));
            }
            print(sb.toString());
        }
        print(")");
        if (ast.getStatements().isEmpty()) {
            print(" {}");
        } else {
            print(" {");
            indent++;
            for (Ast.Statement s : ast.getStatements()) {
                newline(indent);
                visit(s);
            }
            indent--;
            newline(indent);
            print("}");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        Environment.Variable v = ast.getVariable();
        print(v.getType().getJvmName());
        print(" ");
        print(v.getJvmName());
        if (ast.getValue().isPresent()) {
            print(" = ");
            visit(ast.getValue().get());
        }
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        visit(ast.getReceiver());
        print(" = ");
        visit(ast.getValue());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        print("if (");
        visit(ast.getCondition());
        print(")");
        if (ast.getThenStatements().isEmpty()) {
            print(" {}");
        } else {
            print(" {");
            indent++;
            for (Ast.Statement s : ast.getThenStatements()) {
                newline(indent);
                visit(s);
            }
            indent--;
            newline(indent);
            print("}");
        }
        if (!ast.getElseStatements().isEmpty()) {
            print(" else");
            print(" {");
            indent++;
            for (Ast.Statement s : ast.getElseStatements()) {
                newline(indent);
                visit(s);
            }
            indent--;
            newline(indent);
            print("}");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.For ast) {
        print("for ( ");
        if (ast.getInitialization() != null) {
            printForHeaderPart(ast.getInitialization());
        }
        print("; ");
        visit(ast.getCondition());
        print("; ");
        if (ast.getIncrement() != null) {
            printForHeaderPart(ast.getIncrement());
        }
        print(" )");
        if (ast.getStatements().isEmpty()) {
            print(" {}");
        } else {
            print(" {");
            indent++;
            for (Ast.Statement s : ast.getStatements()) {
                newline(indent);
                visit(s);
            }
            indent--;
            newline(indent);
            print("}");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        print("while (");
        visit(ast.getCondition());
        print(")");
        if (ast.getStatements().isEmpty()) {
            print(" {}");
        } else {
            print(" {");
            indent++;
            for (Ast.Statement s : ast.getStatements()) {
                newline(indent);
                visit(s);
            }
            indent--;
            newline(indent);
            print("}");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        print("return ");
        visit(ast.getValue());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        Object lit = ((Ast.Expression.Literal) ast).getLiteral();
        if (lit == null) {
            print("null");
        } else if (lit instanceof Boolean) {
            print(((Boolean) lit) ? "true" : "false");
        } else if (lit instanceof BigInteger) {
            print(((BigInteger) lit).toString());
        } else if (lit instanceof BigDecimal) {
            print(((BigDecimal) lit).toString());
        } else if (lit instanceof Character) {
            char c = (Character) lit;
            print("'", String.valueOf(c), "'");
        } else if (lit instanceof String) {
            String s = (String) lit;
            print("\"", s, "\"");
        } else {
            print(lit.toString());
        }
        return null;
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
