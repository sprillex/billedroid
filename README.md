# billedroid: bill-e Android Companion App

`billedroid` is the official Android companion application for the **bill-e** home automation and server daemon. Built with modern Android practices (Kotlin, Jetpack Compose, Room, Material 3), `billedroid` acts as the management terminal, AI rule compiler, and secure cryptographic intent signer for your `bill-e` instance.

---

## Key Features

- **AI Natural Language Compiler**: Uses Google Gemini to translate voice and text requests into deterministic `bill-e` JSON rules.
- **Human-in-the-Loop Verification**: Visual UI review cards display exact rule conditions and actions before any payload is signed or dispatched.
- **Cryptographic Intent Signing**: Hardware-backed ECDSA signatures (`secp256r1`) generated in Android Keystore (StrongBox/TEE) for every rule installation request.
- **Offline-First Architecture**: Room DB local caching, Jetpack DataStore preferences, and unidirectional data flow (UDF).
- **Actionable Push Notifications**: Real-time push alerts with instant action callbacks (*Done*, *Snooze*, *Dismiss*).

---

## Technical Stack

- **Language**: Kotlin 2.x
- **UI Framework**: Jetpack Compose + Material 3
- **Architecture**: MVVM + Clean Architecture / Repository Pattern
- **Persistence**: Room Database + Jetpack DataStore
- **Cryptography**: Android Keystore (`EC` `secp256r1`), `BiometricPrompt` API
- **AI Integration**: Google Gemini Android SDK
- **Networking**: Ktor Client / Retrofit + `Kotlinx.serialization`

---

## Documentation & Roadmap

For full technical specifications, cryptographic protocol details, API contracts, and phased development plans, refer to [ROADMAP.md](ROADMAP.md).
