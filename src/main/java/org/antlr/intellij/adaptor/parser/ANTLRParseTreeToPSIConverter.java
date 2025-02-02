package org.antlr.intellij.adaptor.parser;

import com.intellij.lang.Language;
import com.intellij.lang.PsiBuilder;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import org.antlr.intellij.adaptor.lexer.PSIElementTypeFactory;
import org.antlr.intellij.adaptor.lexer.RuleIElementType;
import org.antlr.intellij.adaptor.lexer.TokenIElementType;
import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.*;

/**
 * This is how we build an intellij PSI tree from an ANTLR parse tree.
 * We let the ANTLR parser build its kind of ParseTree, and then
 * we convert to a PSI tree in one go using a standard ANTLR ParseTreeListener.
 * <p>
 * The list of SyntaxError objects are pulled from the parser and used
 * for error message highlighting (error nodes don't have the info).
 */
public class ANTLRParseTreeToPSIConverter implements ParseTreeListener {
    protected final Language language;
    protected final PsiBuilder builder;
    protected final Deque<PsiBuilder.Marker> markers = new ArrayDeque<>();
    protected final List<TokenIElementType> tokenElementTypes;
    protected final List<RuleIElementType> ruleElementTypes;
    protected List<SyntaxError> syntaxErrors;
    /**
     * Map an error's start char index (usually start of a token) to the error object.
     */
    protected Map<Integer, SyntaxError> tokenToErrorMap = new HashMap<>();

    public ANTLRParseTreeToPSIConverter(Language language, Parser parser, PsiBuilder builder) {
        this.language = language;
        this.builder = builder;

        this.tokenElementTypes = PSIElementTypeFactory.getTokenIElementTypes(language);
        this.ruleElementTypes = PSIElementTypeFactory.getRuleIElementTypes(language);

        for (ANTLRErrorListener listener : parser.getErrorListeners()) {
            if (listener instanceof SyntaxErrorListener) {
                syntaxErrors = ((SyntaxErrorListener) listener).getSyntaxErrors();
                for (SyntaxError error : syntaxErrors) {
                    // record first error per token
                    int StartIndex = error.offendingSymbol().getStartIndex();
                    if (!tokenToErrorMap.containsKey(StartIndex)) {
                        tokenToErrorMap.put(StartIndex, error);
                    }
                }
            }
        }
    }

    protected final Language getLanguage() {
        return language;
    }

    protected final PsiBuilder getBuilder() {
        return builder;
    }

    protected final Deque<PsiBuilder.Marker> getMarkers() {
        return markers;
    }

    protected final List<TokenIElementType> getTokenElementTypes() {
        return tokenElementTypes;
    }

    protected final List<RuleIElementType> getRuleElementTypes() {
        return ruleElementTypes;
    }

    @Override
    public void visitTerminal(TerminalNode node) {
        builder.advanceLexer();
    }

    /**
     * Summary. For any syntax error thrown by the parser, there will be an
     * ErrorNode in the parse tree and this method will process it.
     * All errors correspond to actual tokens in the input except for
     * missing token errors.
     * <p>
     * There are multiple error situations to consider:
     * <p>
     * 1. Extraneous token. The parse tree will have an ErrorNode for token.
     * <p>
     * 2. Token mismatch. The parse tree will have an ErrorNode for token.
     * <p>
     * 3. Missing token. The parse tree will have an ErrorNode, but
     * it does not correspond to any bit of the input. We underline
     * the current token.
     * <p>
     * 4. NoViableAlt (input inconsistent with any rule alt).
     * The parse tree will have an ErrorNode for token.
     * <p>
     * 5. Tokens consumed to resync the parser during recovery.
     * The parse tree will have an ErrorNode for each token.
     * <p>
     * This is complicated by errors that occur at EOF, but I have
     * modified error strategy to add error nodes for EOF if needed.
     * <p>
     * Another complication. During prediction, we might match n
     * tokens and then fail on the n+1 token, leading to NoViableAltException.
     * But, it's offending token is at n+1 not current token where
     * prediction started (which we use to find syntax errors). So,
     * SyntaxError objects return start not offending token in this case.
     */
    public void visitErrorNode(ErrorNode node) {
        ProgressIndicatorProvider.checkCanceled();

        Token badToken = node.getSymbol();
        boolean isConjuredToken = badToken.getTokenIndex() < 0;
        int nodeStartIndex = badToken.getStartIndex();
        SyntaxError error = tokenToErrorMap.get(nodeStartIndex);

        if (error != null) {
            PsiBuilder.Marker errorMarker = builder.mark();
            if (badToken.getStartIndex() >= 0 &&
                    badToken.getType() != Token.EOF &&
                    !isConjuredToken) {
                // we advance lexer if error occurred at a real token
                // Missing tokens should highlight the token at the missing position
                // but can't consume a token that does not exist.
                builder.advanceLexer();
            }
            String message = String.format("%s%n", error.message());
            errorMarker.error(message);
        } else {
            if (isConjuredToken) {
                PsiBuilder.Marker errorMarker = builder.mark();
                errorMarker.error(badToken.getText()); // says "<missing X>" or similar
            } else {
                // must be a real token consumed during recovery; just consume w/o highlighting it as an error
                builder.advanceLexer();
            }
        }
    }

    @Override
    public void enterEveryRule(ParserRuleContext ctx) {
        ProgressIndicatorProvider.checkCanceled();
        markers.push(getBuilder().mark());
    }

    @Override
    public void exitEveryRule(ParserRuleContext ctx) {
        ProgressIndicatorProvider.checkCanceled();
        PsiBuilder.Marker marker = markers.pop();
        marker.done(getRuleElementTypes().get(ctx.getRuleIndex()));
    }
}
