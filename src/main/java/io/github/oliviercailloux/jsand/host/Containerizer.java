package io.github.oliviercailloux.jsand.host;

import static com.google.common.base.Verify.verify;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Network;
import com.github.dockerjava.api.model.Network.Ipam.Config;
import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class Containerizer {
  private static final String IMAGE_NAME = "ghcr.io/oliviercailloux/djm-conf";
  private static final String COMPILE_CONTAINER_NAME = "JSandTestCompile";
  private static final String CONTAINER_CODE_DIR = "/code/";
  private static final String TARGET_MAVEN_REPOSITORY = "/Maven repository/";
  private static final String NETWORK_NAME = "JSand";
  private static final String NETWORK_NAME_ISOLATE = "JSandIsolate";
  private static final String RUN_CONTAINER_NAME = "JSandTestRun";

  private final Path hostCodeDir;
  private DockerHelper dockerHelper;
  private final Path hostMavenRepository;
  private CompileContainedResult compileResult;

  private Containerizer(Path hostCodeDir, Path mavenRepository) {
    this.hostCodeDir = hostCodeDir;
    dockerHelper = DockerHelper.create();
    this.hostMavenRepository = mavenRepository;
    compileResult = null;
  }

  public void removeCompileContainerIfExists() {
    dockerHelper.container(COMPILE_CONTAINER_NAME)
        .ifPresent(container -> dockerHelper.client().removeContainerCmd(container.getId()).exec());
  }

  public void createNetworkIfNotExists() {
    if (dockerHelper.network(NETWORK_NAME).isEmpty()) {
      dockerHelper.createNetwork(NETWORK_NAME);
    }
  }

  public CompileContainedResult compile() throws InterruptedException {
    ImmutableList<String> compileCmd = ImmutableList.of("mvn", "-B", "compile");
    String compileContainerId = dockerHelper.createAndExec(IMAGE_NAME, COMPILE_CONTAINER_NAME,
        CONTAINER_CODE_DIR, NETWORK_NAME, roBinds(), compileCmd);

    String compiledImageId = dockerHelper.client().commitCmd(compileContainerId).exec();
    compileResult = new CompileContainedResult(compileContainerId, compiledImageId);
    return compileResult;
  }

  private ImmutableMap<String, String> roBinds() {
    return ImmutableMap.of(hostCodeDir.toString(), CONTAINER_CODE_DIR,
        hostMavenRepository.toString(), TARGET_MAVEN_REPOSITORY);
  }

  public ExecutedContainer run(String mainClass) throws InterruptedException {
    if (compileResult == null) {
      throw new IllegalStateException("No image to run.");
    }

    Network extIsolNet =
        dockerHelper.network(NETWORK_NAME_ISOLATE).orElseThrow(VerifyException::new);
    List<Config> configs = extIsolNet.getIpam().getConfig();
    verify(configs.size() == 1);
    Config config = Iterables.getOnlyElement(configs);
    String hostIp = config.getGateway();

    ImmutableList<String> runCmd = ImmutableList.of("mvn", "-B", "-Dexec.executable=java",
        "-Dexec.mainClass=" + mainClass, "org.codehaus.mojo:exec-maven-plugin:3.3.0:exec");
    ExecutedContainer ran = dockerHelper.createAndExecLogging(compileResult.compiledImageId(),
        RUN_CONTAINER_NAME, CONTAINER_CODE_DIR, NETWORK_NAME_ISOLATE, roBinds(), runCmd, hostIp);
    return ran;
  }
}
