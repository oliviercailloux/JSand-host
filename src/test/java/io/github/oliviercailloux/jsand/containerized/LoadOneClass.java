package io.github.oliviercailloux.jsand.containerized;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import io.github.oliviercailloux.jsand.common.ClassSenderService;
import java.lang.reflect.Constructor;
import java.util.function.IntSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoadOneClass {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(LoadOneClass.class);

  public static void main(String[] args) throws Exception {
    HostRegistry hostRegistry = HostRegistry.access();
    ClassSenderService classSenderService = hostRegistry.classSenderService();
    ClassLoader l = new ClassFetcher(classSenderService);

    boolean caught = false;
    try {
      l.loadClass("io.github.oliviercailloux.jsand.containerized.DoesNotExist");
    } catch (ClassNotFoundException e) {
      caught=true;
      LOGGER.info("Caught: {}.", e);
    }

    String className = "io.github.oliviercailloux.jsand.containerized.UniverseAndEverythingIntSupplier";
    Class<?> clazz = l.loadClass(className);
    ImmutableSet<Constructor<?>> constrs = ImmutableSet.copyOf(clazz.getDeclaredConstructors());
    Constructor<?> constr = Iterables.getOnlyElement(constrs);
    IntSupplier impl = (IntSupplier) constr.newInstance();
    LOGGER.info("The answer: {}.", impl.getAsInt());
    int status;
    if (caught && impl.getAsInt() == 42){
      status = 0;
    }  else {
      status = 1;
    }
    System.exit(status);
  }
}
