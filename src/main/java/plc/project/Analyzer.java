package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
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
        Environment.Type t = Environment.getType(ast.getTypeName());
        if (ast.getValue().isPresent()) {
            visit(ast.getValue().get());
            requireAssignable(t, ast.getValue().get().getType());
        } else if (ast.getConstant()) {
            throw new RuntimeException("const field needs init");
        }
        Environment.Variable v = scope.defineVariable(ast.getName(), ast.getName(), t, ast.getConstant(), Environment.NIL);
        ast.setVariable(v);
        return null;
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
        if (ast.getValue().isPresent()) {
            visit(ast.getValue().get());
        }

        Environment.Type t;
        if (ast.getTypeName().isPresent()) {
            t = Environment.getType(ast.getTypeName().get());
            if (ast.getValue().isPresent()) {
                requireAssignable(t, ast.getValue().get().getType());
            }
        } else if (ast.getValue().isPresent()) {
            t = ast.getValue().get().getType();
        } else {
            throw new RuntimeException("missing type");
        }

        Environment.Variable v = scope.defineVariable(ast.getName(), ast.getName(), t, false, Environment.NIL);
        ast.setVariable(v);
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        if (!(ast.getReceiver() instanceof Ast.Expression.Access)) {
            throw new RuntimeException("receiver not access");
        }
        visit(ast.getReceiver());
        visit(ast.getValue());
        Environment.Variable v = ((Ast.Expression.Access) ast.getReceiver()).getVariable();
        if (v.getConstant()) {
            throw new RuntimeException("assigning constant");
        }
        requireAssignable(v.getType(), ast.getValue().getType());
        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        visit(ast.getCondition());
        if (ast.getCondition().getType() != Environment.Type.BOOLEAN) {
            throw new RuntimeException("if cond not boolean");
        }
        if (ast.getThenStatements().isEmpty()) {
            throw new RuntimeException("empty then");
        }

        Scope old = scope;
        scope = new Scope(old);
        for (Ast.Statement s : ast.getThenStatements()) {
            visit(s);
        }
        scope = old;

        if (!ast.getElseStatements().isEmpty()) {
            Scope old2 = scope;
            scope = new Scope(old2);
            for (Ast.Statement s : ast.getElseStatements()) {
                visit(s);
            }
            scope = old2;
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.For ast) {
        Scope old = scope;
        scope = new Scope(old);

        Environment.Type loopType = null;
        if (ast.getInitialization() != null) {
            visit(ast.getInitialization());
            if (ast.getInitialization() instanceof Ast.Statement.Assignment) {
                Ast.Statement.Assignment a = (Ast.Statement.Assignment) ast.getInitialization();
                if (a.getReceiver() instanceof Ast.Expression.Access) {
                    loopType = ((Ast.Expression.Access) a.getReceiver()).getVariable().getType();
                }
            }
        }

        visit(ast.getCondition());
        if (ast.getCondition().getType() != Environment.Type.BOOLEAN) {
            scope = old;
            throw new RuntimeException("for cond not boolean");
        }

        if (ast.getIncrement() != null) {
            visit(ast.getIncrement());
            if (loopType != null && ast.getIncrement() instanceof Ast.Statement.Assignment) {
                Ast.Statement.Assignment a2 = (Ast.Statement.Assignment) ast.getIncrement();
                if (a2.getReceiver() instanceof Ast.Expression.Access) {
                    Environment.Type t2 = ((Ast.Expression.Access) a2.getReceiver()).getVariable().getType();
                    if (t2 != loopType) {
                        scope = old;
                        throw new RuntimeException("for incr type mismatch");
                    }
                }
            }
        }

        if (ast.getStatements().isEmpty()) {
            scope = old;
            throw new RuntimeException("for body empty");
        }
        for (Ast.Statement s : ast.getStatements()) {
            visit(s);
        }

        scope = old;
        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        visit(ast.getCondition());
        if (ast.getCondition().getType() != Environment.Type.BOOLEAN) {
            throw new RuntimeException("while cond not boolean");
        }
        Scope old = scope;
        scope = new Scope(old);
        for (Ast.Statement s : ast.getStatements()) {
            visit(s);
        }
        scope = old;
        return null;
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
        Object lit = ((Ast.Expression.Literal) ast).getLiteral();
        if (lit == null) {
            ast.setType(Environment.Type.NIL);
            return null;
        }
        if (lit instanceof Boolean) {
            ast.setType(Environment.Type.BOOLEAN);
            return null;
        }
        if (lit instanceof Character) {
            ast.setType(Environment.Type.CHARACTER);
            return null;
        }
        if (lit instanceof String) {
            ast.setType(Environment.Type.STRING);
            return null;
        }
        if (lit instanceof BigInteger) {
            BigInteger bi = (BigInteger) lit;
            try {
                bi.intValueExact();
            } catch (ArithmeticException e) {
                throw new RuntimeException("int out of range");
            }
            ast.setType(Environment.Type.INTEGER);
            return null;
        }
        if (lit instanceof BigDecimal) {
            BigDecimal bd = (BigDecimal) lit;
            double d = bd.doubleValue();
            if (Double.isInfinite(d) || Double.isNaN(d)) {
                throw new RuntimeException("decimal out of range");
            }
            ast.setType(Environment.Type.DECIMAL);
            return null;
        }
        return null;
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
