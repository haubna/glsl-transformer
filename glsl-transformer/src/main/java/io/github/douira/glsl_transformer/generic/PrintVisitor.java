package io.github.douira.glsl_transformer.generic;

import java.util.LinkedList;

import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;

/**
 * The print visitor visits the parse tree and reprints it while preserving the
 * position and content of hidden tokens. This means it preserves all newlines
 * even if the parsed nodes that contains them was removed. This is useful
 * because it doesn't necessarily change the line numbers during patching.
 * 
 * An edit context tells the print visitor what changes have been made to the
 * original parse tree and from which alternative token streams it has to read
 * when it prints newly inserted nodes.
 */
public class PrintVisitor extends AbstractParseTreeVisitor<Void> {
  /**
   * The attributed interval is used to attribute intervals in the print list to
   * local root nodes. Which root node an interval belongs to is used during
   * printing to read from the token stream for which the interval was
   * constructed.
   */
  private record AttributedInterval(ParseTree localRoot, Interval interval) {
  }

  private final LinkedList<AttributedInterval> printIntervals = new LinkedList<>();
  private Interval cachedInterval;
  private ParseTree currentRoot;

  private PrintVisitor() {
  }

  private static boolean inInterval(Interval interval, int el) {
    return interval.a <= el && interval.b >= el;
  }

  /**
   * Prints the given parse tree that references the given token stream. An edit
   * context is constructed internally. Use this method only if you're not doing
   * any patching.
   * 
   * @see #printTree(BufferedTokenStream, ParseTree, EditContext)
   * @param rootTokenStream The token stream for the parse tree
   * @param tree            The parse tree to print
   * @return The printed parse tree
   */
  public static String printTree(BufferedTokenStream rootTokenStream, ParseTree tree) {
    return printTree(rootTokenStream, tree, new EditContext(tree, rootTokenStream));
  }

  /**
   * Prints the given parse tree that references the given token stream.
   * Additionally an edit context must be passed that contains the tokens streams
   * and locations of added or removed nodes. This edit context will have been
   * generated by a transformation such as when the method
   * {@link io.github.douira.glsl_transformer.transform.PhaseCollector#transformTree(io.github.douira.glsl_transformer.GLSLParser.TranslationUnitContext, BufferedTokenStream)}
   * is used.
   * 
   * @param rootTokenStream The token stream for the parse tree
   * @param tree            The parse tree to print
   * @param editContext     The edit context generated by transformations
   *                        performed on the tree
   * @return The printed parse tree that includes the results of the
   *         transformations
   */
  public static String printTree(BufferedTokenStream rootTokenStream, ParseTree tree, EditContext editContext) {
    return new PrintVisitor().visitAndJoin(rootTokenStream, tree, Interval.of(0, rootTokenStream.size() - 1),
        editContext);
  }

  /**
   * This is the lowest level printing method that should be used publicly. It
   * takes a parse tree and a token stream and prints it. Additionally an interval
   * is given that determines which tokens of the string should be printed.
   * 
   * @see #printTree(BufferedTokenStream, ParseTree, EditContext)
   * @param rootTokenStream The token stream for the parse tree
   * @param rootNode        The parse tree to print
   * @param bounds          An token index interval of the tokens that should be
   *                        included in the printed string
   * @param editContext     The edit context generated by transformations
   * @return The printed parse tree that is within the bounds and contains the
   *         results of the transformations applied to the tree
   */
  public String visitAndJoin(BufferedTokenStream rootTokenStream, ParseTree rootNode, Interval bounds,
      EditContext editContext) {
    // add the tokens before the root node too
    var rootInterval = rootNode.getSourceInterval();
    addInterval(bounds.a, rootInterval.a - 1);

    // visit the whole tree and accumulate tokens and intervals
    currentRoot = rootNode;
    visit(rootNode);

    // and also the tokens after the root node
    addInterval(rootInterval.b + 1, bounds.b);

    // convert the list of tokens and intervals into just tokens,
    // and then into their strings
    var builder = new StringBuilder(512); // guessing
    for (var attributedInterval : printIntervals) {
      var interval = attributedInterval.interval();
      var localRootData = editContext.getLocalRootData(attributedInterval.localRoot());
      var omissionSet = localRootData.omissionSet();

      for (var token : localRootData.tokenStream().getTokens(interval.a, interval.b)) {
        // don't print EOF, only print the tokens in side the printing bounds,
        // but always allow inserted nodes,
        // if an edit context is given, only print if allowed by it
        var tokenIndex = token.getTokenIndex();
        if (token.getType() != Lexer.EOF && (tokenIndex == -1
            || inInterval(bounds, tokenIndex) && (omissionSet == null || omissionSet.isTokenAllowed(token)))) {
          builder.append(token.getText());
          // builder.append(',');
        }
      }
    }
    return builder.toString();
  }

