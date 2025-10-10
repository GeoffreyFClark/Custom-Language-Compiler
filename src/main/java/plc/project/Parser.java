package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have its own function, and reference to other rules correspond
 * to calling those functions.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        // field* method*
        List<Ast.Field> fields = new ArrayList<>();
        List<Ast.Method> methods = new ArrayList<>();
        while (peek("LET")) {
            fields.add(parseField());
        }
        while (peek("DEF")) {
            methods.add(parseMethod());
        }
        if (tokens.has(0)) {
            throw new ParseException("Unexpected token.", tokens.get(0).getIndex());
        }
        return new Ast.Source(fields, methods);
    }

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a field, aka {@code LET}.
     */
    public Ast.Field parseField() throws ParseException {
        // LET CONST? name (= expr)? ;
        if (!match("LET")) {
            if (tokens.has(0)) throw new ParseException("Expected LET.", tokens.get(0).getIndex());
            else throw new ParseException("Expected LET.", eofIndex());
        }
        boolean constant = match("CONST");

        if (!peek(Token.Type.IDENTIFIER)) {
            if (tokens.has(0)) throw new ParseException("Expected identifier.", tokens.get(0).getIndex());
            else throw new ParseException("Expected identifier.", eofIndex());
        }
        String name = tokens.get(0).getLiteral();
        tokens.advance();

        Optional<Ast.Expression> value = Optional.empty();
        if (match("=")) {
            if (!tokens.has(0)) throw new ParseException("Expected expression.", eofIndex());
            value = Optional.of(parseExpression());
        }

        if (!match(";")) {
            if (tokens.has(0)) throw new ParseException("Expected ';'.", tokens.get(0).getIndex());
            else throw new ParseException("Expected ';'.", eofIndex());
        }
        return new Ast.Field(name, constant, value);
    }

    /**
     * Parses the {@code method} rule. This method should only be called if the
     * next tokens start a method, aka {@code DEF}.
     */
    public Ast.Method parseMethod() throws ParseException {
        // DEF name ( params ) DO stmts END
        if (!match("DEF")) {
            if (tokens.has(0)) throw new ParseException("Expected DEF.", tokens.get(0).getIndex());
            else throw new ParseException("Expected DEF.", eofIndex());
        }
        if (!peek(Token.Type.IDENTIFIER)) {
            if (tokens.has(0)) throw new ParseException("Expected identifier.", tokens.get(0).getIndex());
            else throw new ParseException("Expected identifier.", eofIndex());
        }
        String name = tokens.get(0).getLiteral();
        tokens.advance();

        if (!match("(")) {
            if (tokens.has(0)) throw new ParseException("Expected '('.", tokens.get(0).getIndex());
            else throw new ParseException("Expected '('.", eofIndex());
        }

        List<String> params = new ArrayList<>();
        if (!peek(")")) {
            if (!peek(Token.Type.IDENTIFIER)) {
                if (tokens.has(0)) throw new ParseException("Expected identifier.", tokens.get(0).getIndex());
                else throw new ParseException("Expected identifier.", eofIndex());
            }
            params.add(tokens.get(0).getLiteral());
            tokens.advance();
            while (match(",")) {
                if (!peek(Token.Type.IDENTIFIER)) {
                    if (tokens.has(0)) throw new ParseException("Expected identifier.", tokens.get(0).getIndex());
                    else throw new ParseException("Expected identifier.", eofIndex());
                }
                params.add(tokens.get(0).getLiteral());
                tokens.advance();
            }
        }

        if (!match(")")) {
            if (tokens.has(0)) throw new ParseException("Expected ')'.", tokens.get(0).getIndex());
            else throw new ParseException("Expected ')'.", eofIndex());
        }

        if (!match("DO")) {
            if (tokens.has(0)) throw new ParseException("Expected DO.", tokens.get(0).getIndex());
            else throw new ParseException("Expected DO.", eofIndex());
        }

        List<Ast.Statement> statements = new ArrayList<>();
        while (!peek("END")) {
            if (!tokens.has(0)) throw new ParseException("Expected END.", eofIndex());
            statements.add(parseStatement());
        }
        tokens.advance(); // END

        return new Ast.Method(name, params, statements);
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, for, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Statement parseStatement() throws ParseException {
        // delegate other forms (P2B)
        if (peek("LET")) return parseDeclarationStatement();
        if (peek("IF")) return parseIfStatement();
        if (peek("FOR")) return parseForStatement();
        if (peek("WHILE")) return parseWhileStatement();
        if (peek("RETURN")) return parseReturnStatement();

        // Part 2A: either "expr ;" or "expr = expr ;"
        Ast.Expression lhs = parseExpression();
        if (match("=")) {
            // need value
            if (!tokens.has(0)) {
                throw new ParseException("Expected expression.", eofIndex());
            }
            if (peek(";")) {
                throw new ParseException("Expected expression.", tokens.get(0).getIndex());
            }
            Ast.Expression rhs = parseExpression();
            // require ';'
            if (!match(";")) {
                if (tokens.has(0)) throw new ParseException("Expected ';'.", tokens.get(0).getIndex());
                else throw new ParseException("Expected ';'.", eofIndex());
            }
            return new Ast.Statement.Assignment(lhs, rhs);
        } else {
            if (!match(";")) {
                if (tokens.has(0)) throw new ParseException("Expected ';'.", tokens.get(0).getIndex());
                else throw new ParseException("Expected ';'.", eofIndex());
            }
            return new Ast.Statement.Expression(lhs);
        }
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        // LET name (= expr)? ;
        if (!match("LET")) {
            if (tokens.has(0)) throw new ParseException("Expected LET.", tokens.get(0).getIndex());
            else throw new ParseException("Expected LET.", eofIndex());
        }
        if (!peek(Token.Type.IDENTIFIER)) {
            if (tokens.has(0)) throw new ParseException("Expected identifier.", tokens.get(0).getIndex());
            else throw new ParseException("Expected identifier.", eofIndex());
        }
        String name = tokens.get(0).getLiteral();
        tokens.advance();

        Optional<Ast.Expression> value = Optional.empty();
        if (match("=")) {
            if (!tokens.has(0)) throw new ParseException("Expected expression.", eofIndex());
            value = Optional.of(parseExpression());
        }

        if (!match(";")) {
            if (tokens.has(0)) throw new ParseException("Expected ';'.", tokens.get(0).getIndex());
            else throw new ParseException("Expected ';'.", eofIndex());
        }
        return new Ast.Statement.Declaration(name, value);
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        // IF expr DO stmts (ELSE stmts)? END
        if (!match("IF")) {
            if (tokens.has(0)) throw new ParseException("Expected IF.", tokens.get(0).getIndex());
            else throw new ParseException("Expected IF.", eofIndex());
        }
        Ast.Expression condition = parseExpression();

        if (!match("DO")) {
            if (tokens.has(0)) throw new ParseException("Expected DO.", tokens.get(0).getIndex());
            else throw new ParseException("Expected DO.", eofIndex());
        }

        List<Ast.Statement> thenStmts = new ArrayList<>();
        while (!peek("ELSE") && !peek("END")) {
            if (!tokens.has(0)) throw new ParseException("Expected END.", eofIndex());
            thenStmts.add(parseStatement());
        }

        List<Ast.Statement> elseStmts = new ArrayList<>();
        if (match("ELSE")) {
            while (!peek("END")) {
                if (!tokens.has(0)) throw new ParseException("Expected END.", eofIndex());
                elseStmts.add(parseStatement());
            }
        }

        if (!match("END")) {
            if (tokens.has(0)) throw new ParseException("Expected END.", tokens.get(0).getIndex());
            else throw new ParseException("Expected END.", eofIndex());
        }

        return new Ast.Statement.If(condition, thenStmts, elseStmts);
    }

    /**
     * Parses a for statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a for statement, aka
     * {@code FOR}.
     */
    public Ast.Statement.For parseForStatement() throws ParseException {
        // FOR '(' (id = expr)? ';' expr ';' (id = expr)? ')' stmts END
        if (!match("FOR")) {
            if (tokens.has(0)) throw new ParseException("Expected FOR.", tokens.get(0).getIndex());
            else throw new ParseException("Expected FOR.", eofIndex());
        }
        if (!match("(")) {
            if (tokens.has(0)) throw new ParseException("Expected '('.", tokens.get(0).getIndex());
            else throw new ParseException("Expected '('.", eofIndex());
        }

        Ast.Statement init = null;
        if (peek(Token.Type.IDENTIFIER)) {
            String n = tokens.get(0).getLiteral();
            tokens.advance();
            if (!match("=")) {
                if (tokens.has(0)) throw new ParseException("Expected '='.", tokens.get(0).getIndex());
                else throw new ParseException("Expected '='.", eofIndex());
            }
            Ast.Expression v = parseExpression();
            init = new Ast.Statement.Assignment(new Ast.Expression.Access(Optional.empty(), n), v);
        }

        if (!match(";")) {
            if (tokens.has(0)) throw new ParseException("Expected ';'.", tokens.get(0).getIndex());
            else throw new ParseException("Expected ';'.", eofIndex());
        }

        Ast.Expression condition = parseExpression();

        if (!match(";")) {
            if (tokens.has(0)) throw new ParseException("Expected ';'.", tokens.get(0).getIndex());
            else throw new ParseException("Expected ';'.", eofIndex());
        }

        Ast.Statement incr = null;
        if (peek(Token.Type.IDENTIFIER)) {
            String n2 = tokens.get(0).getLiteral();
            tokens.advance();
            if (!match("=")) {
                if (tokens.has(0)) throw new ParseException("Expected '='.", tokens.get(0).getIndex());
                else throw new ParseException("Expected '='.", eofIndex());
            }
            Ast.Expression v2 = parseExpression();
            incr = new Ast.Statement.Assignment(new Ast.Expression.Access(Optional.empty(), n2), v2);
        }

        if (!match(")")) {
            if (tokens.has(0)) throw new ParseException("Expected ')'.", tokens.get(0).getIndex());
            else throw new ParseException("Expected ')'.", eofIndex());
        }

        List<Ast.Statement> body = new ArrayList<>();
        while (!peek("END")) {
            if (!tokens.has(0)) throw new ParseException("Expected END.", eofIndex());
            body.add(parseStatement());
        }

        if (!match("END")) {
            if (tokens.has(0)) throw new ParseException("Expected END.", tokens.get(0).getIndex());
            else throw new ParseException("Expected END.", eofIndex());
        }

        return new Ast.Statement.For(init, condition, incr, body);
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        // WHILE expr DO stmts END
        if (!match("WHILE")) {
            if (tokens.has(0)) throw new ParseException("Expected WHILE.", tokens.get(0).getIndex());
            else throw new ParseException("Expected WHILE.", eofIndex());
        }
        Ast.Expression cond = parseExpression();

        if (!match("DO")) {
            if (tokens.has(0)) throw new ParseException("Expected DO.", tokens.get(0).getIndex());
            else throw new ParseException("Expected DO.", eofIndex());
        }

        List<Ast.Statement> body = new ArrayList<>();
        while (!peek("END")) {
            if (!tokens.has(0)) throw new ParseException("Expected END.", eofIndex());
            body.add(parseStatement());
        }

        if (!match("END")) {
            if (tokens.has(0)) throw new ParseException("Expected END.", tokens.get(0).getIndex());
            else throw new ParseException("Expected END.", eofIndex());
        }

        return new Ast.Statement.While(cond, body);
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        // RETURN expr ;
        if (!match("RETURN")) {
            if (tokens.has(0)) throw new ParseException("Expected RETURN.", tokens.get(0).getIndex());
            else throw new ParseException("Expected RETURN.", eofIndex());
        }
        if (!tokens.has(0)) throw new ParseException("Expected expression.", eofIndex());
        Ast.Expression value = parseExpression();

        if (!match(";")) {
            if (tokens.has(0)) throw new ParseException("Expected ';'.", tokens.get(0).getIndex());
            else throw new ParseException("Expected ';'.", eofIndex());
        }
        return new Ast.Statement.Return(value);
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expression parseExpression() throws ParseException {
        return parseLogicalExpression();
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expression parseLogicalExpression() throws ParseException {
        // logical_expression ::= comparison_expression (('AND' | 'OR') comparison_expression)*
        // accept && and || too
        Ast.Expression expr = parseEqualityExpression();
        while (true) {
            String op = null;
            if (peek("AND")) { op = "AND"; }
            else if (peek("OR")) { op = "OR"; }
            else if (peek("&&")) { op = "&&"; }
            else if (peek("||")) { op = "||"; }
            if (op == null) break;
            tokens.advance(); // consume operator
            Ast.Expression right = parseEqualityExpression();
            expr = new Ast.Expression.Binary(op, expr, right);
        }
        return expr;
    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    public Ast.Expression parseEqualityExpression() throws ParseException {
        // additive_expression (('<' | '<=' | '>' | '>=' | '==' | '!=') additive_expression)*
        Ast.Expression expr = parseAdditiveExpression();
        while (true) {
            String op = null;
            if (peek("<=")) op = "<=";
            else if (peek(">=")) op = ">=";
            else if (peek("==")) op = "==";
            else if (peek("!=")) op = "!=";
            else if (peek("<")) op = "<";
            else if (peek(">")) op = ">";
            if (op == null) break;
            tokens.advance();
            Ast.Expression right = parseAdditiveExpression();
            expr = new Ast.Expression.Binary(op, expr, right);
        }
        return expr;
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expression parseAdditiveExpression() throws ParseException {
        Ast.Expression expr = parseMultiplicativeExpression();
        while (peek("+") || peek("-")) {
            String op = tokens.get(0).getLiteral();
            tokens.advance();
            Ast.Expression right = parseMultiplicativeExpression();
            expr = new Ast.Expression.Binary(op, expr, right);
        }
        return expr;
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expression parseMultiplicativeExpression() throws ParseException {
        Ast.Expression expr = parseSecondaryExpression();
        while (peek("*") || peek("/")) {
            String op = tokens.get(0).getLiteral();
            tokens.advance();
            Ast.Expression right = parseSecondaryExpression();
            expr = new Ast.Expression.Binary(op, expr, right);
        }
        return expr;
    }

    /**
     * Parses the {@code secondary-expression} rule.
     */
    public Ast.Expression parseSecondaryExpression() throws ParseException {
        Ast.Expression expr = parsePrimaryExpression();
        while (peek(".")) {
            tokens.advance(); // '.'
            if (!peek(Token.Type.IDENTIFIER)) {
                if (!tokens.has(0)) {
                    throw new ParseException("Expected identifier.", eofIndex());
                } else {
                    throw new ParseException("Expected identifier.", tokens.get(0).getIndex());
                }
            }
            String name = tokens.get(0).getLiteral();
            tokens.advance();
            if (peek("(")) {
                tokens.advance();
                List<Ast.Expression> args = new ArrayList<>();
                if (peek(")")) {
                    tokens.advance(); // zero args
                } else {
                    args.add(parseExpression());
                    while (peek(",")) {
                        tokens.advance();
                        if (peek(")")) {
                            // catch case of trailing comma i.e. name(expr,)
                            throw new ParseException("Expected expression.", tokens.get(0).getIndex());
                        }
                        args.add(parseExpression());
                    }
                    if (!peek(")")) {
                        if (tokens.has(0)) throw new ParseException("Expected ')'.", tokens.get(0).getIndex());
                        else throw new ParseException("Expected ')'.", eofIndex());
                    }
                    tokens.advance();
                }
                expr = new Ast.Expression.Function(Optional.of(expr), name, args);
            } else {
                expr = new Ast.Expression.Access(Optional.of(expr), name);
            }
        }
        return expr;
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expression parsePrimaryExpression() throws ParseException {
        if (!tokens.has(0)) {
            throw new ParseException("Expected expression.", eofIndex());
        }

        // special case: NIL/TRUE/FALSE keywords (come in as identifiers)
        if (peek(Token.Type.IDENTIFIER)) {
            String w = tokens.get(0).getLiteral();
            if ("NIL".equals(w)) { tokens.advance(); return new Ast.Expression.Literal(null); }
            if ("TRUE".equals(w)) { tokens.advance(); return new Ast.Expression.Literal(Boolean.TRUE); }
            if ("FALSE".equals(w)) { tokens.advance(); return new Ast.Expression.Literal(Boolean.FALSE); }
        }

        // integer
        if (peek(Token.Type.INTEGER)) {
            String s = tokens.get(0).getLiteral();
            tokens.advance();
            if (s.startsWith("+")) s = s.substring(1);
            return new Ast.Expression.Literal(new BigInteger(s));
        }
        // decimal
        if (peek(Token.Type.DECIMAL)) {
            String s = tokens.get(0).getLiteral();
            tokens.advance();
            if (s.startsWith("+")) s = s.substring(1);
            return new Ast.Expression.Literal(new BigDecimal(s));
        }
        // character
        if (peek(Token.Type.CHARACTER)) {
            String raw = tokens.get(0).getLiteral(); // e.g. "'c'" or "'\\n'"
            tokens.advance();
            String inside = raw.substring(1, raw.length() - 1);
            String un = unescapeBasic(inside); // simple, not fancy
            char ch = un.length() > 0 ? un.charAt(0) : '\0';
            return new Ast.Expression.Literal(Character.valueOf(ch));
        }
        // string
        if (peek(Token.Type.STRING)) {
            String raw = tokens.get(0).getLiteral(); // "\"Hello\\n\""
            tokens.advance();
            String inside = raw.substring(1, raw.length() - 1);
            return new Ast.Expression.Literal(unescapeBasic(inside));
        }

        // '(' expression ')'
        if (peek("(")) {
            tokens.advance();
            Ast.Expression e = parseExpression();
            if (!peek(")")) {
                if (tokens.has(0)) throw new ParseException("Expected ')'.", tokens.get(0).getIndex());
                else throw new ParseException("Expected ')'.", eofIndex());
            }
            tokens.advance();
            return new Ast.Expression.Group(e);
        }

        // identifier or function call
        if (peek(Token.Type.IDENTIFIER)) {
            String name = tokens.get(0).getLiteral();
            tokens.advance();
            if (peek("(")) {
                tokens.advance();
                List<Ast.Expression> args = new ArrayList<>();
                if (peek(")")) {
                    tokens.advance();
                } else {
                    args.add(parseExpression());
                    while (peek(",")) {
                        tokens.advance();
                        if (peek(")")) {
                            throw new ParseException("Expected expression.", tokens.get(0).getIndex());
                        }
                        args.add(parseExpression());
                    }
                    if (!peek(")")) {
                        if (tokens.has(0)) throw new ParseException("Expected ')'.", tokens.get(0).getIndex());
                        else throw new ParseException("Expected ')'.", eofIndex());
                    }
                    tokens.advance();
                }
                return new Ast.Expression.Function(Optional.empty(), name, args);
            }
            return new Ast.Expression.Access(Optional.empty(), name);
        }

        // invalid start
        throw new ParseException("Expected expression.", tokens.get(0).getIndex());
    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!tokens.has(i)) return false;
            Object p = patterns[i];
            Token t = tokens.get(i);
            if (p instanceof Token.Type) {
                if (t.getType() != p) return false;
            } else if (p instanceof String) {
                if (!t.getLiteral().equals(p)) return false;
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        if (patterns.length == 1) {
            if (peek(patterns[0])) { tokens.advance(); return true; }
            return false;
        }
        // allow matching any of these patterns (instead of exact sequence)
        for (Object p : patterns) {
            if (peek(p)) { tokens.advance(); return true; }
        }
        return false;
    }

    // EOF index computation, just doing it here instead of in TokenStream
    private int eofIndex() {
        if (tokens.index == 0) return 0;
        Token last = tokens.tokens.get(tokens.index - 1);
        return last.getIndex() + last.getLiteral().length();
    }

    // basic unescape for strings/chars, just the ones listed in the spec
    private String unescapeBasic(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(i + 1);
                switch (n) {
                    case 'b': b.append('\b'); i++; break;
                    case 'n': b.append('\n'); i++; break;
                    case 'r': b.append('\r'); i++; break;
                    case 't': b.append('\t'); i++; break;
                    case '\'': b.append('\''); i++; break;
                    case '\"': b.append('\"'); i++; break;
                    case '\\': b.append('\\'); i++; break;
                    default: b.append(n); i++; break; // pass-through unknown
                }
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}
