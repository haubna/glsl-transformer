package me.douira.glsl_transformer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

public class App {
  private static enum Input {
    TINY("/tiny.glsl"), DIRECTIVE_TEST("/directiveTest.glsl"), SHADER("/shader.glsl"),
    KAPPA("/unlicensed/composite3.glsl"), BENCHMARK1("/unlicensed/benchmark1.glsl"),
    BENCHMARK2("/unlicensed/benchmark2.glsl"), TEST("/unlicensed/test.glsl");

    String path;

    Input(String path) {
      this.path = path;
    }
  }

  private static Set<String> bannedFilenameFragments = Set.of("ray", "preprocessor");

  public static void main(String[] args) throws IOException, URISyntaxException {
    // processInput(Input.TEST);
    processDirectory("/glslang-test");
  }

  private static void processDirectory(String path) throws IOException, URISyntaxException {
    Files.walk(Paths.get(App.class.getResource(path).toURI())).filter(file -> {
      var fileName = file.getFileName().toString().toLowerCase();
      return !bannedFilenameFragments.stream().anyMatch(banned -> fileName.contains(banned));
    }).map(Path::toFile).forEach(file -> {
      if (file.isDirectory()) {
        return;
      }
      System.out.println("Processing file " + file.getName());
      try {
        processFile(file);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      System.out.println();
    });
  }

  private static void processInput(Input input) throws IOException {
    processFile(App.class.getResource(input.path).openStream());
  }

  private static void processFile(File file) throws IOException {
    processFile(new FileInputStream(file));
  }

  private static void processFile(InputStream inputStream) throws IOException {
    CharStream input;

    input = CharStreams.fromStream(inputStream);
    inputStream.close();

    var lexer = new GLSLLexer(input);
    var commonTokenStream = new CommonTokenStream(lexer);

    var startNanos = System.nanoTime();
    var parser = new GLSLParser(commonTokenStream);
    var translationUnitContext = parser.translationUnit();
    System.out.println("parsing took " + (System.nanoTime() - startNanos) / 1e6 + " ms.");

    // new DebugVisitor().visit(translationUnitContext);
    // System.out.println(translationUnitContext.toInfoString(parser));

    // before any edits
    // System.out.println(PrintVisitor.printTree(commonTokenStream,
    // translationUnitContext));

    var editContext = new EditContext();
    translationUnitContext.children.add(2, new StringNode("\nexample declaration;"));
    ParseTreeWalker.DEFAULT.walk(new TransformationVisitor(editContext), translationUnitContext);
    editContext.finishEditing();

    startNanos = System.nanoTime();
    var printResult = PrintVisitor.printTree(commonTokenStream, translationUnitContext, editContext);
    System.out.println("printing took " + (System.nanoTime() - startNanos) / 1e6 + " ms.");

    // after edits
    // System.out.println(printResult);

    // var tokens = commonTokenStream.getTokens();
    // for (var token : tokens) {
    // System.out.println(token);
    // }
    inputStream.close();
  }
}