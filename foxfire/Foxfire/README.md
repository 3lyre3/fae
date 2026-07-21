# Foxfire 🟢✨

A tiny green-lit HTML viewer for Android. It renders a local `.html` file in a
full-screen WebView, gives you **find-in-page**, and registers itself so it
shows up under **Open with** when you long-press an HTML file.

Nothing else. That's the whole spell.

---

## Build & install (easy path — Android Studio)

1. Unzip this folder somewhere.
2. **Android Studio → File → Open →** pick the `Foxfire` folder.
3. Let it **sync**. If it offers to upgrade the Android Gradle Plugin / Gradle,
   accept — the pinned versions are just a known-good baseline.
4. Plug in your Pixel with **USB debugging** on, then press **Run ▶**.
   (Or **Build → Build App Bundle(s) / APK(s) → Build APK(s)**, then copy the
   file from `app/build/outputs/apk/debug/` to your phone and tap to install.)

## Build & install (headless — SDK + Gradle)

With the Android command-line SDK installed and `ANDROID_HOME` set:

```bash
cd Foxfire
gradle wrapper          # generates the wrapper scripts + jar once
./gradlew assembleDebug # APK lands in app/build/outputs/apk/debug/
```

Then `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

---

## Using it

- **Long-press** any `.html` in your file manager → **Open with** → **Foxfire**.
- Tap the glowing search button to find text; ↑ / ↓ step through matches.
- Launching Foxfire on its own shows a little firefly and a reminder of how to
  open files.

## The one honest caveat

Whether Foxfire appears in the **Open with** list depends on how your file
manager labels the file. Foxfire registers for `text/html`, which is what
**Files by Google**, most managers, and browser downloads report — so those
work. A manager that hands out a generic type with no `.html` in the path may
not surface Foxfire; opening the same file from Google Files or a browser's
Downloads is the reliable route.

## Knobs worth knowing

- App name lives in `app/src/main/res/values/strings.xml` (`app_name`).
- Colours are in `.../values/colors.xml` — `foxfire_glow` is the green.
- The launcher icon and empty-state firefly are hand-drawn vectors in
  `.../drawable/` (`ic_launcher_foreground.xml`, `firefly.xml`).
- JavaScript is enabled so interactive documents render. If you want a stricter
  reader, set `javaScriptEnabled = false` in `MainActivity.configureWebView()`.

---

## Building entirely from your phone (no PC)

An "Open with" handler must be a real installed app, so an APK has to be
compiled somewhere. You don't need a computer for that — pick either route.

### Route A — Cloud build (recommended, least effort)

The heavy lifting runs on GitHub's servers; your phone only triggers it and
downloads the result.

1. Make a new GitHub repo (the GitHub mobile app or the website both work).
2. Get these files into it. Easiest from a phone: use **Termux** just for git
   (`pkg install git unzip`, unzip this project, `git add . && git commit &&
   git push`) — Termux is only doing git here, so none of the usual on-device
   build headaches apply. Or let your own agent push it.
3. The included workflow (`.github/workflows/build.yml`) runs automatically.
   Open the **Actions** tab, wait for the green check.
4. Open the finished run → **Artifacts** → download **foxfire-debug-apk**.
5. Tap the downloaded APK to install. You'll be asked to allow
   "install unknown apps" for your browser/files app — allow it once.
6. Long-press any `.html` → **Open with → Foxfire**.

### Route B — Fully offline, on-device (no cloud, more setup)

Build right on the Pixel using Termux with a proot Ubuntu inside it:

```
pkg install proot-distro
proot-distro install ubuntu
proot-distro login ubuntu
# now inside a normal glibc Linux userland:
apt update && apt install -y openjdk-17-jdk wget unzip
# install the Android command-line tools + SDK, then:
gradle assembleDebug
```

The proot Ubuntu layer matters: it gives a normal glibc environment, so
Google's `aapt2` runs (bare Termux uses a different libc and stock `aapt2`
fails — this is the classic on-device gotcha). It's slower than the cloud
route but needs no account and no network once set up.
