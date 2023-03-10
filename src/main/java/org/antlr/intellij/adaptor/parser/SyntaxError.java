package org.antlr.intellij.adaptor.parser;

import org.antlr.v4.runtime.NoViableAltException;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;

import java.util.Objects;

/**
 * A syntax error from parsing language of plugin. These are
 * created by SyntaxErrorListener.
 */
public class SyntaxError {
    private final Recognizer<?, ?> recognizer;
    private final Token offendingSymbol;
    private final int line;
    private final int charPositionInLine;
    private final String message;
    private final RecognitionException exception;

    public SyntaxError(Recognizer<?, ?> recognizer,
                       Token offendingSymbol,
                       int line, int charPositionInLine,
                       String msg,
                       RecognitionException exception) {
        this.recognizer = recognizer;
        this.offendingSymbol = offendingSymbol;
        this.line = line;
        this.charPositionInLine = charPositionInLine;
        this.message = msg;
        this.exception = exception;
    }

    public Recognizer<?, ?> getRecognizer() {
        return recognizer;
    }

    public Token getOffendingSymbol() {
        if (exception instanceof NoViableAltException noViableAltException) {
            // the error node in parse tree will have the start token as bad token
            // even if many lookahead tokens were matched before failing to find
            // a viable alt.
            return noViableAltException.getStartToken();
        }
        return offendingSymbol;
    }

    public int getLine() {
        return line;
    }

    public int getCharPositionInLine() {
        return charPositionInLine;
    }

    public String getMessage() {
        return message;
    }

    public RecognitionException getException() {
        return exception;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SyntaxError that = (SyntaxError) o;
        return getLine() == that.getLine() && getCharPositionInLine() == that.getCharPositionInLine() && getRecognizer().equals(that.getRecognizer()) && Objects.equals(getOffendingSymbol(), that.getOffendingSymbol()) && getMessage().equals(that.getMessage()) && getException().equals(that.getException());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getRecognizer(), getOffendingSymbol(), getLine(), getCharPositionInLine(), getMessage(), getException());
    }
}
