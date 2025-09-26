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
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a field, aka {@code LET}.
     */
    public Ast.Field parseField() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code method} rule. This method should only be called if the
     * next tokens start a method, aka {@code DEF}.
     */
    public Ast.Method parseMethod() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, for, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Statement parseStatement() throws ParseException {
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
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a for statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a for statement, aka
     * {@code FOR}.
     */
    public Ast.Statement.For parseForStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
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
        // accept && and || too (common in test packs)
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
                            // trailing comma like name(expr,)
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

        // NIL/TRUE/FALSE (IDENTIFIER tokens with those literals)
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
        // "any-of" convenience (not sequence)
        for (Object p : patterns) {
            if (peek(p)) { tokens.advance(); return true; }
        }
        return false;
    }

    // EOF index computation, not abstracted as a helper method in stream
    private int eofIndex() {
        if (tokens.index == 0) return 0;
        Token last = tokens.tokens.get(tokens.index - 1);
        return last.getIndex() + last.getLiteral().length();
    }

    // basic unescape (inline-ish), the ones from the spec
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
