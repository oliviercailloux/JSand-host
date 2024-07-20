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
      copyCreateDir(p.delegate(), p.getFileSystem().getPath(relative));
    }
  }

  public void copyCreateDir(Path sourceDir, Path relative) throws IOException {
    Path target = PathUtils.resolve(targetDir, relative);
    copyCreateDirTo(sourceDir.resolve(relative), target);
  }

  public void copyLogbackSetup() throws IOException {
    Class<?> clz = RemoteClientAppender.class;
    CloseablePathFactory cls = PathUtils.fromResource(clz, clz.getSimpleName() + ".java");
    copyCreateDirTo(cls, targetDir.resolve("src/main/resources/logback.xml"));
    
    
    CloseablePathFactory conf = PathUtils.fromResource(getClass(), "logback containerized configuration.xml");
    
    Document doc = DomHelper.domHelper().asDocument(conf);
    Element confEl = doc.getDocumentElement();
    verify(confEl.getTagName().equals("configuration"));
    Element appenderEl = Iterables.getOnlyElement(DomHelper.toElements(confEl.getElementsByTagName("appender")));
    verify(appenderEl.getTagName().equals("appender"));
    String classAt = DomHelper.getAttribute(appenderEl, XmlName.localName("class"));
    verify(classAt.equals(clz.getName()));
    
    copyCreateDirTo(conf, targetDir.resolve("src/main/resources/logback.xml"));
  }
}
