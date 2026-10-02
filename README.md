# SnapWeb PoC — push-and-build repo (Android + iOS)

A tiny wrapper that loads **web.snapchat.com** inside a native WebView while
masquerading as a **desktop browser**, so you get Snapchat's chat/calls client
with **no Spotlight feed**. This repo is wired so you just **push it to GitHub**
and CI builds the apps — no local Android Studio or Xcode needed to get a build.

> Viability probe, not a product. The experiment is: does web.snapchat.com
> render acceptably on a phone, and is it genuinely Spotlight-free? See the
> Caveats at the bottom — login, Snapchat+ and ban-risk are unchanged by any of
> this.

## Why it gates, and how this clears it

A bare user-agent swap gets bounced to "open the app" — the real gate is screen
**width**. "Request Desktop Site" works because it also forces a wide viewport.
So: **iOS** uses `preferredContentMode = .desktop`; **Android** injects a script
before the page's own JS that forces a 1280px viewport and hides touch. Expect a
**zoomed-out desktop layout** you pinch-zoom — that *is* the usability verdict.

## Repo layout

    .github/workflows/build.yml   ← builds both on every push
    codemagic.yaml                ← optional Codemagic equivalent
    android/                      ← complete Gradle project
      app/src/main/java/com/example/snapweb/MainActivity.kt
      app/src/main/AndroidManifest.xml
      app/build.gradle.kts, build.gradle.kts, settings.gradle.kts, ...
    ios/                          ← XcodeGen project
      project.yml                 ← generates the .xcodeproj (nothing fragile to commit)
      Sources/SnapWebApp.swift, Sources/ContentView.swift

Two files are deliberately **not** committed because they're binary/fragile, and
are regenerated instead:
- Android `gradle-wrapper.jar` → created by the `Generate Gradle wrapper` CI step
  (or by opening the project in Android Studio).
- iOS `.xcodeproj` → created by `xcodegen generate` in CI (or locally).

## Use it (the push-and-build path)

1. Create a new GitHub repo and push this folder to it.
2. The **Build apps** workflow runs automatically (Actions tab).
3. **Android**: open the finished run → download the **snapweb-android-apk**
   artifact → install the APK on your Android phone. Done. Free, no accounts.
4. **iOS**: the job compiles the app unsigned to prove it builds — but a
   simulator build can't go on your iPhone, and the simulator is useless for
   this test anyway (Snapchat Web login needs a QR scan from a real logged-in
   app). To get it onto your actual iPhone, see below.

### The honest split
- **Android is a full win in CI**: build + install + test, free, no Apple/Google
  account needed for a debug APK.
- **iOS needs Apple no matter the service**: a device-installable IPA requires an
  **Apple Developer account ($99/yr)** plus signing (or TestFlight). CI can
  automate signing once you add an App Store Connect API key, but it can't remove
  the requirement. For a one-off test, the free 7-day Xcode sideload on a
  borrowed Mac is the cheaper route; for repeated/no-Mac builds, pay the $99 and
  flip the iOS job to a signed archive + TestFlight.

## Local fallback (if you'd rather use the IDEs)

- **Android**: open the `android/` folder in Android Studio → it creates the
  wrapper and runs on a connected phone.
- **iOS**: `brew install xcodegen && cd ios && xcodegen generate && open
  SnapWeb.xcodeproj` → set your signing team → run on device.

## Using Codemagic instead of Actions

`codemagic.yaml` mirrors the Actions jobs. It's a **starter** — verify the
`instance_type` names and image versions against current Codemagic docs. Point
Codemagic at your repo and it builds the same tree.

## Testing checklist (once a build is on a phone)

1. Login screen instead of the app-wall? (If walled: bump the Chrome version in
   the Android UA, or raise the forced width 1280 → 1440.)
2. Can you log in? (Expect the QR / confirm-on-phone step.)
3. **Is it actually Spotlight-free?** Hunt for any feed entry point — confirming
   its absence is the whole point.
4. Chat usable, and does it survive a kill + reopen (cookies/localStorage)?
5. Calls/camera at all? (Nice-to-have; WebRTC through a spoofed-desktop WebView
   can be flaky.)
6. How bad is the zoomed-out layout? Tolerable or not — that's the verdict.

## Caveats — unchanged by the build tooling

- **Login is chained to the real app** (QR/confirm from a recent logged-in
  Snapchat app). Fine for your test; it's the chicken-and-egg for a kid device.
- **Snapchat+ ($4/mo) may be required** for web to work at all.
- **Use a throwaway test account** — UA + viewport spoofing is ToS-adjacent and
  could flag it. Never a kid's primary.
- **It can break any week** when Snap changes the site.
- **Not shareable** — a you-for-your-own-kids experiment, for the guide's
  advanced appendix as the lone non-dead-end for iOS Snapchat.
