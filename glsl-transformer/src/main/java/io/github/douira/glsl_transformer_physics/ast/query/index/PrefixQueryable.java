package io.github.douira.glsl_transformer_physics.ast.query.index;

import java.util.Set;
import java.util.stream.Stream;

public interface PrefixQueryable<E> {
  Stream<Set<E>> prefixQuery(String prefix);

  default Stream<E> prefixQueryFlat(String prefix) {
    return prefixQuery(prefix).flatMap(Set::stream);
  }
}
