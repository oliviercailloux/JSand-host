package io.github.oliviercailloux.jsand.common;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClassSenderService extends Remote {
  public byte[] clazz(String name) throws RemoteException, ClassNotFoundException;
}
