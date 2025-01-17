package io.github.douira.glsl_transformer_physics.ast.data;

import org.antlr.v4.runtime.Token;

public class TypeUtil {
  public static <T extends TokenTyped> T enumFromToken(T[] enumValues, Token token) {
    for (T value : enumValues) {
      if (value.getTokenType() == token.getType()) {
        return value;
      }
    }
    throw new IllegalArgumentException("Unknown token: " + token.getText());
  }

  public static <T> T enumFromToken(T[] values, int[] tokenTypes, Token token) {
    if (values.length != tokenTypes.length) {
      throw new IllegalArgumentException("values.length != tokenTypes.length");
    }
    for (int i = 0; i < values.length; i++) {
      if (tokenTypes[i] == token.getType()) {
        return values[i];
      }
    }
    throw new IllegalArgumentException("Unknown token: " + token.getText());
  }
}
