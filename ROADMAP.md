# bill-e Android Companion App: Master Roadmap & Implementation Guide

## 1. Executive Summary & Core Objective
The **bill-e** Android companion application (`billedroid`) is a lightweight, offline-first client (Jetpack Compose + Room + Material 3) that serves as the primary management terminal and cryptographic intent signer for the `bill-e` home server.

### Core App Responsibilities:
1. **AI Request Compiler**: Interface with Google Gemini API (audio/text input) to parse natural language prompts and compile them into structured, deterministic `bill-e` rule schemas.
2. **Action Review & Validation**: Render human-readable UI review cards for proposed actions/rules, eliminating prompt fatigue and raw hash/JSON validation.
3. **Cryptographic Intent Signer**: Securely sign user-approved JSON payloads using the Android Keystore (TEE/StrongBox) and dispatch them via authenticated HTTPS to the `bill-e` daemon.
4. **Dashboard & Rule Terminal**: Display a real-time status dashboard, rule execution history, and local rule configurations.
5. **Actionable Push Notifications**: Receive actionable alerts and process immediate callback actions ("Done", "Snooze", "Dismiss").

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
- **Networking**: Ktor Client or Retrofit + OkHttp with standard HTTPS, TLS certificate pinning option, and strict `Kotlinx.serialization` JSON mapping.
- **AI Integration**: Google Gemini Android SDK / Ktor client integration for multimodal (audio/text) rule compilation.

---

## 3. Cryptographic Intent Signing Specification

### Key Generation Protocol
- **Key Alias**: `bille_device_signing_key`
- **Key Algorithm**: `KeyProperties.KEY_ALGORITHM_EC` (Elliptic Curve `secp256r1` / `P-256`)
- **Digest**: `KeyProperties.DIGEST_SHA256`
- **Purpose**: `KeyProperties.PURPOSE_SIGN`
- **Hardware Security**: Enable `setIsStrongBoxBacked(true)` when hardware support is detected; fallback gracefully to standard TEE.
- **Device ID**: Lowercase Hex SHA-256 fingerprint of the X.509/PEM-encoded public key.

### Canonical Signing Format
To prevent whitespace and serialization mismatch between Android and the Python `bill-e` daemon, signatures are generated over exact raw UTF-8 bytes formatted as:

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
- **Request Body**:
```json
{
  "device_id": "pubkey_fingerprint_sha256_hex",
  "device_name": "User's Phone",
  "public_key_pem": "-----BEGIN PUBLIC KEY-----\nMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE...\n-----END PUBLIC KEY-----"
}
```
- **Response (200 OK)**:
```json
{
  "status": "registered",
  "device_id": "pubkey_fingerprint_sha256_hex"
}
```

### B. Signed Rule Installation Endpoint
- **Endpoint**: `POST /api/rules/install`
- **Headers**:
```http
POST /api/rules/install HTTP/1.1
Host: bille.local:8080
Content-Type: application/json
X-Device-ID: pubkey_fingerprint_sha256_hex
X-Signature: MEYCIQDx8...base64_signature_bytes...
X-Nonce: 4d2b2f6e-7128-4f10-9b34-8c83e29f8f2e
X-Timestamp: 2026-08-30T21:15:00Z
```
- **Request Body**:
```json
{
  "task_id": "hvac_cooling_advisory",
  "name": "HVAC Natural Cooling Advisory",
  "cooldown_hours": 4,
  "conditions": {
    "all": [
      {"source": "hvac.ac_status", "operator": "equals", "value": "on"},
      {"source": "hvac.indoor_temp_f", "operator": "between", "value": [68, 72]},
      {"source": "weather.outdoor_temp_f", "operator": "lt", "value": 68},
      {"source": "weather.is_raining", "operator": "equals", "value": false}
    ]
  },
  "action": {
    "title": "HVAC Cooling Advisory",
    "message": "Outdoor temp is pleasant. Turn off AC and open windows!",
    "actions": ["Done", "Snooze"]
  }
}
```
- **Response (200 OK)**:
```json
{
  "status": "installed",
  "task_id": "hvac_cooling_advisory",
  "file_path": "rules/hvac_cooling_advisory.json"
}
```

