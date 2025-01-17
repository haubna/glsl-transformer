package io.github.douira.glsl_transformer_physics.cst.transform.lifecycle;

import java.util.function.Supplier;

import io.github.douira.glsl_transformer_physics.job_parameter.JobParameters;

/**
 * Combines activatable and lifecycle user functionality.
 */
public interface ActivatableLifecycleUser<T extends JobParameters> extends LifecycleUser<T>, Activatable {
  /** Override to make type more specific */
  @Override
  ActivatableLifecycleUser<T> activation(Supplier<Boolean> activation);
}
