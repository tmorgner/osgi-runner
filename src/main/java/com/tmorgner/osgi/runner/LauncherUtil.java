package com.tmorgner.osgi.runner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

public class LauncherUtil {
  /**
   * The property name used to specify an URL to the system
   * property file.
   **/
  public static final String SYSTEM_PROPERTIES_PROP = "felix.system.properties";
  /**
   * The default name used for the system properties file.
   **/
  public static final String SYSTEM_PROPERTIES_FILE_VALUE = "system.properties";
  /**
   * The property name used to specify an URL to the configuration
   * property file to be used for the created the framework instance.
   **/
  public static final String CONFIG_PROPERTIES_PROP = "felix.config.properties";
  /**
   * Name of the configuration directory.
   */
  public static final String CONFIG_DIRECTORY = "conf";
  /**
   * The default name used for the configuration properties file.
   **/
  public static final String CONFIG_PROPERTIES_FILE_VALUE = "config.properties";
  private static final String DELIM_START = "${";
  private static final String DELIM_STOP = "}";

  private static URL findFile(String systemPropertyRef, String defaultLocation) {
    Objects.requireNonNull(systemPropertyRef);
    Objects.requireNonNull(defaultLocation);

    String custom = System.getProperty(systemPropertyRef);
    if (custom != null) {
      try {
        return new URL(custom);
      } catch (MalformedURLException ex) {
        System.err.print("Main: " + ex);
        return null;
      }
    } else {
      // Determine where the configuration directory is by figuring
      // out where felix.jar is located on the system class path.
      File confDir = findConfigDirectory();

      try {
        return new File(confDir, defaultLocation).toURI().toURL();
      } catch (MalformedURLException ex) {
        System.err.print("Main: " + ex);
        return null;
      }
    }
  }

  /**
   * <p>
   * Loads the properties in the system property file associated with the
   * framework installation into <tt>System.setProperty()</tt>. These properties
   * are not directly used by the framework in anyway. By default, the system
   * property file is located in the <tt>conf/</tt> directory of the Felix
   * installation directory and is called "<tt>system.properties</tt>". The
   * installation directory of Felix is assumed to be the parent directory of
   * the <tt>felix.jar</tt> file as found on the system class path property.
   * The precise file from which to load system properties can be set by
   * initializing the "<tt>felix.system.properties</tt>" system property to an
   * arbitrary URL.
   * </p>
   **/
  public static void loadSystemProperties() {
    // The system properties file is either specified by a system
    // property or it is in the same directory as the Felix JAR file.
    // Try to load it from one of these places.

    // See if the property URL was specified as a property.

    // Read the properties file.
    Properties props = new Properties();
    URL propURL = findFile(SYSTEM_PROPERTIES_PROP, SYSTEM_PROPERTIES_FILE_VALUE);
    if (propURL == null) {
      return;
    }
    InputStream is = null;
    try {
      is = propURL.openConnection().getInputStream();
      props.load(is);
      is.close();
    } catch (FileNotFoundException ex) {
      // Ignore file not found.
    } catch (Exception ex) {
      System.err.println(
          "Main: Error loading system properties from " + propURL);
      System.err.println("Main: " + ex);
      try {
        if (is != null) is.close();
      } catch (IOException ex2) {
        // Nothing we can do.
      }
      return;
    }

    // Perform variable substitution on specified properties.
    for (Enumeration e = props.propertyNames(); e.hasMoreElements(); ) {
      String name = (String) e.nextElement();
      System.setProperty(name, props.getProperty(name));
    }
  }

  private static File findConfigDirectory() {
    File confDir = null;
    String classpath = System.getProperty("java.class.path");
    int index = classpath.toLowerCase().indexOf("felix.jar");
    int start = classpath.lastIndexOf(File.pathSeparator, index) + 1;
    if (index >= start) {
      // Get the path of the felix.jar file.
      String jarLocation = classpath.substring(start, index);
      // Calculate the conf directory based on the parent
      // directory of the felix.jar directory.
      confDir = new File(
          new File(new File(jarLocation).getAbsolutePath()).getParent(),
          CONFIG_DIRECTORY);
    } else {
      // Can't figure it out so use the current directory as default.
      confDir = new File(System.getProperty("user.dir"), CONFIG_DIRECTORY);
    }
    return confDir;
  }

