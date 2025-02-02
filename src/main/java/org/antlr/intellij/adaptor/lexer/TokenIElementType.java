package org.antlr.intellij.adaptor.lexer;

import com.intellij.lang.Language;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a token in the language of the plug-in. The "token type" of
 * leaf nodes in jetbrains PSI tree. Corresponds to ANTLR's int token type.
 * Intellij lexer token types are instances of IElementType:
 * <p>
 * "Interface for token types returned from lexical analysis and for types
 * of nodes in the AST tree."
 * <p>
 * We differentiate between parse tree subtree roots and tokens with
 * {@link RuleIElementType} and {@link TokenIElementType}, respectively.
 */
public class TokenIElementType extends IElementType {
    private final int antlrTokenType;

    public TokenIElementType(int antlrTokenType,
                             @NotNull @NonNls String debugName,
                             @Nullable Language language) {
        super(debugName, language);
        this.antlrTokenType = antlrTokenType;
    }

    public int getANTLRTokenType() {
        return antlrTokenType;
    }
}
