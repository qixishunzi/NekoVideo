# Neko Video

Neko Video 是面向 Android 14+ 的本地 + 局域网播放器。可以扫描手机里的视频，浏览 WebDAV / SMB / SFTP / FTP，也可以直接打开 HTTP(S) 点播，包括 HLS 和 DASH。

界面用 Jetpack Compose 和 [Miuix](https://github.com/miuix-kotlin-multiplatform/miuix)。播放器控件参考 AVKit：干净的浮层、长按加速、以及常见的手机手势。

[English](README.md)

| | |
| --- | --- |
| 作者 | 起汐顺子 |
| 仓库 | https://github.com/qixishunzi/NekoVideo |
| 许可 | [MIT](LICENSE) |
| 版本 | 1.0.0 |
| 包名 | `com.neko.video` |
| 最低 / 目标 SDK | 34 / 37 |

## 页面

底栏五个入口：

1. **媒体库** — 最近播放、文件夹、全部视频
2. **网络** — 已保存的服务器，以及直接打开地址
3. **历史** — 续播进度和已看完的条目
4. **收藏** — 标星的视频和文件夹
5. **设置** — 播放、字幕、音频、网络、主题

## 功能

### 本地媒体库

- 授予 `READ_MEDIA_VIDEO` 后扫描本机视频
- 三个分页：最近播放 / 文件夹 / 全部
- 可用系统选择器挑单个文件，或导入整个文件夹
- 文件夹导入会递归子目录，不只扫第一层
- 文件夹按层级浏览：首屏只列出顶层文件夹，进入后再看子文件夹与视频
- 排序：名称（数字自然序）、修改时间、大小、时长，均可升降序，选择会被记住
- 媒体库搜索：模糊（词序命中即可跳字）或精确（需为连续子串），范围可选全部视频或当前文件夹，结果沿用当前排序
- 支持刷新；新的扫描 / 导入会取消上一次任务

### 网络源

| 协议 | 常见用途 |
| --- | --- |
| WebDAV | NAS、Nextcloud、Alist 等 DAV 根目录 |
| SMB | Windows / Samba 共享（需要填写共享名） |
| SFTP | SSH 文件服务 |
| FTP | 传统 FTP，支持匿名登录 |
| HTTP(S) | 直链、HLS（`.m3u8`）、DASH |

保存前可以先测试连接。进子目录后返回键回到上一级，而不是直接退出。可以只显示视频，也可以把当前文件夹当成播放列表连播。

服务器密码用 Android Keystore（AES-GCM）加密，只存在本机。

### 播放器

- Media3 ExoPlayer
- 左右滑动快进快退，上下滑动调音量 / 亮度
- 双击快进快退（间隔可在设置里改）
- 长按加速（默认 2 倍，已经是 2 倍再长按到 3 倍）
- 双指缩放画面
- 续播弹窗，也可以从头播放
- 播放列表自动播下一集
- 外挂字幕，以及音轨 / 字幕轨切换
- 画面比例：适应、填充、裁剪、拉伸、16:9、4:3
- 横屏全屏会隐藏系统栏，退出播放器后恢复竖屏
- 画中画
- 后台播放（Media3 会话 / 通知栏）
- 可接收其他应用传来的 `http` / `https` / `content` / `file` 视频 Intent
- 流量网络播网片时可选提醒

从播放器返回时会立刻清掉画面，避免上一页还残留最后一帧。

### 外观

- 浅色、深色、跟随系统
- 壁纸取色（Material You / Monet），或指定主色
- 深色模式下可选纯黑（AMOLED）
- 顶栏 / 底栏可选模糊（Android 13+）
- 可选悬浮底栏和液态玻璃效果

## 手势

| 手势 | 作用 |
| --- | --- |
| 单击 | 显示 / 隐藏控件 |
| 左 / 右双击 | 后退 / 前进 |
| 左右拖动 | 进度 |
| 左侧上下拖 | 亮度 |
| 右侧上下拖 | 音量 |
| 长按 | 临时加速 |
| 双指捏合 | 缩放画面 |
| 返回（已锁定） | 先解锁，再退出 |

## 设置项

**播放：** 自动续播、自动下一集、保持亮屏、后台播放、画中画、默认倍速、双击跳转秒数、完播阈值、默认画面比例。

**字幕 / 音频：** 默认语言、字幕大小、音频焦点。

**网络：** 默认 User-Agent（应用 / Android / Chrome / 自定义）、额外请求头、超时、流量提醒。

**主题：** 模式、动态取色、主色、配色风格、AMOLED、模糊、悬浮底栏、液态玻璃。

**其他：** 清除历史、清除缓存、关于，以及一键打开本 GitHub 仓库。

## 环境要求

- Android 14 及以上真机或模拟器（`minSdk 34`）
- arm64-v8a（发布包按当前 Android 14 设备裁了 ABI）
- 带 JDK 17 的 Android Studio（自带 JBR 即可）

## 构建

```bash
git clone https://github.com/qixishunzi/NekoVideo.git
cd NekoVideo
./gradlew :app:assembleDebug
```

Windows：

```bat
gradlew.bat :app:assembleDebug
```

产物在 `app/build/outputs/apk/debug/`。

打正式包请用你自己的签名，不要把 keystore、`keystore.properties`、`local.properties` 提交进仓库。

```bat
gradlew.bat :app:assembleRelease
```

## 目录结构

```
app/src/main/java/com/neko/video/
  ui/           Compose 页面（媒体库、网络、播放器、设置）
  player/       Media3 引擎、会话、缓存
  remote/       SMB / SFTP / FTP 客户端
  webdav/       WebDAV 客户端
  repository/   Room 与媒体访问
  settings/     DataStore 设置
```

## 技术栈

| 模块 | 选型 |
| --- | --- |
| 语言 | Kotlin 2.x |
| UI | Jetpack Compose、[Miuix](https://github.com/miuix-kotlin-multiplatform/miuix)、MaterialKolor |
| 播放 | AndroidX Media3 1.7（ExoPlayer、HLS、DASH、UI、会话） |
| 存储 | Room、DataStore、Android Keystore |
| 图片 | Coil |
| 网络 | OkHttp、SMBJ、SSHJ、Apache Commons Net |

## 隐私

- 无账号、无统计、无云同步
- 播放历史和收藏只存在本机 Room 数据库
- 服务器密码经 Keystore 加密
- 为了局域网 NAS，允许明文 HTTP；能用 HTTPS 就用 HTTPS，只连你信任的网络

## 参与

欢迎提 Issue 和 Pull Request。界面请保持现有的 Miuix 风格，不要加入用户数据上传或统计。

## 许可

[MIT](LICENSE) © 2026 起汐顺子
