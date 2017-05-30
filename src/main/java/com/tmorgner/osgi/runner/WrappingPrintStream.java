package com.tmorgner.osgi.runner;

import java.io.IOException;
import java.io.PrintStream;

public class WrappingPrintStream extends PrintStream {
  private PrintStream out;

  public WrappingPrintStream(PrintStream out) {
    super(out);
    this.out = out;
  }

  @Override
  public void write(int b) {
    out.write(b);
  }

  @Override
  public void write(byte[] b) throws IOException {
    out.write(b);
  }

  @Override
  public void write(byte[] b, int off, int len) {
    out.write(b, off, len);
  }

  @Override
  public void flush() {
    out.flush();
  }

  @Override
  public void close() {
    out.close();
  }
}
