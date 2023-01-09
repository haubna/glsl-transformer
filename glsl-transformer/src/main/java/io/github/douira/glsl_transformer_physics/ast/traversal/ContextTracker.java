package io.github.douira.glsl_transformer_physics.ast.traversal;

import io.github.douira.glsl_transformer_physics.ast.node.basic.ASTNode;

public interface ContextTracker {
  default void enterContext(ASTNode node) {
  }
}
