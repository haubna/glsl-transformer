package io.github.douira.glsl_transformer_physics.basic;

import io.github.douira.glsl_transformer_physics.*;
import io.github.douira.glsl_transformer_physics.basic.EnhancedParser.ParsingStrategy;
import io.github.douira.glsl_transformer_physics.cst.token_filter.TokenFilter;

/**
 * The parser interface provides the basic methods for interfacing with an
 * object that contains an {@link EnhancedParser}.
 */
public interface ParserInterface {
  GLSLLexer getLexer();

  GLSLParser getParser();

  void setThrowParseErrors(boolean throwParseErrors);

  void setParsingStrategy(ParsingStrategy parsingStrategy);

  void setSLLOnly();

  void setLLOnly();

  void setParseTokenFilter(TokenFilter<?> parseTokenFilter);

  TokenFilter<?> getParseTokenFilter();
}
