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

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Consumer;

/**
 * <p>
 * This class is the default way to instantiate and execute the framework. It is not
 * intended to be the only way to instantiate and execute the framework; rather, it is
 * one example of how to do so. When embedding the framework in a host application,
 * this class can serve as a simple guide of how to do so. It may even be
 * worthwhile to reuse some of its property handling capabilities.
 * </p>
 **/
public class Main {
  /**
   * Switch for specifying bundle directory.
   **/
  public static final String BUNDLE_DIR_SWITCH = "-b";

  /**
   * The property name used to specify whether the launcher should
   * install a shutdown hook.
   **/
  public static final String SHUTDOWN_HOOK_PROP = "felix.shutdown.hook";

  private static final String[] EXTRA_PACKAGES = {
      "sun.misc"
  };

  private static final String[] JAVAFX_PACKAGES = {
      "javafx.animation",
      "javafx.application",
      "javafx.beans",
      "javafx.beans.binding",
      "javafx.beans.property",
      "javafx.beans.property.adapter",
      "javafx.beans.value",
      "javafx.css",
      "javafx.collections",
      "javafx.collections.transformation",
      "javafx.concurrent",
      "javafx.embed.swing",
      "javafx.embed.awt",
      "javafx.event",
      "javafx.fxml",
      "javafx.geometry",
      "javafx.print",
      "javafx.scene",
      "javafx.scene.canvas",
      "javafx.scene.chart",
      "javafx.scene.control",
      "javafx.scene.control.cell",
      "javafx.scene.effect",
      "javafx.scene.image",
      "javafx.scene.input",
      "javafx.scene.layout",
      "javafx.scene.media",
      "javafx.scene.paint",
      "javafx.scene.shape",
      "javafx.scene.text",
      "javafx.scene.transform",
      "javafx.scene.web",
      "javafx.stage",
      "javafx.util",
      "javafx.util.converter",
      "netscape.javascript",
      // implementation details for controlsfx
      "com.sun.javafx.collections",
      "com.sun.javafx.css",
      "com.sun.javafx.css.converters",
      "com.sun.javafx.event",
      "com.sun.javafx.runtime",
      "com.sun.javafx.scene.control",
      "com.sun.javafx.scene.control.behavior",
      "com.sun.javafx.scene.control.skin",
      "com.sun.javafx.scene.text",
      "com.sun.javafx.scene.traversal",
      "com.sun.javafx.webkit",
      "com.sun.webkit",
      "javax.swing,javax.swing.table"
  };
  private static Framework m_fwk = null;

  protected static Framework getFramework() {
    return m_fwk;
  }

