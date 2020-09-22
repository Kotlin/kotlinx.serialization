# kotlinx.serialization release checklist

To release new `<version>` of `kotlinx.serialization`:

1. Checkout `dev` branch and update:<br>
   `git checkout dev && git pull`

3. Make sure the `master` branch is fully merged into `dev`:<br>
    `git merge origin/master`

4. Search & replace `<old-version>` with `<version>` across the project files. Should replace in:
   * [`README.md`](README.md)
   * [`gradle.properties`](gradle.properties)
   * [`integration-test/gradle.properties`](integration-test/gradle.properties)

   Update Kotlin version, if necessary.

5. Write release notes in [`CHANGELOG.md`](CHANGELOG.md):
   * Use old releases as example of style.
   * Write each change on a single line (don't wrap with CR).
   * Study commit message from previous release.

    [git changelog](https://github.com/tj/git-extras/blob/master/Commands.md#git-changelog) from git-extras may help you with that.

6. Tag version:<br>
    `git tag v<version>`

8. Push your changes:<br>
   `git push origin dev && git push origin --tags`

1. On [TeamCity integration server](https://teamcity.jetbrains.com/project.html?projectId=KotlinTools_KotlinxSerialization&tab=projectOverview):
   * Wait until "Runtime library (Build)" configuration for committed `dev` branch passes tests.
   * Run "Runtime library (Depoly - Train)" configuration with selected changes from `dev`.
      * For intermediate releases, you may override version with `reverse.dep.*.system.DeployVersion` build parameter.

4. In [Bintray](https://bintray.com/kotlin/kotlinx/kotlinx.serialization.runtime) admin interface:
   * Publish artifacts of the new version.
   * Wait until newly published version becomes the most recent.

6. Create a new release in [Github releases](https://github.com/Kotlin/kotlinx.serialization/releases). Use created git tag for title and changelog message for body.

1. Switch back to master branch and update it:<br>
   ```
   git checkout master && git pull
   git merge --ff-only dev
   git push origin master
   ```

5. Announce new release in [Slack](https://kotlinlang.slack.com)
