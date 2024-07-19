package io.github.oliviercailloux.jsand.host;

import io.github.oliviercailloux.jsand.common.RemoteLoggerService;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

public class RemoteLoggerImpl implements RemoteLoggerService {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(RemoteLoggerImpl.class);

  @Override
  public void log(String originalLoggerName, Level level, Instant timeStamp, String message,
      Object[] argumentArray) {
    LOGGER.makeLoggingEventBuilder(level).log(message + "; original timestamp "
        + timeStamp.toString() + "; original logger name " + originalLoggerName,
        argumentArray);
  }
}
