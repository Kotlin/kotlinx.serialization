# Contributing Guidelines

There are two main ways to contribute to the project &mdash; submitting issues and submitting 
fixes/changes/improvements via pull requests.

## Submitting issues

Both bug reports and feature requests are welcome.
Submit issues [here](https://github.com/Kotlin/kotlinx.serialization/issues).

* Search for existing issues to avoid reporting duplicates.
* When submitting a bug report:
  * Use a 'bug report' template when creating a new issue.
  * Test it against the most recently released version. It might have been already fixed.
  * By default, we assume that your problem reproduces in Kotlin/JVM. Please, mention if the problem is
    specific to Kotlin/JS or Kotlin/Native. 
  * Include the code that reproduces the problem. Provide the complete reproducer code, yet minimize it as much as possible.
  * However, don't put off reporting any weird or rarely appearing issues just because you cannot consistently 
    reproduce them.
  * If the bug is in behavior, then explain what behavior you've expected and what you've got.  
* When submitting a feature request:
  * Use a 'feature request' template when creating a new issue.
  * Explain why you need the feature &mdash; what's your use-case, what's your domain.
  * Explaining the problem you face is more important than suggesting a solution. 
    Even if you don't have a proposed solution, please report your problem.
  * If there is an alternative way to do what you need, then show the code of the alternative.

## Submitting PRs

We love PRs. Submit PRs [here](https://github.com/Kotlin/kotlinx.serialization/pulls).
However, please keep in mind that maintainers will have to support the resulting code of the project,
so do familiarize yourself with the following guidelines. 

* All development (both new features and bug fixes) is performed in the `dev` branch.
  * The `master` branch always contains sources of the most recently released version.
  * Base PRs against the `dev` branch.
  * The `dev` branch is pushed to the `master` branch during release.
  * Documentation in markdown files can be updated directly in the `master` branch, 
    unless the documentation is in the source code, and the patch changes line numbers.
* If you fix documentation:
  * After fixing/changing code examples in the [`docs`](docs) folder or updating any references in the markdown files
    run the [Knit tool](#running-the-knit-tool) and commit the resulting changes as well. 
    Your changes will not pass the tests otherwise.
  * If you plan extensive rewrites/additions to the docs, then please [contact the maintainers](#contacting-maintainers)
    to coordinate the work in advance.    
* If you make any code changes:
  * Follow the [Kotlin Coding Conventions](https://kotlinlang.org/docs/reference/coding-conventions.html). 
    * Use 4 spaces for indentation. 
    * Use imports with '*'.
  * [Build the project](#building) to make sure it all works and passes the tests.
* If you fix a bug:
  * Write the test that reproduces the bug.
  * Fixes without tests are accepted only in exceptional circumstances if it can be shown that writing the 
    corresponding test is too hard or otherwise impractical.
  * Follow the style of writing tests that is used in this project: 
    name test functions as `testXxx`. Don't use backticks in test names.
* If you introduce any new public APIs:
  * All new APIs must come with documentation and tests.
  * All new APIs are initially released with `@ExperimentalSerializationApi` annotation and are graduated later.
  * [Update the public API dumps](#updating-the-public-api-dump) and commit the resulting changes as well. 
    It will not pass the tests otherwise.
  * If you plan large API additions, then please start by submitting an issue with the proposed API design  
    to gather community feedback.
  * [Contact the maintainers](#contacting-maintainers) to coordinate any big piece of work in advance.
* If you propose/implement a new serialization format:
  * Follow the general advice on new public APIs above.
  * Note, that you can keep new format implementation in your own repository to be able to perform proper maintenance 
    and to have a separate release cycle. 
  * You can submit a PR to the [list of community-supported formats](formats/README.md#other-community-supported-formats) 
    with a description of your library.
* Comment on the existing issue if you want to work on it. Ensure that the issue not only describes a problem,
  but also describes a solution that has received positive feedback. Propose a solution if there isn't any.

## Building

You can find all the instructions [here](docs/building.md)

### Running the Knit tool

* Use [Knit](https://github.com/Kotlin/kotlinx-knit/blob/master/README.md) for updates to documentation:
  * Run `./gradlew knit` to update example files, links, tables of content.
  * Commit updated documents and examples together with other changes.

### Updating the public API dump

* Use [Binary Compatibility Validator](https://github.com/Kotlin/binary-compatibility-validator/blob/master/README.md) for updates to public API:
  * Run `./gradlew apiDump` to update API index files. 
  * Commit updated API indexes together with other changes.

## Releases

* Full release procedure checklist is [here](RELEASING.md).

## Contacting maintainers

* If something cannot be done, not convenient, or does not work &mdash; submit an [issue](#submitting-issues).
* "How to do something" questions &mdash; [StackOverflow](https://stackoverflow.com).
* Discussions and general inquiries &mdash; use `#serialization` channel in [KotlinLang Slack](https://kotl.in/slack).

