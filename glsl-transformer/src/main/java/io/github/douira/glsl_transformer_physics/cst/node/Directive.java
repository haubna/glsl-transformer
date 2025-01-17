package io.github.douira.glsl_transformer_physics.cst.node;

/**
 * This class models unparsed directives with the # sign. Parsed directives are
 * modelled as regular parse tree nodes (for now).
 */
public class Directive extends StringNode {
  /**
   * The types of directives that can be generated.
   */
  public static enum DirectiveType {
    /**
     * #define
     */
    DEFINE,

    /**
     * #include
     */
    INCLUDE,

    /**
     * #undef
     */
    UNDEF,

    /**
     * #if
     */
    IF,

    /**
     * #ifdef
     */
    IFDEF,

    /**
     * #ifndef
     */
    IFNDEF,

    /**
     * #else
     */
    ELSE,

    /**
     * #elif
     */
    ELIF,

    /**
     * #endif
     */
    ENDIF,

    /**
     * #error
     */
    ERROR,

    /**
     * #line
     */
    LINE,

    /**
     * # (without a name and without content)
     */
    EMPTY
  }

  private final DirectiveType type;

  /**
   * Crates a new directive with the given directive type and content after the
   * directive name. Newlines in the content are escaped with GLSL's line
   * continuation marker "\".
   * 
   * @param type    The type of the directive.
   * @param content The content to put after the directive name
   */
  public Directive(DirectiveType type, String content) {
    super(cleanContent(content));

    if (type == null) {
      throw new IllegalArgumentException("Non-null type must be used to construct a directive!");
    }

    if (type == DirectiveType.EMPTY) {
      throw new IllegalArgumentException("The EMPTY type may only be used with the corresponding constructor!");
    }

    this.type = type;
  }

  /**
   * Creates a new directive of the empty type. It receives no content because
   * it's not allowed to have content as per the GLSL spec.
   */
  public Directive() {
    super("");
    this.type = DirectiveType.EMPTY;
  }

  private static String cleanContent(String content) {
    return content == null ? null : content.trim().replace("\n", "\\\n");
  }

  @Override
  protected String getPrinted() {
    if (type == DirectiveType.EMPTY) {
      return "#\n";
    }

    return ("#"
        + type.name().toLowerCase() // type can never be null here
        + " " + getContent()).trim() + "\n";
  }
}
