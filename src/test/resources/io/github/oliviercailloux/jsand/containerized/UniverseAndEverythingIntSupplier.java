package io.github.oliviercailloux.jsand.containerized;

import java.util.function.IntSupplier;

public class UniverseAndEverythingIntSupplier implements IntSupplier{

  @Override
  public int getAsInt() {
    return 42;
  }
  
}
