package io.github.douira.glsl_transformer.core;

import io.github.douira.glsl_transformer.GLSLParser;
import io.github.douira.glsl_transformer.GLSLParser.TranslationUnitContext;
import io.github.douira.glsl_transformer.core.target.HandlerTarget;
import io.github.douira.glsl_transformer.core.target.ParsedReplaceTarget;
import io.github.douira.glsl_transformer.core.target.ThrowTarget;
import io.github.douira.glsl_transformer.transform.RunPhase;
import io.github.douira.glsl_transformer.transform.Transformation;
import io.github.douira.glsl_transformer.transform.TransformationPhase.InjectionPoint;

/**
 * The wrap identifier transformation wraps the usage of a certain identifier
 * with new code by replacing its usage with a new expression and inserting code
 * that takes care of handling the conversion from the new to the old value. It
 * also checks that the wrapped value isn't already present in the code.
 */
public class WrapIdentifier<T> extends Transformation<T> {
  /**
   * Creates a new wrap identifier transformation.
   * 
   * @param wrapTarget       The identifier to replace
   * @param wrapResult       The identifier that will be used to replace it
   * @param replaceTarget    The target that is used as a replacement
   * @param wrappingInjector A transformation phase that does the additional code
   *                         injection
   */
  public WrapIdentifier(
      String wrapTarget,
      String wrapResult,
      HandlerTarget<T> replaceTarget,
      RunPhase<T> wrappingInjector) {
    // throw if the wrap result already exists
    addPhase(new SearchTerminals<T>(
        ThrowTarget.fromMessage(wrapResult,
            "The wrapper '" + wrapResult + "' can't already be in the string!")));

    // replace the wrap target with the wrap result
    addPhase(new SearchTerminals<T>(replaceTarget));

    // inject the wrapper code
    addConcurrentPhase(wrappingInjector);

    // TODO: does the wrapping injector need to be told about something?
    // should there be some kind of communication system for phases that were not
    // directly constructed within a transformation?
  }

  /**
   * Creates a new wrap identifier transformation that uses a parsed replace
   * target that replaces identifiers with an expression. (which may also just be
   * an identifier)
   * 
   * @param <T>              The job parameter type
   * @param wrapTarget       The identifier to replace
   * @param wrapResult       The identifier that will be used to replace it
   * @param wrapExpression   The expression to insert instead of the
   *                         {@code wrapTarget}
   * @param wrappingInjector A transformation phase that does the additional code
   *                         injection
   * @return The wrap identifier transformation with the given parameters
   */
  public static <T> WrapIdentifier<T> fromExpression(
      String wrapTarget,
      String wrapResult,
      String wrapExpression,
      RunPhase<T> wrappingInjector) {
    return new WrapIdentifier<T>(
        wrapTarget,
        wrapResult,
        new ParsedReplaceTarget<T>(wrapTarget, wrapExpression, GLSLParser::expression),
        wrappingInjector);
  }

  /**
   * Creates a new wrap identifier transformation that inserts a parsed expression
   * as a replacement and inserts a new external declaration.
   * 
   * @see #fromExpression(String, String, String, RunPhase)
   * 
   * @param <T>            The job parameter type
   * @param wrapTarget     The identifier to replace
   * @param wrapResult     The identifier that will be used to replace it
   * @param wrapExpression The expression to insert instead of the
   *                       {@code wrapTarget}
   * @param location       The injection location for the new code
   * @param injectedCode   The code to parse and inject as an external declaration
   *                       at the given location
   * @return The wrap identifier transformation with the given parameters
   */
  public static <T> WrapIdentifier<T> withExternalDeclaration(
      String wrapTarget,
      String wrapResult,
      String wrapExpression,
      InjectionPoint location,
      String injectedCode) {
    return fromExpression(wrapTarget, wrapResult, wrapExpression, new RunPhase<>() {
      @Override
      protected void run(TranslationUnitContext ctx) {
        injectExternalDeclaration(location, injectedCode);
      }
    });
  }
}