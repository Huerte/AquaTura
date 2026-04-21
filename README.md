<div align="center">

# AquaTura PH

[![Version](https://img.shields.io/badge/version-1.0-blue.svg)](#)
[![Platform](https://img.shields.io/badge/platform-Android-blueviolet.svg)](#)
[![License](https://img.shields.io/badge/license-AGPL--3.0-green.svg)](#)

**AI-powered fish species identification for sustainable Philippine fisheries**

</div>

---

## Features

AquaTura PH is a mobile application designed for fisherfolk, seafood consumers, and regulatory agencies to identify fish species using AI-driven image recognition. Built to be **accurate**, **accessible**, and **actionable**.

- **Species Classification:** Identifies 31 fish species commonly found in Philippine waters with 96.29% overall accuracy.

- **Two-Stage Detection Pipeline:** Non-fish filtering followed by species classification ensures reliability and reduces false positives.

- **Camera Capture & Gallery Import:** Uses Android CameraX for responsive image capture with live preview, or import existing photos from your gallery.

- **Detailed Fish Information:** View species details sourced from FishBase, conservation status, and similar species alternatives.

- **Confidence Thresholds:** Two-tier confidence system (65% high confidence, 45% detection threshold) balances accuracy and usability.

- **Responsive Layout:** Adapts seamlessly across tablets and smartphones using Material Design 3 guidelines.

- **Offline Classification:** Core species identification works without internet connectivity. Detailed fish information and "Learn More" links require a connection.

---

## Installation

### Download the App

Head to the [**Releases**](../../releases) page and download the latest APK file (`AquaTura-vX.X.apk`) from the most recent release tag. Install it on your Android device:

1. Download the `.apk` file from the latest release
2. On your Android device, allow installation from unknown sources if prompted
3. Open the downloaded APK to install
4. Launch AquaTura PH

> **Minimum Requirements:** Android 5.0 (API 21) or higher

---

### Developer Setup

If you want to build the project from source:

#### Prerequisites

- Android Studio 4.2+
- Android SDK 21+ (Target SDK 34+)
- JDK 11+

#### Build Steps

```bash
git clone <REPOSITORY_URL>
cd AquaTura
./gradlew assembleDebug
```

Place `fish_model.tflite` and `labels.txt` in `app/src/main/assets/` before building.

---

## Usage

### Basic Workflow

1. **Launch the App** — Open AquaTura PH on your Android device
2. **Select Image Source** — Choose between camera capture or gallery import
3. **Capture/Select Fish Image** — Ensure the fish is clearly visible and well-lit
4. **Wait for Analysis** — The app processes the image through two stages:
   - Stage 1: Non-fish filtering (rejects non-fish objects)
   - Stage 2: Species classification
5. **Review Results** — View identified species with confidence score
6. **Explore Details** — Tap to see:
   - Species information from FishBase
   - Conservation status
   - Similar species alternatives
   - Learn More links (requires internet)
7. **Report Issues** — Use the "Report Error" button to send feedback

### Tips for Best Results

- **Lighting:** Use natural or well-lit conditions
- **Focus:** Ensure the fish is in sharp focus
- **Angle:** Side views work best (avoid extreme angles)
- **Distance:** Fill most of the frame with the fish
- **Background:** Minimize clutter behind the fish

---

## Supported Species (31 Classes)

| # | Species | # | Species |
|---|---------|---|---------|
| 1 | Anchovies (Dilis) | 17 | Great Trevally |
| 2 | Asian Catfish (Hito) | 18 | Green Spotted Puffer (Botete) |
| 3 | Bangus (Milkfish) | 19 | Grouper |
| 4 | Barramundi | 20 | Grunt |
| 5 | Bigeye Trevally | 21 | Herring |
| 6 | Bighead Carp | 22 | Horse Mackerel |
| 7 | Black Pomfret | 23 | Indian Mackerel |
| 8 | Cardinalfish | 24 | Jack Mackerel |
| 9 | Climbing Perch (Sepat) | 25 | Japanese Sea Bream |
| 10 | Crevalle Jack | 26 | Moonfish |
| 11 | Emperor Fish | 27 | Mudfish (Luwak) |
| 12 | Flounder | 28 | Mullet |
| 13 | Freshwater Eel | 29 | Scad |
| 14 | Garfish | 30 | Snapper |
| 15 | Goby (Ito) | 31 | *Jellyfish (rejected as non-fish)* |
| 16 | Grass Carp | | |

**Overall Accuracy:** 96.29% · **Minimum Species Accuracy:** 85%

---

## Model Information

- **Architecture:** YOLOv8n-cls (Ultralytics)
- **Input Size:** 224×224 RGB images
- **Output:** 31-class fish species classification
- **Format:** TensorFlow Lite (int8 quantized)
- **Model Size:** <5MB (optimized)

---

## Known Issues & Limitations

### Model Limitations
- **Limited to 31 species:** Cannot identify fish outside trained classes
- **Confusion pairs:** Bangus/Tenpounder, Mudfish/Freshwater Eel
- **Life stage variations:** May struggle with juvenile vs adult fish
- **Image quality dependent:** Poor lighting/blur reduces accuracy
- **Viewing angle sensitive:** Side views work best

### Current App Limitations
- **Requires internet for full features:** "Learn More" needs web browser
- **Single fish only:** Cannot identify multiple fish in one image
- **Still images only:** Video analysis not yet implemented
- **Offline database:** Fish details embedded in app (may become outdated)

### Data Limitations
- **Imbalanced dataset:** Grass Carp (1,258 images) vs Green Spotted Puffer (111 images)
- **Geographic bias:** Primarily Philippine fish species
- **Limited variations:** More diverse angles and conditions needed

---

## Project Structure

```
AquaTura/
│
├── app/                              # Main application module
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/               # Kotlin/Java source code
│   │   │   │   ├── ui/             # UI components and activities
│   │   │   │   ├── ml/             # TensorFlow Lite integration
│   │   │   │   ├── data/           # Data models and repositories
│   │   │   │   └── util/           # Utility functions
│   │   │   ├── res/                # Resources (layouts, drawables, strings)
│   │   │   ├── assets/             # TFLite model and labels
│   │   │   └── AndroidManifest.xml
│   │   ├── androidTest/            # Instrumented tests
│   │   └── test/                   # Unit tests
│   ├── build.gradle.kts            # App-level build configuration
│   └── proguard-rules.pro          # ProGuard obfuscation rules
│
├── gradle/
│   └── wrapper/                    # Gradle wrapper
│
├── build.gradle.kts                # Root build configuration
├── settings.gradle.kts             # Gradle settings
├── gradle.properties               # Gradle properties
├── local.properties                # Local environment config (git-ignored)
├── .gitignore                      # Git ignore rules
└── README.md                       # This file
```

---

## Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| Model predicts wrong species with high confidence | Unclear image or poor angle | Retake photo with better lighting and side angle |
| App shows "No fish detected" | Fish doesn't fill frame | Move closer or crop tighter in gallery |
| Model returns "Unknown species" | Fish not in 31-class dataset | Use "Report Error" to request new species |
| Slow inference on device | Running on CPU instead of GPU | Check TFLite delegate configuration |
| App crashes on startup | Model file missing from assets | Ensure `fish_model.tflite` in `app/src/main/assets/` |
| Camera permission denied | Permission not granted | Grant camera permission in app settings |

---

## Contributing

### Code Contribution

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/improvement`)
3. Commit changes (`git commit -am 'Add improvement'`)
4. Push to branch (`git push origin feature/improvement`)
5. Open a Pull Request

### Data Collection Guidelines

To improve model accuracy, follow these guidelines when collecting fish images:

1. **High Quality:** Minimum 640×640 resolution
2. **Clear Focus:** Fish should be in sharp focus
3. **Good Lighting:** Natural or well-lit conditions preferred
4. **Single Fish:** One fish per image
5. **Full Body:** Complete fish visible (not cropped)
6. **Variety:** Multiple angles, sizes, and backgrounds
7. **No Duplicates:** Check for similar images

---

## License

Distributed under the AGPL-3.0 License. See `LICENSE` for details.

**Attribution:**
- **Model Framework:** Ultralytics YOLOv8 (AGPL-3.0)
- **Fish Data:** Public domain sources, FishBase.se
- **Icons:** Material Icons by Google
- **Camera:** Android CameraX library

---

## References

- [Ultralytics YOLOv8 Documentation](https://docs.ultralytics.com)
- [TensorFlow Lite Documentation](https://www.tensorflow.org/lite)
- [FishBase — Fish Species Database](https://www.fishbase.se)
- [IUCN Red List](https://www.iucnredlist.org)
- [Material Design 3](https://m3.material.io)
- [BFAR — Bureau of Fisheries and Aquatic Resources](https://www.bfar.da.gov.ph)

---

## Team & Acknowledgments

**Development Team:**
- AI/ML: Model training and optimization
- Mobile Development: Android app implementation
- UI/UX: Material Design interface
- Testing & QA: Comprehensive validation

---

© 2026 AquaTura PH. All Rights Reserved.
