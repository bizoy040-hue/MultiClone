# MultiClone
Runs a second copy of installed apps inside Android's **Work Profile** (legitimate, OS-supported isolation).

## Build in the cloud (GitHub Actions)
1. Create a GitHub repo and push this folder to it.
2. Actions tab -> "Build release APK" runs on every push (or press "Run workflow").
3. Open the finished run -> Artifacts -> `MultiClone-release-apk`.

Optional stable signing: add repo secrets KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD.
Without them a throwaway key is generated, so each build has a different signature (uninstall before updating).

## Build locally
Android Studio Koala or newer with JDK 17: open the folder, then Build > Build APK(s).
CLI: `gradle assembleRelease` (Gradle 8.7; no wrapper jar is bundled).

## Use
Install -> Dashboard -> Create clone space (system dialog) -> Apps tab: select apps -> "Add to clone space"
-> Clones tab or the launcher's Work tab.

## Limits
One work profile per device; adding existing apps needs Android 9+; some apps block work profiles;
no SMS/notification access; no security bypass.
