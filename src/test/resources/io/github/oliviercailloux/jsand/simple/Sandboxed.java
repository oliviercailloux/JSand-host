package io.github.oliviercailloux.simple;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Sandboxed {
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(Sandboxed.class);

	public static void main(String[] args) throws Exception {
		new Sandboxed().proceed();
	}

	public void proceed() {
		LOGGER.info("Hello World!");
	}
}
