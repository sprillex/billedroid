# bill-e Companion API Specification (`API.md`)

This document provides the complete API reference for `billedroid`, the Android companion application for the **bill-e** home automation daemon, as well as its integration contracts with the Google Gemini AI Model API.

---

## 1. Overview & General Protocols

- **Base URLs**:
  - **Local/Server Daemon**: `http://10.0.2.2:8080` (Default for Android Emulator, dynamically configurable via Jetpack DataStore / `UserPreferencesRepository`).
  - **Google Gemini AI API**: `https://generativelanguage.googleapis.com`
- **Protocols**: `HTTP/1.1`, `HTTPS`, Server-Sent Events (`text/event-stream`).
- **Data Interchange Format**: `application/json` (strictly UTF-8 encoded).
- **Versioning Conventions**:
  - `bill-e` Daemon API: `/api/v1/` for core endpoints, `/api/rules/` for rule lifecycle management.
  - Google Gemini API: `v1beta`.

---

## 2. Authentication & Cryptographic Signing Scheme

### A. Cryptographic Intent Signing Protocol (Daemon Requests)

To ensure non-repudiation and prevent unauthorized automated action execution, state-modifying HTTP requests sent to the `bill-e` daemon (e.g. `POST /api/rules/install`) must be cryptographically signed using an elliptic curve key pair generated in hardware-backed Android Keystore.

- **Key Pair Specification**:
  - **Key Alias**: `bille_device_signing_key`
  - **Algorithm**: Elliptic Curve `secp256r1` (`P-256`)
  - **Digest & Signature**: `SHA256withECDSA`
  - **Hardware Security**: Enforced StrongBox backing with TEE fallback.
- **Canonical Payload Signing Format**:
  Signatures are generated over the exact UTF-8 raw bytes formed by joining `X-Timestamp`, `X-Nonce`, and the raw HTTP JSON body with newlines:
  ```
  PayloadToSign = UTF8("${X-Timestamp}\n${X-Nonce}\n${RAW_JSON_BODY}")
  ```
- **Required HTTP Headers**:
  - `X-Device-ID`: Lowercase SHA-256 hex fingerprint of the device's X.509/PEM public key.
  - `X-Signature`: Base64-encoded signature of `PayloadToSign`.
  - `X-Nonce`: Random UUID v4 string.
  - `X-Timestamp`: ISO-8601 UTC timestamp string (e.g., `2026-08-30T21:15:00Z`).

### B. AI Compiler Authentication

- **Gemini API Key**:
  - Required for natural language rule compilation endpoints.
  - Authenticated via the `key` URL query parameter on requests to `https://generativelanguage.googleapis.com`.

---

## 3. Standard Envelopes & Error Formats

### A. Standard HTTP Status Codes

- `200 OK`: Request completed successfully.
- `201 Created`: Resource (device or rule) successfully created/installed.
- `400 Bad Request`: Missing required headers, invalid payload structure, or malformed JSON.
- `401 Unauthorized`: Invalid or missing signature, device fingerprint, or API key.
- `403 Forbidden`: Signature verification failed or device authorization rejected.
- `404 Not Found`: Endpoint or task ID not found.
- `422 Unprocessable Entity`: Rule JSON failed schema validation (e.g., unsupported operator or invalid condition value format).
- `429 Too Many Requests`: AI service rate limits exceeded.
- `500 Internal Server Error`: Server failure or error executing rule installation on the daemon.

### B. Standard Error Response Envelope

When an API request fails, responses return a JSON payload formatted as follows:

```json
{
  "error": {
    "code": 401,
    "message": "Signature verification failed for X-Device-ID: e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
  }
}
```

---

## 4. Endpoints by Resource

### A. Device Registration (`POST /api/v1/devices/register`)

Registers an Android device and its hardware-backed public key with the `bill-e` home daemon.

