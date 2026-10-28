# Island Notifier v0.2

Shows Bluetooth and Wi-Fi connect/disconnect events in OriginOS Origin Island by posting a
media-style notification under YT Music's package name.

## Use
1. Install the APK (uninstall v0.1 once; later updates install over it).
2. Open the app > Start > allow permissions.
3. "Send test to island" to check.
4. Settings > Apps > Island Notifier: battery = Unrestricted, allow auto-start, lock it in recents.

## Build (GitHub Actions)
Push to `main` > Actions > Build APK > download artifact `IslandNotifier-debug-apk`.

## Update files from Termux
    unzip -o ~/storage/downloads/IslandNotifier_v2.zip -d ~/IslandNotifier
    cd ~/IslandNotifier && git add . && git commit -m "v0.2" && git push
