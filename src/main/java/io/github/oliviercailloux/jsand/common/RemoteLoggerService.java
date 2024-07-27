package io.github.oliviercailloux.jsand.common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.time.Instant;
import org.slf4j.event.Level;

public interface RemoteLoggerService extends Remote {
  void log(String loggerName, Level level, Instant timeStamp, String message,
      Object[] argumentArray) throws RemoteException;
}
