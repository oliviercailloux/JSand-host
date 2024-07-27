package io.github.oliviercailloux.jsand.containerized;

import static com.google.common.base.Preconditions.checkNotNull;

import io.github.oliviercailloux.jsand.common.ClassSenderService;
import io.github.oliviercailloux.jsand.common.JSand;
import io.github.oliviercailloux.jsand.common.ReadyService;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class HostRegistry {
  public static HostRegistry access() throws RemoteException {
    return new HostRegistry(JSand.REGISTRY_HOST, Registry.REGISTRY_PORT);
  }
  
  private final Registry registry;

  public HostRegistry(String host, int port) throws RemoteException {
    checkNotNull(host);
    registry = LocateRegistry.getRegistry(host, port);
  }

  public Registry rmiRegistry() {
    return registry;
  }

  public ReadyService readyService() throws RemoteException, NotBoundException {
    return (ReadyService)registry.lookup(JSand.READY_SERVICE_NAME);
  }

  public ClassSenderService classSenderService() throws RemoteException, NotBoundException {
    return (ClassSenderService)registry.lookup(JSand.CLASS_SENDER_SERVICE_NAME);
  }
}
