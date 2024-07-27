package io.github.oliviercailloux.jsand.host;

import static com.google.common.base.Verify.verify;

import com.google.common.collect.Iterables;
import io.github.oliviercailloux.jaris.io.CloseablePath;
import io.github.oliviercailloux.jaris.io.CloseablePathFactory;
import io.github.oliviercailloux.jaris.io.PathUtils;
import io.github.oliviercailloux.jaris.xml.DomHelper;
import io.github.oliviercailloux.jaris.xml.XmlName;
import io.github.oliviercailloux.jsand.containerized.logback.RemoteClientAppender;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class JavaSourcer {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(JavaSourcer.class);
  
  public static void copyCreateDirTo(CloseablePathFactory source, Path target) throws IOException {
    try (CloseablePath p = source.path()) {
      copyCreateDirTo(p.delegate(), target);
    }
  }

  public static void copyCreateDirTo(Path source, Path target) throws IOException {
    Files.createDirectories(target.getParent());
    LOGGER.info("Copying {} to {}.", source, target);
    Files.copy(source, target);
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
      copyCreateDir(p.delegate(), relative);
    }
  }

  public void copyCreateDir(Path sourceDir, String relative) throws IOException {
    Path relativePath = sourceDir.getFileSystem().getPath(relative);
    Path target = PathUtils.resolve(targetDir, relativePath);
    copyCreateDirTo(sourceDir.resolve(relativePath), target);
  }

  public void copyLogbackConf() throws IOException {
    CloseablePathFactory conf = PathUtils.fromResource(getClass(), "logback containerized configuration.xml");
    copyCreateDirTo(conf, targetDir.resolve("src/main/resources/logback.xml"));
  }
}
