package io.github.oliviercailloux.jsand;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelloImpl implements Hello {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(HelloImpl.class);

  private final CountDownLatch latch;

  public HelloImpl() {
    latch = new CountDownLatch(1);
  }

  @Override
  public void hello() {
    LOGGER.info("Current time: {}.", Instant.now());
    latch.countDown();
  }

  public CountDownLatch latch() {
    return latch;
  }
}
