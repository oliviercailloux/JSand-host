package io.github.oliviercailloux.jsand;

import io.github.oliviercailloux.jsand.common.ReadyService;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SendHello {
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(SendHello.class);

	public static void main(String[] args) throws Exception {
    LOGGER.info("Starting client.");
    Registry registryJ1 = LocateRegistry.getRegistry("host.docker.internal", Registry.REGISTRY_PORT);
    ReadyService hello = (ReadyService)registryJ1.lookup("Hello");

    hello.ready();
    LOGGER.info("Ending client.");
	}
}
