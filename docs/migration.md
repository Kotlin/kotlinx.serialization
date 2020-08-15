# Migration from 0.20.0 version to 1.0.0

For adopters of earlier versions of `kotlinx.serialization`, a dedicated migration path is prepared.
During the preparation of serialization 1.0.0 release, most of the API has been changed, renamed, moved to 
a separate package or made internal. IDEA migrations were introduced, but unfortunately not all API can be migrated 
with automatic replacements.

To simplify your migrations path, it is recommended to enable star imports in IDE (so all extensions are imported automatically) first.

1. Update `kotlinx.serialization` to version 1.0.0-RC (this is the last version that has migrations for pre-1.0.0 versions)
2. Rename dependency from `kotlinx-serialization-runtime` to `kotlinx-serialization-core`
3. For multiplatform usages, remove dependencies to platform-specific artifacts (e.g. `kotlinx-serialization-runtime-js`), they are [no longer required](/README.md#multiplatform-common-js-native) by Gradle.
4. Update Kotlin to 1.4.0
5. Start applying replacements for the deprecated code
6. If some signatures are not resolved, try to hit `alt + Enter` and import the signature
7. If methods are still not resolved, it is recommended to use star imports for `kotlinx.serialization` signatures in the problematic file

For less trivial issues, it is recommended to study [the changelog](../CHANGELOG.md#100-rc--2020-08-17) or to ask for help in `#serialization` Slack channel.
