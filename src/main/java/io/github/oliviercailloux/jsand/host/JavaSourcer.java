package io.github.oliviercailloux.jsand.host;

import io.github.oliviercailloux.jaris.io.CloseablePath;
import io.github.oliviercailloux.jaris.io.CloseablePathFactory;
import io.github.oliviercailloux.jaris.io.PathUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JavaSourcer {
  public static void copyCreateDir(CloseablePathFactory sourceDir, String relative, Path target) throws IOException {
    try (CloseablePath p = sourceDir.path()) {
      copyCreateDir(p, p.getFileSystem().getPath(relative), target);
    }
  }

  public static void copyCreateDir(Path sourceDir, Path relative, Path target) throws IOException {
    Files.createDirectories(target.getParent());
    Files.copy(sourceDir.resolve(relative), target);
  }

  public static JavaSourcer targetDir(Path targetDir) {
    return new JavaSourcer(targetDir);
  }
  
  private final Path targetDir;

  private JavaSourcer(Path targetDir) {
    this.targetDir = targetDir;
  }

  public void copyCreateDir(CloseablePathFactory sourceDir, String relative) throws IOException {
    try (CloseablePath p = sourceDir.path()) {
      copyCreateDir(p, p.getFileSystem().getPath(relative));
    }
  }

  public void copyCreateDir(Path sourceDir, String relative, Path target) throws IOException {
    copyCreateDir(sourceDir, sourceDir.getFileSystem().getPath(relative), target);
  }

  public void copyCreateDir(Path sourceDir, Path relative) throws IOException {
    Path target = PathUtils.resolve(targetDir, relative);
    copyCreateDir(sourceDir, relative, target);
  }
}
