package io.github.douira.glsl_transformer_physics.cst.print;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.*;

/**
 * The caching interval set is a regular interval set
 * {@link org.antlr.v4.runtime.misc.IntervalSet} but the @link
 * org.antlr.v4.runtime.misc.IntervalSet#contains(int)} method does caching of
 * queries. Repeatedly requesting the same query is a common operation and
 * therefore caching it like this can be helpful.
 */
public class CachingIntervalSet extends IntervalSet {
  private Interval lastIntervalHit;

  /**
   * {@inheritDoc}
   * 
   * Copied from ANTLR's
   * {@link org.antlr.v4.runtime.misc.IntervalSet#contains(int)} but with an
   * addition of caching. The cache size is 1. This method will test the cached
   * interval if it hasn't been invalidated since.
   */
  @Override
  public boolean contains(int el) {
    if (lastIntervalHit != null && lastIntervalHit.a <= el && lastIntervalHit.b >= el) {
      return true;
    }

    int n = intervals.size();
    int l = 0;
    int r = n - 1;
    // Binary search for the element in the (sorted,
    // disjoint) array of intervals.
    while (l <= r) {
      int m = (l + r) / 2;
      Interval I = intervals.get(m);
      int a = I.a;
      int b = I.b;
      if (b < el) {
        l = m + 1;
      } else if (a > el) {
        r = m - 1;
      } else { // now: el >= a && el <= b
        lastIntervalHit = I;
        return true;
      }
    }
    return false;
  }

  /**
   * Add cache invalidation.
   */
  @Override
  public void clear() {
    invalidateCache();
    super.clear();
  }

  /**
   * Makes this method accessible and add cache invalidation.
   */
  @Override
  public void add(Interval interval) {
    invalidateCache();
    super.add(interval);
  }

  private void invalidateCache() {
    lastIntervalHit = null;
  }

  /**
   * Checks if the given token is covered by this set if it's being used as an
   * omission set. Tokens that are included in one of this interval set's sets and
   * aren't hidden are not printed.
   * 
   * @param token The token to check
   * @return {@code true} if the token should be printed
   */
  public boolean tokenNotOmitted(Token token) {
    return token.getChannel() != Token.DEFAULT_CHANNEL || !contains(token.getTokenIndex());
  }
}
