# Island Notifier

Android app (Kotlin) that uses YT Music's package name so OriginOS Origin Island shows its notifications.

## Build without a PC
1. Create a new GitHub repo (empty, branch `main`).
2. Get these files into it (see options below).
3. Repo > Actions > "Build APK" runs automatically (or press "Run workflow").
4. When green, open the run > Artifacts > download `IslandNotifier-debug-apk`, unzip, install `app-debug.apk`.
5. Before installing: UNINSTALL the real YT Music and your modded player (same package name can't coexist).

## Getting files into GitHub from a phone
Browsers can't upload folders. Easiest is Termux:

    pkg install git unzip
    termux-setup-storage
    unzip ~/storage/downloads/IslandNotifier.zip -d IslandNotifier
    cd IslandNotifier
    git init && git add . && git commit -m "init"
    git branch -M main
    git remote add origin https://github.com/<user>/<repo>.git
    git push -u origin main

(Password = a GitHub personal access token with "repo" scope.)

Alternative: in the GitHub web editor use "Add file > Create new file" and type the full path
(e.g. `app/src/main/AndroidManifest.xml`), then paste the contents. Do this for every file,
including `.github/workflows/build.yml`.
# Island-Notifier
