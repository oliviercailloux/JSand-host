package io.github.oliviercailloux.jsand.host;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.io.Resources;
import io.github.oliviercailloux.jsand.common.ClassSenderService;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

public class ClassSender implements ClassSenderService {

  public static ClassSenderService create(ClassLoader loader) {
    return new ClassSender(loader);
  }

  public static ClassSenderService create(Path root) {
    URL u;
    try {
      u = root.toUri().toURL();
    } catch (MalformedURLException e) {
      // TODO generalize using https://github.com/marschall/path-classloader
      throw new IllegalArgumentException("Unknown protocol.", e);
    }
    ClassLoader loader = new URLClassLoader("host loader", new URL[] {u}, null);
    return new ClassSender(loader);
  }

  private final ClassLoader loader;

  private ClassSender(ClassLoader loader) {
    this.loader = checkNotNull(loader);
  }

  @Override
  public byte[] clazz(String name) throws ClassNotFoundException {
    /* TODO */
    String binaryName = name.replace('.', '/') + ".class";
    URL res = loader.getResource(binaryName);
    if (res == null) {
      throw new ClassNotFoundException("Could not find resource " + binaryName);
    }
    try {
      return Resources.toByteArray(res);
    } catch (IOException e) {
      throw new ClassNotFoundException("Error while reading resource " + res, e);
    }
  }
}
