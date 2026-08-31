# Merlin TV

Android/Android TV IPTV and public-media app rebuilt from the behaviour of the supplied Merlin Media APK.

## Included

- UK and USA live-TV catalogues loaded at runtime from the public `iptv-org` country playlists.
- Media3/ExoPlayer playback for HLS and ordinary HTTP(S) video.
- A small starter Movies/Series catalogue using openly reachable demo/open-film streams.
- GitHub Release update checking for `merlinthedev848/merlin-tv`.
- In-app APK download followed by the normal Android package-installer confirmation.
- Android TV launcher support and landscape-first UI.

## Update model

The app checks:

`https://api.github.com/repos/merlinthedev848/merlin-tv/releases/latest`

The latest release should have a semantic tag such as `v1.2.1` and contain an `.apk` asset. The repository/release endpoint must be publicly readable; do **not** embed a GitHub PAT in the APK for a private repository.

Android normally requires the user to approve installation of a sideloaded update. The app cannot silently replace itself on a normal unmanaged device.

### Signing is critical

Every update APK must use the **same signing certificate** as the APK already installed. If the original `merlin_media_1.1.0.apk` was signed with a key you control, configure CI with that key. If you do not have the original signing key, Android will not accept this rebuilt app as an update to 1.1.0; uninstall the old package once and install the rebuilt app, then keep the new signing key for all future releases.

## GitHub Actions signing secrets

For release builds, configure these repository secrets:

- `MERLIN_KEYSTORE_BASE64` — base64 of the JKS/keystore
- `MERLIN_KEYSTORE_PASSWORD`
- `MERLIN_KEY_ALIAS`
- `MERLIN_KEY_PASSWORD`

Then push a tag such as `v1.2.0`. The release workflow builds and attaches the signed APK.

## Content policy

Keep sources to broadcaster-authorised, public-domain, Creative Commons/openly licensed, or other legitimately public streams. Playlist entries can disappear or be geo-restricted; the app should treat individual stream failures as normal.
