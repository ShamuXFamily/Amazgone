# Amazgone

A **fake Amazon-style shop with a game layer**, built with Kotlin Multiplatform + Compose Multiplatform (iOS + Android, one shared UI).
There is **no real money**: everything is paid in virtual coins that players earn by playing mini-games.

- 🛍️ Real-looking catalog: ~200 products from [DummyJSON](https://dummyjson.com/docs/products) + live PC-game deals from [CheapShark](https://apidocs.cheapshark.com/)
  + hand-curated **New arrivals** (e.g. iPhone 18 Pro, iPhone Duo) from a public Firestore `catalog` collection
- 🌗 Light / Dark / System theme switch (Account → Appearance)
- 🔎 Offline full-text search (Room FTS4) with filters/sorting, topped up by remote search when online
- 🏬 **Stores**: every product has a seller — official brand stores (Apple, Samsung, Sony, Nintendo, Microsoft, Xiaomi,
  Bose, Canon, DJI, Logitech, Garmin, adidas… with real products and credited Wikimedia Commons photos), brand stores,
  real digital game shops (Steam, GOG, Epic…) and Amazgone itself; store pages, directory and "Visit the store" links
- 🛒 Cart, wishlist, Amazon-style one-page checkout (saved address, Standard/Express delivery, one shipment per store,
  **hold-to-pay**), order history with sync status
- 🕊️ **Couriers through history**: camel, pigeon, Inca runners, clipper, steam train, Pony Express, truck, biplane,
  cargo jet, drone, rocket, teleporter. Delivery time = real great-circle distance from the store's home city
  (Apple → Cupertino…) to your geocoded address ÷ the courier's speed; unlock faster couriers once with coins in the
  **Courier Garage**; follow each parcel live on an **OpenStreetMap** map
- 📦 Order tracker (Placed → Confirmed → Shipped → Delivered) with "Order received", and **verified-purchase reviews**
  (star rating + comment) that publish to everyone's product page
- 🔔 **Notifications**: local OS notifications (no push server) plus an in-app inbox with a bell badge and slide-down
  banner — order confirmed/refunded, courier departed, delivered, review prompts, spin/scratch ready, flash sale ending,
  level-ups, achievements and courier unlocks; tapping one opens the right screen
- 🎰 Daily **spin wheel**, **scratch cards**, hourly **lightning deals**, coupons
- 🏆 XP, levels, achievements and an online **leaderboard**
- 📴 **Offline-first**: the app works fully in airplane mode; changes sync when you're back online
- ✨ Material 3 Expressive + a motion system (shared-element transitions, fly-to-cart, physics wheel, confetti…)

## Architecture

| Concern | Choice |
|---|---|
| Pattern | **MVI + Clean Architecture**, packaged by feature (`domain` / `data` / `presentation`), enforced by Konsist tests |
| Local data | **Room KMP** is the single source of truth; the UI only observes Room `Flow`s |
| Sync | Every mutation = Room write + **outbox** row in one transaction → `SyncEngine` (FIFO, backoff, idempotent ids) → Firebase |
| Money | Append-only **ledger** (coins + XP). Balances are sums; offline purchases/rewards are *pending* until the server confirms or rejects them (automatic refund) |
| Backend | Firebase Auth (username → synthetic email) + Cloud Firestore, both over their **REST APIs** with Ktor |
| Analytics | Firebase Analytics via the native SDKs (Android BOM, iOS SPM) behind a shared `Analytics` API with GA4 event names |
| DI | Koin |
| UI | Compose Multiplatform, Material 3 Expressive, type-safe Navigation, Coil 3 |

See [`CLAUDE.md`](CLAUDE.md) for the project rules and [`docs/PLAN.md`](docs/PLAN.md) for the full design.

**Why REST instead of the Firebase SDKs?** The KMP wrapper (GitLive) targets Kotlin 2.2 + Firebase iOS 11.8 via CocoaPods; this
project is on Kotlin 2.4 with SPM. The REST APIs keep everything in `commonMain`, testable with Ktor `MockEngine`, with Room as
the only cache (which offline-first requires anyway). Security rules apply exactly the same.

## Setup

### 1. Firebase (one-time, in the [Firebase console](https://console.firebase.google.com/), project `amazgone-251d6`)
1. **Authentication → Sign-in method → Email/Password → Enable** (already done).
2. **Firestore Database → Create database** (production mode, pick a region). This also enables the Cloud Firestore API.
3. **Firestore → Rules** → paste the contents of [`firestore.rules`](firestore.rules) → **Publish**.
4. **Firestore → Indexes**: none needed (the leaderboard uses a single-field order).

Config files (gitignored — never commit them):
- iOS: `iosApp/iosApp/GoogleService-Info.plist`
- Android: `androidApp/google-services.json` (the build extracts the API key/project id into resources)

Without them the app still runs, local-only (guest mode).

### 2. Toolchain
Android Studio (for its JDK 21), Xcode 26+. From a terminal:
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

## Run

- **iOS**: open `iosApp/iosApp.xcodeproj` in Xcode and run, or build for the simulator:
  ```bash
  xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 17 Pro' build
  ```
- **Android**:
  ```bash
  ./gradlew :androidApp:assembleDebug
  ```

## Test

```bash
./gradlew :shared:jvmTest :shared:testAndroidHostTest :shared:iosSimulatorArm64Test
```

```bash
./gradlew :shared:koverVerify
```

- `commonTest`: domain rules, ViewModels, Firebase REST clients (Ktor `MockEngine`)
- `dbTest` (runs on **JVM and iOS**): Room/FTS, outbox atomicity, checkout → sync → refund flows, and ViewModels wired through
  the **real Koin graph** with an in-memory database and a scripted Firebase backend (`TestGraph`)
- `androidHostTest`: Konsist architecture rules
- Coverage gate: **≥ 80 % lines** (Kover). Compose UI, DI wiring and drawing code are verified on the iOS Simulator instead.

## Tools

- `tools/seed/build_seed.py` — regenerates the bundled offline catalog (`composeResources/files/seed`: raw API JSON + thumbnails)
- `tools/catalog/push_catalog.py` — uploads curated products (same JSON as `files/seed/new_arrivals.json`) to Firestore `catalog/{id}` using your own `gcloud` login; clients can only read that collection. Product images are CC BY-SA (Wikimedia Commons) and are credited on the detail page.
- `tools/stores/build_logos.py` — regenerates store logos (`composeResources/drawable/logo_*.xml` + `BrandLogos.kt`) from [Simple Icons](https://simpleicons.org) (CC0). Brands not in Simple Icons get a monogram in their real brand colour. Logos are the brands' trademarks and only identify the seller; the app is a demo, not affiliated with any brand.
- `tools/theme/generate_colors.py` — regenerates the M3 light/dark palettes from the brand seeds (navy `#131921`, orange `#FF9900`)

## Known limitations

- Security rules bound the economy (coins only go down except bounded game rewards behind server-timed cooldowns) but a
  determined client could still, e.g., under-pay an order. A Cloud Functions checkout would close that (needs the Blaze plan).
- Rules have not been run against the Firebase Emulator in CI yet (the Firebase CLI isn't installed here).
- Android has been built and unit-tested but not run on an emulator in this environment.
