#reference "tools/AutoCake.Maven/tools/AutoCake.Maven.dll"
#load      "tools/AutoCake.Maven/tools/tasks.cake"

CreateDirectory("build-artefacts/maven");
CreateDirectory("build-artefacts/local-repo");
// MavenActions.Settings.Properties.Add("maven.repo.local", "build-artefacts/local-repo");

var buildType = Argument("buildType", "development");

//////////////////////////////////////////////////////////////////////
// TASK TARGETS
//////////////////////////////////////////////////////////////////////

Task("Default")
   .Does(() => {
     var settings = new MavenSettings();
     settings.Goal.Add("clean");
     settings.Goal.Add("package");

     if (buildType == "development") {
       settings.Goal.Add("install");
     }
     else {
       settings.Goal.Add("deploy");
     }

     MavenActions.RunMaven(settings);
   });

//////////////////////////////////////////////////////////////////////
// EXECUTION
//////////////////////////////////////////////////////////////////////

var target = Argument("target", "Default");
RunTarget(target);
