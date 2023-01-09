package io.github.douira.glsl_transformer_physics.job_parameter;

import org.antlr.v4.runtime.*;

import io.github.douira.glsl_transformer_physics.basic.Transformer;

public interface ParameterizedTransformer<T, V> extends Transformer<V>, ParameterHolder<T> {
  default V transform(V str, T parameters) throws RecognitionException {
    return withJobParameters(parameters, () -> transform(str));
  }
}
