OSGI Launcher
-------------

This is a simple OSGI launcher based on the Felix boot code. It can 
start any OSGI framework that supports the Framework-API.

By default the launcher will search for bundles in the "bundles"
directory. This launcher does not support specifying deployed bundles
via properties - my use-case requires that the installation is stable 
and not prone to user interference.

This launcher scans the bundle directory for sub-directories. 

After launching, the deployment code will validate that all bundles
have been started. A bundle that is not started is an indicator of a
configuration problem like not finding a required package. The launcher
does not validate services - it is up to the validation code at higher
levels to do so. 