- **HTTP Method**: `POST`
- **Path**: `/api/v1/devices/register`
- **Access / Permissions**: Public / Unauthenticated initial registration.
- **Headers**:
  - `Content-Type: application/json`

#### Request Body Schema

| Field Name | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `device_id` | String | Yes | Lowercase SHA-256 fingerprint of the X.509 public key. |
| `device_name` | String | Yes | Human-readable manufacturer and model name (e.g., `"Google Pixel 8"`). |
| `public_key_pem` | String | Yes | PEM-encoded X.509 public key string. |

```json
{
  "device_id": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
  "device_name": "Google Pixel 8",
  "public_key_pem": "-----BEGIN PUBLIC KEY-----\nMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE...\n-----END PUBLIC KEY-----"
}
```

#### Response Schemas

- **Success (`200 OK` / `201 Created`)**:
  ```json
  {
    "status": "registered",
    "device_id": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
  }
  ```
- **Known Errors**:
  - `400 Bad Request`: Invalid public key format or missing parameters.
  - `422 Unprocessable Entity`: Malformed device ID checksum.

---

### B. Signed Rule Installation (`POST /api/rules/install`)

Installs a compiled automation rule payload onto the daemon. Requires valid cryptographic intent signature headers.

- **HTTP Method**: `POST`
- **Path**: `/api/rules/install`
- **Access / Permissions**: Authenticated via Cryptographic Signature (`X-Signature`).
- **Headers**:
  - `Content-Type: application/json`
  - `X-Device-ID: <SHA256_HEX_FINGERPRINT>` (Required)
  - `X-Signature: <BASE64_ECDSA_SIGNATURE>` (Required)
  - `X-Nonce: <UUID_V4_STRING>` (Required)
  - `X-Timestamp: <ISO8601_UTC_STRING>` (Required)

#### Request Body Schema

| Field Name | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `task_id` | String | Yes | Unique snake_case rule identifier. |
| `name` | String | Yes | Human-readable title of the rule. |
| `cooldown_hours` | Integer | No | Minimum hours between consecutive rule triggers (default: `1`). |
| `conditions` | Object | Yes | Rule trigger logic specifying `all` or `any` condition arrays. |
| `conditions.all` | Array | No | List of conditions that must all evaluate to true. |
| `conditions.any` | Array | No | List of conditions where at least one must evaluate to true. |
| `condition.source` | String | Yes | Sensor or entity state string (e.g. `"indoor.temperature"`). |
| `condition.operator` | String | Yes | Operator: `equals`, `not_equals`, `gt`, `gte`, `lt`, `lte`, `between`, `in`, `contains`. |
| `condition.value` | Any | Yes | String, Number, Boolean, `[min, max]` number array, or String array. |
| `action` | Object | Yes | Action payload to execute on trigger. |
| `action.title` | String | Yes | Notification header title. |
| `action.message` | String | Yes | Action notification message string. |
| `action.actions` | Array | No | Action button labels (default: `["Done", "Snooze"]`). |

```json
{
  "task_id": "temp_alert_living_room",
  "name": "High Living Room Temperature Warning",
  "cooldown_hours": 2,
  "conditions": {
    "all": [
      {
        "source": "indoor.temperature",
        "operator": "gt",
        "value": 28.5
      }
    ],
    "any": []
  },
  "action": {
    "title": "Temperature Alert",
    "message": "Living room temperature exceeds 28.5°C.",
    "actions": ["Done", "Snooze"]
  }
}
```

#### Response Schemas

- **Success (`200 OK` / `201 Created`)**:
  ```json
  {
    "status": "installed",
    "task_id": "temp_alert_living_room",
    "file_path": "/etc/bill-e/rules/temp_alert_living_room.json"
  }
  ```
- **Known Errors**:
  - `400 Bad Request`: Payload formatting error or expired timestamp (`abs(now - timestamp) > 300s`).
  - `401 Unauthorized`: Invalid signature or unknown `X-Device-ID`.
  - `422 Unprocessable Entity`: Invalid condition operator or invalid field structure.

