package io.github.douira.glsl_transformer_physics.ast.node.basic;

import java.util.List;

public interface ListNode<Child extends ASTNode> {
  List<Child> getChildren();
}
