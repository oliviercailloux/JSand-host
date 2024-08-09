package io.github.oliviercailloux.jsand.host;

import com.google.common.base.VerifyException;
import com.google.common.io.Resources;
import io.github.oliviercailloux.jsand.common.ClassSenderService;
import java.io.IOException;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClassSenderImpl implements ClassSenderService {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(ClassSenderImpl.class);

  private final byte[] bytes;

  public ClassSenderImpl() {
    URL classUrl =
        getClass().getResource("../containerized/UniverseAndEverythingIntSupplier.class");
    try {
      bytes = Resources.toByteArray(classUrl);
    } catch (IOException e) {
      throw new VerifyException(e);
    }
  }

  @Override
  public byte[] clazz(String name) throws ClassNotFoundException {
    if (!name
        .equals("io.github.oliviercailloux.jsand.containerized.UniverseAndEverythingIntSupplier")) {
      throw new ClassNotFoundException("Unknown class: " + name);
    }
    LOGGER.info("Asked for class {}, returning {} bytes.", name, bytes.length);
    return bytes;
  }
}
