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
   * Wait until "Runtime library (Build – Aggregated)" configuration for committed `dev` branch passes tests.
   * Run "Runtime library (Deploy - Train)" configuration:
     * On 'Changes' tab, select `dev` branch and corresponding commit.
     * On 'Parameters' tab, find 'Deploy version' and fill in with `<version>`.

4. In [Sonatype](oss.sonatype.org/#stagingRepositories) admin interface:
   * Close the repository and wait for it to verify.
   * Release it.
   
5. Update documentation website:<br>
    `./update_docs.sh <version> push`

6. Create a new release in [Github releases](https://github.com/Kotlin/kotlinx.serialization/releases). Use created git tag for title and changelog message for body.

1. Switch back to master branch and update it:<br>
   ```
   git checkout master && git pull
   git merge --ff-only dev
   git push origin master
   ```

5. Announce new release in [Slack](https://kotlinlang.slack.com).
