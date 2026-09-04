# billedroid

`billedroid` is the official Android companion application for the **bill-e** home automation server daemon. Built using modern Android architecture (Kotlin, Jetpack Compose, Room, and Material 3), `billedroid` acts as an offline-first management dashboard, natural language rule compiler powered by Google Gemini, and secure hardware-backed cryptographic intent signer for your `bill-e` system.

---

## Features

- **AI Natural Language Compiler**: Translates voice recordings and freeform text requests into deterministic `bill-e` JSON automation rules via Google Gemini 1.5 Flash.
- **Human-in-the-Loop Verification**: Visual UI preview cards display extracted rule conditions and actions for review before signing or installation.
- **Cryptographic Intent Signing**: Hardware-backed ECDSA signature generation (`secp256r1` / `P-256`) utilizing Android Keystore (StrongBox/TEE) for every mutating daemon request.
- **Offline-First Architecture**: Local persistence using Room DB for installed rules and trigger execution history, Jetpack DataStore for configuration, and unidirectional data flow (UDF).
- **Real-Time SSE Sync**: Live daemon connectivity tracking, environmental sensor telemetry, and execution event notifications via Server-Sent Events (SSE).
- **Actionable Notifications**: Instant push alerts with actionable callbacks (*Done*, *Snooze*, *Dismiss*).

---

## Tech Stack & Architecture

- **Language & Runtime**: Kotlin 2.x, Java 17+
- **UI Framework**: Jetpack Compose + Material 3 (Dynamic Color, Dark Mode)
- **Architecture**: MVVM + Clean Architecture / Offline-First Repository Pattern
- **Dependency Injection**: Dagger Hilt
- **Persistence**:
  - **Room Database**: Caching installed rules, device metadata, and trigger history.
  - **Jetpack DataStore**: Server connection settings, API keys, and user preferences.
- **Cryptography & Security**: Android Keystore (`bille_device_signing_key`, `EC` `secp256r1`), `BiometricPrompt` authorization API.
- **Networking**: Retrofit 2 + OkHttp 4 + `Kotlinx.serialization` JSON parser, OkHttp `EventSource` for SSE.
- **AI Integration**: Google Gemini API (`generativelanguage.googleapis.com`).
- **Target Platform**: Android SDK 26+ (`minSdk = 26`, `targetSdk = 35`, `compileSdk = 35`).

---

## Repository Layout

```
billedroid/
├── app/
│   ├── build.gradle.kts                   # Application Gradle dependencies & build configuration
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml        # Android Application Manifest
│       │   ├── java/com/bille/android/
│       │   │   ├── BilleApplication.kt    # Application entry point & Hilt root
│       │   │   ├── crypto/                # Android Keystore ECDSA key manager
│       │   │   ├── data/
│       │   │   │   ├── local/             # Room Database entities, DAOs, & DataStore
│       │   │   │   ├── remote/            # Retrofit services, SSE client, & interceptors
│       │   │   │   └── repository/        # Repositories (Rule, Device, Sync, Gemini)
│       │   │   ├── di/                    # Dagger Hilt DI modules (App, Database, Network)
│       │   │   ├── domain/                # Rule schemas, models, and JSON validator
│       │   │   ├── notification/          # Notification manager & broadcast receivers
│       │   │   └── presentation/          # Jetpack Compose UI screens & ViewModels
│       │   │       ├── MainActivity.kt    # Single activity with Navigation Host
│       │   │       ├── compiler/          # AI Rule Compiler screen & speech UI
│       │   │       ├── dashboard/         # Active rules & trigger history screen
│       │   │       └── settings/          # Server URL & Gemini key preferences
│       │   └── res/                       # App resources (strings, xml, launcher icons)
│       └── test/                          # Unit test suite
├── build.gradle.kts                       # Root project build script
├── settings.gradle.kts                    # Gradle project settings
├── gradle/                                # Gradle wrapper & version catalogs
├── API.md                                 # Complete API Reference & Signatures Protocol
└── ROADMAP.md                             # Architectural Roadmap
```

---

## Prerequisites & Setup

### Environment Requirements
- **JDK**: Java Development Kit (JDK) 17 or higher.
- **Android SDK**: API Level 35 (`compileSdk` / `targetSdk`).
- **Android Studio**: Android Studio Jellyfish (2023.3.1) or newer recommended.

### Step-by-Step Setup Commands

1. **Clone the repository**:
   ```bash
   git clone https://github.com/your-org/billedroid.git
   cd billedroid
   ```

2. **Verify Gradle environment and sync dependencies**:
   ```bash
   ./gradlew --version
   ```

3. **Build the debug APK**:
   ```bash
   ./gradlew assembleDebug
   ```

---

## Configuration

`billedroid` manages runtime credentials and server connections using Jetpack DataStore preferences and Android Keystore.

| Configuration Parameter | Default Value | Description |
| :--- | :--- | :--- |
| **Server Base URL** | `http://10.0.2.2:8080` | `bill-e` server daemon URL (`10.0.2.2` maps to `localhost` on Android Emulator). Configurable in Settings. |
| **Gemini API Key** | Empty String `""` | User-provided Google Gemini API key for natural language compiler. Configurable in Settings. |
| **Keystore Alias** | `bille_device_signing_key` | Hardware-backed keypair alias in Android Keystore used for signing rule payloads. |

To configure server options in the app UI:
1. Launch `billedroid`.
2. Open the **Settings** screen.
3. Enter your `bill-e` Server URL (e.g., `http://192.168.1.100:8080`) and Google Gemini API Key.
4. Tap **Register Device** to register your hardware public key with the daemon.

---

## Running the Application

### Deploying via CLI to an Emulator or Connected Device

Ensure an Android emulator is running or an Android device is attached via USB debugging (`adb devices`).

- **Install Debug Build**:
  ```bash
  ./gradlew installDebug
  ```

- **Build Production Release APK**:
  ```bash
  ./gradlew assembleRelease
  ```

---

## Testing

`billedroid` includes unit tests covering rule schema validation, local repositories, and SSE event deserialization logic.

- **Run All Unit Tests**:
  ```bash
  ./gradlew test
  ```

- **Run Debug Variant Unit Tests**:
  ```bash
  ./gradlew testDebugUnitTest
  ```

- **Run Release Variant Unit Tests**:
  ```bash
  ./gradlew testReleaseUnitTest
  ```

---

## API Reference

`billedroid` communicates with the `bill-e` home automation daemon over REST and Server-Sent Events (SSE), and integrates with the Google Gemini API for natural language compilation.

All state-modifying requests require hardware-backed cryptographic payload signing using an `EC secp256r1` keypair in Android Keystore.

For full endpoint specifications, request/response JSON schemas, error codes, and signature generation contracts, refer to **[API.md](./API.md)**.
