package plc.project;

import java.util.Arrays;
import java.util.List;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {
    public Scope scope;
    private Ast.Method method;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {
        for (Ast.Field f : ast.getFields()) {
            visit(f);
        }
        for (Ast.Method m : ast.getMethods()) {
            visit(m);
        }
        Environment.Function mainFunc;
        try {
            mainFunc = scope.lookupFunction("main", 0);
        } catch (RuntimeException e) {
            throw new RuntimeException("main/0 missing");
        }
        if (mainFunc.getReturnType() != Environment.Type.INTEGER) {
            throw new RuntimeException("main must return Integer");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Method ast) {
        List<Environment.Type> pts = new java.util.ArrayList<>();
        for (String tn : ast.getParameterTypeNames()) {
            pts.add(Environment.getType(tn));
        }
        Environment.Type rt = ast.getReturnTypeName().isPresent()
                ? Environment.getType(ast.getReturnTypeName().get())
                : Environment.Type.NIL;

        Environment.Function fn = scope.defineFunction(ast.getName(), ast.getName(), pts, rt, args -> Environment.NIL);
        ast.setFunction(fn);

        Scope old = scope;
        scope = new Scope(old);
        for (int i = 0; i < ast.getParameters().size(); i++) {
            String p = ast.getParameters().get(i);
            Environment.Type t = pts.get(i);
            scope.defineVariable(p, p, t, false, Environment.NIL);
        }

        Ast.Method prev = method;
        method = ast;
        for (Ast.Statement s : ast.getStatements()) {
            visit(s);
        }
        method = prev;
        scope = old;
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.For ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        visit(ast.getValue());
        Environment.Type target = method == null ? Environment.Type.NIL : method.getFunction().getReturnType();
        requireAssignable(target, ast.getValue().getType());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        throw new UnsupportedOperationException();  // TODO
    }

}
