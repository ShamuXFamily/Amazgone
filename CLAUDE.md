# Amazgone — project rules

Fake Amazon-style shop with a game layer. Kotlin Multiplatform (Android + iOS), Compose Multiplatform shared UI.
There is NO real money anywhere. The only currency is virtual coins.

## Commands
- JDK: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`
- Tests: `./gradlew :shared:jvmTest :shared:testAndroidHostTest :shared:iosSimulatorArm64Test` · Coverage gate: `./gradlew :shared:koverVerify` (≥80%)
- Test layout: `commonTest` (pure/domain/fakes) · `src/dbTest` (Room + real Koin graph via `TestGraph`, runs on JVM and iOS) · `androidHostTest` (Konsist)
- iOS framework: `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64` → build `iosApp` in Xcode/xcodebuild
- Android: `./gradlew :androidApp:assembleDebug`

## Non-negotiable rules
1. **Offline-first.** The UI reads ONLY from Room `Flow`s. Network/Firestore never feed the UI directly; they sync into Room.
   Every user mutation = Room write + `outbox` row in ONE transaction; `SyncEngine` pushes it later. The outbox id is the idempotency key.
   Firestore offline persistence stays DISABLED. The app must launch and work in airplane mode, using the bundled seed catalog.
2. **Server-authoritative economy.** Coins are an append-only `wallet_tx` ledger. The balance is always calculated from it and never stored as a single editable number.
   Offline purchases and rewards are PENDING until the Firestore transaction confirms them. If the server rejects one, reverse it and tell the user.
3. **iOS Simulator first.** Verify every feature on the iOS Simulator before Android. A phase is not done until it runs on the simulator.
4. **Material Design 3 only.** Use M3 components and `AmazgoneTheme` (light and dark ColorSchemes from the navy/orange seeds). Colors come only from `MaterialTheme.colorScheme`, text styles from `MaterialTheme.typography`, and shapes from `MaterialTheme.shapes`. No raw hex values or hardcoded dp values in feature code; use theme or dimension tokens. Adaptive layouts use `NavigationSuiteScaffold` and WindowSizeClass.
5. **Motion system.** Every animation uses `core/ui/motion/MotionTokens`, with no hardcoded durations or easing. Respect `LocalReduceMotion`.
   Use shared elements for product transitions, and add haptics on key moments.
6. **Catalog sources** go through the `CatalogSource` interface: DummyJSON (primary), CheapShark (games, needs a User-Agent header), and optionally Open Food Facts.
   Map every source into the shared domain `Product` with the id `"{source}:{id}"`. Screens never use DTOs directly.
7. **MVI + Clean Architecture.** Package by feature; each feature has `domain/` (pure Kotlin models, repository interfaces, use cases), `data/` (sources, DTOs, Room, mappers, repository implementations) and `presentation/` (Contract, ViewModel, Screen).
   Dependencies point `presentation → domain ← data`, and `domain` never imports frameworks. Every screen has an `XxxContract` (State, Intent, Effect) and a ViewModel that extends `core/presentation/mvi/MviViewModel`.
   The UI sends only Intents. State is immutable and updated with `copy()`. One-shot events (navigation, snackbar, animation triggers) are Effects. Screens are stateless (`state`, `onIntent`).
   One use case per action. Konsist tests in `androidHostTest` enforce these rules, so don't break them.
8. **Files** 200–400 lines and at most 800. Functions under 50 lines. Nesting at most 4 levels. No hardcoded values: use constants or config.
9. **TDD**: write the test first. Test commonTest logic with fakes, Turbine and Ktor `MockEngine`. Test Firestore code against the Firebase Emulator.
10. **Security:** never commit `GoogleService-Info.plist` or `google-services.json` (they are gitignored). Every change to Firestore data must also update `firestore.rules` and its emulator tests.
   Validate username (3–20 chars, `[a-z0-9_]`) and password (≥8 chars) at the UI boundary.
11. **Errors:** show the user a friendly message and log the details. Never swallow an exception. Show loading, error and empty states on every screen.

## Strings
Compose resources: write a literal `%` (never `%%`) and a plain `'` (never `\'`); neither is unescaped at runtime.

## Database
Pre-release: bump `AppDatabase.SCHEMA_VERSION` on EVERY entity/DAO schema change (destructive fallback only triggers on a version bump). Add real migrations before public release.

## Backend
Firebase is used through its REST APIs (`core/remote`: `FirebaseAuthApi`, `FirestoreClient`), not an SDK. Every server write is an
outbox handler; Set writes that change a timestamp must carry `transforms` in the SAME write so `firestore.rules` sees both.
Keep reward/XP/cooldown constants in sync with `firestore.rules` (see its header comment).
Outbox: `PushResult.Rejected` = server said no → compensate. `Retry` = transient → back off, then park (never compensate).

## Analytics
Log events only through `core/analytics/Analytics` (typed methods, GA4 recommended names where one exists), injected into ViewModels.
Coins are virtual: send them as `value`/`price_coins` WITHOUT a `currency`, so Firebase never reports them as revenue.
Never log PII (username, address, review text); `identify()` takes the Firebase uid only. Analytics must never throw.
Platform sinks: `FirebaseAnalyticsSink` in androidMain (needs google-services.json values) and iosApp/FirebaseAnalyticsSink.swift.

## Delivery
Couriers live in `delivery/domain/model/Courier.kt`; their unlock prices are mirrored in `firestore.rules` (`courierPrices()`),
and `FirestoreRulesSyncTest` fails if they drift. Store origins: `Origins.kt`. Addresses are geocoded with OpenStreetMap
Nominatim (≤1 request/s, identifying User-Agent) with country-centre fallback. Maps use tile.openstreetmap.org: keep the
"© OpenStreetMap contributors" credit visible, the app User-Agent on image requests, and never prefetch tiles in bulk.

## Auth
Firebase Auth uses email only, so a username maps to `"{username}@amazgone.local"`. Uniqueness is enforced by `usernames/{username}` in Firestore.
Register and first login need a network connection. A guest (local) account works offline.

## Catalog sources (researched 2026-09-18)
- DummyJSON `https://dummyjson.com/products`: 194 products in 24 categories. Supports search, sortBy, limit/skip, select, and reviews.
- CheapShark `https://www.cheapshark.com/api/1.0/deals`: live game deals. It REQUIRES a descriptive User-Agent header. For images, use the Steam header `https://cdn.akamai.steamstatic.com/steam/apps/{steamAppID}/header.jpg`.
- Rejected: FakeStoreAPI (only 20 items), Platzi/escuelajs (anyone can edit the data, so quality is poor), reactbd (returns HTML).

## Full plan
See `docs/PLAN.md`.
