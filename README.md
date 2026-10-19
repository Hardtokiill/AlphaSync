# AlphaSync – Laboratory Staff Android App

Android (Java + XML) client for **AlphaSync – Integrated Health Intelligence and Operations Management System** (NHLS).
Stack: Firebase Auth, Cloud Firestore, FCM, Security Rules, Room (offline), WorkManager (auto-sync), ViewBinding, Material 3.
No Kotlin, no Compose. Firebase Storage is deliberately **not** added (nothing in the spec needs files).

## 1. Run it (10 minutes)
1. Android Studio (Ladybug or newer, JDK 17) → **Open** this folder → let Gradle sync.
2. Firebase Console → create a project → **Add Android app**, package `com.example.alphasync`
   → download `google-services.json` → put it in `app/` (see `app/google-services.json.example`).
3. Enable **Authentication → Email/Password**, create **Cloud Firestore** (production mode).
4. Publish the rules: paste `firestore.rules` into Firestore → Rules (or `firebase deploy --only firestore:rules`).
5. Add test data (Firestore console):

```
laboratories/lab_jhb        { name: "NHLS Johannesburg", location: "Braamfontein", networkStatus: "ONLINE" }
users/<uid of your auth user> { name: "Thandi Mokoena", email: "<same email>", role: "LAB_STAFF", laboratoryId: "lab_jhb" }
```
(`<uid>` is shown in Authentication → Users.) Roles other than `LAB_STAFF` are refused by the mobile app.
6. Run on a device/emulator (API 26+).

## 2. Architecture (maps to the spec's package layout)
| Package | Purpose |
|---|---|
| `activities/` | `LoginActivity`, `MainActivity` (bottom nav host, shared ViewModels, FCM topic/token, permission) |
| `fragments/` | Home, Inventory, UpdateStock, Disease, CaptureDisease, NetworkStatus, Alerts, Profile |
| `adapters/` | RecyclerView adapters (DiffUtil) |
| `models/` | Firestore POJOs: User, Laboratory, Inventory, DiseaseRecord, Alert, SyncRecord |
| `firebase/` | `FirebaseManager` (timeouts, friendly errors), FCM service, notification helper |
| `repository/` | Firestore access (User, Inventory, Disease, Alert) |
| `offline/` | Room DB, `OfflineRepository`, `SyncManager`, `SyncWorker` |
| `viewmodels/` | Session, Inventory, Disease, Alerts |
| `utils/` | `NetworkUtils`, `ValidationUtils`, `DateUtils`, `Messages` |

### Offline flow (the key requirement)
1. Save → `NetworkUtils.isOnline()` (ConnectivityManager, validated internet).
2. **Online** → Firestore `set()` with a 15 s timeout → "Saved and synchronised".
3. **Offline / failure** → row stored in Room as `PENDING` → "No internet connection. Your information has been saved and will synchronise when connection is restored." → WorkManager job queued with a `NETWORK_CONNECTED` constraint.
4. When connectivity returns (WorkManager, or the network callback, or **Synchronise Now**) `SyncManager` uploads pending rows, marks them `SYNCED`, writes an audit doc in `syncRecords`, updates "last sync".
5. **No duplicates:** one sync at a time (`synchronized`), deterministic document ids (UUID for disease records; `labId_item-slug` for stock, so saving an existing item name *updates* it), and a row is only marked synced if it wasn't edited mid-upload.
6. Lists show cloud + local rows merged; unsynced ones display **Pending Sync** / **Sync failed**.

### Stock status rules (`Inventory.getStockStatus()`)
Expired = expiry date passed · Critical = quantity 0 or ≤ half the minimum · Low Stock = below the minimum · otherwise Available.
Saving a Low/Critical item also creates an `alerts` document.

### Notifications (FCM)
- App subscribes to topic `lab_<laboratoryId>` and stores its token in `users/{uid}.fcmToken`.
- Android 13+ `POST_NOTIFICATIONS` is requested at runtime. Tapping a notification opens the screen in the `destination` data field (`home|inventory|disease|alerts|profile`).
- **Demo without any server:** Firebase Console → Messaging → new notification → target topic `lab_lab_jhb` → add custom data `destination = alerts`.
- **Optional automation:** `functions/index.js` sends a push whenever an alert document is created (needs the Blaze plan: `firebase deploy --only functions`).

### Security
Firebase Auth only (no passwords stored) · `firestore.rules`: lab-scoped reads/writes, field-by-field validation, staff cannot delete, cannot edit submitted disease records, can only flip an alert's `read`, can only update their own `fcmToken` · mobile app refuses non-staff roles · `allowBackup=false`, HTTPS only · `google-services.json` is git-ignored · password reset messages don't reveal whether an account exists.
Change password = emailed Firebase reset link (avoids handling the old password in-app).

## 3. Test checklist (spec section 13)
Run unit tests: `./gradlew test` (validation rules). Manual:
- **Auth:** correct login → Home · wrong password → "Incorrect email or password." · Forgot password → neutral confirmation · Logout → Login · reopen app while signed in → straight to Home.
- **Inventory:** list shows status chips · add quantity `-5` / `abc` → field error · set quantity below minimum → Low Stock chip + Home warning + alert created.
- **Disease:** capture valid record → appears with ✔ Synced · empty name / no severity / future date → errors.
- **Offline:** airplane mode → capture disease + update stock → confirmation text + ⏳ Pending Sync, Home/Network show pending count → airplane mode off → records become ✔ Synced, pending = 0, "last sync" updates; check Firestore has exactly one document each.
- **Notifications:** send the console test message (above) → notification appears → tap opens Alerts → tap an alert → marked read, bottom-nav badge decreases.
- **Security:** rules playground → unauthenticated read of `inventory` denied; staff of lab A cannot read lab B; staff cannot delete or write to `users`.
- **UI:** rotate, small/large screens, scroll forms, bottom-nav and back navigation.

## 4. Known limits / easy extensions
- Lists are sorted client-side (no composite indexes needed); fine for a lab's data volume, add `limit()`/paging if it grows.
- "Laboratory network connection unavailable" is shown live as the Offline banner (Home / Network Status) rather than as a stored alert.
- If a lab has no alert documents yet the Alerts tab shows its empty state.
