package io.github.oliviercailloux.jsand.host;

import io.github.oliviercailloux.jsand.common.ReadyService;
import java.util.concurrent.CountDownLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReadyWaiter implements ReadyService {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(ReadyWaiter.class);

  private final CountDownLatch latch;

  public ReadyWaiter() {
    latch = new CountDownLatch(1);
  }

  @Override
  public void ready() {
    LOGGER.info("Ready.");
    latch.countDown();
  }

  public CountDownLatch latch() {
    return latch;
  }
}
