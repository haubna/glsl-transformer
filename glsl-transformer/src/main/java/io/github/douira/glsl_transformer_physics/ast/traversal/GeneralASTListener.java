package io.github.douira.glsl_transformer_physics.ast.traversal;

import io.github.douira.glsl_transformer_physics.ast.node.basic.InnerASTNode;

public interface GeneralASTListener extends ContextTracker {
  default void enterEveryNode(InnerASTNode node) {
  }

  default void exitEveryNode(InnerASTNode node) {
  }

  default void afterEnterEveryNode(InnerASTNode node) {
  }

  default void beforeExitEveryNode(InnerASTNode node) {
  }
}
