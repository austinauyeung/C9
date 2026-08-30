<div align="center">
<img src='./app/src/main/ic_launcher-playstore.png' width=100>
</div>

---

# C9: Click on 9 keys
![GitHub release (latest by date)](https://img.shields.io/github/v/release/austinauyeung/C9) ![Android Version](https://img.shields.io/badge/Android-7.0%2B-brightgreen) ![GitHub all releases](https://img.shields.io/github/downloads/austinauyeung/C9/total) ![License](https://img.shields.io/github/license/austinauyeung/C9)

<div align="center">
<img src='./docs/imgs/Screenshot_20250319_213956.png' width=200>
<img src='./docs/imgs/Screenshot_20250319_214019.png' width=200>
</div>

C9 is a dual-cursor application that takes inspiration from T9 to provide gestures on Android feature phones and Android TV devices. Features of the application include:

- 🌎 Universal Android 7.0+ support via Shizuku as needed
- ⚡ Introduction of a grid cursor focused on efficiency
- 🖱️ Standard cursor to provide a traditional proxy for touchscreen gestures
- ⚙️ Remappable cursor activation keys and integration with button mappers
- 🔀 Translation of key presses into near-native taps, double taps, long press (and drag), scrolling, and zoom
- ✨ Additional quality-of-life features such as cursor auto-hide and restore, clickable detection, and landscape orientation support

## Table of Contents
- [Installation](#installation)
- [Acknowledgments](#acknowledgments)
- [Overview](#overview)
  - [Grid Cursor](#grid-cursor)
  - [Standard Cursor](#standard-cursor)
- [Troubleshooting](#troubleshooting)
- [FAQs](#faqs)
- [License](#license)

## Installation
The latest version can be found under [releases](https://github.com/austinauyeung/C9/releases).

### Option 1
Install using the standard package installer. Allow the accessibility service using the banner in the application.

### Option 2
Install using adb:
```
>> adb install path/to/apk
>> adb shell settings put secure enabled_accessibility_services com.austinauyeung.nyuma.c9/com.austinauyeung.nyuma.c9.accessibility.AppAccessibilityService
```

### Additional installation for certain Android versions
Refer to the following table to determine if you will need to [install Shizuku](https://shizuku.rikka.app/guide/setup/) to use this application:

| Android Version | Shizuku Required | Notes                                                                                                                                                                                                                                                       |
|-----------------| --- |-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 7               | Maybe | Shizuku is needed specifically to support features such as long press and drag. This requirement can be overridden, but gestures will be restricted to just clicks.                                                                                         |
| 8-10            | Maybe | If you are experiencing poor ("blocky") scroll and zoom performance, first try `C9 > Developer Options > Improve Non-Shizuku Gestures`, which will attempt to dispatch smoother gestures but may stutter. For the most optimal gestures, Shizuku is needed. |
| 11-12           | Maybe | Shizuku is needed if the application does not work as-is (i.e. no gestures can be dispatched) and/or you have had trouble in the past with other cursor apps. This may happen on Android 11 phones and Android 12 for TV.                                   |
| 13+             | No |                                                                                                                                                                                                                                                             |

Once installed, navigate to, and enable, `C9 > Developer Options > Enable Shizuku Integration`.

Unless your device is rooted, you will need to manually restart the Shizuku service upon reboot. Alternatively, you can install Shizuku forks, such as [thedjchi/Shizuku](https://github.com/thedjchi/Shizuku) or [thejaustin/ShiuzkuPlus](https://github.com/thejaustin/ShizukuPlus), which automatically start the service on boot.

## Acknowledgments
As the [package name](#option-2) would suggest, this application is dedicated to my late cat, [Nyuma](./docs/imgs/IMG_3226.jpg).

- Allegra and [Arlie](./docs/imgs/IMG_5199.jpg) for their support
- Everyone on the [releases](https://github.com/austinauyeung/C9/releases) page for their feature suggestions and bug reports
- `sam-club` for extensive testing
- `Dev-in-the-BM` for testing and the Shizuku suggestion
- `anonymousfliphones` for testing

## Overview
The majority of modern applications are touch-oriented. The goal of C9 is to mimic the touchscreen gestures required of these applications as closely as possible by adapting button presses in each of the two cursor modes.

Both modes can be enabled simultaneously (by mapping their activation key or shortcut), but only one cursor can be active at a time. Note that while a cursor is active, all numpad and D-pad buttons are intercepted by the application.

For more instructions on using each cursor, see the [wiki](https://github.com/austinauyeung/C9/wiki).

### Grid Cursor
<br />

<div align="center">
<img src='./docs/gifs/Screen_recording_20251023_000250.gif' width=200>
</div>

<br />

The grid cursor trades precision for efficiency, taking advantage of the fact that many interactions with UI elements do not require pixel-by-pixel precision. `n` grid levels produce `9^n` points onscreen that can be reached with at most `n` numpad clicks. The visualizations below show the points that can be reached with two grid levels/clicks (81 points), three grid levels/clicks (729 points), and four grid levels/clicks (6561 points).

<br />

<div align="center">
<img src='./docs/imgs/Screenshot_20250319_003605.png' width=200>
<img src='./docs/imgs/Screenshot_20250319_003623.png' width=200>
<img src='./docs/imgs/Screenshot_20250319_003643.png' width=200>
</div>

<br />

### Standard Cursor
<br />

<div align="center">
<img src='./docs/gifs/Screen_recording_20250405_185611.gif' width=300>
</div>

<br />

A standard cursor is included for actions requiring more precision and for those who strictly prefer a traditional pointer. The demo above shows auto-hiding in text fields as well as long press and drag.

## Troubleshooting
### Verifying Shizuku authorization
A green banner on the main page indicates that Shizuku authorization has been granted to C9. Only the third screenshot below indicates successful authorization.
<div align="center">
<img src='./docs/imgs/Screenshot_20250328_194724.png' width=200>
<img src='./docs/imgs/Screenshot_20250328_194745.png' width=200>
<img src='./docs/imgs/Screenshot_20250328_194815.png' width=200>
</div>

### Cursor does not deactivate
If you are unable to deactivate the cursor, clear the internal activation key, which unmaps that cursor and hides any active cursor, even if it was activated using a button mapper.

## FAQs
### Where can I make feature suggestions or report bugs?
You can use the [issues](https://github.com/austinauyeung/C9/issues) tab for both.

### What is Shizuku?
Shizuku allows applications in general to perform actions that require elevated privileges. In C9, it is required to dispatch gestures on Android 7 and 11 using [InputManager](https://developer.android.com/reference/android/hardware/input/InputManager) instead of the standard dispatch using [AccessibilityService](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService).

## License
[Apache License Version 2.0](./LICENSE)
