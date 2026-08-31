# bill-e Android Companion App: Master Roadmap & Implementation Guide

## 1. Executive Summary & Core Objective
The **bill-e** Android companion application (`billedroid`) is a lightweight, offline-first client (Jetpack Compose + Room + Material 3) that serves as the primary management terminal and cryptographic intent signer for the `bill-e` home server.

---

## 2. Technical Stack & Architecture Guidelines
- **Language & UI Framework**: Kotlin 2.x, Jetpack Compose, Material 3 (Dynamic Color / Dark Theme support), Single-Activity architecture.
- **Architecture Pattern**: MVVM + Clean Architecture / Offline-First Repository Pattern with Unidirectional Data Flow (UDF).
- **Local Persistence**:
  - **Room Database**: Local caching for installed rules, device key metadata, trigger execution history, and pending sync states.
  - **Jetpack DataStore (Preferences)**: Server connection credentials, connection URL, biometric options, and user preferences.
- **Cryptography & Security**:
  - **Android Keystore**: `AndroidKeyStore` provider, `EC` `secp256r1` (`P-256`) with `SHA256withECDSA`.
  - **Biometric Authentication**: `BiometricPrompt` API for authorization before signing rules.
- **Networking**: Retrofit + OkHttp with standard HTTPS, custom OkHttp canonical signing interceptor, and strict `Kotlinx.serialization` JSON mapping.
- **AI Integration**: Google Gemini API client integration for rule compilation.

---

## 3. Cryptographic Intent Signing Specification

### Key Generation Protocol
- **Key Alias**: `bille_device_signing_key`
- **Key Algorithm**: `KeyProperties.KEY_ALGORITHM_EC` (Elliptic Curve `secp256r1` / `P-256`)
- **Digest**: `KeyProperties.DIGEST_SHA256`
- **Purpose**: `KeyProperties.PURPOSE_SIGN`
- **Hardware Security**: Enabled `setIsStrongBoxBacked(true)` with automatic fallback to standard TEE.
- **Device ID**: Lowercase Hex SHA-256 fingerprint of the X.509/PEM-encoded public key.

### Canonical Signing Format
Signatures are generated over exact raw UTF-8 bytes formatted as:

```
PayloadToSign = UTF8("${X-Timestamp}\n${X-Nonce}\n${RAW_JSON_BODY}")
```

- **`X-Timestamp`**: ISO-8601 UTC timestamp (e.g., `2026-08-30T21:15:00Z`). Daemon enforces `abs(now - timestamp) <= 300` seconds.
- **`X-Nonce`**: Standard UUID v4 string (e.g., `4d2b2f6e-7128-4f10-9b34-8c83e29f8f2e`).
- **`RAW_JSON_BODY`**: Exact UTF-8 JSON payload sent in the HTTP request body.

---

## 4. API Endpoint Contracts

### A. Device Registration Endpoint
- **Endpoint**: `POST /api/v1/devices/register`
- **Headers**: `Content-Type: application/json`

### B. Signed Rule Installation Endpoint
- **Endpoint**: `POST /api/rules/install`

---

## 5. Next Milestones & Future Expansion Roadmap
- [ ] Multimodal Voice Audio Recording & Live Speech-to-Text Transcription for AI Compiler.
- [ ] `BiometricPrompt` API integration prior to rule signing authorization.
- [ ] WebSocket / Server-Sent Events (SSE) live daemon status sync feed.
- [x] Dynamic base URL selection in Settings UI mapped to Retrofit HTTP client.
