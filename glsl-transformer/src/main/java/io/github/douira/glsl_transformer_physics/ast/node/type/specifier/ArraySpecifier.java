package io.github.douira.glsl_transformer_physics.ast.node.type.specifier;

import java.util.List;
import java.util.stream.Stream;

import io.github.douira.glsl_transformer_physics.ast.node.basic.ListASTNode;
import io.github.douira.glsl_transformer_physics.ast.node.expression.Expression;
import io.github.douira.glsl_transformer_physics.ast.query.Root;
import io.github.douira.glsl_transformer_physics.ast.traversal.*;

//TODO: any of these expressions can be null
public class ArraySpecifier extends ListASTNode<Expression> {
  public ArraySpecifier(Stream<Expression> children) {
    super(children);
  }

  public List<Expression> getDimensions() {
    return getChildren();
  }

  @Override
  public <R> R accept(ASTVisitor<R> visitor) {
    return visitor.visitArraySpecifier(this);
  }

  @Override
  public void enterNode(ASTListener listener) {
    listener.enterArraySpecifier(this);
  }

  @Override
  public void exitNode(ASTListener listener) {
    listener.exitArraySpecifier(this);
  }

  @Override
  public ArraySpecifier clone() {
    return new ArraySpecifier(getClonedChildren());
  }

  @Override
  public ArraySpecifier cloneInto(Root root) {
    return (ArraySpecifier) super.cloneInto(root);
  }

  @Override
  public ArraySpecifier cloneSeparate() {
    return (ArraySpecifier) super.cloneSeparate();
  }
}
