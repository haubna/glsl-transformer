package io.github.douira.glsl_transformer_physics.cst.transform.lifecycle;

import static io.github.douira.glsl_transformer_physics.util.ConfigUtil.*;

import java.util.function.Supplier;

import io.github.douira.glsl_transformer_physics.cst.transform.ExecutionPlanner;
import io.github.douira.glsl_transformer_physics.job_parameter.JobParameters;

/**
 * This implementation of a lifecycle user may be used if a class can be
 * extended. A custom implementation should be considered if another class needs
 * to be extended. An instance of this class could also be held as a "trait" and
 * accessed through delegate methods.
 */
public class LifecycleUserImpl<T extends JobParameters> implements ActivatableLifecycleUser<T> {
  private ExecutionPlanner<T> planner;
  private boolean initialized = false;
  private Supplier<Boolean> activation;

  @Override
  public ExecutionPlanner<T> getPlanner() {
    return planner;
  }

  @Override
  public void setPlanner(ExecutionPlanner<T> planner) {
    this.planner = planner;
  }

  @Override
  public boolean isInitialized() {
    return initialized;
  }

  @Override
  public void setInitialized() {
    initialized = true;
  }

  @Override
  public LifecycleUserImpl<T> activation(Supplier<Boolean> activation) {
    this.activation = activation;
    return this;
  }

  @Override
  public boolean isActive() {
    return withDefault(activation, true);
  }
}
