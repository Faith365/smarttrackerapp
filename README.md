# SmartTrackerApp 📱🔐

**SmartTrackerApp** is a modern Android application designed to intelligently track and recover lost or stolen Android devices, even under extreme conditions like factory resets, offline mode, or SIM swaps.

This system enhances traditional tracking solutions by integrating AI-powered predictive analysis, multi-user FIDO2/WebAuthn authentication, and stealth background monitoring.

---

## 🚀 Features

### 🔍 Intelligent Tracking
- Predicts device movement using AI, even when offline
- Uses last known location, movement patterns, and theft hotspots
- Supports both real-time GPS and predictive offline tracking

### 🔒 Multi-User Authentication
- FIDO2/WebAuthn-based login for secure, passwordless access
- Allows trusted delegates (e.g. family, IT admin) to access tracking controls
- Role-based access management to limit/restrict functions per user

### 🛥️ Stealth Mode
- Runs silently in the background
- Cannot be detected or disabled by unauthorized users
- Auto-resumes on reboot or factory reset (if supported by firmware)

### 📡 Remote Recovery Tools
- Trigger alarms remotely
- Lock or wipe the device from a cloud-based interface
- View live and historic location trails

---

## 🧩 Architecture Overview

```
User Device (stolen) --> Cloud Server --> Web Dashboard (user/admin)
     |                     |                  |
     |----> Stealth Ping --|---> Predictive AI
     |----> GPS/WiFi       |---> Secure Auth via WebAuthn
```

- Firebase handles cloud storage, authentication, and real-time sync
- Node.js backend supports request handling, security, and prediction logic
- Google Maps API is used for geolocation services and map overlays
- AI modules run trained models for movement forecasting

---

## 🛠️ Tech Stack

| Layer               | Tools / Libraries                         |
|--------------------|--------------------------------------------|
| 📱 Frontend (Mobile) | Kotlin, Android SDK, Jetpack Compose       |
| 🔐 Authentication    | WebAuthn (FIDO2), BiometricPrompt API       |
| ☁️ Backend (Cloud)   | Firebase (Firestore, Auth), Node.js        |
| 🗽 Geolocation       | Google Maps API, FusedLocationProvider     |
| 🧠 AI Layer          | TensorFlow Lite (planned), custom ML models|
| 🪪 Testing/Debugging | Postman, Android Emulator, Firebase Console|

---

## 🧪 System Capabilities

| Metric                        | Description                                  |
|------------------------------|----------------------------------------------|
| 📍 Tracking Accuracy          | Works indoors/outdoors, AI-based predictions |
| 🢑 Delegate Access Support | Secure multi-user login using WebAuthn       |
| 🔕 Stealth Coverage           | Silent background tracking, persists reboots |
| 🔒 Resilience to Reset        | Stealth tracker reinitializes after reset    |
| ⚖️ Privacy-Compliant          | Designed to comply with GDPR/CCPA principles |

---

## 📸 Screenshots

*(To be added)*

---

## 🧰 Setup Instructions

### Requirements
- Android Studio Arctic Fox or later
- Minimum SDK: 24 (Android 7.0)
- Firebase Project (with Firestore & Auth enabled)
- Google Maps API key

### Steps
1. Clone this repo:
   ```bash
   git clone https://github.com/Faith365/smarttrackerapp.git
   ```
2. Open in Android Studio
3. Add your `google-services.json` to the `/app` folder
4. Add your Maps API key to `local.properties`:
   ```
   MAPS_API_KEY=your_key_here
   ```
5. Build & run on device/emulator

---

## 🧠 Future Enhancements

- 📡 Offline prediction using embedded ML models
- 🧾 Forensic logging for legal recovery
- 🌍 Dashboard web portal for family/law enforcement
- 📦 Integration with encrypted cloud backups

---

## 📜 License

This project is licensed under the [MIT License](LICENSE).

---

## 😋 Contact

