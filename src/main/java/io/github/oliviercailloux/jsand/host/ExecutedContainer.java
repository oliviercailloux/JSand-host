package io.github.oliviercailloux.jsand.host;

public record ExecutedContainer(String id, int exitCode, String out, String err) {
}
