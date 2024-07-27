package io.github.oliviercailloux.jsand.host;

import static com.google.common.base.Preconditions.checkState;

import io.github.oliviercailloux.jsand.common.ClassSenderService;
import io.github.oliviercailloux.jsand.common.JSand;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Registerer {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(Registerer.class);

  public static final String JAVA_RMI_SERVER_HOSTNAME = "java.rmi.server.hostname";

  public static Registerer create() {
    return new Registerer();
  }

  private Registry registry;

  public Registerer() {
    registry = null;
  }

  public void setHostIp(String ip) {
    System.setProperty(JAVA_RMI_SERVER_HOSTNAME, ip);
  }

  public Registry createRegistry() {
    checkState(System.getProperty(JAVA_RMI_SERVER_HOSTNAME) != null);
    try {
      registry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
    } catch (RemoteException e) {
      throw new IllegalStateException(e);
    }
    LOGGER.info("Created registry: {}", registry);
    return registry;
  }

  public Registry ensureRegistry() {
    try {
      registry = LocateRegistry.getRegistry();
      registry.list();
    } catch (RemoteException e) {
      if (e instanceof java.rmi.ConnectException) {
        LOGGER.debug("Looked for registry, got {}; creating one.", e);
        registry = createRegistry();
      } else {
        throw new IllegalStateException(e);
      }
    }
    return registry;
  }

  public void registerLogger() {
    checkState(registry != null);
    register(JSand.LOGGER_SERVICE_NAME, new RemoteLoggerImpl());
  }

  public ReadyWaiter registerReadyWaiter() {
    checkState(registry != null);
    ReadyWaiter readyWaiter = new ReadyWaiter();
    register(JSand.READY_SERVICE_NAME, readyWaiter);
    return readyWaiter;
  }

  public void registerClassSender(ClassSenderService classSender) {
    checkState(registry != null);
    register(JSand.CLASS_SENDER_SERVICE_NAME, classSender);
  }

  public void register(String name, Remote impl) {
    checkState(registry != null);
    try {
      Remote stub = UnicastRemoteObject.exportObject(impl, 0);
      registry.rebind(name, stub);
    } catch (RemoteException e) {
      throw new IllegalStateException(e);
    }
  }
}
