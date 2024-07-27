package io.github.oliviercailloux.jsand.containerized;

import io.github.oliviercailloux.jsand.common.ClassSenderService;
import java.rmi.RemoteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClassFetcher extends ClassLoader {
@SuppressWarnings("unused")
private static final Logger LOGGER = LoggerFactory.getLogger(ClassFetcher.class);

  private ClassSenderService sender;

  public ClassFetcher(ClassSenderService sender) {
    this.sender = sender;
  }


  @Override
  public Class<?> findClass(String name) throws ClassNotFoundException {
    byte[] classBytes;
    LOGGER.info("Fetching class {}.", name);
    try {
      classBytes = sender.clazz(name);
    } catch (RemoteException e) {
      throw new ClassNotFoundException("Could not load class " + name, e);
    }
    return defineClass(name, classBytes, 0, classBytes.length);
  }
}
