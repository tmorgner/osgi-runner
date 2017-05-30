package com.tmorgner.osgi.runner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class AutoDeployBundlesTest {
  private Logger logger = LogManager.getLogger();

  private String getBaseDirRaw() {
    final String basedir = System.getProperty("basedir");
    if (basedir != null) {
      logger.debug("Using basedir as supplied by surefire");
      return basedir + "/src/test/data";
    }
    return "src/test/data";
  }

  private String getValidatedBaseDir() {
    String bd = getBaseDirRaw();
    final Path path = Paths.get(bd, "test-data.txt");
    if (!Files.exists(path)) {
      Assert.fail("Unable to locate test data. I hoped to find it at " + path);
    }
    return bd;
  }

  @Test
  public void scanTestData() {
    final String validatedBaseDir = getValidatedBaseDir();
    AutoDeployBundles b = new AutoDeployBundles(logger::debug);
    b.scan(10, new File(validatedBaseDir).getAbsoluteFile());
    final List<BundlesWithRunLevel> bundles = b.getBundles();
    Assert.assertEquals(3, bundles.size());
    Assert.assertThat(find(bundles, 10).getFiles(),
        IsIterableContainingInAnyOrder.containsInAnyOrder(
            Paths.get("src", "test","data", "50 - Not a rc dir", "empty.jar").toFile().getAbsoluteFile(),
            Paths.get("src", "test","data", "empty.jar").toFile().getAbsoluteFile(),
            Paths.get("src", "test","data", "no-runlevel", "empty.jar").toFile().getAbsoluteFile(),
            Paths.get("src", "test","data", "second.jar").toFile().getAbsoluteFile()
        )
    );
    Assert.assertThat(find(bundles, 20).getFiles(),
        IsIterableContainingInAnyOrder.containsInAnyOrder(
            Paths.get("src", "test","data", "rc20-runlevel context", "empty.jar").toFile().getAbsoluteFile(),
            Paths.get("src", "test","data", "rc20-runlevel context", "nested", "empty.jar").toFile().getAbsoluteFile(),
            Paths.get("src", "test","data", "rc20-runlevel context", "second.jar").toFile().getAbsoluteFile()
        )
    );
    Assert.assertThat(find(bundles, 30).getFiles(),
        IsIterableContainingInAnyOrder.containsInAnyOrder(
            Paths.get("src", "test","data", "rc20-runlevel context", "rc30-nested", "empty.jar").toFile().getAbsoluteFile()
        )
    );
  }

  private BundlesWithRunLevel find(List<BundlesWithRunLevel> bundles, int level) {
    final List<BundlesWithRunLevel> collect =
        bundles.stream().filter(e -> e.getRunLevel() == level).collect(Collectors.toList());
    Assert.assertEquals(1, collect.size());
    return collect.get(0);
  }

  @Test
  public void scanOfInvalidDirDoesNotCrash() {
    AutoDeployBundles b = new AutoDeployBundles(logger::debug) {
      @Override
      protected File[] listFiles(File dir) {
        return null;
      }
    };
    b.scan(10, new File("whatever"));
    Assert.assertEquals(0, b.getBundles().size());
  }
}
