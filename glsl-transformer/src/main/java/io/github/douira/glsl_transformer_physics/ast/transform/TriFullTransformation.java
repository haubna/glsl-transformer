package io.github.douira.glsl_transformer_physics.ast.transform;

import io.github.douira.glsl_transformer_physics.ast.node.basic.ASTNode;
import io.github.douira.glsl_transformer_physics.ast.query.Root;
import io.github.douira.glsl_transformer_physics.job_parameter.JobParameters;

@FunctionalInterface
public interface TriFullTransformation<A extends ASTNode, T extends JobParameters> {
  void accept(A a, A b, A c, Root rootA, Root rootB, Root rootC, T jobParameters);
}