  /**
   * <p>
   * This method performs the main task of constructing an framework instance
   * and starting its execution. The following functions are performed
   * when invoked:
   * </p>
   * <ol>
   * <li><i><b>Examine and verify command-line arguments.</b></i> The launcher
   * accepts a "<tt>-b</tt>" command line switch to set the bundle auto-deploy
   * directory and a single argument to set the bundle cache directory.
   * </li>
   * <li><i><b>Read the system properties file.</b></i> This is a file
   * containing properties to be pushed into <tt>System.setProperty()</tt>
   * before starting the framework. This mechanism is mainly shorthand
   * for people starting the framework from the command line to avoid having
   * to specify a bunch of <tt>-D</tt> system property definitions.
   * The only properties defined in this file that will impact the framework's
   * behavior are the those concerning setting HTTP proxies, such as
   * <tt>http.proxyHost</tt>, <tt>http.proxyPort</tt>, and
   * <tt>http.proxyAuth</tt>. Generally speaking, the framework does
   * not use system properties at all.
   * </li>
   * <li><i><b>Read the framework's configuration property file.</b></i> This is
   * a file containing properties used to configure the framework
   * instance and to pass configuration information into
   * bundles installed into the framework instance. The configuration
   * property file is called <tt>config.properties</tt> by default
   * and is located in the <tt>conf/</tt> directory of the Felix
   * installation directory, which is the parent directory of the
   * directory containing the <tt>felix.jar</tt> file. It is possible
   * to use a different location for the property file by specifying
   * the desired URL using the <tt>felix.config.properties</tt>
   * system property; this should be set using the <tt>-D</tt> syntax
   * when executing the JVM. If the <tt>config.properties</tt> file
   * cannot be found, then default values are used for all configuration
   * properties. Refer to the
   * <a href="Felix.html#Felix(java.util.Map)"><tt>Felix</tt></a>
   * constructor documentation for more information on framework
   * configuration properties.
   * </li>
   * <li><i><b>Copy configuration properties specified as system properties
   * into the set of configuration properties.</b></i> Even though the
   * Felix framework does not consult system properties for configuration
   * information, sometimes it is convenient to specify them on the command
   * line when launching Felix. To make this possible, the Felix launcher
   * copies any configuration properties specified as system properties
   * into the set of configuration properties passed into Felix.
   * </li>
   * <li><i><b>Add shutdown hook.</b></i> To make sure the framework shutdowns
   * cleanly, the launcher installs a shutdown hook; this can be disabled
   * with the <tt>felix.shutdown.hook</tt> configuration property.
   * </li>
   * <li><i><b>Create and initialize a framework instance.</b></i> The OSGi standard
   * <tt>FrameworkFactory</tt> is retrieved from <tt>META-INF/services</tt>
   * and used to create a framework instance with the configuration properties.
   * </li>
   * <li><i><b>Auto-deploy bundles.</b></i> All bundles in the auto-deploy
   * directory are deployed into the framework instance.
   * </li>
   * <li><i><b>Start the framework.</b></i> The framework is started and
   * the launcher thread waits for the framework to shutdown.
   * </li>
   * </ol>
   * <p>
   * It should be noted that simply starting an instance of the framework is not
   * enough to create an interactive session with it. It is necessary to install
   * and start bundles that provide a some means to interact with the framework;
   * this is generally done by bundles in the auto-deploy directory or specifying
   * an "auto-start" property in the configuration property file. If no bundles
   * providing a means to interact with the framework are installed or if the
   * configuration property file cannot be found, the framework will appear to
   * be hung or deadlocked. This is not the case, it is executing correctly,
   * there is just no way to interact with it.
   * </p>
   * <p>
   * The launcher provides two ways to deploy bundles into a framework at
   * startup, which have associated configuration properties:
   * </p>
   * <ul>
   * <li>Bundle auto-deploy - Automatically deploys all bundles from a
   * specified directory, controlled by the following configuration
   * properties:
   * <ul>
   * <li><tt>felix.auto.deploy.dir</tt> - Specifies the auto-deploy directory
   * from which bundles are automatically deploy at framework startup.
   * The default is the <tt>bundle/</tt> directory of the current directory.
   * </li>
   * <li><tt>felix.auto.deploy.action</tt> - Specifies the auto-deploy actions
   * to be found on bundle JAR files found in the auto-deploy directory.
   * The possible actions are <tt>install</tt>, <tt>update</tt>,
   * <tt>start</tt>, and <tt>uninstall</tt>. If no actions are specified,
   * then the auto-deploy directory is not processed. There is no default
   * value for this property.
   * </li>
   * </ul>
   * </li>
   * <li>Bundle auto-properties - Configuration properties which specify URLs
   * to bundles to install/start:
   * <ul>
   * <li><tt>felix.auto.install.N</tt> - Space-delimited list of bundle
   * URLs to automatically install when the framework is started,
   * where <tt>N</tt> is the start level into which the bundle will be
   * installed (e.g., felix.auto.install.2).
   * </li>
   * <li><tt>felix.auto.start.N</tt> - Space-delimited list of bundle URLs
   * to automatically install and start when the framework is started,
   * where <tt>N</tt> is the start level into which the bundle will be
   * installed (e.g., felix.auto.start.2).
   * </li>
   * </ul>
   * </li>
   * </ul>
   * <p>
   * These properties should be specified in the <tt>config.properties</tt>
   * so that they can be processed by the launcher during the framework
   * startup process.
   * </p>
   *
   * @param args Accepts arguments to set the auto-deploy directory and/or
   *             the bundle cache directory.
   * @throws Exception If an error occurs.
   **/
  public static void main(String[] args) throws Exception {

    System.setOut(new WrappingPrintStream(System.out));
    System.exit(start(args));
  }

