package io.github.douira.glsl_transformer_physics.cst.core.target;

import io.github.douira.glsl_transformer_physics.cst.node.StringNode;
import io.github.douira.glsl_transformer_physics.job_parameter.JobParameters;
import io.github.douira.glsl_transformer_physics.tree.TreeMember;

/**
 * A terminal replace target replaces the target with a terminal string node.
 */
public abstract class TerminalReplaceTarget<T extends JobParameters> extends ReplaceTarget<T> {
  /**
   * Creates a new terminal placement target with a search string.
   * 
   * @param needle The search string
   */
  public TerminalReplaceTarget(String needle) {
    super(needle);
  }

  /**
   * Creates a new terminal replace target with no search string. The
   * {@link #getNeedle()} method should be overwritten if this constructor is
   * used.
   * 
   * @see io.github.douira.glsl_transformer_physics.cst.core.target.HandlerTargetImpl#HandlerTargetImpl()
   */
  protected TerminalReplaceTarget() {
  }

  /**
   * Returns the content to insert as a terminal replacement node.
   * 
   * @return The string to insert as a terminal replacement node
   */
  protected abstract String getTerminalContent();

  @Override
  protected TreeMember getReplacement(TreeMember node, String match) {
    return new StringNode(getTerminalContent(), false);
  }
}
