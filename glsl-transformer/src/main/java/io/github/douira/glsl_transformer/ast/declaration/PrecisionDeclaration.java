package io.github.douira.glsl_transformer.ast.declaration;

import io.github.douira.glsl_transformer.ast.node.type.qualifier.PrecisionQualifier;
import io.github.douira.glsl_transformer.ast.node.type.specifier.TypeSpecifier;
import io.github.douira.glsl_transformer.ast.traversal.*;

public class PrecisionDeclaration extends Declaration {
  protected PrecisionQualifier precisionQualifier;
  protected TypeSpecifier typeSpecifier;

  public PrecisionDeclaration(
      PrecisionQualifier precisionQualifier,
      TypeSpecifier typeSpecifier) {
    this.precisionQualifier = setup(precisionQualifier);
    this.typeSpecifier = setup(typeSpecifier);
  }

  @Override
  public DeclarationType getDeclarationType() {
    return DeclarationType.PRECISION;
  }

  @Override
  public <R> R declarationAccept(ASTVisitor<R> visitor) {
    return visitor.visitPrecisionDeclaration(this);
  }

  @Override
  public void enterNode(ASTListener listener) {
    listener.enterPrecisionDeclaration(this);
  }

  @Override
  public void exitNode(ASTListener listener) {
    listener.exitPrecisionDeclaration(this);
  }
}
