package com.tmorgner.osgi.runner;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

class BundlesWithRunLevel {
  private int runLevel;
  private List<File> bundles;

  public BundlesWithRunLevel(int runLevel) {
    this.runLevel = runLevel;
    this.bundles = new ArrayList<>();
  }

  public void add(File file) {
    this.bundles.add(file);
  }

  public int getRunLevel() {
    return runLevel;
  }

  public List<File> getFiles() {
    return new ArrayList<>(bundles);
  }


}
