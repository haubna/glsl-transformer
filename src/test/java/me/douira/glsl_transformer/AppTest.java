package me.douira.glsl_transformer;

import org.antlr.v4.runtime.*;

import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for parser.
 */
public class AppTest {
    /*
     * private MarkupParser setup(String input) { CharStream inputStream =
     * CharStreams.fromString(input); this.markupLexer = new
     * MarkupLexer(inputStream); CommonTokenStream commonTokenStream = new
     * CommonTokenStream(markupLexer); MarkupParser markupParser = new
     * MarkupParser(commonTokenStream);
     * 
     * StringWriter writer = new StringWriter(); this.errorListener = new
     * MarkupErrorListener(writer); markupLexer.removeErrorListeners(); // uncomment
     * this line if you want to see errors in the lexer //
     * markupLexer.addErrorListener(errorListener);
     * markupParser.removeErrorListeners();
     * markupParser.addErrorListener(errorListener);
     * 
     * return markupParser; }
     * 
     * @Test public void testAttribute() { MarkupParser parser =
     * setup("author=\"john\""); // we have to manually push the correct mode
     * this.markupLexer.pushMode(MarkupLexer.BBCODE);
     * 
     * parser.attribute(); TokenStream ts = parser.getTokenStream();
     * 
     * assertEquals(MarkupLexer.ID, ts.get(0).getType());
     * assertEquals(MarkupLexer.EQUALS, ts.get(1).getType());
     * assertEquals(MarkupLexer.STRING, ts.get(2).getType());
     * 
     * assertEquals("", this.errorListener.getSymbol()); }
     */
}