package io.github.oliviercailloux.jsand;

import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SendHello {
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(SendHello.class);

	public static void main(String[] args) throws Exception {
    LOGGER.info("Hosts: {}.", Files.readString(Path.of("/etc/hosts")));
    LOGGER.info("Starting client.");
    Registry registryJ1 = LocateRegistry.getRegistry("host.docker.internal", Registry.REGISTRY_PORT);
    Hello hello = (Hello)registryJ1.lookup("Hello");

    hello.hello();
    LOGGER.info("Ending client.");
	}
}