---

## 5. Gemini AI Rule Compilation Specification
- **Provider**: Google Gemini SDK (Gemini 1.5 Flash / Pro).
- **Input**: Natural voice audio recordings (processed via AudioRecord/MediaRecorder) or text prompts.
- **Output Constraint**: Strict JSON response enforcing the `bill-e` rule schema format via Structured Outputs / JSON Schema prompts.
- **Workflow**:
  1. User records audio or types request (e.g., "Alert me when it's under 68 outside so I can open windows if the AC is on").
  2. App sends prompt/audio to Gemini with system instructions defining valid operators (`equals`, `gt`, `lt`, `between`, etc.) and sources (`hvac.*`, `weather.*`, `sensor.*`).
  3. Gemini outputs JSON rule object.
  4. App validates JSON locally before passing to UI Review Card.

---

## 6. Actionable Push Notifications Specification
- **Service**: Firebase Cloud Messaging (FCM) or UnifiedPush local WebSocket fallback.
- **Notification Payload**: Includes `rule_id`, `title`, `message`, and `available_actions` array.
- **Interactive Action Buttons**:
  - **Done**: Sends callback to server marking action completed.
  - **Snooze**: Silences triggers for configured duration (e.g., 1 hour, 4 hours).
  - **Dismiss**: Clears notification and logs event in Room DB.

---

## 7. Phased Implementation Roadmap

### Phase 1: Project Architecture Baseline & Core Setup
- Set up Android Gradle project with Kotlin 2.x, Jetpack Compose, Material 3, and Hilt/Koin DI.
- Establish Clean Architecture layer package structure (`data`, `domain`, `presentation`, `di`, `crypto`).
- Configure Kotlinx Serialization, Coroutines, and Flow patterns.

### Phase 2: Cryptographic Engine & Keystore Security Layer
- Implement `KeyStoreManager` supporting `EC secp256r1` generation.
- StrongBox detection with automatic fallback to standard TEE.
- Implement SHA-256 fingerprint generation for `device_id` and PEM public key exporter.
- Implement canonical payload string construction and ECDSA signature generator.
- Integrate `BiometricPrompt` API for biometric approval before signing actions.

### Phase 3: Gemini AI Compiler & Rule Schema Generation
- Integrate Google Gemini SDK client.
- Create audio recording utility and natural language text prompt handler.
- Define system prompt templates for structured rule extraction matching `bill-e` schema.
- Implement local JSON schema validator for Gemini outputs.

### Phase 4: Offline-First Local Persistence & Repository Layer
- Setup Room Database with entities for `RuleEntity`, `TriggerHistoryEntity`, `DeviceKeyEntity`.
- Setup Jetpack DataStore for server settings (host, port, API keys, biometric policies).
- Implement offline-first repository pattern with reactive Flow streams.

### Phase 5: HTTPS Networking & Daemon Integration Layer
- Build HTTP client (Ktor/Retrofit) with TLS certificate handling and error recovery.
- Implement Device Registration client (`POST /api/v1/devices/register`).
- Implement Signed Rule Installation client (`POST /api/rules/install`) with headers (`X-Device-ID`, `X-Signature`, `X-Nonce`, `X-Timestamp`).
- Implement status polling and sync engine.

### Phase 6: Actionable Push Notification System & Callbacks
- Implement FCM / Push Notification Receiver Service.
- Create custom notification builder with action buttons ("Done", "Snooze", "Dismiss").
- Implement `BroadcastReceiver` / `WorkManager` workers to process button clicks and dispatch callbacks to server.

### Phase 7: UI/UX Dashboard, Rule Review Cards & Management Terminal
- **Home Dashboard**: System status, connected daemon health, active rules, and trigger log feed.
- **AI Rule Creator Screen**: Voice record UI, live transcription, and Gemini compile trigger.
- **Human Review Card UI**: Clear visual presentation of proposed rule conditions and actions before signing.
- **Device Management Screen**: Display public key fingerprint, connection settings, and cryptographic key status.
