package com.tmorgner.osgi.runner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;

import java.util.function.Consumer;

public class FelixTestIT {
  private Logger logger = LogManager.getLogger();

  private class ValidateStartup implements FrameworkListener, BundleListener{
    BundleContext context;
    boolean failed;

    public ValidateStartup() {
    }

    @Override
    public void frameworkEvent(FrameworkEvent event) {
      if (event.getType() == FrameworkEvent.ERROR) {
        failed = true;
        logger.debug("Received framework ERROR event, marking tests as failed.");
      }
      if (event.getType() == FrameworkEvent.STARTED) {
        // everything is up to date. Shut down the framework.
        try {
          context.getBundle(0).stop();
          logger.debug("Received framework STARTED event, success, shutting down.");
        } catch (BundleException e) {
          logger.debug("Received exception while shutting down.", e);
          failed = true;
        }
      }
    }

    @Override
    public void bundleChanged(BundleEvent event) {
      if (event.getType() == BundleEvent.UNRESOLVED) {
        final Bundle bnd = event.getBundle();
        logger.debug("Found an unresolved bundle {}-{}", bnd.getSymbolicName(), bnd.getVersion());
        failed = true;
      }
    }
  }

  @Test(timeout = 5000)
  public void testFelixStarts() throws Exception {
    ValidateStartup l = new ValidateStartup();
    Consumer<BundleContext> validator = (context) -> {
      context.addFrameworkListener(l);
      l.context = context;
      logger.debug("Received context during startup.");
    };

    logger.debug("Starting OSGI framework ...");
    Assert.assertEquals(0, Main.start(new String[0], validator));
    Assert.assertFalse(l.failed);
  }
}
