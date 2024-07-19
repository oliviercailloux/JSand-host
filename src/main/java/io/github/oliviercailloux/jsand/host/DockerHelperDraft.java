package io.github.oliviercailloux.jsand.host;

import static com.google.common.base.Verify.verify;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.CreateNetworkCmd;
import com.github.dockerjava.api.command.CreateNetworkResponse;
import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Network;
import com.github.dockerjava.api.model.StreamType;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MoreCollectors;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DockerHelperDraft {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(DockerHelperDraft.class);
  private static final Logger LOGGER_DOCKER_OUT = LoggerFactory.getLogger(DockerHelperDraft.class + ".out");
  private static final Logger LOGGER_DOCKER_ERR = LoggerFactory.getLogger(DockerHelperDraft.class + ".err");

  public enum ConnectivityMode {
    EXTERNAL, INTERNAL
  }

  private static class MemoryAdapter extends ResultCallback.Adapter<Frame> {
    private final StringBuilder out = new StringBuilder();
    private final StringBuilder err = new StringBuilder();

    @Override
    public void onNext(Frame object) {
      StringBuilder dest = switch(object.getStreamType()) {
        case STDOUT -> out;
        case STDERR -> err;
        case STDIN, RAW -> throw new VerifyException();
      };
      String text = new String(object.getPayload());
      dest.append(text);
    }

    public String out() {
      return out.toString();
    }

    public String err() {
      return err.toString();
    }
  }

  public static DockerHelperDraft create() {
    DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
    DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
        .dockerHost(config.getDockerHost()).sslConfig(config.getSSLConfig()).maxConnections(100)
        .connectionTimeout(Duration.ofSeconds(30)).responseTimeout(Duration.ofSeconds(45)).build();
    DockerClient dockerClient = DockerClientImpl.getInstance(config, httpClient);
    return new DockerHelperDraft(dockerClient);
  }

  private final DockerClient dockerClient;

  public DockerHelperDraft(DockerClient dockerClient) {
    this.dockerClient = dockerClient;
  }

  public Optional<Container> container(String name) {
    List<Container> matchingsWide = dockerClient.listContainersCmd().withShowAll(true)
        .withNameFilter(ImmutableSet.of(name)).exec();
    // ImmutableSet<Container> matchings =
    // matchingsWide.stream().filter(e -> ImmutableSet.copyOf(e.getNames()).contains(name))
    // .collect(ImmutableSet.toImmutableSet());
    ImmutableSet<Container> matchings = ImmutableSet.copyOf(matchingsWide);
    verify(matchings.size() <= 1, "Containers: %s.".formatted(matchings));
    Optional<Container> matching = matchings.stream().collect(MoreCollectors.toOptional());
    return matching;
  }

  public Optional<Network> network(String name) {
    List<Network> matchingsWide = dockerClient.listNetworksCmd().withNameFilter(name).exec();
    ImmutableSet<Network> matchings = matchingsWide.stream().filter(e -> e.getName().equals(name))
        .collect(ImmutableSet.toImmutableSet());
    verify(matchings.size() <= 1, "Networks: %s.".formatted(matchings));
    Optional<Network> matching = matchings.stream().collect(MoreCollectors.toOptional());
    return matching;
  }

  public DockerClient client() {
    return dockerClient;
  }

  public String createNetwork(String name) {
    return createNetwork(name, ConnectivityMode.EXTERNAL);
  }

  public String createNetwork(String name, ConnectivityMode mode) {
    CreateNetworkCmd cmd = dockerClient.createNetworkCmd().withName(name)
        .withOptions(ImmutableMap.of("com.docker.network.bridge.name", name.toLowerCase() + "0"));
    if (mode == ConnectivityMode.INTERNAL) {
      cmd.withInternal(true);
      verify(cmd.getInternal());
    }
    CreateNetworkResponse response = cmd.exec();
    if (response.getWarnings() == null) {
      LOGGER.warn("No warnings returned from createNetworkCmd");
    } else {
      ImmutableSet<String> warnings = ImmutableSet.copyOf(response.getWarnings());
      verify(warnings.size() == 0, "Warnings: %s.".formatted(warnings));
    }
    String id = response.getId();
    verify(id != null);
    return id;
  }

  public String createContainer(String imageName, String containerName, String workDir,
      String networkName, Map<String, String> roBinds, List<String> cmd, String gateway) {
    CreateContainerCmd createCmd =
        dockerClient.createContainerCmd(imageName).withName(containerName);
    if (!workDir.isEmpty()) {
      createCmd.withWorkingDir(workDir);
    }
    verify(createCmd.getWorkingDir().equals(workDir));
    createCmd.withCmd(cmd);
    HostConfig hostConfig = createCmd.getHostConfig();
    ImmutableSet<Bind> binds = roBinds.entrySet().stream()
        .map(e -> new Bind(e.getKey(), new Volume(e.getValue()), AccessMode.ro))
        .collect(ImmutableSet.toImmutableSet());
    hostConfig.setBinds(binds.toArray(new Bind[0]));
    if (!gateway.isEmpty()) {
      hostConfig.withExtraHosts("host.docker.internal:" + gateway);
    }
    hostConfig.withNetworkMode(networkName);
    CreateContainerResponse response = createCmd.exec();
    ImmutableSet<String> warnings = ImmutableSet.copyOf(response.getWarnings());
    verify(warnings.size() == 0, "Warnings: %s.".formatted(warnings));
    String id = response.getId();
    verify(id != null);
    return id;
  }

  public ExecutedContainer createAndExecLogging(String imageName, String containerName, String workDir,
      String networkName, Map<String, String> roBinds, List<String> cmd, String gateway)
      throws InterruptedException {
        MemoryAdapter logger = new MemoryAdapter();
        String id = createAndExec(imageName, containerName, workDir, networkName, roBinds, cmd, gateway, logger);
        return new ExecutedContainer(id, logger.out(), logger.err());
      }

  public String createAndExec(String imageName, String containerName, String workDir,
      String networkName, Map<String, String> roBinds, List<String> cmd, String gateway)
      throws InterruptedException {
        ResultCallback.Adapter<Frame> logger = new ResultCallback.Adapter<>() {
          @Override
          public void onNext(Frame object) {
            Logger slfLogger = switch(object.getStreamType()) {
              case STDOUT -> LOGGER_DOCKER_OUT;
              case STDERR -> LOGGER_DOCKER_ERR;
              case STDIN, RAW -> throw new VerifyException();
            };
            slfLogger.info(new String(object.getPayload()));
          }
        };
        return createAndExec(imageName, containerName, workDir, networkName, roBinds, cmd, gateway, logger);
          }

  private String createAndExec(String imageName, String containerName, String workDir,
      String networkName, Map<String, String> roBinds, List<String> cmd, String gateway, ResultCallback.Adapter<Frame> logger)
      throws InterruptedException {
    String id =
        createContainer(imageName, containerName, workDir, networkName, roBinds, cmd, gateway);
    dockerClient.startContainerCmd(id).exec();

    dockerClient.logContainerCmd(id).withStdOut(true).withStdErr(true).withFollowStream(true)
        .exec(logger);
    logger.awaitCompletion();

    return id;
  }

  public String createAndExec(String compiledImageId, String runContainerName,
      String containerCodeDir, String networkNameIsolate, ImmutableMap<String, String> roBinds,
      ImmutableList<String> runCmd) throws InterruptedException {
    return createAndExec(compiledImageId, runContainerName, containerCodeDir, networkNameIsolate,
        roBinds, runCmd, "");
  }
}
