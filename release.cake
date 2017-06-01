#reference "tools/AutoCake.Maven/tools/AutoCake.Maven.dll"
#reference "tools/AutoCake.Release/tools/AutoCake.Release.dll"
#load      "tools/AutoCake.Release/tools/release-tasks.cake"
#load      "tools/AutoCake.Release/tools/git-tasks.cake"
#load      "tools/AutoCake.Maven/tools/tasks.cake"

GitFlow.ApplyVersionNumbers = (cause, version) => {
  var versionInfo = GitVersioningAliases.FetchVersion();
  if (cause == "development") {
    // This will yield a snapshot 1.0.0-SNAPSHOT version
    MavenActions.UpdateMavenVersionNumber(cause,versionInfo.MajorMinorPatch + "-SNAPSHOT");
  }
  else if (cause == "release") {
    // This will yield a clean 1.0.0
    MavenActions.UpdateMavenVersionNumber(cause,versionInfo.MajorMinorPatch);
  }
  else {
     // cause == "staging"
     // This will yield 1.0.0-beta.1+2 (beta-build 1 with 2 commits in the branch)
    MavenActions.UpdateMavenVersionNumber(cause,versionInfo.FullSemVer);
  }
  MavenActions.RunMaven("versions:commit");
};

//////////////////////////////////////////////////////////////////////
// ARGUMENTS
//////////////////////////////////////////////////////////////////////

Task("Default")
    .IsDependentOn("Attempt-Release");

GitFlow.RunBuildTarget = () => 
{
  // See release-scripts/README.md for additional configuration options
  // and details on the syntax of this call.
  var versionInfo = GitVersioningAliases.FetchVersion();
  var buildType = "development";
  if (versionInfo.BranchName == GitFlow.State.StagingBranch)
  {
    buildType = "staging";
  }
  else if (versionInfo.BranchName == GitFlow.ReleaseTargetBranch)
  {
    buildType = "release";
  }
  
  CakeRunnerAlias.RunCake(Context, new CakeSettings {
        Arguments = new Dictionary<string, string>()
        {
            { "targetdir", "build-artefacts/" + versionInfo.FullSemVer },
            { "buildType", buildType }
        }
  });
};

var target = Argument("target", "Default");
RunTarget(target);
