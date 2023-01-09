package io.github.douira.glsl_transformer_physics.ast.print.token;

public class Marker extends PrintToken {
  @Override
  public String getContent() {
    return null;
  }

  @Override
  public boolean isCommonFormattingNewline() {
    return false;
  }

  @Override
  public boolean endsWithNewline() {
    return false;
  }
}
