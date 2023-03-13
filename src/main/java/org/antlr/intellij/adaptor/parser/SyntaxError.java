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
public record SyntaxError (
        Recognizer<?, ?> recognizer,
        Token offendingSymbol,
        int line,
        int charPositionInLine,
        String message,
        RecognitionException exception
) {
    @Override
    public Token offendingSymbol() {
        if (exception instanceof NoViableAltException noViableAltException) {
            // the error node in parse tree will have the start token as bad token
            // even if many lookahead tokens were matched before failing to find
            // a viable alt.
            return noViableAltException.getStartToken();
        }
        return offendingSymbol;
    }
}
