package com.udacity.webcrawler.profiler;

import java.io.UncheckedIOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;

import javax.inject.Inject;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Objects;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

/**
 * Concrete implementation of the {@link Profiler}.
 */
final class ProfilerImpl implements Profiler {

  private final Clock clock;
  private final ProfilingState state = new ProfilingState();
  private final ZonedDateTime startTime;

  @Inject
  ProfilerImpl(Clock clock) {
    this.clock = Objects.requireNonNull(clock);
    this.startTime = ZonedDateTime.now(clock);
  }

  @Override
  public <T> T wrap(Class<T> klass, T delegate) {

    Objects.requireNonNull(klass);
    Objects.requireNonNull(delegate);
    boolean hasProfiledMethod = false;

    for (java.lang.reflect.Method method : klass.getMethods()) {

      if (method.isAnnotationPresent(Profiled.class)) {
        hasProfiledMethod = true;
        break;
      }
    }

    if (!hasProfiledMethod) {
      throw new IllegalArgumentException(
              "No methods annotated with @Profiled");
    }

    ProfilingMethodInterceptor interceptor =
            new ProfilingMethodInterceptor(clock, delegate, state);

    Object proxy = Proxy.newProxyInstance(
            klass.getClassLoader(),
            new Class<?>[]{klass},
            interceptor);

    return klass.cast(proxy);
  }

  @Override
  public void writeData(Path path) {

    try (Writer writer = Files.newBufferedWriter(path)) {
      writeData(writer);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void writeData(Writer writer) throws IOException {
    writer.write("Run at " + RFC_1123_DATE_TIME.format(startTime));
    writer.write(System.lineSeparator());
    state.write(writer);
    writer.write(System.lineSeparator());
  }
}
