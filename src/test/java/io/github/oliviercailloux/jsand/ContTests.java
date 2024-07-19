package io.github.oliviercailloux.jsand;

import static com.google.common.base.Verify.verify;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Network;
import com.github.dockerjava.api.model.Network.Ipam.Config;
import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.io.MoreFiles;
import io.github.oliviercailloux.jaris.io.CloseablePath;
import io.github.oliviercailloux.jaris.io.PathUtils;
import io.github.oliviercailloux.jsand.common.ReadyService;
import io.github.oliviercailloux.jsand.common.RemoteLoggerService;
import io.github.oliviercailloux.jsand.host.DockerHelperDraft;
import io.github.oliviercailloux.jsand.host.ExecutedContainer;
import io.github.oliviercailloux.jsand.host.ReadyServiceImpl;
import io.github.oliviercailloux.jsand.host.RemoteLoggerImpl;
import io.github.oliviercailloux.jsand.host.DockerHelperDraft.ConnectivityMode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContTests {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(ContTests.class);

  private static final String IMAGE_NAME = "ghcr.io/oliviercailloux/djm-conf";
  private static final String COMPILE_CONTAINER_NAME = "JSandTestCompile";
  private static final String RUN_CONTAINER_NAME = "JSandTestRun";
  private static final String NETWORK_NAME = "JSand";
  private static final String NETWORK_NAME_ISOLATE = "JSandIsolate";
  private static final String CONTAINER_CODE_DIR = "/code/";

  private void copyCreateDir(Path sourceDir, String relative, Path targetDir) throws IOException {
    copyCreateDir(sourceDir, sourceDir.getFileSystem().getPath(relative), targetDir);
  }

  private void copyCreateDir(Path sourceDir, Path relative, Path targetDir) throws IOException {
    Path target = PathUtils.resolve(targetDir, relative);
    Files.createDirectories(target.getParent());
    Files.copy(sourceDir.resolve(relative), target);
  }

  @Test
  void testLog(@TempDir Path hostCodeDir) throws Exception {
    try (CloseablePath simple = PathUtils.fromUri(ContTests.class.getResource("simple/").toURI())) {
      copyCreateDir(simple, "pom.xml", hostCodeDir);
      copyCreateDir(simple, "Sandboxed.java",
          hostCodeDir.resolve("src/main/java/io/github/oliviercailloux/simple/"));
      copyCreateDir(simple, "logback.xml", hostCodeDir.resolve("src/main/resources/"));
    }

    DockerHelperDraft dockerHelper = DockerHelperDraft.create();
    DockerClient dockerClient = dockerHelper.client();

    Optional<Network> extNet = dockerHelper.network(NETWORK_NAME);
    if (extNet.isEmpty()) {
      dockerHelper.createNetwork(NETWORK_NAME);
    }

    dockerHelper.container(COMPILE_CONTAINER_NAME)
        .ifPresent(container -> dockerClient.removeContainerCmd(container.getId()).exec());

    ImmutableMap<String, String> roBinds = ImmutableMap.of(hostCodeDir.toString(),
        CONTAINER_CODE_DIR, "/home/olivier/.m2/repository/", "/Maven repository/");
    ImmutableList<String> compileCmd = ImmutableList.of("mvn", "-B", "compile");
    String compileContainerId = dockerHelper.createAndExec(IMAGE_NAME, COMPILE_CONTAINER_NAME,
        CONTAINER_CODE_DIR, NETWORK_NAME, roBinds, compileCmd);

    String compiledImageId = dockerClient.commitCmd(compileContainerId).exec();

    Optional<Network> extIsolNet = dockerHelper.network(NETWORK_NAME_ISOLATE);
    if (extIsolNet.isEmpty()) {
      dockerHelper.createNetwork(NETWORK_NAME_ISOLATE, ConnectivityMode.INTERNAL);
    }

    dockerHelper.container(RUN_CONTAINER_NAME)
        .ifPresent(container -> dockerClient.removeContainerCmd(container.getId()).exec());

    ImmutableList<String> runCmd = ImmutableList.of("mvn", "-B", "-Dexec.executable=java",
        "-Dexec.mainClass=io.github.oliviercailloux.simple.Sandboxed",
        "org.codehaus.mojo:exec-maven-plugin:3.3.0:exec");
    String runContainerId = dockerHelper.createAndExec(compiledImageId, RUN_CONTAINER_NAME,
        CONTAINER_CODE_DIR, NETWORK_NAME_ISOLATE, roBinds, runCmd);
  }

  @Test
  void testHello(@TempDir Path hostCodeDir) throws Exception {
    try (CloseablePath simple = PathUtils.fromUri(ContTests.class.getResource("simple/").toURI())) {
      copyCreateDir(simple, "pom.xml", hostCodeDir);
      copyCreateDir(Path.of(""), "src/main/java/io/github/oliviercailloux/jsand/Hello.java",
          hostCodeDir);
      copyCreateDir(Path.of(""), "src/main/java/io/github/oliviercailloux/jsand/SendHello.java",
          hostCodeDir);
          copyCreateDir(Path.of(""), "src/main/java/io/github/oliviercailloux/rmi/RemoteLoggerService.java",
          hostCodeDir);
      copyCreateDir(Path.of(""), "src/main/java/io/github/oliviercailloux/rmi/logback/RemoteClientAppender.java",
          hostCodeDir);
      Path target = hostCodeDir.resolve("src/main/resources/logback.xml");
      Files.createDirectories(target.getParent());
      Files.copy(simple.resolve("logback-conf.xml"), target);
      }

    DockerHelperDraft dockerHelper = DockerHelperDraft.create();
    DockerClient dockerClient = dockerHelper.client();

    Optional<Network> extNet = dockerHelper.network(NETWORK_NAME);
    if (extNet.isEmpty()) {
      dockerHelper.createNetwork(NETWORK_NAME);
    }

    dockerHelper.container(COMPILE_CONTAINER_NAME)
        .ifPresent(container -> dockerClient.removeContainerCmd(container.getId()).exec());

    if (dockerHelper.network(NETWORK_NAME_ISOLATE).isEmpty()) {
      dockerHelper.createNetwork(NETWORK_NAME_ISOLATE, ConnectivityMode.INTERNAL);
    }
    Network extIsolNet = dockerHelper.network(NETWORK_NAME_ISOLATE).orElseThrow(VerifyException::new);
    List<Config> configs = extIsolNet.getIpam().getConfig();
    verify(configs.size() == 1);
    Config config = Iterables.getOnlyElement(configs);
    String gateway = config.getGateway();
    verify(gateway.equals("172.19.0.1"));

    System.setProperty("java.rmi.server.hostname", gateway);
    Registry registryJ1 = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
    LOGGER.info("Registry: {}", registryJ1);
    ReadyServiceImpl hello = new ReadyServiceImpl();
    ReadyService stub = (ReadyService) UnicastRemoteObject.exportObject(hello, 0);
    registryJ1.rebind("Hello", stub);
    RemoteLoggerService remoteLogger = (RemoteLoggerService) UnicastRemoteObject.exportObject(new RemoteLoggerImpl(), 0);
    registryJ1.rebind(RemoteLoggerService.SERVICE_NAME, remoteLogger);

    ImmutableMap<String, String> roBinds = ImmutableMap.of(hostCodeDir.toString(),
        CONTAINER_CODE_DIR, "/home/olivier/.m2/repository/", "/Maven repository/");
    ImmutableList<String> compileCmd = ImmutableList.of("mvn", "-B", "compile");
    String compileContainerId = dockerHelper.createAndExec(IMAGE_NAME, COMPILE_CONTAINER_NAME,
        CONTAINER_CODE_DIR, NETWORK_NAME, roBinds, compileCmd);

    String compiledImageId = dockerClient.commitCmd(compileContainerId).exec();

    dockerHelper.container(RUN_CONTAINER_NAME)
        .ifPresent(container -> dockerClient.removeContainerCmd(container.getId()).exec());

    ImmutableList<String> runCmd = ImmutableList.of("mvn", "-B", "-Dexec.executable=java",
        "-Dexec.mainClass=io.github.oliviercailloux.jsand.SendHello",
        "org.codehaus.mojo:exec-maven-plugin:3.3.0:exec");
    String runContainerId = dockerHelper.createAndExec(compiledImageId, RUN_CONTAINER_NAME,
        CONTAINER_CODE_DIR, NETWORK_NAME_ISOLATE, roBinds, runCmd, gateway);

    hello.latch().await();
  }

  @Test
  void testLogFails(@TempDir Path hostCodeDir) throws Exception {
    try (CloseablePath simple = PathUtils.fromUri(ContTests.class.getResource("simple/").toURI())) {
      copyCreateDir(simple, "pom.xml", hostCodeDir);
      copyCreateDir(Path.of(""), "src/main/java/io/github/oliviercailloux/jsand/Hello.java",
          hostCodeDir);
      copyCreateDir(Path.of(""), "src/main/java/io/github/oliviercailloux/jsand/SendHello.java",
          hostCodeDir);
      copyCreateDir(Path.of(""), "src/main/java/io/github/oliviercailloux/rmi/RemoteLoggerService.java",
          hostCodeDir);
      copyCreateDir(Path.of(""), "src/main/java/io/github/oliviercailloux/rmi/logback/RemoteClientAppender.java",
          hostCodeDir);
      Path target = hostCodeDir.resolve("src/main/resources/logback.xml");
      Files.createDirectories(target.getParent());
      Files.copy(simple.resolve("logback-conf.xml"), target);
      }

    DockerHelperDraft dockerHelper = DockerHelperDraft.create();
    DockerClient dockerClient = dockerHelper.client();

    Optional<Network> extNet = dockerHelper.network(NETWORK_NAME);
    if (extNet.isEmpty()) {
      dockerHelper.createNetwork(NETWORK_NAME);
    }

    dockerHelper.container(COMPILE_CONTAINER_NAME)
        .ifPresent(container -> dockerClient.removeContainerCmd(container.getId()).exec());

    if (dockerHelper.network(NETWORK_NAME_ISOLATE).isEmpty()) {
      dockerHelper.createNetwork(NETWORK_NAME_ISOLATE, ConnectivityMode.INTERNAL);
    }
    Network extIsolNet = dockerHelper.network(NETWORK_NAME_ISOLATE).orElseThrow(VerifyException::new);
    List<Config> configs = extIsolNet.getIpam().getConfig();
    verify(configs.size() == 1);
    Config config = Iterables.getOnlyElement(configs);
    String gateway = config.getGateway();
    verify(gateway.equals("172.19.0.1"));

    System.setProperty("java.rmi.server.hostname", gateway);
    Registry registryJ1 = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
    LOGGER.info("Registry: {}", registryJ1);
    ReadyServiceImpl hello = new ReadyServiceImpl();
    ReadyService stub = (ReadyService) UnicastRemoteObject.exportObject(hello, 0);
    registryJ1.rebind("Hello", stub);

    ImmutableMap<String, String> roBinds = ImmutableMap.of(hostCodeDir.toString(),
        CONTAINER_CODE_DIR, "/home/olivier/.m2/repository/", "/Maven repository/");
    ImmutableList<String> compileCmd = ImmutableList.of("mvn", "-B", "compile");
    String compileContainerId = dockerHelper.createAndExec(IMAGE_NAME, COMPILE_CONTAINER_NAME,
        CONTAINER_CODE_DIR, NETWORK_NAME, roBinds, compileCmd);

    String compiledImageId = dockerClient.commitCmd(compileContainerId).exec();

    dockerHelper.container(RUN_CONTAINER_NAME)
        .ifPresent(container -> dockerClient.removeContainerCmd(container.getId()).exec());

    ImmutableList<String> runCmd = ImmutableList.of("mvn", "-B", "-Dexec.executable=java",
        "-Dexec.mainClass=io.github.oliviercailloux.jsand.SendHello",
        "org.codehaus.mojo:exec-maven-plugin:3.3.0:exec");
    ExecutedContainer run = dockerHelper.createAndExecLogging(compiledImageId, RUN_CONTAINER_NAME,
        CONTAINER_CODE_DIR, NETWORK_NAME_ISOLATE, roBinds, runCmd, gateway);
    assertTrue(run.err().contains("Failed to initialize Configurator"));
    assertTrue(run.err().contains("java.rmi.NotBoundException: Logger"));
    assertTrue(run.out().contains("BUILD SUCCESS"));
    hello.latch().await();
  }
}