  /**
   * Adds an interval to the current list of intervals to print. The interval is
   * attributed to the current local root node.
   * 
   * @param a The left bound of the interval
   * @param b The right bound of the interval
   */
  private void addInterval(int a, int b) {
    // doing this check here saves an object construction if the interval is invalid
    // (and it often is invalid)
    if (a > b || a < 0 || b < 0) {
      return;
    }

    // cache a single interval since often the same interval is added repeatedly
    Interval interval;
    if (cachedInterval != null && a == cachedInterval.a && b == cachedInterval.b) {
      interval = cachedInterval;
    } else {
      interval = Interval.of(a, b);
      cachedInterval = interval;
    }

    addInterval(interval);
  }

  /**
   * Adds an interval to the current list of intervals to print. The interval is
   * attributed to the current local root node. If possible, this will join the
   * interval with the last added interval in order to reduce memory usage and
   * overhead during printing. Oftentimes the intervals are adjacent and share the
   * same local root, which makes them joinable. If an interval is empty, nothing
   * needs to be done with it since it can't contribute any tokens for printing.
   * 
   * @param newInterval The interval to add to the print list
   */
  private void addInterval(Interval newInterval) {
    if (newInterval.length() == 0) {
      return;
    }

    // join the given interval onto the last interval if possible without holes
    if (!printIntervals.isEmpty()) {
      var last = printIntervals.getLast();
      var lastInterval = last.interval();
      if (currentRoot == last.localRoot()
          && (!lastInterval.disjoint(newInterval) || lastInterval.adjacent(newInterval))) {
        if (lastInterval.properlyContains(newInterval)) {
          return;
        }

        printIntervals.removeLast();
        newInterval = lastInterval.union(newInterval);
      }
    }

    printIntervals.add(new AttributedInterval(currentRoot, newInterval));
  }

  /**
   * {@inheritDoc}
   * 
   * The visitor method that the print visitor overrides in order to collect each
   * node's intervals. The current local root is updated when a local root node is
   * found. Any created intervals for printing are attributed to the current local
   * root in order to know which token stream the interval's indexes refer to.
   * 
   * @implNote intervals that cover tokens that are not part of children (which
   *           are only hidden tokens like whitespace because terminal nodes are
   *           children too) are moved after inserted nodes, which are local
   *           roots, through
   *           this process. this happens because fetchNext is not updated for
   *           local roots
   *           which causes the next non-local-root child (or at the after end of
   *           the child
   *           list) to add the whole interval covering the child after the child
   *           itself has
   *           been visited.
   */
  @Override
  public Void visitChildren(RuleNode node) {
    final var context = (ExtendedContext) node.getRuleContext();
    final var superInterval = context.getSourceInterval();

    var fetchNext = superInterval.a;
    if (context.children != null) {
      for (var child : context.children) {
        var isLocalRoot = child.getParent() == null;

        ParseTree previousRoot = null;
        Interval childInterval = null;
        if (isLocalRoot) {
          // set as new current root
          previousRoot = currentRoot;
          currentRoot = child;
        } else {
          childInterval = child.getSourceInterval();
          addInterval(fetchNext, childInterval.a - 1);
        }

        child.accept(this);

        if (isLocalRoot) {
          // switch root back
          currentRoot = previousRoot;
        } else {
          fetchNext = childInterval.b + 1;
        }
      }
    }

    addInterval(fetchNext, superInterval.b);
    return null;
  }

  @Override
  public Void visitTerminal(TerminalNode node) {
    // empty terminal nodes have an empty source interval and have no effect
    addInterval(node.getSourceInterval());
    return null;
  }
}