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

     MavenActions.RunMaven("clean");

     if (buildType == "development") {
       MavenActions.RunMaven("install");
     }
     else {
       MavenActions.RunMaven("deploy");
     }
   });

//////////////////////////////////////////////////////////////////////
// EXECUTION
//////////////////////////////////////////////////////////////////////

var target = Argument("target", "Default");
RunTarget(target);
