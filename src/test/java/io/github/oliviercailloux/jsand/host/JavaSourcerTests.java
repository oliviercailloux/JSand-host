package io.github.oliviercailloux.jsand.host;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.oliviercailloux.jaris.io.CloseablePathFactory;
import io.github.oliviercailloux.jaris.io.PathUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaSourcerTests {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(JavaSourcerTests.class);

  @Test
  void testJavaSourcer(@TempDir(cleanup = CleanupMode.ON_SUCCESS) Path hostCodeDir) throws Exception {
    JavaSourcer sourcer = JavaSourcer.targetDir(hostCodeDir);
    CloseablePathFactory simple = PathUtils.fromUri(ContainerizerTests.class.getResource("simple/").toURI());
    sourcer.copyCreateDir(simple, "pom.xml");
    JavaSourcer.copyCreateDirTo(simple.resolve("Sandboxed.java"),
        hostCodeDir.resolve("src/main/java/io/github/oliviercailloux/simple/Sandboxed.java"));
    sourcer.copyLogbackConf();

    assertTrue(Files.exists(hostCodeDir.resolve("pom.xml")));
    assertTrue(Files.exists(hostCodeDir.resolve("src/main/java/io/github/oliviercailloux/simple").resolve("Sandboxed.java")));
    assertTrue(Files.exists(hostCodeDir.resolve("src/main/resources/logback.xml")));
  }
}
