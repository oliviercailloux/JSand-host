package io.github.oliviercailloux.jsand.containerized;

import io.github.oliviercailloux.jsand.common.ReadyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SendReady {
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(SendReady.class);

	public static void main(String[] args) throws Exception {
    LOGGER.info("Starting client.");
    HostRegistry hostRegistry = HostRegistry.access();
    ReadyService ready = hostRegistry.readyService();

    ready.ready();
    LOGGER.info("Ending client.");
	}
}
