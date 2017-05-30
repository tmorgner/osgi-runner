/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.tmorgner.osgi.runner;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.service.startlevel.StartLevel;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class AutoProcessor {
  /**
   * The property name used for the bundle directory.
   **/
  public static final String AUTO_DEPLOY_DIR_PROPERTY = "felix.auto.deploy.dir";
  /**
   * The default name used for the bundle directory.
   **/
  public static final String AUTO_DEPLOY_DIR_VALUE = "bundle";
  /**
   * The property name used to specify auto-deploy actions.
   **/
  public static final String AUTO_DEPLOY_ACTION_PROPERTY = "felix.auto.deploy.action";
  /**
   * The property name used to specify auto-deploy start level.
   **/
  public static final String AUTO_DEPLOY_STARTLEVEL_PROPERTY = "felix.auto.deploy.startlevel";
  /**
   * The name used for the auto-deploy install action.
   **/
  public static final String AUTO_DEPLOY_INSTALL_VALUE = "install";
  /**
   * The name used for the auto-deploy start action.
   **/
  public static final String AUTO_DEPLOY_START_VALUE = "start";
  /**
   * The name used for the auto-deploy update action.
   **/
  public static final String AUTO_DEPLOY_UPDATE_VALUE = "update";
  /**
   * The name used for the auto-deploy uninstall action.
   **/
  public static final String AUTO_DEPLOY_UNINSTALL_VALUE = "uninstall";
  /**
   * The property name prefix for the launcher's auto-install property.
   **/
  public static final String AUTO_INSTALL_PROP = "felix.auto.install";
  /**
   * The property name prefix for the launcher's auto-start property.
   **/
  public static final String AUTO_START_PROP = "felix.auto.start";

  private Consumer<String> logger;

  public AutoProcessor(Consumer<String> logger) {
    Objects.requireNonNull(logger);

    this.logger = logger;
  }

  /**
   * Used to instigate auto-deploy directory process and auto-install/auto-start
   * configuration property processing during.
   *
   * @param configMap Map of configuration properties.
   * @param context   The system bundle context.
   **/
  public boolean process(Map<String, String> configMap, BundleContext context) {
    Objects.requireNonNull(context);
    Objects.requireNonNull(configMap);

    // Determine if auto deploy actions to perform.
    List<String> actionList = prepareActions(configMap);

    // Perform auto-deploy actions.
    if (actionList.isEmpty()) {
      logger.accept("Auto-Deploy: No auto-deploy actions defined.");
      return true;
    }

    logger.accept("Auto-Deploy: " + String.join(",", actionList));

    int startLevel = getFrameworkStartLevel(context, configMap);

    // Get list of already installed bundles as a map.
    Map<String, Bundle> installedBundleMap = new HashMap<>();
    for (Bundle bundle : context.getBundles()) {
      installedBundleMap.put(bundle.getLocation(), bundle);
    }

    // Get the auto deploy directory.
    String autoDir = configMap.getOrDefault(AUTO_DEPLOY_DIR_PROPERTY, AUTO_DEPLOY_DIR_VALUE);
    // Look in the specified bundle directory to create a list
    // of all JAR files to install.
    AutoDeployBundles bundleSet = new AutoDeployBundles(logger);
    bundleSet.scan(startLevel, new File(autoDir));
    boolean success = true;
    // Install bundle JAR files and remember the bundle objects.
    final List<Bundle> startBundleList = new ArrayList<>();
    for (BundlesWithRunLevel list : bundleSet.getBundles()) {
      for (File bundleFile : list.getFiles()) {
        // Look up the bundle by location, removing it from
        // the map of installed bundles so the remaining bundles
        // indicate which bundles may need to be uninstalled.
        final String key = bundleFile.toURI().toString();
        Bundle b = installedBundleMap.remove(key);

        try {
          // If the bundle is not already installed, then install it
          // if the 'install' action is present.
          if ((b == null) && actionList.contains(AUTO_DEPLOY_INSTALL_VALUE)) {
            b = context.installBundle(key);
            logger.accept("Installed " + key);
          }
          // If the bundle is already installed, then update it
          // if the 'update' action is present.
          else if ((b != null) && actionList.contains(AUTO_DEPLOY_UPDATE_VALUE)) {
            b.update();
            logger.accept("Updated " + key);
          }

          // If we have found and/or successfully installed a bundle,
          // then add it to the list of bundles to potentially start
          // and also set its start level accordingly.
          if ((b != null) && !isFragment(b)) {
            startBundleList.add(b);
            setBundleStartLevel(b, list.getRunLevel());
            logger.accept(String.format("Start level for %s set to %d", key, list.getRunLevel()));
          }
        } catch (BundleException ex) {
          logger.accept(String.format("Auto-deploy install [%s]: %s%s%n", key, ex, (ex.getCause() != null) ? " - " + ex.getCause() : ""));
          success = false;
        }
      }
    }

    // Uninstall all bundles not in the auto-deploy directory if
    // the 'uninstall' action is present.
    if (actionList.contains(AUTO_DEPLOY_UNINSTALL_VALUE)) {
      for (Map.Entry<String, Bundle> entry : installedBundleMap.entrySet()) {
        Bundle b = entry.getValue();
        if (b.getBundleId() != 0) {
          try {
            b.uninstall();
            logger.accept("Uninstalled " + entry.getKey());
          } catch (BundleException ex) {
            logger.accept(String.format("Auto-deploy uninstall: %s%s", ex, (ex.getCause() != null) ? " - " + ex.getCause() : ""));
            success = false;
          }
        }
      }
    }

    // Start all installed and/or updated bundles if the 'start'
    // action is present.
    if (actionList.contains(AUTO_DEPLOY_START_VALUE)) {
      for (Bundle bundle : startBundleList) {
        try {
          bundle.start();
          logger.accept("started " + bundle.getLocation());
        } catch (BundleException ex) {
          logger.accept(String.format("Auto-deploy start: %s%s", ex, (ex.getCause() != null) ? " - " + ex.getCause() : ""));
          success = false;
        }
      }
    }
    return success;
  }

  private List<String> prepareActions(Map<String, String> configMap) {
    String action = configMap.get(AUTO_DEPLOY_ACTION_PROPERTY);
    action = (action == null) ? "" : action;
    List<String> actionList = new ArrayList<>();
    for (final String token : action.split(",")) {
      String s = token.trim().toLowerCase();
      if (s.equals(AUTO_DEPLOY_INSTALL_VALUE)
          || s.equals(AUTO_DEPLOY_START_VALUE)
          || s.equals(AUTO_DEPLOY_UPDATE_VALUE)
          || s.equals(AUTO_DEPLOY_UNINSTALL_VALUE)) {
        actionList.add(s);
      }
    }
    return actionList;
  }

  private boolean isFragment(Bundle bundle) {
    return bundle.getHeaders().get(Constants.FRAGMENT_HOST) != null;
  }

  private void setBundleStartLevel(Bundle b, int level) {
    final BundleStartLevel adapt = b.adapt(BundleStartLevel.class);
    if (adapt != null) {
      adapt.setStartLevel(level);
      return;
    }

    // use the legacy way ..
    final BundleContext context = b.getBundleContext();
    StartLevel sl = context.getService(context.getServiceReference(StartLevel.class));
    if (sl == null) {
      throw new IllegalStateException("OSGI framework is not able to produce a StartLevel service. This is bad.");
    }
    sl.setBundleStartLevel(b, level);
  }

  private int getFrameworkStartLevel(BundleContext context,
                                     Map<String, String> configMap) {
    int startLevel = 1;
    // Retrieve the Start Level service, since it will be needed
    // to set the start level of the installed bundles.

    FrameworkStartLevel sl = context.getBundle(0).adapt(FrameworkStartLevel.class);
    if (sl != null) {
      startLevel = sl.getInitialBundleStartLevel();
    } else {
      // legacy mode ...
      final ServiceReference<StartLevel> slRef = context.getServiceReference(StartLevel.class);
      StartLevel slOld = slRef != null ? context.getService(slRef) : null;
      if (slOld == null) {
        throw new IllegalStateException("OSGI framework is not able to produce a StartLevel service. This is bad.");
      }
      startLevel = slOld.getInitialBundleStartLevel();
    }
    // Get start level for auto-deploy bundles.
    final String startLevelProp = configMap.get(AUTO_DEPLOY_STARTLEVEL_PROPERTY);
    if (startLevelProp != null) {
      try {
        startLevel = Integer.parseInt(startLevelProp);
      } catch (NumberFormatException ex) {
        // Ignore and keep default level.
      }
    }
    return startLevel;
  }

}