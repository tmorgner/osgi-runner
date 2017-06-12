OSGI Launcher
-------------

This is a simple OSGI launcher based on the Felix boot code. It can 
start any OSGI framework that supports the Framework-API.

By default the launcher will search for bundles in the "bundles"
directory. This launcher does not support specifying deployed bundles
via properties - my use-case requires that the installation is stable 
and not prone to user interference.

This launcher scans the bundle directory for sub-directories. 
If the directory name starts with a number, that number is treated
as run-level for all bundles within that directory.

After launching, the deployment code will validate that all bundles
have been started. A bundle that is not started is an indicator of a
configuration problem like not finding a required package. The launcher
does not validate services - it is up to the validation code at higher
levels of abstraction to do so. 


The launcher main class is intentionally opened up so that it can be 
called from validation code. The Main#start method accepts a list
of Consumer<BundleContext> instances that can be used to programmatically
validate the state of the framework. These instances receive the 
system-bundle context for their work.
