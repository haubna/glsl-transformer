package io.github.douira.glsl_transformer_physics.ast.node.basic;

import io.github.douira.glsl_transformer_physics.ast.query.Root;
import io.github.douira.glsl_transformer_physics.ast.traversal.ASTListener;

public abstract class InnerASTNode extends ASTNode {
  public abstract void enterNode(ASTListener listener);

  public abstract void exitNode(ASTListener listener);

  @Override
  public abstract InnerASTNode clone();

  @Override
  public InnerASTNode cloneInto(Root root) {
    return (InnerASTNode) super.cloneInto(root);
  }

  @Override
  public InnerASTNode cloneSeparate() {
    return (InnerASTNode) super.cloneSeparate();
  }
}
