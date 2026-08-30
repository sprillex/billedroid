# billedroid
Bille Android companion app 


Below is the roadmap for the server.  This is for context.  Upon first read by an AI agent all data relevant to the Android app should be extracted to place in the roadmap and all non relevant data including this message should be removed.
# bill-e Master Development Roadmap & Companion API Specification

## Executive Summary
`bill-e` is designed as a lightweight, deterministic personal assistant running locally on low-power hardware with minimal resource overhead (<30 MB RAM, near-zero idle CPU, SD-card wear protection). It executes local schedules, weather monitoring, and household reminders via actionable push notifications.

---

# Android Companion App API & Cryptographic Intent Signing Spec

### 1. Security Architecture & Verification Flow

```
[User Audio / Text] ──> [AI Agent Synthesis] ──> [Structured Rule JSON]
                                                          │
                                                          ▼
                                            [Android App UI Card Review]
                                            (Human reviews exact actions)
                                                          │
                                                 (User Taps "Approve")
                                                          │
                                                          ▼
                                            [Android Keystore TEE / StrongBox]
                                            (Signs Payload + Nonce + Timestamp)
                                                          │
                                                          ▼
                                            [HTTPS POST /api/rules/install]
                                            X-Device-ID | X-Signature | X-Nonce
                                                          │
                                                          ▼
                                            [bill-e Verification Server]
                                            1. Check Timestamp (<= 300s window)
                                            2. Check Nonce (Prevent Replay)
                                            3. Verify Signature vs trusted_devices
                                            4. Validate JSON Schema Guardrails
                                            5. Write rule to rules/<id>.json
```

---

### 2. API Endpoints Specification

#### Endpoint A: Device Public Key Registration
`POST /api/v1/devices/register`
* **Purpose**: Registers the Android device's public key (EC secp256r1 or RSA 2048+) into `bill-e`'s local SQLite `trusted_devices` table.
* **Request Headers**: `Content-Type: application/json`
* **Request Body**:
```json
{
  "device_id": "pubkey_fingerprint_sha256_hex",
  "device_name": "User's Pixel 8",
  "public_key_pem": "-----BEGIN PUBLIC KEY-----\nMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE...\n-----END PUBLIC KEY-----"
}
```
* **Response (200 OK)**:
```json
{
  "status": "registered",
  "device_id": "pubkey_fingerprint_sha256_hex"
}
```

---

#### Endpoint B: Signed Rule Installation
`POST /api/rules/install`
* **Purpose**: Installs a human-approved rule after verifying signature, timestamp freshness, anti-replay nonce, and schema guardrails.
* **Request Headers**:
```http
POST /api/rules/install HTTP/1.1
Host: bille.local:8080
Content-Type: application/json
X-Device-ID: pubkey_fingerprint_sha256_hex
X-Signature: MEYCIQDx8...base64_signature_bytes...
X-Nonce: 4d2b2f6e-7128-4f10-9b34-8c83e29f8f2e
X-Timestamp: 2026-08-30T21:15:00Z
```
* **Request Payload**:
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
* **Response (200 OK - Successful Verification & Installation)**:
```json
{
  "status": "installed",
  "task_id": "hvac_cooling_advisory",
  "file_path": "rules/hvac_cooling_advisory.json"
}
```

---

# Master Development Roadmap

### Phase 1: Core Architecture & Engine Baseline (Completed)
- Standard library Python 3 daemon (<30 MB RAM target).
- SQLite WAL mode state management.
- HTTP Webhook server & Home Assistant Blueprint.

### Phase 2: High-Priority Android Companion API & Feature Rollout (Ordered Targets)
1. **[HIGHEST PRIORITY] Android Cryptographic Intent Signing & Verification Engine**:
   - `trusted_devices` & `used_nonces` SQLite tables in `bille/db.py`.
   - Device Public Key registration endpoint (`POST /api/v1/devices/register`).
   - Signature, Anti-Replay Nonce, and Timestamp verification module in `bille/server.py`.
   - Strict JSON Schema & Operator Guardrail validator before writing rules to `rules/*.json`.
2. **Natural HVAC Cooling Advisory**
3. **Freezing Pipe Protection Alert**
4. **Holiday Garbage & Recycling Delay Tracking**
5. **HVAC Filter Replacement Counter**
6. **Open Door / Window Rain Guard**
7. **Smoke / Carbon Monoxide Sensor Cooldown Alerts**
8. **Astronomy, Weather & Outdoor Activities Suite** (Aurora, Meteor Showers, UV Index, Wind, AQI)
9. **Hydration / Stretch Break Reminders**
10. **Medication / Daily Supplement Tracking**
11. **Appointment Commute Buffer Alert**

### Phase 3: Future Backlog / Potential New Features
* Humidifier / Dehumidifier Window Guard
* Peak Electricity Rate Avoidance & Solar Excess Reminders
* Water Softener Salt Level Tracking
* Dishwasher Clean / Dirty State Tracking
* Garage Door Evening Security Check
* Summertime Vehicle Heat Warning
* Sump Pump Failure Protection
* Bedtime Wind-down Routine
* Sunlight Exposure / Vitamin D Prompt
* School / Street Parking Sweeping Reminder
* Package Delivery / Mail Arrival Prompt
