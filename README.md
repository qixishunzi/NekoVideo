# Neko Video

Neko Video is an Android 14+ player for videos on the phone and on the LAN. It scans the local library, browses WebDAV / SMB / SFTP / FTP shares, and opens HTTP(S) streams including HLS and DASH.

The UI is built with Jetpack Compose and [Miuix](https://github.com/miuix-kotlin-multiplatform/miuix). Playback controls follow an AVKit-style layout: a clean overlay, hold-to-speed, and the usual phone gestures.

[中文说明](README.zh-CN.md)

| | |
| --- | --- |
| Author | 起汐顺子 |
| Repository | https://github.com/qixishunzi/NekoVideo |
| License | [MIT](LICENSE) |
| Version | 1.0.0 |
| Package | `com.neko.video` |
| Min / target SDK | 34 / 37 |

## Screens

The bottom bar has five tabs:

1. **Library** — recently played, folders, all videos
2. **Network** — saved servers and a direct URL box
3. **History** — resume position and completed titles
4. **Favorites** — videos and folders you starred
5. **Settings** — playback, subtitles, audio, network, theme

## Features

### Local library

- Scans device videos after `READ_MEDIA_VIDEO` is granted
- Tabs: recently played / folders / all
- Pick a single file or a whole folder from the system picker
- Folder import walks the tree, not only the top level
- Folders are browsed as a tree: the first screen lists top-level folders only, then you
  descend into sub-folders and videos
- Sorting by name (natural order), date modified, size, or duration, ascending or
  descending; the choice is remembered
- Search the library: fuzzy (query words must appear in order, gaps allowed) or exact
  (each word must be a contiguous substring), over all videos or just the current folder;
  results keep the current sort
- Pull-to-refresh; scan and import jobs cancel if you start another one

### Network sources

| Protocol | Typical use |
| --- | --- |
| WebDAV | NAS, Nextcloud, Alist, and similar DAV roots |
| SMB | Windows / Samba shares (share name required) |
| SFTP | SSH file servers |
| FTP | Classic FTP, including anonymous login |
| HTTP(S) | Direct file, HLS (`.m3u8`), DASH |

You can test a server before saving it, browse folders in place (back goes to the parent, not out of the app), filter to videos only, and play a folder as a playlist.

Passwords are encrypted with Android Keystore (AES-GCM) and never leave the device.

### Player

- Media3 ExoPlayer
- Horizontal swipe to seek, vertical swipe for volume / brightness
- Double-tap seek (interval is configurable)
- Long-press speed-up (2x, or 3x if you are already at 2x)
- Pinch to zoom
- Resume dialog, or start over
- Auto play next item in the current playlist
- External subtitle files, plus audio / subtitle track picker
- Aspect modes: fit, fill, crop, stretch, 16:9, 4:3
- Landscape fullscreen hides system bars; leaving the player restores portrait
- Picture-in-Picture
- Background play through a Media3 session / notification
- Opens `http` / `https` / `content` / `file` video intents from other apps
- Optional warning when you start a network stream on mobile data

Returning from the player clears the video surface immediately so the last frame does not linger on the previous screen.

### Appearance

- Light, dark, or follow system
- Dynamic color from wallpaper (Material You / Monet), or a fixed key color
- AMOLED black in dark mode
- Optional blur on the top and bottom bars (Android 13+)
- Optional floating pill navigation bar and liquid-glass treatment

## Gestures

| Gesture | Action |
| --- | --- |
| Tap | Show / hide controls |
| Double-tap left / right | Seek backward / forward |
| Horizontal drag | Seek |
| Vertical drag (left) | Brightness |
| Vertical drag (right) | Volume |
| Long-press | Temporary speed-up |
| Pinch | Scale the picture |
| Back (locked) | Unlock first, then leave |

## Settings

**Playback:** auto resume, auto play next, keep screen on, background play, PiP, default speed, double-tap seek seconds, completion threshold, default aspect ratio.

**Subtitles / audio:** preferred languages, subtitle size, audio focus.

**Network:** default User-Agent (app / Android / Chrome / custom), extra headers, timeout, mobile-data warning.

**Theme:** mode, dynamic color, key color, palette, AMOLED, blur, floating bar, liquid glass.

**Other:** clear history, clear cache, about, and a row that opens this GitHub repository.

## Requirements

- A phone or emulator on Android 14 or newer (`minSdk 34`)
- arm64-v8a (the release ABI filter matches current Android 14 devices)
- Android Studio with JDK 17 (the bundled JBR is enough)

## Build

Clone and assemble a debug APK:

```bash
git clone https://github.com/qixishunzi/NekoVideo.git
cd NekoVideo
./gradlew :app:assembleDebug
```

Windows:

```bat
gradlew.bat :app:assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/`.

For a release build, add your own signing config. Do not commit a keystore, `keystore.properties`, or `local.properties`.

```bat
gradlew.bat :app:assembleRelease
```

## Project layout

```
app/src/main/java/com/neko/video/
  ui/           Compose screens (library, network, player, settings)
  player/       Media3 engine, session, cache
  remote/       SMB / SFTP / FTP clients
  webdav/       WebDAV client
  repository/   Room + media access
  settings/     DataStore preferences
```

## Stack

| Area | Library |
| --- | --- |
| Language | Kotlin 2.x |
| UI | Jetpack Compose, [Miuix](https://github.com/miuix-kotlin-multiplatform/miuix), MaterialKolor |
| Playback | AndroidX Media3 1.7 (ExoPlayer, HLS, DASH, UI, session) |
| Storage | Room, DataStore, Android Keystore |
| Images | Coil |
| Network | OkHttp, SMBJ, SSHJ, Apache Commons Net |

## Privacy

- No account, no analytics, no cloud sync
- Watch history and favorites stay in the on-device Room database
- Server passwords are Keystore-encrypted
- Cleartext HTTP is allowed so LAN NAS boxes keep working; use HTTPS when you can, and only join networks you trust

## Contributing

Issues and pull requests are welcome. Please keep the UI in the existing Miuix style, and do not add tracking or upload of user data.

## License

[MIT](LICENSE) © 2026 起汐顺子
