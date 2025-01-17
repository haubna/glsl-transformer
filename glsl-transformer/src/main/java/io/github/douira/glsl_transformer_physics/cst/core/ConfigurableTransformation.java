package io.github.douira.glsl_transformer_physics.cst.core;

import java.util.*;
import java.util.function.Supplier;

import io.github.douira.glsl_transformer_physics.cst.transform.Transformation;
import io.github.douira.glsl_transformer_physics.job_parameter.JobParameters;

/**
 * Extends a transformation with methods for making it easily configurable. The
 * intended structure for each configurable property of the core transformation
 * is as follows:
 * 
 * - The property foo as a field that holds a supplier that gives the actual
 * value of the property
 * - Public setters foo that accept a supplier, just a value (which is
 * then packaged into a ValueSupplier) or a new cache policy. If they are
 * caching setters they're registered in order to be notified of invalidation
 * events (new job parameters, new transformation job etc).
 * - An internal getter foo that returns the value generated by the supplier in
 * field foo and optionally applies something like an activation function.
 * - A protected getter getFoo method that creates default structures. This
 * method is to be overwritten by extending subclasses. It uses the internal
 * getters like bar and baz.
 * 
 * This class automatically keeps track of all caching suppliers in a list so
 * that it can notify them of cache invalidation events. For this reason, users
 * of the configuration scheme shouldn't set suppliers without using the methods
 * provided here.
 */
public class ConfigurableTransformation<T extends JobParameters> extends Transformation<T> {
  private Collection<CachingSupplier<?>> cachingSuppliers = new LinkedList<>();
  private T lastJobParameters;

  /**
   * Swaps the current supplier of a configuration property with a new one. This
   * also updates the entry in the caching supplier list.
   * 
   * @param <V>             The value type
   * @param currentSupplier The current supplier of the configuration property
   * @param newSupplier     The new supplier to use
   * @return The new current supplier
   */
  protected <V> Supplier<V> swapSupplier(Supplier<V> currentSupplier, Supplier<V> newSupplier) {
    if (currentSupplier != newSupplier) {
      removeSupplier(currentSupplier);
    }
    return addSupplier(newSupplier);
  }

  /**
   * Swaps suppliers but instead of using a new supplier it creates a value
   * supplier that doesn't have to be registered as a caching supplier.
   * 
   * @param <V>             The value type
   * @param currentSupplier The current supplier of the configuration property
   * @param newValue        The value to use in the new value supplier
   * @return The new current supplier
   */
  protected <V> Supplier<V> swapSupplier(Supplier<V> currentSupplier, V newValue) {
    return swapSupplier(currentSupplier, new ValueSupplier<V>(newValue));
  }

  /**
   * Removes a supplier from the caching supplier list.
   * 
   * @param currentSupplier The supplier to remove
   */
  protected void removeSupplier(Supplier<?> currentSupplier) {
    if (currentSupplier == null) {
      throw new IllegalStateException("The current supplier is null!");
    }
    if (CachingSupplier.class.isInstance(currentSupplier)) {
      cachingSuppliers.remove((CachingSupplier<?>) currentSupplier);
    }
  }

  /**
   * Adds a supplier to the caching supplier list if necessary.
   * 
   * @param <V>      The value type
   * @param supplier The supplier to possibly add
   * @return The given supplier
   */
  protected <V> Supplier<V> addSupplier(Supplier<V> supplier) {
    if (supplier == null) {
      throw new IllegalStateException("The new supplier is null!");
    }
    if (CachingSupplier.class.isInstance(supplier)) {
      cachingSuppliers.remove((CachingSupplier<V>) supplier);
    }
    return supplier;
  }

  /**
   * Returns a supplier that uses the same internal supplier as the given current
   * supplier but has a different cache policy. Edge cases that make this
   * operation trivial are specially handled. This may simply cause an unwrapping
   * of the given caching supplier or returning the same supplier as given if the
   * cache policy is already the same.
   * 
   * @param <V>             The value type
   * @param currentSupplier The current supplier to use as the base
   * @param newPolicy       The new cache policy
   * @return A supplier with the same internal supplier but the given cache policy
   */
  protected <V> Supplier<V> swapPolicy(Supplier<V> currentSupplier, CachePolicy newPolicy) {
    if (CachingSupplier.class.isInstance(currentSupplier)) {
      return swapSupplier(currentSupplier, ((CachingSupplier<V>) currentSupplier).getSupplierWithPolicy(newPolicy));
    } else {
      return cachingSupplier(newPolicy, currentSupplier);
    }
  }

  /**
   * Creates a new caching supplier with the given cache policy and registers it.
   * 
   * @param <V>         The value type
   * @param cachePolicy The cache policy to use
   * @param newSupplier The supplier to use
   * @return The new caching supplier
   */
  protected <V> Supplier<V> cachingSupplier(CachePolicy cachePolicy, Supplier<V> newSupplier) {
    return addSupplier(CachingSupplier.of(cachePolicy, newSupplier));
  }

  /**
   * Creates a new value supplier with the given value.
   * 
   * @param <V>      The value type
   * @param newValue The value to use
   * @return The new value supplier
   */
  protected <V> Supplier<V> value(V newValue) {
    return new ValueSupplier<V>(newValue);
  }

  /**
   * Makes sure the cache policy of the returned supplier is
   * {@link CachePolicy#ONCE}. This is not the same as a value supplier since in
   * this case the supplier may never be evaluated.
   * 
   * @param <V>      The value type
   * @param supplier The supplier to use
   * @return The same supplier but with the cache policy set to
   *         {@link CachePolicy#ONCE}
   */
  protected <V> Supplier<V> once(Supplier<V> supplier) {
    return swapPolicy(supplier, CachePolicy.ONCE);
  }

  private void invalidateCachingSuppliers(CachePolicy fulfilledPolicy) {
    for (CachingSupplier<?> supplier : cachingSuppliers) {
      supplier.invalidate(fulfilledPolicy);
    }
  }

  @Override
  protected void triggerJobInternal() {
    var newJobParameters = getJobParameters();
    if (lastJobParameters == null || !lastJobParameters.equals(newJobParameters)) {
      invalidateCachingSuppliers(CachePolicy.ON_FIXED_PARAMETER_CHANGE);
      lastJobParameters = newJobParameters;
    } else {
      invalidateCachingSuppliers(CachePolicy.ON_JOB);
    }
  }
}