---

### C. Live Daemon Event Stream (`GET /api/v1/sync/events`)

Server-Sent Events (SSE) HTTP connection for streaming real-time status updates, sensor telemetry, and execution trigger events.

- **HTTP Method**: `GET`
- **Path**: `/api/v1/sync/events`
- **Access / Permissions**: Daemon Connection.
- **Headers**:
  - `Accept: text/event-stream`

#### Stream Event Schemas

1. **`event: status`**
   - Transmitted periodically to update system health.
   ```json
   {
     "status": "ONLINE",
     "uptime_seconds": 86400,
     "active_rules": 12
   }
   ```

2. **`event: state_update`**
   - Transmitted when environmental or system state changes occur.
   ```json
   {
     "indoor_temp": 22.5,
     "outdoor_temp": 14.0,
     "kp_index": 3.2,
     "hvac_mode": "COOL"
   }
   ```

3. **`event: trigger_event`**
   - Transmitted when a daemon rule fires.
   ```json
   {
     "task_id": "temp_alert_living_room",
     "rule_name": "High Living Room Temperature Warning",
     "title": "Temperature Alert",
     "message": "Living room temperature exceeds 28.5°C.",
     "action_taken": "DISPATCH_NOTIF",
     "timestamp": 1700000000000
   }
   ```

---

### D. Gemini AI Rule Compiler (`POST /v1beta/models/gemini-1.5-flash:generateContent`)

Generates structured JSON rule schemas from natural language text prompts or audio transcriptions using Google Gemini 1.5 Flash.

- **HTTP Method**: `POST`
- **Base Host**: `https://generativelanguage.googleapis.com`
- **Path**: `/v1beta/models/gemini-1.5-flash:generateContent`
- **Query Parameters**:
  - `key` (String, Required): Valid Google Gemini API key.
- **Headers**:
  - `Content-Type: application/json`

#### Request Body Schema

```json
{
  "system_instruction": {
    "parts": [
      {
        "text": "You are the bill-e AI Assistant Compiler. Your job is to compile natural language user requests into a valid bill-e rule JSON object."
      }
    ]
  },
  "contents": [
    {
      "role": "user",
      "parts": [
        {
          "text": "Send me a notification if living room temperature goes above 28.5 degrees"
        }
      ]
    }
  ],
  "generation_config": {
    "response_mime_type": "application/json",
    "temperature": 0.2
  }
}
```

#### Response Schemas

- **Success (`200 OK`)**:
  ```json
  {
    "candidates": [
      {
        "content": {
          "parts": [
            {
              "text": "{\n  \"task_id\": \"temp_alert_living_room\",\n  \"name\": \"High Living Room Temperature Warning\",\n  \"cooldown_hours\": 2,\n  \"conditions\": {\n    \"all\": [\n      {\n        \"source\": \"indoor.temperature\",\n        \"operator\": \"gt\",\n        \"value\": 28.5\n      }\n    ],\n    \"any\": []\n  },\n  \"action\": {\n    \"title\": \"Temperature Alert\",\n    \"message\": \"Living room temperature exceeds 28.5°C.\",\n    \"actions\": [\"Done\", \"Snooze\"]\n  }\n}"
            }
          ]
        }
      }
    ]
  }
  ```
- **Known Errors**:
  - `400 Bad Request`: Invalid API request or missing parameters.
  - `401 Unauthorized` / `403 Forbidden`: Invalid or missing Gemini API key.
  - `429 Too Many Requests`: Rate limit or quota exceeded.

---

## 5. Pagination & Streaming Query Conventions

- **Streaming Semantics**: Real-time daemon status and trigger history updates stream over Server-Sent Events (`GET /api/v1/sync/events`). The client maintains an active connection and processes events on receive.
- **Entity Mutation**: Endpoint state updates (device registration, rule installation) act on single entities using standard HTTP POST requests and do not use limit/offset query pagination.
