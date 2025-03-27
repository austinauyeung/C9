<div align="center">
<img src='./app/src/main/ic_launcher-playstore.png' width=100>
</div>

---

# C9: Click on 9 keys
![GitHub release (latest by date)](https://img.shields.io/github/v/release/austinauyeung/C9) ![GitHub all releases](https://img.shields.io/github/downloads/austinauyeung/C9/total) ![License](https://img.shields.io/github/license/austinauyeung/C9) ![API Level](https://img.shields.io/badge/API-26%2B-brightgreen)

<div align="center">
<img src='./docs/imgs/Screenshot_20250319_213956.png' width=200>
<img src='./docs/imgs/Screenshot_20250319_214019.png' width=200>
</div>

C9 is a dual-cursor application that takes inspiration from T9 to provide clicks using the numpad on Android feature phones. The primary features of the application include:

- 🤖 Android 8.0+ support
- 🌎 Universal Android 11 compatibility via Shizuku as needed
- ⚡ Introduction of a grid cursor focused on efficiency
- 🖱️ Standard cursor to provide a traditional proxy for touchscreen gestures
- ⚙️ Remappable cursor activation keys and integration with button mappers
- 🔀 Translation of key presses into near-native taps, double taps, long press (and drag), scrolling, and zoom

## Overview
### Grid Cursor

<br />

<div align="center">
<img src='./docs/gifs/Screen_recording_20250321_015105.gif' width=200>
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

The following options can be configured:
- Grid cursor activation key
- Number of grid levels
- Grid persistence after clicking in the final grid
- Grid opacity
- Hide grid numbers
- Adjust grid line visibility

#### Instructions
- The default activation key is the pound (#) key.
- To activate the grid cursor:
    - Hold the activation key.
    - Alternatively, you can use a button mapper to map the "Activate Grid Cursor" shortcut. However, an activation key must still be assigned.
- When activated, press the activation key to quickly reset any grid back to the main grid.
- When activated, press any number to advance to the next subgrid.
- See the table below for gesture dispatch.
- To deactivate, hold the activation key.
    - If you are using a button mapper, it may be possible to use your button mapper to deactivate the cursor as long as it does not conflict with buttons reserved and intercepted by the cursor.

#### Usage
| Gesture | Mapped buttons | Dispatch location | Advances grid |
| --- | --- | --- | --- |
| Navigate grid | Click numpad 1-9. | The selected number. | True |
| Tap | Click numpad 1-9 in the final grid level. | The center of the selected number's cell. | True |
| Tap | Click D-pad center. | If a number is first held, the center of its cell in the current grid. Else, center of the screen. | False |
| Double Tap | Double click D-pad center. | If a number is first held, the center of its cell in the current grid. Else, center of the screen. | False |
| Scroll | Click D-pad directions. | If a number is first held, the center of its cell in the current grid. Else, center of the screen. | False |
| Zoom | Click star (*) and pound (#). | If a number is first held, the center of its cell in the current grid. Else, center of the screen. | False |

### Standard Cursor

<br />

<div align="center">
<img src='./docs/gifs/Screen_recording_20250319_004530.gif' width=300>
</div>

<br />

A standard cursor is included for actions requiring more precision and for those who strictly prefer a traditional pointer.

The following options can be configured:
- Standard cursor activation key
- Control scheme
  - Standard: D-pad moves, numpad scrolls
  - Swapped: D-pad scrolls, numpad moves
  - Toggle: D-pad scrolls and moves (toggle using activation key)
- Enable cursor wrap around
- Cursor speed
- Cursor acceleration: accelerated cursor speed when held
- Cursor acceleration threshold: duration after which cursor speed is accelerated
- Cursor size

#### Instructions
- The default activation key is the star (*) key.
- To activate the standard cursor:
    - Hold the activation key.
    - Alternatively, you can use a button mapper to map the "Activate Grid Cursor" shortcut. However, an activation key must still be assigned.
- See the table below for gesture dispatch.
- When activated, either D-pad center or numpad 5 can be used to long press. For example, in the standard control scheme, it may be easier to long press numpad 5 instead of D-pad center and then press one of the D-pad directions to long press and drag.
- When activated and if in the toggle control scheme, press the activation key to toggle between cursor movement and scrolling.
- To deactivate, hold the activation key.
    - If you are using a button mapper, it may be possible to use your button mapper to deactivate the cursor as long as it does not conflict with buttons reserved and intercepted by the cursor.

#### Usage
All gestures are dispatched at the cursor's current location:

| Gesture | Mapped buttons |
| --- | --- |
| Cursor Movement | Click D-pad directions or numpad 2/4/6/8 (depends on control scheme). |
| Tap | Click D-pad center or numpad 5. |
| Double Tap | Double click D-pad center or numpad 5. |
| Long Press/Drag | Hold D-pad center or numpad 5 to long press, then move cursor to drag. Release D-pad center or numpad 5 to end the gesture. |
| Scroll | Click D-pad directions or numpad 2/4/6/8 (depends on control scheme). |
| Zoom | Numpad 1 and 3. |

### Common
Because of their different navigation paradigms, each cursor mode maps gestures uniquely as shown in the above tables. While both modes can be **enabled** simultaneously (by setting an activation key), only one cursor can be **active** at a time. As a final note, <ins>all buttons in the numpad and D-pad are generally reserved/intercepted while the cursor is active</ins>.

The following options can be configured, which affects scrolls and zooms in both modes:
- Natural scrolling
- Gesture visualizations
- Gesture style
  - Fixed: gestures are controlled and fixed distance
  - Inertia: gestures resemble touchscreen flicks
- Gesture duration
- Scroll distance

## Installation
The latest version can be found under [releases](https://github.com/austinauyeung/C9/releases).

### Option 1
Install using the standard package installer. Allow the accessibility service using the banner in the application.

### Option 2
Install using adb:
```
>> adb install path/to/apk
>> adb shell settings put secure enabled_accessibility_services com.austinauyeung.nyuma.c9/com.austinauyeung.nyuma.c9.accessibility.service.OverlayAccessibilityService
```

### Additional installation for Android 11
If you are on Android 11, please first try the application as-is. If gestures cannot be dispatched successfully, or if you have had trouble in the past with other cursor apps, you will need to [install Shizuku](https://shizuku.rikka.app/guide/setup/) to use this application. Once installed, navigate to, and enable, `C9 > Debug Options > Enable Shizuku Integration`.

Note that unless your device is rooted, you will need to restart the Shizuku service upon reboot.

## Known Issues
- On the Vortex V3, the numpad backlight may not function when the cursor is active.
    - This is likely due to the cursors' interception of key presses. There is an experimental setting "Allow Passthrough" that may fix this at the expense of unintended behavior in the underlying application.

## FAQs
### Where can I make feature suggestions or report bugs?
Thanks for using and testing C9! You can use the [issues](https://github.com/austinauyeung/C9/issues) tab for both. For bugs, please provide logs using the built-in logger or `adb logcat C9App:V *:S`.

### How else can I contribute?
Please feel free to submit a pull request, create a video walkthrough, or provide anything else you think would be helpful!

### What is Shizuku?
Shizuku allows applications in general to perform actions that require elevated privileges. In C9, it is required to dispatch gestures on Android 11 using [InputManager](https://developer.android.com/reference/android/hardware/input/InputManager) instead of the standard dispatch using [AccessibilityService](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService).

### What does it mean for the cursor to intercept button presses?
The cursors sit between your button presses and the underlying application. If a button is used by the cursor, the cursor will consume it and prevent the underlying application from receiving the button press.

### Why are button mappers allowed for activation but not deactivation?
As a result of the cursors' interception, there is no guarantee that there will not be conflict with your button mapper shortcut. Because of this, and for now, deactivation must be performed through the cursor.

## License
[Apache License Version 2.0](./LICENSE)

## Acknowledgements
- Allegra, [Arlie](./docs/imgs/IMG_4194.jpg), and [Nyuma](./docs/imgs/IMG_3226.jpg) for their support
- `sam-club` for extensive testing
- `Dev-in-the-BM` for testing and the Shizuku suggestion
- `anonymousfliphones` for testing