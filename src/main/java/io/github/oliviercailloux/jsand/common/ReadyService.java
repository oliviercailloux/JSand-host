package io.github.oliviercailloux.jsand.common;

import java.rmi.Remote;

public interface ReadyService extends Remote {
  public void ready() throws java.rmi.RemoteException;
}
