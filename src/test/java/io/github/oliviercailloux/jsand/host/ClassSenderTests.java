package io.github.oliviercailloux.jsand.host;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.oliviercailloux.jsand.common.ClassSenderService;
import io.github.oliviercailloux.jsand.containerized.LoadOneClass;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

public class ClassSenderTests {
  @Test
  void testSendCompiled() throws Exception {
    ClassSenderService sender = ClassSender.create(Path.of("target/test-classes/"));
    byte[] clazz = sender.clazz(LoadOneClass.class.getName());
    assertEquals(0xCAFEBABE, ByteBuffer.wrap(clazz, 0, 4).getInt());
  }
}
