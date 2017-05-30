package com.tmorgner.osgi.runner;

import org.osgi.framework.BundleContext;

public interface ValidatingTask {
  default void validate(BundleContext c) throws Exception {
  }
}