  /**
   * <p>
   * Loads the configuration properties in the configuration property file
   * associated with the framework installation; these properties
   * are accessible to the framework and to bundles and are intended
   * for configuration purposes. By default, the configuration property
   * file is located in the <tt>conf/</tt> directory of the Felix
   * installation directory and is called "<tt>config.properties</tt>".
   * The installation directory of Felix is assumed to be the parent
   * directory of the <tt>felix.jar</tt> file as found on the system class
   * path property. The precise file from which to load configuration
   * properties can be set by initializing the "<tt>felix.config.properties</tt>"
   * system property to an arbitrary URL.
   * </p>
   *
   * @return A <tt>Properties</tt> instance or <tt>null</tt> if there was an error.
   **/
  public static Map<String, String> loadConfigProperties(Map<String, String> map) {
    // The config properties file is either specified by a system
    // property or it is in the conf/ directory of the Felix
    // installation directory.  Try to load it from one of these
    // places.

    // See if the property URL was specified as a property.
    URL propURL = findFile(CONFIG_PROPERTIES_PROP, CONFIG_PROPERTIES_FILE_VALUE);
    if (propURL == null) {
      return map;
    }
    // Read the properties file.
    Properties props = new Properties();
    InputStream is = null;
    try {
      // Try to load config.properties.
      is = propURL.openConnection().getInputStream();
      props.load(is);
      is.close();
    } catch (Exception ex) {
      // Try to close input stream if we have one.
      try {
        if (is != null) is.close();
      } catch (IOException ex2) {
        // Nothing we can do.
      }

      return map;
    }

    // Perform variable substitution for system properties and
    // convert to dictionary.
    for (Enumeration e = props.propertyNames(); e.hasMoreElements(); ) {
      String name = (String) e.nextElement();
      map.put(name,
          substVars(props.getProperty(name), name, null, props));
    }

    return map;
  }

  public static void copySystemProperties(Map<String, String> configProps) {
    for (Enumeration e = System.getProperties().propertyNames();
         e.hasMoreElements(); ) {
      String key = (String) e.nextElement();
      if (key.startsWith("felix.") || key.startsWith("org.osgi.framework.")) {
        configProps.put(key, System.getProperty(key));
      }
    }
  }

  /**
   * <p>
   * This method performs property variable substitution on the
   * specified value. If the specified value contains the syntax
   * <tt>${&lt;prop-name&gt;}</tt>, where <tt>&lt;prop-name&gt;</tt>
   * refers to either a configuration property or a system property,
   * then the corresponding property value is substituted for the variable
   * placeholder. Multiple variable placeholders may exist in the
   * specified value as well as nested variable placeholders, which
   * are substituted from inner most to outer most. Configuration
   * properties override system properties.
   * </p>
   *
   * @param val         The string on which to perform property substitution.
   * @param currentKey  The key of the property being evaluated used to
   *                    detect cycles.
   * @param cycleMap    Map of variable references used to detect nested cycles.
   * @param configProps Set of configuration properties.
   * @return The value of the specified string after system property substitution.
   * @throws IllegalArgumentException If there was a syntax error in the
   *                                  property placeholder syntax or a recursive variable reference.
   **/
  public static String substVars(String val, String currentKey,
                                 Map<String, String> cycleMap, Properties configProps)
      throws IllegalArgumentException {
    // If there is currently no cycle map, then create
    // one for detecting cycles for this invocation.
    if (cycleMap == null) {
      cycleMap = new HashMap<>();
    }

    // Put the current key in the cycle map.
    cycleMap.put(currentKey, currentKey);

    // Assume we have a value that is something like:
    // "leading ${foo.${bar}} middle ${baz} trailing"

    // Find the first ending '}' variable delimiter, which
    // will correspond to the first deepest nested variable
    // placeholder.
    int stopDelim = -1;
    int startDelim = -1;

    do {
      stopDelim = val.indexOf(DELIM_STOP, stopDelim + 1);
      // If there is no stopping delimiter, then just return
      // the value since there is no variable declared.
      if (stopDelim < 0) {
        return val;
      }
      // Try to find the matching start delimiter by
      // looping until we find a start delimiter that is
      // greater than the stop delimiter we have found.
      startDelim = val.indexOf(DELIM_START);
      // If there is no starting delimiter, then just return
      // the value since there is no variable declared.
      if (startDelim < 0) {
        return val;
      }
      while (stopDelim >= 0) {
        int idx = val.indexOf(DELIM_START, startDelim + DELIM_START.length());
        if ((idx < 0) || (idx > stopDelim)) {
          break;
        } else if (idx < stopDelim) {
          startDelim = idx;
        }
      }
    }
    while ((startDelim > stopDelim) && (stopDelim >= 0));

    // At this point, we have found a variable placeholder so
    // we must perform a variable substitution on it.
    // Using the start and stop delimiter indices, extract
    // the first, deepest nested variable placeholder.
    String variable =
        val.substring(startDelim + DELIM_START.length(), stopDelim);

    // Verify that this is not a recursive variable reference.
    if (cycleMap.get(variable) != null) {
      throw new IllegalArgumentException(
          "recursive variable reference: " + variable);
    }

    // Get the value of the deepest nested variable placeholder.
    // Try to configuration properties first.
    String substValue = (configProps != null)
        ? configProps.getProperty(variable, null)
        : null;
    if (substValue == null) {
      // Ignore unknown property values.
      substValue = System.getProperty(variable, "");
    }

    // Remove the found variable from the cycle map, since
    // it may appear more than once in the value and we don't
    // want such situations to appear as a recursive reference.
    cycleMap.remove(variable);

    // Append the leading characters, the substituted value of
    // the variable, and the trailing characters to get the new
    // value.
    val = val.substring(0, startDelim)
        + substValue
        + val.substring(stopDelim + DELIM_STOP.length(), val.length());

    // Now perform substitution again, since there could still
    // be substitutions to make.
    val = substVars(val, currentKey, cycleMap, configProps);

    // Return the value.
    return val;
  }
}
