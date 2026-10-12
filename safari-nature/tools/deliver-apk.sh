#!/usr/bin/env bash
# Builds the debug and release APKs and drops them in the NAS share, as for the other projects:
# ~/VM-Exchange/APK/SafariNature/SafariNature-<version>-debug.apk and SafariNature-<version>.apk.
# Bump versionCode and versionName in android/app/build.gradle.kts before each delivery.
#
# Optional: DEST=/other/folder
set -euo pipefail

root=$(cd "$(dirname "$0")/.." && pwd)
dest="${DEST:-$HOME/VM-Exchange/APK/SafariNature}"
export JAVA_HOME="${JAVA_HOME:-$HOME/Android/jdk-21}"
export PATH="$JAVA_HOME/bin:$PATH" # apksigner looks for java on the PATH
sdk="${ANDROID_HOME:-$HOME/Android/Sdk}"
apksigner="$(ls -d "$sdk"/build-tools/*/ | sort -V | tail -1)apksigner"
aapt2="$(ls -d "$sdk"/build-tools/*/ | sort -V | tail -1)aapt2"

# One heavy job at a time on the VM.
if pgrep -f "qemu-system|emulator64" >/dev/null; then
    echo "An emulator is running: stop it before building." >&2
    exit 1
fi

version=$(sed -nE 's/^[[:space:]]*versionName = "([^"]+)".*/\1/p' "$root/android/app/build.gradle.kts")
debug_name="SafariNature-$version-debug.apk"
release_name="SafariNature-$version.apk"
for f in "$debug_name" "$release_name"; do
    if [ -e "$dest/$f" ]; then
        echo "$dest/$f already exists: bump versionCode and versionName." >&2
        exit 1
    fi
done

(cd "$root/android" && ./gradlew --console=plain :core:test :app:assembleDebug :app:assembleRelease)

out="$root/android/app/build/outputs/apk"
debug_apk=$(ls "$out"/debug/*.apk)
release_apk=$(ls "$out"/release/*.apk)
case "$release_apk" in
    *unsigned*)
        echo "Unsigned release APK: SAFARI_NATURE_* properties missing from ~/.gradle/gradle.properties." >&2
        exit 1
        ;;
esac
"$apksigner" verify "$release_apk"
# A children's app: no Internet permission may slip in through a library.
if "$aapt2" dump permissions "$release_apk" | grep -q "android.permission.INTERNET"; then
    echo "The release APK asks for Internet access: find the library that adds it." >&2
    exit 1
fi

mkdir -p "$dest"
cp "$debug_apk" "$dest/$debug_name"
cp "$release_apk" "$dest/$release_name"
ls -l "$dest/$debug_name" "$dest/$release_name"
