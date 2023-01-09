package io.github.douira.glsl_transformer_physics.cst.core.target;

import io.github.douira.glsl_transformer_physics.job_parameter.JobParameters;
import io.github.douira.glsl_transformer_physics.tree.TreeMember;

/**
 * A throw target that has a fixed message it puts into the exception that's
 * thrown when the target is found.
 */
public class ThrowTargetImpl<T extends JobParameters> extends ThrowTarget<T> {
  private final String message;

  /**
   * Creates a new throw target with a fixed needle and exception message.
   * 
   * @param needle  The search string
   * @param message The exception message
   */
  public ThrowTargetImpl(String needle, String message) {
    super(needle);
    this.message = message;
  }

  @Override
  protected String getMessage(TreeMember node, String match) {
    return message;
  }
}
