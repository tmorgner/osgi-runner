package com.tmorgner.osgi.runner;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Recursively collects all bundles in the deployment directory. If the bundle is contained in a directory
 * starting with "rcNNNN" then bundles under that directory will be started with runlevel NNNN. Any non numeric
 * content after the rcNNNN pattern will be ignored and can be used for documentation purposes.
 * <p>
 * Valid names are "rc0001", "rc1", "rc1-Some description" and so on.
 */
public class AutoDeployBundles {
  private final Consumer<String> logger;
  private final Map<Integer, BundlesWithRunLevel> runLevels;
  private Pattern runlevelMatcher;

  public AutoDeployBundles(Consumer<String> logger) {
    this.logger = logger;
    this.runLevels = new TreeMap<>();
    this.runlevelMatcher = Pattern.compile("^rc(\\d{1,7}).*");
  }

  public List<BundlesWithRunLevel> getBundles() {
    return new ArrayList<>(runLevels.values());
  }

  protected File[] listFiles(File dir) {
    return dir.listFiles();
  }

  public void scan(int runLevel, File directory) {
    File[] files = listFiles(directory);
    if (files != null) {
      Arrays.sort(files);
      for (File file : files) {
        String name = file.getName();
        if (file.isFile() && name.endsWith(".jar")) {
          add(runLevel, file);
        }
        else if (file.isDirectory()) {
          if (name.matches("\\.{1,2}")) {
            continue;
          }

          // no one needs more than a few million runlevels
          final Matcher matcher = runlevelMatcher.matcher(name);
          if (matcher.find()) {
            String number = matcher.group(1);
            final int n = Integer.parseInt(number);
            scan(n, file);
          }
          else {
            scan(runLevel, file);
          }
        }
      }
    }
  }

  private void add(int runLevel, File file) {
    logger.accept("Found bundle " + file + " at run-level " + runLevel);

    BundlesWithRunLevel bundlesWithRunLevel = runLevels.get(runLevel);
    if (bundlesWithRunLevel == null) {
      bundlesWithRunLevel = new BundlesWithRunLevel(runLevel);
    }
    runLevels.put(runLevel, bundlesWithRunLevel);
    bundlesWithRunLevel.add(file);
  }
}
