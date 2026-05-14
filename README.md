# Japanese Book Translator (Android)

Capture pages of a Japanese book → ML Kit on-device OCR (Japanese) → ML Kit on-device translation (JA→VI) → export a Vietnamese PDF.

All processing happens on the device. No data leaves the phone after the initial model download.

## Features
- CameraX-based capture flow
- On-device Japanese text recognition (Google ML Kit `text-recognition-japanese`)
- On-device Japanese → Vietnamese translation (Google ML Kit `translate`)
- Multi-page session: review, delete, retake
- Export to a single A4 PDF with the scanned image + translated text per page

## Build

### Locally (with Android Studio)
1. Open this folder in Android Studio Iguana or newer.
2. Let Gradle sync (will download Android SDK 34, build-tools, dependencies).
3. Run on a device/emulator (minSdk 24, Android 7.0+).

### CI build (GitHub Actions, free)
Push to GitHub. The workflow in `.github/workflows/build.yml` runs `gradle assembleDebug` and uploads the APK as an artifact. See `HUONG_DAN.md` (Vietnamese) for step-by-step instructions.

## Architecture
- `MainActivity` — entry point, page count, export PDF, clear pages
- `CameraActivity` — CameraX preview + shutter, kicks off processing pipeline
- `OcrEngine` — wraps `TextRecognition` with `JapaneseTextRecognizerOptions`
- `TranslationManager` — wraps `Translation.getClient` with JA→VI, lazily downloads model
- `PageStore` — persists captured pages (JSON file in `filesDir`)
- `PdfExporter` — builds an A4 PDF using `android.graphics.pdf.PdfDocument`. Each input page emits: 1 image page + 1+ text pages (paginated)
- `ResultActivity` — preview of OCR + translation for a single captured page

## Notes
- The ML Kit translation model is small (~30 MB) and downloads on first use.
- Vietnamese rendering in `PdfDocument` uses the system font; on Android 7+ Vietnamese diacritics render correctly.
- Layout reconstruction (positional overlay onto the original image) is intentionally out of scope — ML Kit returns line boxes but not styling, so reproducing the book’s typography is not reliable. The exporter pairs original image + translated text instead.
