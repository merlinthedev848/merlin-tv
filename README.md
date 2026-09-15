# Merlin TV

Modern Android and Android TV IPTV and streaming application built with Jetpack Compose and Media3/ExoPlayer.

## ✨ Features & Overhaul Highlights

- **Live TV Catalogs**: Real-time loading from public UK and USA `iptv-org` playlists, complete with channel logos (`tvg-logo`), groups/categories, and metadata.
- **D-Pad & Remote Navigation**: Full Android TV remote control support with glowing focus rings, smooth scaling animations, and D-pad click handlers.
- **Channel Surfing & OSD**: Previous / Next channel jumping while in full-screen playback, play/pause controls, and aspect ratio cycling (Fit, Zoom, Stretch).
- **Favorites & History**: Bookmark favorite channels and automatically track recently watched streams with persistent local storage.
- **Search & Filter**: Real-time channel search by name, group, or country, along with dynamic category pill filters (News, Sports, Entertainment, Movies, etc.).
- **Software Update Checker**: Built-in "Check Updates" button that directly queries `https://api.github.com/repos/merlinthedev848/merlin-tv/releases/latest`, parses changelogs, downloads the latest APK in-app with a progress indicator, and launches the Android package installer.
- **Custom Playlists**: Add and manage custom M3U/M3U8 playlist URLs with custom labels and automatic cache refresh.
- **Resilient Playback**: Spoofed User-Agent headers to prevent HTTP 403 Forbidden errors on public streams, auto-buffering spinner, and automatic retry/skip overlays on stream errors.

## 🚀 Building & Releasing

### Local Build
```bash
./gradlew assembleDebug
```

### GitHub Actions Release
1. Configure repository secrets if signing with a private keystore:
   - `MERLIN_KEYSTORE_BASE64`
   - `MERLIN_KEYSTORE_PASSWORD`
   - `MERLIN_KEY_ALIAS`
   - `MERLIN_KEY_PASSWORD`
2. Push a semantic tag (e.g. `v1.2.1`):
   ```bash
   git tag v1.2.1
   git push origin v1.2.1
   ```
3. GitHub Actions builds the release/debug APK and attaches it to the GitHub release.

## 📱 App Architecture

```
app/src/main/java/com/example/merlinmedia/
├── MainActivity.kt               # Main entry point and app router
├── model/
│   └── MediaModels.kt           # Data classes for channels, updates, and playback
├── data/
│   ├── CatalogRepository.kt     # Multi-source M3U loader & sample catalogs
│   ├── FavoritesManager.kt      # SharedPreferences persistence for favorites & history
│   └── M3uParser.kt             # Robust regex-based M3U parser with logo & group extraction
├── updater/
│   └── UpdateManager.kt         # GitHub Release checker, progress downloader & APK installer
└── ui/
    ├── theme/
    │   └── Theme.kt             # Obsidian & Cyan dark theme for TV
    ├── components/
    │   └── TvComponents.kt      # Focusable cards, channel rows, category chips, search bar
    ├── screens/
    │   ├── HomeScreen.kt        # Tab navigation, category filter, channel grid
    │   └── PlayerScreen.kt      # Media3 ExoPlayer with OSD, channel surfing & aspect toggle
    └── dialogs/
        ├── UpdateDialog.kt      # Interactive GitHub update checker & installer
        └── SettingsDialog.kt    # Custom M3U playlist manager & app details
```