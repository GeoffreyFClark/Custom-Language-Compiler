package plc.project;

import java.math.BigInteger;
import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import java.math.RoundingMode;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        for (Ast.Field f : ast.getFields()) {
            visit(f);
        }
        for (Ast.Method m : ast.getMethods()) {
            visit(m);
        }
        Environment.Function main = scope.lookupFunction("main", 0);
        return main.invoke(new ArrayList<>());
    }

    @Override
    public Environment.PlcObject visit(Ast.Field ast) {
        Environment.PlcObject value;
        if (ast.getValue().isPresent()) {
            value = visit(ast.getValue().get());
        } else {
            value = Environment.NIL;
        }
        scope.defineVariable(ast.getName(), ast.getConstant(), value);
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Method ast) {
        Scope definingScope = scope;
        scope.defineFunction(ast.getName(), ast.getParameters().size(), args -> {
            Scope old = scope;
            scope = new Scope(definingScope);
            try {
                for (int i = 0; i < ast.getParameters().size(); i++) {
                    scope.defineVariable(ast.getParameters().get(i), false, args.get(i));
                }
                for (Ast.Statement s : ast.getStatements()) {
                    visit(s);
                }
                return Environment.NIL;
            } catch (Return r) {
                return r.value;
            } finally {
                scope = old;
            }
        });
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Declaration ast) {
        Environment.PlcObject value;
        if (ast.getValue().isPresent()) {
            value = visit(ast.getValue().get());
        } else {
            value = Environment.NIL;
        }
        scope.defineVariable(ast.getName(), false, value);
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {
        if (!(ast.getReceiver() instanceof Ast.Expression.Access)) {
            throw new RuntimeException("Receiver is not assignable.");
        }
        Ast.Expression.Access acc = (Ast.Expression.Access) ast.getReceiver();
        Environment.PlcObject v = visit(ast.getValue());

        if (acc.getReceiver().isPresent()) {
            Environment.PlcObject obj = visit(acc.getReceiver().get());
            Environment.Variable var = obj.getField(acc.getName());
            if (var.getConstant()) {
                throw new RuntimeException("Cannot assign to constant field.");
            }
            var.setValue(v);
        } else {
            Environment.Variable var = scope.lookupVariable(acc.getName());
            if (var.getConstant()) {
                throw new RuntimeException("Cannot assign to constant variable.");
            }
            var.setValue(v);
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {
        Boolean cond = requireType(Boolean.class, visit(ast.getCondition()));
        Scope old = scope;
        scope = new Scope(old);
        try {
            if (cond) {
                for (Ast.Statement s : ast.getThenStatements()) {
                    visit(s);
                }
            } else {
                for (Ast.Statement s : ast.getElseStatements()) {
                    visit(s);
                }
            }
        } finally {
            scope = old;
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.For ast) {
        Scope old = scope;
        scope = new Scope(old);
        try {
            if (ast.getInitialization() != null) {
                visit(ast.getInitialization());
            }
            while (requireType(Boolean.class, visit(ast.getCondition()))) {
                for (Ast.Statement s : ast.getStatements()) {
                    visit(s);
                }
                if (ast.getIncrement() != null) {
                    visit(ast.getIncrement());
                }
            }
        } finally {
            scope = old;
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.While ast) {
        Scope old = scope;
        scope = new Scope(old);
        try {
            while (requireType(Boolean.class, visit(ast.getCondition()))) {
                for (Ast.Statement s : ast.getStatements()) {
                    visit(s);
                }
            }
        } finally {
            scope = old;
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) {
        Environment.PlcObject v = visit(ast.getValue());
        throw new Return(v);
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
        if (ast.getLiteral() == null) {
            return Environment.NIL;
        }
        return Environment.create(ast.getLiteral());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {
        return visit(ast.getExpression());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
        String op = ast.getOperator();

        if (op.equals("AND") || op.equals("&&")) {
            Boolean left = requireType(Boolean.class, visit(ast.getLeft()));
            if (!left) {
                return Environment.create(false);
            }
            Boolean right = requireType(Boolean.class, visit(ast.getRight()));
            return Environment.create(left && right);
        }

        if (op.equals("OR") || op.equals("||")) {
            Boolean left = requireType(Boolean.class, visit(ast.getLeft()));
            if (left) {
                return Environment.create(true);
            }
            Boolean right = requireType(Boolean.class, visit(ast.getRight()));
            return Environment.create(left || right);
        }

        Environment.PlcObject lObj = visit(ast.getLeft());
        Environment.PlcObject rObj = visit(ast.getRight());
        Object L = lObj.getValue();
        Object R = rObj.getValue();

        if (op.equals("==")) {
            return Environment.create(Objects.equals(L, R));
        }
        if (op.equals("!=")) {
            return Environment.create(!Objects.equals(L, R));
        }

        if (op.equals("<") || op.equals("<=") || op.equals(">") || op.equals(">=")) {
            if (!(L instanceof Comparable) || L.getClass() != R.getClass()) {
                throw new RuntimeException("Invalid comparison operands.");
            }
            @SuppressWarnings({"rawtypes","unchecked"})
            int cmp = ((Comparable) L).compareTo(R);
            boolean res;
            switch (op) {
                case "<":  res = cmp < 0; break;
                case "<=": res = cmp <= 0; break;
                case ">":  res = cmp > 0; break;
                default:   res = cmp >= 0; break;
            }
            return Environment.create(res);
        }

        if (op.equals("+")) {
            if (L instanceof String || R instanceof String) {
                return Environment.create(String.valueOf(L) + String.valueOf(R));
            } else if (L instanceof BigInteger && R instanceof BigInteger) {
                return Environment.create(((BigInteger) L).add((BigInteger) R));
            } else if (L instanceof BigDecimal && R instanceof BigDecimal) {
                return Environment.create(((BigDecimal) L).add((BigDecimal) R));
            } else {
                throw new RuntimeException("Invalid + operands.");
            }
        }

        if (op.equals("-")) {
            if (L instanceof BigInteger && R instanceof BigInteger) {
                return Environment.create(((BigInteger) L).subtract((BigInteger) R));
            } else if (L instanceof BigDecimal && R instanceof BigDecimal) {
                return Environment.create(((BigDecimal) L).subtract((BigDecimal) R));
            } else {
                throw new RuntimeException("Invalid - operands.");
            }
        }

        if (op.equals("*")) {
            if (L instanceof BigInteger && R instanceof BigInteger) {
                return Environment.create(((BigInteger) L).multiply((BigInteger) R));
            } else if (L instanceof BigDecimal && R instanceof BigDecimal) {
                return Environment.create(((BigDecimal) L).multiply((BigDecimal) R));
            } else {
                throw new RuntimeException("Invalid * operands.");
            }
        }

        if (op.equals("/")) {
            if (L instanceof BigInteger && R instanceof BigInteger) {
                BigInteger rr = (BigInteger) R;
                if (rr.equals(BigInteger.ZERO)) throw new RuntimeException("Division by zero.");
                return Environment.create(((BigInteger) L).divide(rr));
            } else if (L instanceof BigDecimal && R instanceof BigDecimal) {
                BigDecimal rr = (BigDecimal) R;
                if (rr.compareTo(BigDecimal.ZERO) == 0) throw new RuntimeException("Division by zero.");
                BigDecimal ll = (BigDecimal) L;
                int scale = Math.max(ll.scale(), rr.scale());
                return Environment.create(ll.divide(rr, scale, RoundingMode.HALF_EVEN));
            } else {
                throw new RuntimeException("Invalid / operands.");
            }
        }

        throw new RuntimeException("Unknown operator: " + op);
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {
        if (ast.getReceiver().isPresent()) {
            Environment.PlcObject obj = visit(ast.getReceiver().get());
            return obj.getField(ast.getName()).getValue();
        } else {
            return scope.lookupVariable(ast.getName()).getValue();
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {
        List<Environment.PlcObject> args = new ArrayList<>();
        for (Ast.Expression e : ast.getArguments()) {
            args.add(visit(e));
        }
        if (ast.getReceiver().isPresent()) {
            Environment.PlcObject obj = visit(ast.getReceiver().get());
            return obj.callMethod(ast.getName(), args);
        } else {
            Environment.Function f = scope.lookupFunction(ast.getName(), args.size());
            return f.invoke(args);
        }
    }

    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}
