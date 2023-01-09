package io.github.douira.glsl_transformer_physics.ast.print.token;

import io.github.douira.glsl_transformer_physics.GLSLParser;
import io.github.douira.glsl_transformer_physics.cst.token_filter.TokenChannel;

public class EOFToken extends ParserToken {
  public EOFToken() {
    super(TokenChannel.HIDDEN, GLSLParser.EOF);
  }

  @Override
  public String getContent() {
    return "";
  }
}
