package io.github.douira.glsl_transformer.ast.transform;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import io.github.douira.glsl_transformer.GLSLParser;
import io.github.douira.glsl_transformer.ast.node.Identifier;
import io.github.douira.glsl_transformer.ast.node.expression.*;
import io.github.douira.glsl_transformer.ast.query.*;
import io.github.douira.glsl_transformer.job_parameter.NonFixedJobParameters;
import io.github.douira.glsl_transformer.test_util.*;
import io.github.douira.glsl_transformer.util.Type;

public class ASTTransformerTest extends TestWithASTTransformer {
  void assertInjectExternalDeclaration(int index, String input, String output) {
    transformer.setTransformation(translationUnit -> {
      translationUnit.children.add(index, transformer.parseNode(
          "int a;",
          translationUnit,
          GLSLParser::externalDeclaration,
          ASTBuilder::visitExternalDeclaration));
    });
    assertEquals(output, transformer.transform(input));
  }

  @Test
  void testInsertion() {
    assertInjectExternalDeclaration(0,
        "int b;\nint c;\n",
        "int a;\nint b;\nint c;\n");
    assertInjectExternalDeclaration(1,
        "int b;\nint c;",
        "int b;\nint a;\nint c;\n");
    assertInjectExternalDeclaration(2,
        "int b;\nint c;",
        "int b;\nint c;\nint a;\n");
  }

  @Test
  void testIdentifierQuery() {
    transformer.setTransformation(tree -> {
      tree.getRoot().identifierIndex.index.prefixMap("a").values()
          .stream().forEach(Index.<Identifier>iterate(node -> node.name += "b"));
    });
    assertEquals(
        "int ab, ab, c;\n",
        transformer.transform("int a, a, c;\n"));
    assertEquals(
        "int ab = 4 + ab + aab, ab, c;\n",
        transformer.transform("int a = 4 + a + aa, a, c;\n"));
  }

  @Test
  void testNodeQuery() {
    transformer.setTransformation(tree -> {
      tree.getRoot().nodeIndex.get(LiteralExpression.class)
          .stream().forEach(literal -> {
            literal.integerValue++;
          });
    });
    assertEquals(
        "int a = 2, b = 3, c = 4;\n",
        transformer.transform("int a = 1, b = 2, c = 3;\n"));
    assertEquals(
        "int a = 2, 3, 4;\n",
        transformer.transform("int a = 1, 2, 3;\n"));
  }

  @Test
  void testNodeQueryAfterModification() {
    transformer.setTransformation(tree -> {
      Root.<LiteralExpression>indexNodes(tree,
          register -> {
            for (var sequence : tree.getRoot().nodeIndex
                .get(SequenceExpression.class)) {
              sequence.expressions.add(
                  register.apply(
                      new LiteralExpression(Type.INT32, 1)));
            }
          });
      tree.getRoot().nodeIndex.get(LiteralExpression.class)
          .stream().forEach(literal -> literal.integerValue++);
    });
    assertEquals(
        "int a = 2, b = 3, c = 4, 2;\n",
        transformer.transform("int a = 1, b = 2, c = 3;\n"));
    assertEquals(
        "int a = 2, 3, 4, 2;\n",
        transformer.transform("int a = 1, 2, 3;\n"));
  }

  @Test
  void testSelfReplacement() {
    transformer.setTransformation(tree -> {
      Root.<Expression>indexNodes(tree,
          register -> {
            var toReplace = new ArrayList<LiteralExpression>();
            for (var node : tree.getRoot().nodeIndex
                .get(LiteralExpression.class)) {
              if (node.integerValue == 3) {
                toReplace.add(node);
              }
            }
            for (var node : toReplace) {
              node.replaceInParent(
                  register.apply(new ReferenceExpression(new Identifier("foo"))));
            }
          });
    });
    assertEquals(
        "int a = 1, b = 2, c = foo;\n",
        transformer.transform("int a = 1, b = 2, c = 3;\n"));
    assertEquals(
        "int a = foo, 2, foo, 5 + foo + b;\n",
        transformer.transform("int a = 3, 2, 3, 5 + 3 + b;\n"));
  }

  @Test
  void testNodeRemovalAndQuery() {
    transformer.setTransformation(tree -> {
      var toRemove = new ArrayList<ReferenceExpression>();
      for (var node : tree.getRoot().nodeIndex
          .get(ReferenceExpression.class)) {
        if (node.getIdentifier().name.equals("a")) {
          toRemove.add(node);
        }
      }
      for (var node : toRemove) {
        node.removeFromParent();
      }
      assertTrue(tree.getRoot().identifierIndex.get("a").isEmpty());
    });
    assertEquals(
        "int x = b, c, d;\n",
        transformer.transform("int x = a, b, c, a, d;\n"));
  }

  @Test
  void testJobParameters() {
    var jobParameters = new NonFixedJobParameters();
    transformer.setTransformation(tree -> {
      assertEquals(transformer.getJobParameters(), jobParameters);
    });
    transformer.transform("", jobParameters);
  }
}