fastlane documentation
----

# Installation

Make sure you have the latest version of the Android SDK and NDK installed (`local.properties` needs
`sdk.dir` and `ndk.dir`; the native Ai library is built by ndk-build during the release build).

For _fastlane_ installation instructions, see [Installing _fastlane_](https://docs.fastlane.tools/#installing-fastlane)

# penteLive: publishing to Google Play

One-time setup:

1. `bundle install` in the repo root (Gemfile pins fastlane).
2. Google Cloud console: create a service account (no roles needed), add a JSON key, download it once and
   store it outside the repo. Enable the "Google Play Android Developer API" for that Cloud project.
3. Play Console > Users and permissions: invite the service account's e-mail address with access to
   penteLive and the production release permission ("Release to production, exclude devices, and use
   Play App Signing").
4. `cp fastlane/.env.example fastlane/.env` and fill in `PLAY_JSON_KEY_PATH` and the upload keystore
   (`ANDROID_KEYSTORE_PATH`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`).
   `app/build.gradle` has no signing config; the build lane hands these to Gradle as injected signing
   properties through the environment, so they never appear on a command line or in the log.

Release: `bundle exec fastlane release version:2.11.14 changelog:"What's new"`. Options:

- `changelog:` (required) becomes the en-US release notes; Google Play allows 500 characters.
- `code:N` sets `android:versionCode`; defaults to the current versionCode + 1.
- `rollout:0.2` makes it a staged rollout to that fraction of users; leave it out for a full release.

Or run `./release`: it checks you are on an up-to-date, clean `main`, asks for the version and release notes, runs the lane and pushes the commit and tag.

It bumps `android:versionName`/`android:versionCode` in `app/src/main/AndroidManifest.xml`, builds a signed
`.aab` (`clean bundleRelease`, copied to `fastlane/build/pentelive.aab`), uploads it to the production track
with the release notes, and Google Play reviews it before it goes live. Then it commits the bump as
`2.11.14` and tags `v2.11.14` locally; push yourself.

The upload writes only the bundle, the release (named after the version) on the track, and the en-US
release notes. Store listing text, images and screenshots are never touched: the release notes go through a
temporary directory, so nothing in `fastlane/metadata/` is ever read. A draft release on the production track
would be replaced by the upload, so the preflight stops the run when one exists.

If Google refuses to submit the release automatically (it asks for `changesNotSentForReview`, e.g. after a
rejected update), the upload fails instead of leaving an unsubmitted release behind; fix the blocker or send
the pending changes for review in Play Console, then rerun.

Before touching anything it checks the version/code format, the changelog length, that the new versionName
and versionCode are higher than the current ones, the service account key, that no `SUPPLY_*` environment
variables are set (they would quietly change the upload), the signing values and that `keytool` can open the
alias in the keystore with the store password, `ndk.dir` in
`local.properties`, that tracked files have no uncommitted changes (untracked files are ignored), that tag
`v2.11.14` does not exist yet, and, read-only on Google Play (it opens an edit and deletes it again), that
the key can reach the app, that the versionCode is higher than every version code Google Play already has
(all tracks and all uploaded bundles/APKs), that the store listing has an en-US language for the release notes,
and that the production track holds no draft release (a staged rollout in progress only gets a
warning: the new release supersedes it).

Recovery: if the build or upload fails (or you press Ctrl-C), the bump in `AndroidManifest.xml` is reverted
automatically; the error says what to do. Nothing is published unless the upload finished; if the
versionCode shows up in Play Console (App bundle explorer) anyway, rerun with a higher `code:` (a
versionCode can't be reused).

# Available Actions

## Android

### android bump

```sh
[bundle exec] fastlane android bump
```

Set android:versionName and android:versionCode in AndroidManifest.xml. No commit.

Usage: bump version:X.Y.Z [code:N]   (code defaults to the current versionCode + 1)

### android build

```sh
[bundle exec] fastlane android build
```

Build a release .aab signed with the upload key from fastlane/.env into fastlane/build/

### android release

```sh
[bundle exec] fastlane android release
```

Bump, build and upload to the production track (full release, or a staged rollout with rollout:0.x).

Then commits the bump as "X.Y.Z" and tags vX.Y.Z (no push). Uploads only the bundle and the en-US release notes.

Usage: release version:X.Y.Z changelog:"What's new" [code:N] [rollout:0.x]

----

The lane list above was generated by _fastlane_; regeneration is switched off in the Fastfile
(`FASTLANE_SKIP_DOCS`) so the setup notes survive. To refresh it, run a lane with `FASTLANE_SKIP_DOCS=0` and re-add the notes.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
