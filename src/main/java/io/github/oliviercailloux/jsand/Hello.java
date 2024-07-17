package io.github.oliviercailloux.jsand;

import java.rmi.Remote;

public interface Hello extends Remote {
  public void hello() throws java.rmi.RemoteException;
}
