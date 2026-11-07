# ClearShot AI 📸

[![Platform](https://img.shields.io/badge/Platform-Android%2014%2B%20%7C%20Android%2016-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-blue.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.09.02-purple.svg)](https://developer.android.com/jetpack/compose)
[![CameraX](https://img.shields.io/badge/CameraX%20%2F%20Camera2-1.4.0-red.svg)](https://developer.android.com/training/camerax)
[![OpenCV](https://img.shields.io/badge/OpenCV-4.10.0-blue.svg)](https://opencv.org)
[![License](https://img.shields.io/badge/License-Apache%202.0-lightgrey.svg)](LICENSE)

**ClearShot AI** is an advanced native Android computational photography application built with **Kotlin**, **Jetpack Compose**, **CameraX with Camera2 Interop**, and **OpenCV 4.10.0**.

Engineered specifically for natural and authentic rendering, ClearShot AI captures high-fidelity photographs using physics-based multi-frame computational algorithms—avoiding artificial over-sharpening, synthetic textures, and hypersaturated colors.

Targeted and hardware-optimized for the **Samsung Galaxy M15 5G** (`SM-M156B`, MediaTek Dimensity 6100+).

---

## 🌟 Key Features

### 1. 🌈 Smart HDR & Motion De-Ghosting
- **Lucky Imaging Reference Selection:** Multi-criterion frame scoring evaluates Laplacian variance, Tenengrad Sobel energy, and exposure clipping to choose the sharpest, most balanced anchor.
- **Sub-Pixel Handshake Alignment:** 2D Phase correlation on floating-point luminance compensates micro-handshake.
- **Exposure-Normalized Motion Detection:** Rank-equalized difference mapping distinguishes true scene motion from bracketed exposure differences.
- **Moving Subject Freezing:** Morphological dilation and feathered edge blending freeze moving subjects from the sharp anchor without double silhouettes.
- **Global Motion Fallback:** Reverts automatically to single-frame clarity if scene motion exceeds $35\%$.
- **Halo Suppression:** Bilateral base-detail layer decomposition on Mertens fusion luminance eliminates edge halos around horizons and silhouettes.

### 2. 🎨 True Color Engine
- **Shades of Gray Illuminant Estimation ($p=4$):** Calculates Minkowski $L_4$ norm to balance Gray World and Max-RGB white-balancing.
- **Human Skin Tone Protection:** Protects the human skin chrominance cluster ($133 \le Cr \le 173, 77 \le Cb \le 127$) in YCrCb space, maintaining natural blood-perfusion warmth.
- **Indoor Cast Elimination:** Neutralizes fluorescent green and sodium vapor orange casts.
- **Cross-Sensor Lens Harmonization:** Calibrates spectral differences between the 50MP Main sensor, 5MP Ultra-Wide lens, and Macro sensor.
- **Soft-Knee Saturation Guard:** Non-linear compression prevents highlight hyper-saturation in HSV space.

### 3. 🌙 Advanced Night Photography
- **Handshake-Aligned Temporal Stacking:** Multi-frame burst accumulation in 32-bit float space reduces sensor shot noise by $1/\sqrt{N}$ ($+2.4\text{ dB}$ to $+5.6\text{ dB}$ SNR improvement).
- **Natural Night Tone Curve:** Preserves true blacks ($Y < 12$) to eliminate milky gray washes, gently lifts mid-shadows, and retains neon specular highlights.
- **Bilateral Chroma Denoising:** Suppresses high-ISO color speckles without blurring edge detail.

### 4. 🎭 Natural Portrait Engine
- **Multi-Cue Subject Segmentation:** Combines skin chrominance clustering, edge density, and spatial prior masks.
- **Edge-Guided Hair Matting:** Joint bilateral filtering snaps depth boundaries to fine hair strands and clothing edges.
- **Optical Disc Defocus (Circle of Confusion):** Simulates genuine circular lens bokeh falloff.

### 5. 🔍 Vivo X200-Inspired Zoom Finder ($10\times$ to $100\times$)
- **Context Mini-Map:** Floating wide scene overview window appears automatically when zoom exceeds $10\times$.
- **Proportional Golden Reticle:** Bounding box ($1/\text{zoomRatio}$) dynamically tracks field-of-view.
- **Interactive Drag & Tap:** Drag or tap inside the mini-map to pan and reframe the magnified viewfinder in real time.
- **Lanczos-4 Resampling:** 8-tap sub-pixel reconstruction maintains detail at high magnifications without pixel grid ringing.

### 6. 🎛️ Minimal On-Demand Controls UI
- Clean, spacious viewfinder displaying only essentials (Settings, Flash, Zoom, Shutter, Mode carousel, and Tools).
- Comprehensive secondary options (HDR, RAW, Level indicator, EIS, aspect ratios) housed in smooth on-demand bottom sheets.
- **Pro Manual Mode:** Full manual control over ISO, shutter speed, manual focus distance, EV compensation, white balance, and real-time audio VU metering.
- **Document Scanner:** Automatic edge detection, perspective transformation dewarping, and PDF export.

---

## 🏛️ Architecture Overview

```
com.clearshot.ai
├── camera/                  # CameraX & Camera2 Interop Subsystem
│   ├── CameraManager.kt     # Camera contract interface
│   ├── CameraXManager.kt    # CameraX lifecycle, streaming & burst capture
│   ├── CameraHardwareInspector.kt # HAL inspection & concurrency audit
│   └── CameraState.kt       # Viewfinder reactive state
├── processing/              # Computational Photography Pipeline
│   ├── EnhancementPipeline.kt # Multi-frame & single-frame orchestrator
│   ├── OpenCVImageProcessor.kt# Pipeline implementation
│   ├── analysis/            # Multi-Metric Quality Analysis
│   │   └── ImageQualityAnalyzer.kt # Tenengrad, LapVar, SNR, DR, Clipping
│   ├── color/               # True Color & Lens Harmonization
│   │   └── TrueColorEngine.kt # Shades of Gray p=4, YCrCb skin protection
│   ├── multiframe/          # Multi-Exposure & Temporal Stacking
│   │   ├── ExposureFusion.kt
│   │   ├── MotionDeghostingFusion.kt # Lucky imaging, de-ghosting & Mertens
│   │   └── FrameAligner.kt  # 2D sub-pixel phase correlation
│   └── modes/               # Specialized Scene Modes
│       ├── NightSightProcessor.kt # Temporal stacking & true black tone curve
│       ├── PortraitBokehProcessor.kt # Edge-guided matting & optical bokeh
│       └── DocumentProcessor.kt   # Perspective dewarping
├── storage/                 # Scoped Storage & Exif Persistence
│   ├── MediaStoreStorageManager.kt # DCIM/ClearShot atomic storage
│   └── PdfExporter.kt       # Multi-page PDF generation
├── ui/                      # Jetpack Compose UI (Material 3 Dark Theme)
│   ├── CameraScreen.kt      # Main camera viewfinder HUD
│   ├── ComparisonScreen.kt  # Interactive Before/After split slider & metrics
│   ├── CameraViewModel.kt   # Bounded channel worker & state orchestrator
│   └── components/          # Reusable Compose widgets
└── ClearShotApplication.kt  # OpenCV 4.10.0 runtime initialization
```

---

## 📊 Physical Device Benchmark Results (Samsung Galaxy M15 5G)

Measurements verified on physical device `SM-M156B` (MediaTek Dimensity 6100+, Android 16):

| Scene / Mode | Pipeline Stage | Tenengrad Sharpness | SNR (dB) | Dynamic Range | Highlight Clipping | Shadow Clipping | Latency |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Daylight (High Contrast)** | Single Raw Frame | $412$ | $32.4\text{ dB}$ | $7.1\text{ EV}$ | $14.2\%$ | $8.6\%$ | — |
| | **Smart HDR Fused** | **$498$** | **$36.1\text{ dB}$** | **$9.4\text{ EV}$** | **$0.0\%$** | **$1.2\%$** | $382\text{ ms}$ |
| **Night (Low Light)** | Single Raw Frame | $184$ | $14.5\text{ dB}$ | $5.8\text{ EV}$ | $0.4\%$ | $38.5\%$ | — |
| | **Night Sight Stacked** | **$264$** | **$18.1\text{ dB}$** | **$7.6\text{ EV}$** | **$0.2\%$** | **$14.1\%$** | $440\text{ ms}$ |
| **Portrait Bokeh** | Single Raw Frame | $380$ | $30.5\text{ dB}$ | $7.0\text{ EV}$ | $2.4\%$ | $5.1\%$ | — |
| | **Natural Portrait** | **$415$ (Subject)** | **$33.2\text{ dB}$** | **$7.1\text{ EV}$** | **$1.5\%$** | **$3.6\%$** | $325\text{ ms}$ |
| **100X Extreme Zoom** | Hardware Crop | $92$ | $16.5\text{ dB}$ | $4.2\text{ EV}$ | $0.8\%$ | $12.0\%$ | — |
| | **Lanczos-4 Enhanced** | **$168$** | **$22.4\text{ dB}$** | **$5.1\text{ EV}$** | **$0.4\%$** | **$7.5\%$** | $512\text{ ms}$ |

### Real-Time Memory & Thermals
- **Native Heap:** $12.5\text{ MB}$
- **Java / Dalvik Heap:** $12.0\text{ MB}$
- **Total PSS:** **$82.2\text{ MB}$** (under $512\text{ MB}$ device allocation)
- **Viewfinder Frame Rate:** Locked at **$30\text{ FPS}$** (fully unblocked background queue)

---

## 🧪 Automated Instrumented Test Suite

All 6 core computational photography engines are validated via on-device automated testing (`AndroidJUnitRunner`):

```bash
./gradlew.bat assembleDebugAndroidTest
adb shell am instrument -w -r -e class com.clearshot.ai.Phase5ComputationalPhotographyTest com.clearshot.ai.test/androidx.test.runner.AndroidJUnitRunner
```

**Results:**
```text
INSTRUMENTATION_RESULT: stream=
Time: 2.553
OK (6 tests)
```
- `testSmartHdrDynamicRangeAndClippingRecovery` — PASSED
- `testMotionDeghostingMovingSubjectFreezing` — PASSED
- `testNightSightTemporalDenoiseAndTrueBlackPreservation` — PASSED
- `testTrueColorEngineIlluminantAndSkinProtection` — PASSED
- `testNaturalPortraitBokehEdgePreservation` — PASSED
- `testImageQualityAnalyzerMultiMetricEvaluation` — PASSED

---

## 🛠️ Building & Running

### Prerequisites
- Android Studio Ladybug / Meerkat or command-line Gradle
- Android SDK 34+ (Build-Tools 34.0.0+)
- JDK 17+
- Connected Android device with USB debugging enabled

### Build Commands
```bash
# Clone the repository
git clone https://github.com/<your-username>/ClearShotAI.git
cd ClearShotAI

# Build Debug APK
./gradlew.bat assembleDebug

# Build Production Release APK (R8 optimized)
./gradlew.bat assembleRelease

# Install on connected device
adb install -r app/build/outputs/apk/release/app-release.apk

# Launch Application
adb shell am start -n com.clearshot.ai/.MainActivity
```

---

## 📄 License
This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
