package io.github.oliviercailloux.jsand.containerized.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.google.common.base.VerifyException;
import io.github.oliviercailloux.jsand.common.JSand;
import io.github.oliviercailloux.jsand.common.RemoteLoggerService;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.time.Instant;
import org.slf4j.event.Level;

public class RemoteClientAppender extends AppenderBase<ILoggingEvent> {
  private RemoteLoggerService remoteLogger;

  @Override
  public void start() {
    Registry registryJ1;
    try {
      registryJ1 = LocateRegistry.getRegistry(JSand.REGISTRY_HOST, Registry.REGISTRY_PORT);
      remoteLogger = (RemoteLoggerService)registryJ1.lookup(JSand.LOGGER_SERVICE_NAME);
    } catch (RemoteException|NotBoundException e) {
      throw new RuntimeException(e);
    }
    super.start();
  }

  @Override
  public void append(ILoggingEvent event) {
    long timeStamp = event.getTimeStamp();
    int logbackIntLevel = event.getLevel().toInt();
    String originalLoggerName = event.getLoggerName();
    Level slfLevel;
    switch (logbackIntLevel) {
      case ch.qos.logback.classic.Level.TRACE_INT:
        slfLevel = Level.TRACE;
        break;
      case ch.qos.logback.classic.Level.DEBUG_INT:
        slfLevel = Level.DEBUG;
        break;
      case ch.qos.logback.classic.Level.INFO_INT:
        slfLevel = Level.INFO;
        break;
      case ch.qos.logback.classic.Level.WARN_INT:
        slfLevel = Level.WARN;
        break;
      case ch.qos.logback.classic.Level.ERROR_INT:
        slfLevel = Level.ERROR;
        break;
      default:
        throw new VerifyException("Unexpected logback level: " + logbackIntLevel);
    }

    String message = event.getMessage();
    Object[] argumentArray = event.getArgumentArray();
    try {
      remoteLogger.log(originalLoggerName, slfLevel, Instant.ofEpochMilli(timeStamp), message, argumentArray);
    } catch (RemoteException e) {
      throw new IllegalStateException(e);
    }
  }
}
