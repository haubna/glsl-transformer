package io.github.douira.glsl_transformer_physics.cst.transform;

import java.util.*;

import org.antlr.v4.runtime.BufferedTokenStream;

import com.github.bsideup.jabel.Desugar;

import io.github.douira.glsl_transformer_physics.*;
import io.github.douira.glsl_transformer_physics.GLSLParser.TranslationUnitContext;
import io.github.douira.glsl_transformer_physics.cst.transform.lifecycle.LifecycleUser;
import io.github.douira.glsl_transformer_physics.cst.traversal.*;
import io.github.douira.glsl_transformer_physics.job_parameter.*;

/**
 * The execution planner finds a valid way of satisfying the root
 * transformation's dependencies. All other transformations and phases are added
 * as dependencies to the root transformation.
 */
public abstract class ExecutionPlanner<T extends JobParameters>
    implements ParameterHolder<T> {
  private Map<T, ExecutionPlan> executionPlanCache = new HashMap<>();
  private Transformation<T> rootTransformation;
  private TranslationUnitContext rootNode;
  private ProxyParseTreeListener proxyListener;
  private T jobParameters;

  /**
   * Creates a new execution planner with no root transformation. One will be
   * created on demand.
   */
  public ExecutionPlanner() {
  }

  /**
   * Creates a new execution planner with a given root transformation.
   * 
   * @param rootTransformation The root transformation to use
   */
  public ExecutionPlanner(Transformation<T> rootTransformation) {
    this.rootTransformation = rootTransformation;
  }

  private class ExecutionPlan {
    List<ExecutionLevel<T>> executionLevels;
    Collection<Transformation<T>> transformations;

    @Desugar
    private static record ExecutionLevel<S extends JobParameters> (
        Collection<TransformationPhase<S>> walkPhases,
        List<TransformationPhase<S>> nonWalkPhases) {
      public ExecutionLevel() {
        this(new ArrayList<>(), new ArrayList<>());
      }
    }

    static class LabeledNode<S extends JobParameters> {
      final LifecycleUser<S> content;
      Collection<LabeledNode<S>> dependencies = new HashSet<>();
      Collection<LabeledNode<S>> dependents = new HashSet<>();

      int executionLevelIndex = Integer.MIN_VALUE;
      boolean dfsFinished = false;

      LabeledNode(LifecycleUser<S> content) {
        this.content = content;
      }

      LabeledNode() {
        content = null;
      }

      /**
       * Creates a two-way connection between this node and a given dependency that's
       * registered on this node. This node is registered as a dependent of the given
       * dependency.
       * 
       * @param dependency The dependency node
       */
      void linkDependency(LabeledNode<S> dependency) {
        dependencies.add(dependency);
        dependency.dependents.add(this);
      }
    }

    @Desugar
    record CollectEntry<S extends JobParameters> (Node<S> nodeToProcess, LabeledNode<S> dependent) {
    }

    @Desugar
    record DFSEntry<S extends JobParameters> (LabeledNode<S> node, boolean enter) {
    }

    /**
     * Calculates the execution plan for the constructed graph of dependencies.
     * 
     * @implNote First it resolves the nested transformation so that the whole
     *           dependency graph can be traversed directly. Then nodes are visited
     *           in toplogically sorted order. Their maximum distance from the first
     *           visited node, an end node, is calculated. Finally the nodes are
     *           sorted into execution levels according to their distance values.
     *           The nodes with the lowest distance are executed first.
     */
    void planExecution() {
      Set<Transformation<T>> transformationSet = new HashSet<>();
      Set<Node<T>> dependenciesProcessed = new HashSet<>();
      Map<Node<T>, LabeledNode<T>> endNodeMap = new HashMap<>();
      Map<LifecycleUser<T>, LabeledNode<T>> contentNodeMap = new HashMap<>();
      Deque<CollectEntry<T>> collectQueue = new LinkedList<>();

      getRootTransformation().setPlanner(ExecutionPlanner.this);
      getRootTransformation().doGraphSetup();
      var rootNode = new LabeledNode<T>();
      collectQueue.add(new CollectEntry<>(new Node<>(getRootTransformation()), rootNode));

      // traverse the tree converting all nodes to labeled nodes and combining
      // dependencies of transformations
      while (!collectQueue.isEmpty()) {
        // node can be: empty (root or end), phase, transformation
        var queueEntry = collectQueue.poll();
        var node = queueEntry.nodeToProcess();
        var content = node.getContent();

        LabeledNode<T> labeledNode;
        if (content == null) {
          // check if a node has already been generated if this is an end node
          labeledNode = Optional
              .ofNullable(endNodeMap.get(node))
              // if there is no content, create a new labeled node for this unlabeled node
              .orElseGet(LabeledNode::new);
        } else {
          // if there content, find the previously created labeled node for it
          labeledNode = Optional
              .ofNullable(contentNodeMap.get(content))
              .orElseGet(() -> {
                var newNode = new LabeledNode<>(content);
                contentNodeMap.put(content, newNode);
                return newNode;
              });
        }

        // tell the node that queued processing this node about this labeled node
        queueEntry.dependent().linkDependency(labeledNode);

        /*
         * Only process dependencies if not done yet for this node.
         * Since this is per node (which are transformation-specific), the same content
         * may be queued for processing multiple times but since labeled nodes are
         * created only once for each content, full duplicate traversal is prevented.
         * Label nodes aren't bound for root nodes, but they aren't depended on by
         * definition. End nodes can have multiple dependents and label nodes for them
         * are de-duplicated using the endNodeMap.
         */
        if (!dependenciesProcessed.contains(node)) {
          dependenciesProcessed.add(node);

          if (Transformation.class.isInstance(content)) {
            // a transformation's dependencies should be dependencies of the end node.
            // use an existing labeled node for this end node if there is one already
            var transformation = (Transformation<T>) content;
            transformationSet.add(transformation);

            var endNode = transformation.getEndDepNode();
            var endLabeledNode = Optional
                .ofNullable(endNodeMap.get(endNode))
                .orElseGet(() -> {
                  var newNode = new LabeledNode<T>();
                  endNodeMap.put(endNode, newNode);
                  return newNode;
                });
            for (var dependency : node.getDependencies()) {
              collectQueue.add(new CollectEntry<>(dependency, endLabeledNode));
            }

            // and the root node as a dependency of the node for the transformation
            // this causes all internal nodes of the transformation to be processed
            collectQueue.add(new CollectEntry<>(transformation.getRootDepNode(), labeledNode));
          } else {
            // queue processing of regular node dependencies
            for (var dependency : node.getDependencies()) {
              collectQueue.add(new CollectEntry<>(dependency, labeledNode));
            }
          }
        }
      }

      // compact the gathered transformations into the final list for fast iteration
      transformations = new ArrayList<>();
      transformations.addAll(transformationSet);

      // check that there are is no undiscovered content and no end nodes are
      // unreachable
      for (var transformation : transformations) {
        for (var contentNode : transformation.getContentNodes()) {
          if (!contentNodeMap.containsKey(contentNode.getContent())) {
            throw new AssertionError(
                "There is a transformation with a node that has content that was not found traversing the dependency graph. The dependency graph is not well-formed.");
          }
        }
        if (!endNodeMap.containsKey(transformation.getEndDepNode())) {
          throw new AssertionError(
              "There is a transformation with an unreachable end node. The dependency graph is not well-formed.");
        }
      }

      Deque<DFSEntry<T>> dfsStack = new LinkedList<>();
      dfsStack.push(new DFSEntry<>(rootNode, true));

      // the topological sort starts at the end node and goes to the root node
      List<LabeledNode<T>> topoSort = new ArrayList<>();

      // generate a topological sort with the first item in the list being an end node
      while (!dfsStack.isEmpty()) {
        if (dfsStack.size() > dependenciesProcessed.size() * 2) {
          throw new AssertionError(
              "The dependency graph could not be satisfied! There is may be a cycle in it or the root and end nodes are messed up. Check for cycles in the graph after construction and after resolving transformations. Also make sure there is a single end and a single (generated) root node.");
        }

        var entry = dfsStack.pop();
        var node = entry.node();
        if (entry.enter()) {
          if (!node.dfsFinished) {
            dfsStack.push(new DFSEntry<>(node, false));
            for (var dependency : node.dependencies) {
              dfsStack.push(new DFSEntry<>(dependency, true));
            }
          }
        } else {
          // nodes will be finished in the order of the topological sort.
          node.dfsFinished = true;
          topoSort.add(node);
        }
      }

      // The minimum execution level of a node is the maximum minimum execution level
      // of all dependencies + 1.
      executionLevels = new ArrayList<>();
      executionLevels.add(new ExecutionLevel<>());
      topoSort.get(0).executionLevelIndex = -1;

      // iterate the nodes topologically sorted in the end to root direction
      for (var node : topoSort) {
        for (var neighbor : node.dependencies) {
          if (neighbor.executionLevelIndex > node.executionLevelIndex) {
            node.executionLevelIndex = neighbor.executionLevelIndex;
          }
        }

        if (TransformationPhase.class.isInstance(node.content)) {
          var phase = (TransformationPhase<T>) node.content;
          if (phase.canWalk()) {
            node.executionLevelIndex++;

            if (executionLevels.size() <= node.executionLevelIndex + 1) {
              executionLevels.add(new ExecutionLevel<>());
            }
            executionLevels.get(node.executionLevelIndex + 1).walkPhases.add(phase);
          } else {
            executionLevels.get(node.executionLevelIndex + 1).nonWalkPhases.add(phase);
          }
        }
      }
    }

    void execute(ExecutionPlanner<T> planner) {
      // refresh each transformation's state before starting the transformation
      for (var transformation : transformations) {
        transformation.setPlanner(planner);
        transformation.initOnce();
        transformation.triggerJobInternal();
        transformation.resetState();
      }

      // iterate the levels in reverse order since level 0 in the execution levels
      // depends on those with higher indexes
      for (var level : executionLevels) {
        proxyListener = new ProxyParseTreeListener();

        // process the concurrently processable walk phases in one tree-walk
        for (var walkPhase : level.walkPhases()) {
          walkPhase.setPlanner(planner);
          walkPhase.initOnce();
          walkPhase.resetState();
          if (walkPhase.checkBeforeWalk(rootNode)) {
            proxyListener.add(walkPhase);
            walkPhase.resetWalkFinishState();
          }
        }
        if (proxyListener.needsWalk()) {
          DynamicParseTreeWalker.walkTree(proxyListener, rootNode);
        }
        for (var walkPhase : level.walkPhases()) {
          walkPhase.runAfterWalk(rootNode);
        }

        // process each non-walking phase individually
        for (var nonWalkPhase : level.nonWalkPhases()) {
          nonWalkPhase.setPlanner(planner);
          nonWalkPhase.initOnce();
          nonWalkPhase.resetState();
          nonWalkPhase.checkBeforeWalk(rootNode);
          nonWalkPhase.runAfterWalk(rootNode);
        }
      }

      proxyListener = null;
    }
  }

  /**
   * Returns this execution planner's parser. How the parser is stored is up to
   * the implementing class.
   * 
   * @return The parser
   */
  public abstract GLSLParser getParser();

  /**
   * Returns the execution planner's lexer.
   * 
   * @return The lexer
   */
  public abstract GLSLLexer getLexer();

  /**
   * Returns the execution planner's current job parameters. This may be null if
   * the caller decides not to pass job parameters. However, a convention to
   * always pass valid job parameters could be established if they are required
   * for transformation phases to function.
   * 
   * @return The job parameters
   */
  @Override
  public T getJobParameters() {
    return jobParameters;
  }

  @Override
  public void setJobParameters(T jobParameters) {
    this.jobParameters = jobParameters;
  }

  /**
   * Runs a function while this transformation manager has the given job
   * parameters set.
   * 
   * @param parameters The job parameters
   * @param run        The function to run while the transformation manager has
   *                   job parameters
   */
  public void withJobParameters(T parameters, Runnable run) {
    this.<Void>withJobParameters(parameters, () -> {
      run.run();
      return null;
    });
  }

  /**
   * Returns the current root node being processed. Access to this method is
   * restricted to classes in this package on purpose. Classes extending
   * {@link TransformationPhase} should not have access to this but rather use it
   * through the provided injection method.
   * 
   * @return The current root node being processed
   */
  public TranslationUnitContext getRootNode() {
    return rootNode;
  }

  /**
   * Registers a single lifecycle user with this execution planner. This can be a
   * transformation or a transformation phase.
   * 
   * Multiple transformations can be added by calling this function multiple times
   * or by adding a single enclosing transformation that includes multiple
   * sub-transformations as concurrent dependencies.
   * 
   * @param rootDependency The node to add as a dependency of the root node
   */
  public void addConcurrent(LifecycleUser<T> rootDependency) {
    getRootTransformation().addRootDependency(rootDependency);
  }

  /**
   * Returns the root transformation that contains all other phases and
   * transformations. This is exposed so that simple structures may be added
   * without requiring nested transformations.
   * 
   * @return The root transformation instance
   */
  public Transformation<T> getRootTransformation() {
    if (rootTransformation == null) {
      rootTransformation = new Transformation<>();
    }
    return rootTransformation;
  }

  /**
   * Sets the root transformation. All dependencies should be accessible through
   * this root transformation. Conditional dependencies can be used in the root
   * transformation if a custom transformation is set with this method.
   * 
   * Note that this doesn't reset the execution plan cache and can cause old
   * execution plans unrelated to the new root transformation to be used when
   * fixed job parameters match. This method is only intended to be used in cases
   * where the root transformation is set once.
   * 
   * @param rootTransformation The root transformation
   */
  public void setRootTransformation(Transformation<T> rootTransformation) {
    this.rootTransformation = rootTransformation;
  }

  /**
   * Called by {@link TransformationPhase} in order to get access to the current
   * proxy listener and tell it to stop and remove the listener that is currently
   * walking.
   */
  void removeCurrentPhaseFromWalk() {
    proxyListener.removeCurrentListener();
  }

  private ExecutionPlan getExecutionPlan() {
    // make sure there is a planned execution plan for the fixed part of the
    // parameters
    var jobParameters = getJobParameters();
    var plan = executionPlanCache.get(jobParameters);
    if (plan == null) {
      plan = new ExecutionPlan(); // gets the job parameters itself during planning
      plan.planExecution();
      executionPlanCache.put(jobParameters, plan);
    }
    return plan;
  }

  /**
   * Triggers execution planning for a given set of fixed job parameters. This can
   * be useful for pre-computing execution plans instead of having them be
   * computed on the fly and cached.
   * 
   * @param parameters The fixed job parameters to compute the execution plan for
   */
  public void planExecutionFor(T parameters) {
    withJobParameters(parameters, this::getExecutionPlan);
  }

  private void execute(TranslationUnitContext ctx) {
    var plan = getExecutionPlan();
    rootNode = ctx;
    plan.execute(this);
    rootNode = null;
  }

  /**
   * Transforms the given parse tree with the registered transformations.
   * 
   * @param ctx         The root node of the parse tree to be transformed
   * @param tokenStream The token stream of the parse tree
   */
  protected void transformTree(TranslationUnitContext ctx, BufferedTokenStream tokenStream) {
    ctx.makeLocalRoot(tokenStream);
    execute(ctx);
  }
}
