package io.github.oliviercailloux.jsand.host;

import java.nio.file.Path;

public class Containerizer {
  private final Path targetDir;
  private String mainClass;

  private Containerizer(Path targetDir) {
    this.targetDir = targetDir;
    this.mainClass = "";
  }
}