For contributions, suggestions or issues, please open an [Issue](# SmartTrackerApp 📱🔐

**SmartTrackerApp** is a modern Android application designed to intelligently track and recover lost or stolen Android devices — even under extreme conditions like factory resets, offline mode, or SIM swaps.

This system enhances traditional tracking solutions by integrating AI-powered predictive analysis, multi-user FIDO2/WebAuthn authentication, and stealth background monitoring.

---

## 🚀 Features

### 🔍 Intelligent Tracking
- Predicts device movement using AI, even when offline
- Uses last known location, movement patterns, and theft hotspots
- Supports both real-time GPS and predictive offline tracking

### 🔒 Multi-User Authentication
- FIDO2/WebAuthn-based login for secure, passwordless access
- Allows trusted delegates (e.g. family, IT admin) to access tracking controls
- Role-based access management to limit/restrict functions per user

### 🛥️ Stealth Mode
- Runs silently in the background
- Cannot be detected or disabled by unauthorized users
- Auto-resumes on reboot or factory reset (if supported by firmware)

### 📡 Remote Recovery Tools
- Trigger alarms remotely
- Lock or wipe the device from a cloud-based interface
- View live and historic location trails

---

## 🧩 Architecture Overview

```
User Device (stolen) --> Cloud Server --> Web Dashboard (user/admin)
     |                     |                  |
     |----> Stealth Ping --|---> Predictive AI
     |----> GPS/WiFi       |---> Secure Auth via WebAuthn
```

- Firebase handles cloud storage, authentication, and real-time sync
- Node.js backend supports request handling, security, and prediction logic
- Google Maps API is used for geolocation services and map overlays
- AI modules run trained models for movement forecasting

---

## 🛠️ Tech Stack

| Layer               | Tools / Libraries                         |
|--------------------|--------------------------------------------|
| 📱 Frontend (Mobile) | Kotlin, Android SDK, Jetpack Compose       |
| 🔐 Authentication    | WebAuthn (FIDO2), BiometricPrompt API       |
| ☁️ Backend (Cloud)   | Firebase (Firestore, Auth), Node.js        |
| 🗽 Geolocation       | Google Maps API, FusedLocationProvider     |
| 🧠 AI Layer          | TensorFlow Lite (planned), custom ML models|
| 🪪 Testing/Debugging | Postman, Android Emulator, Firebase Console|

---

## 🧪 System Capabilities

| Metric                        | Description                                  |
|------------------------------|----------------------------------------------|
| 📍 Tracking Accuracy          | Works indoors/outdoors, AI-based predictions |
| 🢑 Delegate Access Support | Secure multi-user login using WebAuthn       |
| 🔕 Stealth Coverage           | Silent background tracking, persists reboots |
| 🔒 Resilience to Reset        | Stealth tracker reinitializes after reset    |
| ⚖️ Privacy-Compliant          | Designed to comply with GDPR/CCPA principles |

---

## 📸 Screenshots

*(To be added)*

---

## 🧰 Setup Instructions

### Requirements
- Android Studio Arctic Fox or later
- Minimum SDK: 24 (Android 7.0)
- Firebase Project (with Firestore & Auth enabled)
- Google Maps API key

### Steps
1. Clone this repo:
   ```bash
   git clone https://github.com/Faith365/smarttrackerapp.git
   ```
2. Open in Android Studio
3. Add your `google-services.json` to the `/app` folder
4. Add your Maps API key to `local.properties`:
   ```
   MAPS_API_KEY=your_key_here
   ```
5. Build & run on device/emulator

---

## 🧠 Future Enhancements

- 📡 Offline prediction using embedded ML models
- 🧾 Forensic logging for legal recovery
- 🌍 Dashboard web portal for family/law enforcement
- 📦 Integration with encrypted cloud backups

---

## 📜 License

This project is licensed under the [MIT License](LICENSE).

---

## 😋 Contact

For contributions, suggestions or issues, please open an [Issue](https://github.com/Faith365/smarttrackerapp/issues) or submit a Pull Request./issues) or submit a Pull Request.