  @SafeVarargs
  public static int start(String[] args, Consumer<BundleContext>... validators) throws Exception {

    // Look for bundle directory and/or cache directory.
    // We support at most one argument, which is the bundle
    // cache directory.
    String bundleDir = null;
    String cacheDir = null;
    boolean expectBundleDir = false;
    for (String arg : args) {
      if (arg.equals(BUNDLE_DIR_SWITCH)) {
        expectBundleDir = true;
      } else if (expectBundleDir) {
        bundleDir = arg;
        expectBundleDir = false;
      } else {
        cacheDir = arg;
      }
    }

    if ((args.length > 3) || (expectBundleDir && bundleDir == null)) {
      System.out.println("Usage: [-b <bundle-deploy-dir>] [<bundle-cache-dir>]");
      return (0);
    }

    // Load system properties.
    LauncherUtil.loadSystemProperties();

    // Read configuration properties.
    Map<String, String> configProps = new HashMap<>();
    configProps.put("org.osgi.framework.startlevel.beginning", "100");
    configProps.put("felix.log.level", "4");
    configProps.put("felix.auto.deploy.action", "install,start,update,uninstall");
    configProps.put("felix.auto.deploy.dir", "bundles");
    configProps.put("felix.startlevel.bundle", "100");

    // todo: Use a system-fragment bundle instead ..
    configProps.put("org.osgi.framework.system.packages.extra",
        String.join(",",
            String.join(",", EXTRA_PACKAGES),
            String.join(",", JAVAFX_PACKAGES)
        )
    );

    configProps = LauncherUtil.loadConfigProperties(configProps);

    // Copy framework properties from the system properties.
    LauncherUtil.copySystemProperties(configProps);

    // If there is a passed in bundle auto-deploy directory, then
    // that overwrites anything in the config file.
    if (bundleDir != null) {
      configProps.put(AutoProcessor.AUTO_DEPLOY_DIR_PROPERTY, bundleDir);
    }

    // If there is a passed in bundle cache directory, then
    // that overwrites anything in the config file.
    if (cacheDir != null) {
      configProps.put(Constants.FRAMEWORK_STORAGE, cacheDir);
    }
    // If enabled, register a shutdown hook to make sure the framework is
    // cleanly shutdown when the VM exits.
    String enableHook = configProps.get(SHUTDOWN_HOOK_PROP);
    if ((enableHook == null) || !enableHook.equalsIgnoreCase("false")) {
      Runtime.getRuntime().addShutdownHook(new Thread("Felix Shutdown Hook") {
        public void run() {
          try {
            if (m_fwk != null) {
              m_fwk.stop();
              m_fwk.waitForStop(0);
            }
          } catch (Exception ex) {
            System.err.println("Error stopping framework: " + ex);
          }
        }
      });
    }

    try {
      // Create an instance of the framework.
      FrameworkFactory factory = ServiceLoader.load(FrameworkFactory.class).iterator().next();

      m_fwk = factory.newFramework(configProps);
      // Initialize the framework, but don't start it yet.
      m_fwk.init();

      // Use the system bundle context to process the auto-deploy
      // and auto-install/auto-start properties.
      if(!new AutoProcessor(System.out::println).process(configProps, m_fwk.getBundleContext())) {
        if ("true".equals(configProps.getOrDefault("com.tmorgner.osgi.runner.FailOnErrors", "true"))) {
          m_fwk = null;
          return -1;
        }
      }

      // Call the validation hooks.
      for(Consumer<BundleContext> activator: validators) {
        activator.accept(m_fwk.getBundleContext());
      }

      FrameworkEvent event;
      do {
        // Start the framework.
        m_fwk.start();
        // Wait for framework to stop to exit the VM.
        event = m_fwk.waitForStop(0);
      }
      // If the framework was updated, then restart it.
      while (event.getType() == FrameworkEvent.STOPPED_UPDATE);
      // Otherwise, exit.
      // disable the shutdown hook when we shut down in a controlled fashion.
      m_fwk = null;
      return (0);
    } catch (Exception ex) {
      System.err.println("Could not create framework: " + ex);
      ex.printStackTrace();
      return (-1);
    }
  }

}
