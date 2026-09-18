# Amazgone — Fake Amazon-style Shop with Game Layer (KMP)

## Context
`Amazgone` is a fresh Kotlin Multiplatform template (Compose Multiplatform shared UI, Android + iOS, Kotlin 2.4.20, CMP 1.12, AGP 9.1). Only the template's `App.kt` / `Greeting.kt` exist. Goal: build a **fake online shop modelled on Amazon** — real-looking catalog, cart, fake checkout — plus a **game layer**: virtual coin wallet (the only "money"), mini-games that award coins/coupons, XP/levels/achievements, and an **online shared leaderboard**. Accounts are **username + password** via Firebase. No real payments ever.

Decisions (from user):
- Catalog: **multi-source**. The sources were researched and live-tested on 2026-09-18 (see the table below).
- Local persistence: **Room KMP** (cart + product cache).
- Online backend: **Firebase** (Auth + Firestore) for accounts, wallet, orders, achievements, leaderboard.
- MVP includes: browse/search/detail, cart + fake checkout, account + orders + wishlist, reviews + recommendations, wallet, mini-games, levels/achievements/leaderboard.

## Catalog data sources (research results)
| API | Tested result | Verdict |
|---|---|---|
| **DummyJSON** `dummyjson.com/products` | 194 products in 24 categories. Data updated May 2026. Fields include brand, sku, stock, `availabilityStatus`, `discountPercentage`, warranty, shipping, return policy, dimensions, a reviews array and multiple webp images. The server supports search (`/search?q=`), sorting (`sortBy&order`), paging (`limit/skip`), `select` and `/category/{slug}`. | **Primary** general store. It has the richest schema. |
| **CheapShark** `cheapshark.com/api/1.0/deals` | Real video game deals, updated live from Steam and other stores. Fields include sale and normal price, savings %, Steam rating, Metacritic score and `steamAppID`. It requires a descriptive **User-Agent** header. | **"Video Games" department** with real, live prices. This fits the game theme. Images come from `https://cdn.akamai.steamstatic.com/steam/apps/{steamAppID}/header.jpg`, which is larger than the thumbnail `thumb` field. |
| **Open Food Facts** `world.openfoodfacts.org/api/v2/search` | 4.7M real products with brand, image and nutri-score. Requires a User-Agent. | **Optional "Grocery" department** (phase 8+). |
| FakeStoreAPI | Only 20 products | Rejected: too small |
| Platzi (`api.escuelajs.co`) | Anyone can edit the data, and titles like "T-Shirt 123" show it | Rejected: data quality is poor |
| jsondata/fakestoreapi.reactbd | The endpoint returned an HTML page, not JSON | Rejected: unreliable |

Design: a `CatalogSource` interface with `DummyJsonSource`, `CheapSharkSource` and later `OpenFoodFactsSource`. Each source maps its DTOs into one shared domain model, `Product(id = "{source}:{id}", department, …)`. A `CatalogRepository` merges the departments and caches them in Room. Fields a source doesn't have (for example reviews for games) are nullable, and the UI hides those sections. CheapShark ratings (`steamRatingPercent`) are converted to a 5-star value.

## Tech stack (add to `gradle/libs.versions.toml`)
Pick latest versions compatible with Kotlin 2.4.20 at implementation time (verify via Context7/Maven Central).
| Concern | Library |
|---|---|
| Networking | Ktor client (`core`, `content-negotiation`, `serialization-kotlinx-json`, `logging`; engines `okhttp` android / `darwin` ios) |
| JSON | kotlinx.serialization (+ `kotlinSerialization` plugin) |
| DI | Koin (`koin-core`, `koin-compose`, `koin-compose-viewmodel`) |
| Navigation | `org.jetbrains.androidx.navigation:navigation-compose` (type-safe `@Serializable` routes) |
| Images | Coil 3 (`coil-compose`, `coil-network-ktor3`) |
| DB | Room KMP (`room-runtime`, `room-compiler` via KSP, `sqlite-bundled`) + KSP plugin |
| Firebase | GitLive `firebase-kotlin-sdk` (`firebase-auth`, `firebase-firestore`) — KMP wrapper over native SDKs |
| Background sync | `androidx.work:work-runtime-ktx` (Android), `BGTaskScheduler` (iOS, in Swift) |
| Time | kotlinx-datetime (daily-spin cooldowns) |
| Motion | Compose animation APIs (`SharedTransitionLayout`, `AnimatedContent`, `Animatable`, `spring`), **Compottie** (Lottie for KMP) for celebrations |
| Architecture tests | Konsist (in `androidHostTest`) |
| Tests | kotlin-test, `kotlinx-coroutines-test`, Turbine, Ktor `MockEngine`, Kover (80% gate) |

## Architecture: MVI + Clean Architecture
All code lives in `shared/src/commonMain/kotlin/com/cikup/amazgone/`. It is **packaged by feature**, and every feature has three Clean layers.

**Dependency rule:** `presentation → domain ← data`. `domain` is pure Kotlin: no Compose, Ktor, Room, Firebase or Koin imports. Only `data` knows about frameworks. `presentation` sees only domain models and use cases.

```
feature/
  domain/        model/ (entities: Product, CartItem, Order, WalletTx…)  ← pure Kotlin, immutable
                 repository/ (interfaces only)  usecase/ (one action per class, operator fun invoke)
  data/          remote/ (Ktor/Firestore data sources + DTOs)  local/ (Room DAOs + entities)
                 mapper/ (DTO↔Entity↔Domain)  repository/ (Impl: Room SSOT + outbox, per offline-first rules)
  presentation/  XxxContract.kt  (State, Intent, Effect)   XxxViewModel.kt (MVI store)
                 XxxScreen.kt (stateless composable: state + onIntent)   components/
```

**MVI contract** (`core/presentation/mvi/`):
```kotlin
interface UiState; interface UiIntent; interface UiEffect
abstract class MviViewModel<S : UiState, I : UiIntent, E : UiEffect>(initial: S) : ViewModel() {
    val state: StateFlow<S>          // single immutable state, rendered by the Screen
    val effects: Flow<E>             // one-shot: navigation, snackbar, haptic, fly-to-cart trigger (Channel-backed)
    fun onIntent(intent: I)          // the ONLY entry point from the UI
    protected fun setState(reduce: S.() -> S)   // state.update { it.reduce() } — always copy(), never mutate
    protected fun sendEffect(effect: E)
}
```
- The flow is one-way: **UI → Intent → ViewModel → UseCase → Repository (Room Flow) → new State → UI**. Screens are stateless (`XxxScreen(state, onIntent)`), and a `XxxRoute` composable collects `state` and `effects` using `collectAsStateWithLifecycle`. This makes each screen easy to preview.
- Reducers are **pure** `(State, Result) -> State` functions in `XxxReducer.kt` when the logic isn't trivial, which makes them easy to unit test.
- Motion is state-driven. Animations are triggered by state fields or effects, for example `CartEffect.FlyToCart(productId, fromBounds)` or `SpinState.phase`, so they can be tested without a UI.
- Use cases return a `Result<T>`/`DomainResult` or a `Flow<T>`. Errors are mapped to a `DomainError` sealed class and then to user-facing strings in presentation.
- **Architecture is enforced** by **Konsist** tests in `androidHostTest`. They check that `domain` doesn't import `data`, `presentation`, frameworks or Compose, that every `*ViewModel` extends `MviViewModel`, that every `*Contract` defines State, Intent and Effect, and that every `*UseCase` has one public `invoke`.

Example: `catalog/`
```
catalog/domain/     model/Product.kt, Department.kt, Review.kt · repository/CatalogRepository.kt
                    usecase/ObserveProductsUseCase, SearchProductsUseCase, GetProductDetailUseCase, GetRecommendationsUseCase
catalog/data/       remote/DummyJsonSource, CheapSharkSource (: CatalogSource), dto/ · local/ProductDao, ProductFts
                    mapper/ProductMappers.kt · repository/CatalogRepositoryImpl.kt · sync/CatalogSyncer.kt
catalog/presentation/ home/HomeContract, HomeViewModel, HomeScreen · search/… · detail/… · components/ProductCard, RatingStars
```
The other features follow the same layout: `cart/`, `checkout/`, `account/`, `orders/`, `wishlist/`, `wallet/`, `games/{spinwheel,scratchcard,lightningdeal}/`, `progress/`.
- `core/`: `network/` (HttpClient factory), `database/` (AppDatabase, expect/actual builder), `sync/` (outbox, SyncEngine, ConnectivityObserver), `presentation/mvi/`, `designsystem/` (M3 theme and motion), `di/` (a Koin module for each feature and layer).
- `navigation/`: `Routes.kt` (`@Serializable`), `AppNavHost`, and `NavigationSuiteScaffold` with the destinations Home · Games · Cart · Account.
- Platform actuals (`androidMain` / `iosMain`): the Room DB builder, Ktor engine, connectivity, reduce motion, and `MainViewController.kt` (already exists).

### Key design points
- **Username login on Firebase:** Firebase Auth is email-based → map `username` to synthetic email `"{username}@amazgone.local"`; reserve uniqueness via Firestore `usernames/{username}` doc created in the same transaction as `users/{uid}`. Validate username (3–20, `[a-z0-9_]`) and password (≥8) at the UI boundary.
- **Prices:** DummyJSON USD price converted to coins (e.g. `coins = ceil(price * 10)`), constant in `CartCalculator`. New users get a starting grant (e.g. 5,000 coins).
- **Firestore data model:**
  - `users/{uid}`: username, coins, xp, level, lastSpinAt, createdAt
  - `users/{uid}/orders/{orderId}`, `users/{uid}/wishlist/{productId}`, `users/{uid}/achievements/{id}`
  - `leaderboard/{uid}`: username, xp, level (denormalized, queried `orderBy xp desc limit 50`)
- **Checkout** runs in two stages. Stage 1 is a local Room transaction: check the local balance ≥ total, add a pending debit, create the order as `PENDING_SYNC`, add an outbox row, and clear the cart. Stage 2 happens when `SyncEngine` runs: a Firestore transaction verifies coins ≥ total, then decrements coins, writes the order, adds XP and updates the leaderboard. If the server rejects it, the debit is reversed and the cart is restored, with a user-friendly message.
- **Anti-cheat (reasonable for a fake shop):** `firestore.rules` — users write only their own docs; `coins` may only decrease except via reward writes bounded by max reward and `lastSpinAt` ≥ 24h cooldown; leaderboard writes must match own `users` doc. (Cloud Functions would be stronger but require Blaze plan — note as future.)
- **Recommendations:** "Customers also bought" = same-category products from DummyJSON (`/products/category/{c}`), excluding current; "Top rated" from sort by rating.
- **Offline-first.** See the section below.

## Offline-first architecture
**Rule: the UI reads only from Room.** The network and Firestore never feed the UI directly. They sync *into* Room, and every screen observes Room `Flow`s. The app launches and works fully without a connection.

- **Single source of truth.** Every repository exposes `Flow<T>` from Room DAOs. Remote data is written into Room, and Room emits the change to the UI.
- **First launch with no network:** a **bundled seed snapshot** in compose resources (`files/seed/products.json`, about 194 DummyJSON products plus about 60 CheapShark games) is imported into Room on first run. Product images are bundled only as thumbnails. Coil's disk cache (about 250 MB) keeps full images after they've been viewed once.
- **Catalog sync, stale-while-revalidate.** `CatalogSyncer` refreshes whenever the app is in the foreground and the connection comes back, if the data is older than its TTL (DummyJSON 24h, CheapShark deals 1h). It uses paged upserts. Search runs **locally** against a Room FTS4 table, so it works offline. When online, remote search results are merged in.
- **Local writes, then an outbox.** Every user mutation (cart change, wishlist toggle, order, review, game reward, XP gain) is written to Room first, in the same transaction as an `outbox` row (`id, type, payloadJson, createdAt, attempts, status`). `SyncEngine` drains the outbox to Firestore in FIFO order. It uses exponential backoff, and each write has an idempotency key (`outbox.id` is used as the Firestore doc ID, so retries never duplicate).
- **Wallet as a local ledger.** Coins are stored as append-only `wallet_tx` rows. The balance is `SUM(amount)` over confirmed plus pending rows. An offline checkout creates the order as `PENDING_SYNC` and a pending debit. When the device reconnects, the Firestore transaction re-validates the balance and marks the order `CONFIRMED`. If it fails, the order becomes `REJECTED`, the debit is reversed, the user gets a notification, and the cart is restored.
- **Games offline.** Spins and scratches run locally, and the reward is recorded as a pending ledger entry plus an outbox row. The daily-spin cooldown uses the last spin time stored locally and, once synced, the server's time. The security rules check the cooldown against `request.time`, so changing the device clock only earns a reward that the server later rejects.
- **Reading remote data.** Remote user profile, order history, achievements and leaderboard (top 50) are cached in Room and show a "last updated" time plus an offline banner. The leaderboard is read-only offline.
- **Auth offline.** Firebase keeps the session on the device, so the user stays signed in offline. **Register and first login require a connection** (a clear message explains this). Guest mode (a local-only account) works fully offline, and its local data merges into the account when the user registers.
- **Conflict policy.** Cart and wishlist: last write wins, per item, compared by `updatedAt`. Wallet, XP and orders: the server is authoritative, and local pending entries are reconciled against it. `SyncStatus` (Synced / Pending n / Offline / Error) is shown in the Account screen and as a small cloud icon in the top bar.
- **Connectivity and background.** `ConnectivityObserver` is an `expect/actual` (Android `ConnectivityManager.NetworkCallback`, iOS `NWPathMonitor`) that triggers `SyncEngine`. Background sync uses Android **WorkManager** (a unique periodic job plus a one-off job when the network returns) and iOS `BGTaskScheduler` (`BGAppRefreshTask`).
- **Firestore's own offline cache is disabled** (`persistenceEnabled = false`) so Room remains the only local store. That avoids having two caches that disagree.

Room entities: `ProductEntity`, `ProductFts`, `ReviewEntity`, `CartItemEntity`, `WishlistEntity`, `OrderEntity`, `OrderItemEntity`, `WalletTxEntity`, `XpEventEntity`, `AchievementEntity`, `LeaderboardEntryEntity`, `UserProfileEntity`, `OutboxEntity`, `SyncMetaEntity` (last sync time for each source).

## Material Design 3 (UI system)
The dependency `compose-material3` (1.12.0-alpha03) is already in `libs.versions.toml`. All UI is built with M3; there are no custom look-alike components.
- **Theme** (`core/ui/theme/`): `AmazgoneTheme` wraps `MaterialExpressiveTheme` (or `MaterialTheme` if the Expressive API isn't in that version). It has full **light and dark** `ColorScheme`s generated from brand seed colors (primary seed navy `#131921`, secondary/tertiary seed orange `#FF9900`, using the M3 tonal palettes from the Material Theme Builder). Android 12+ can optionally use dynamic color, but it's off by default to keep the brand colors. It uses an M3 `Typography` scale and M3 `Shapes`. Components read colors only through `MaterialTheme.colorScheme.*`, never raw hex values.
- **Motion:** `MotionTokens` build on M3 `MaterialTheme.motionScheme` (Expressive spatial and effects springs). The custom specs above extend it; they don't replace it.
- **Components used:** `NavigationSuiteScaffold` (bottom bar on phones, navigation rail on iPad or wide screens), `TopAppBar`/`LargeTopAppBar` with `exitUntilCollapsedScrollBehavior`, `SearchBar` / `ExpandedFullScreenSearchBar`, `FilterChip`/`AssistChip` (categories and filters), `ElevatedCard` (product cards), `ModalBottomSheet` (filters and quantity), `BadgedBox` (cart count), `PullToRefreshBox` (manual sync), `SnackbarHost` (undo, sync errors), `LinearWavyProgressIndicator`/`LoadingIndicator` (Expressive loading), `ButtonGroup` / `SegmentedButton` (sort), `ListItem`, `AlertDialog`, `DatePicker` not needed.
- **Adaptive layout:** `currentWindowAdaptiveInfo()` / `WindowSizeClass`. Phones show a 2-column grid; tablets and iPad show 3–4 columns with a list-detail layout for Product → Detail.
- **Accessibility:** M3's minimum 48dp touch targets, contrast from the tonal palettes (checked in both light and dark), and `contentDescription` on every icon.

## Motion design system (applies to every feature)
The shared motion code lives in `core/ui/motion/`:
- `MotionTokens`: durations (short 150, medium 300, long 500 ms), easing curves (emphasized, standard) and spring specs (`bouncy`, `snappy`, `gentle`). No animation hardcodes its own timing values.
- `LocalReduceMotion`: an `expect/actual` that reads the OS "reduce motion" setting (Android `ANIMATOR_DURATION_SCALE`, iOS `UIAccessibilityIsReduceMotionEnabled`). When it is on, animations collapse to fades.
- Reusable modifiers: `Modifier.shimmer()` (loading skeletons), `Modifier.pressScale()` (spring press feedback), `Modifier.staggeredEnter(index)` (lists cascading in) and `Modifier.parallax(scrollState)`.
- Haptics: `LocalHapticFeedback` on add-to-cart, spin stop, scratch reveal and level-up.

| Feature | Motion |
|---|---|
| Navigation | `SharedTransitionLayout` around the NavHost. Bottom-bar icons morph and bounce. Screens slide with fade-through. Predictive back on Android and swipe-back on iOS. |
| Home / catalog | Shimmer skeletons, staggered grid entry, a hero deals carousel with parallax and auto-scroll, and category chips with an animated selection indicator. |
| Product card → detail | **Shared-element** transition for the image and title (`sharedElement` / `sharedBounds`). The detail header collapses with parallax. The image pager has a zoom gesture. The rating stars fill one after another. |
| Search | Search bar expands with `animateContentSize`. Results cross-fade with `AnimatedContent`. The filter sheet uses spring physics. |
| Add to cart | **Fly-to-cart**: a copy of the product thumbnail follows a bezier arc to the cart tab, then the cart badge pops with a spring and its count rolls up with `AnimatedContent` slide. |
| Cart | The quantity number rolls when it changes. Swipe-to-delete uses spring settle and undo. The subtotal counts up as a number tween. `animateItem()` reorders the list. |
| Checkout | A step progress bar morphs between steps. The "Pay with Coins" button fills while you **long-press to confirm**. On success, coins fly from the wallet and a Lottie checkmark and confetti play. |
| Wallet | The balance counts up or down, a coin flip animates, and transactions enter staggered. |
| Spin wheel | Physics-based spin with `Animatable` and a decay plus spring settle toward a target chosen beforehand. The pointer ticks with haptics as segments pass. Winning shows a particle burst. |
| Scratch card | The foil erases where the finger drags (Canvas `BlendMode.Clear`). Once about 60% is scratched, the rest auto-reveals and the prize scales in. |
| Lightning deal | The countdown flips digits. The progress bar shows how much stock is claimed. The card pulses when less than 1 minute is left. |
| Levels / achievements | The XP bar fills with a spring. Level-up shows a full-screen Lottie overlay. Achievement badges unlock with a 3D flip (`graphicsLayer.rotationY`) and a shine sweep. |
| Leaderboard | Rows reorder with `animateItem()`, the podium rises in, and the user's own rank is highlighted with a pulse. |
| Auth | The logo animates in. Input errors shake the field. The login button morphs into a loading spinner. |

## Project `CLAUDE.md` (created first, at repo root, as step 0)
It holds the project rules so every future session follows them. Draft content:

```markdown
# Amazgone — project rules

Fake Amazon-style shop with a game layer. Kotlin Multiplatform (Android + iOS), Compose Multiplatform shared UI.
There is NO real money anywhere. The only currency is virtual coins.

## Commands
- Tests: `./gradlew :shared:allTests` · Coverage gate: `./gradlew :shared:koverVerify` (≥80%)
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

## Auth
Firebase Auth uses email only, so a username maps to `"{username}@amazgone.local"`. Uniqueness is enforced by `usernames/{username}` in Firestore.
Register and first login need a network connection. A guest (local) account works offline.
```

## Phased implementation (each phase ends green: build + tests)
0. **Create `CLAUDE.md`** with the content above.
1. **Foundation**: add the dependencies and plugins (KSP, serialization, Room, Koin, Ktor, Coil, Navigation, Compottie), the **M3 theme** (`AmazgoneTheme`, light and dark ColorSchemes, typography, shapes), `NavigationSuiteScaffold`, the **MVI base** (`MviViewModel`, `UiState`/`UiIntent`/`UiEffect`) plus Konsist architecture tests, the **motion system** (`MotionTokens`, `LocalReduceMotion`, shared modifiers), a NavHost with the bottom bar wrapped in `SharedTransitionLayout`, and Koin wiring. Remove the template's `Greeting*` files.
1b. **Offline core**: the Room database with all entities, the `outbox` table, `SyncEngine`, `ConnectivityObserver`, the import of the bundled seed snapshot, the `SyncStatus` UI, and a WorkManager / BGTask skeleton.
2. **Catalog**: the `CatalogSource` interface, the DummyJSON and CheapShark sources (Ktor with a User-Agent header) and DTO→domain mapping, plus the Room product cache, Home (category chips, deals carousel, grid with paging), Search (debounced query, sort/filter by price/rating), Product Detail (image pager, rating, reviews list, "also bought" row, Add to Cart).
3. **Cart** — Room `CartItemEntity`, qty stepper, remove, subtotal in coins, empty state.
4. **Firebase + Accounts** — add `google-services.json` (Android) / `GoogleService-Info.plist` + Firebase iOS SDK via SPM in Xcode (**user must create the Firebase project & provide these files**); Register/Login/Logout with username; Profile screen; starter coins.
5. **Wallet + Checkout + Orders** — coin balance badge, 3-step fake checkout, transactional order placement, order history/detail, wishlist.
6. **Games** — Spin Wheel (daily, Canvas rotation animation), Scratch Card (reveal gesture, coupon or coins), Lightning Deal (countdown, limited-time coin discount on a product).
7. **Progress** — XP rules (purchase, review, daily login, game play), level curve, achievements (first purchase, 10 orders, spin streak…), leaderboard screen, `firestore.rules`.
8. **Polish** — loading/error/empty states everywhere, accessibility labels, README update.

### Learning-mode contribution points (you write 5–10 lines each)
- `cart/domain/CartCalculator.applyCoupon()` — how coupons stack with coin prices.
- `games/spinwheel/domain/SpinRewardTable.pick()` — weighted reward odds (economy balance).
- `progress/domain/LevelCurve.levelFor(xp)` — linear vs exponential level curve.
- `account/domain/UsernameValidator` rules.

## Critical files
- Modify: `gradle/libs.versions.toml`, `build.gradle.kts`, `shared/build.gradle.kts`, `androidApp/build.gradle.kts` (google-services plugin), `shared/src/commonMain/.../App.kt`, `iosApp` Xcode project (Firebase SPM + plist), `iosApp/iosApp/iOSApp.swift` (`FirebaseApp.configure()`), `androidApp/src/main/AndroidManifest.xml` (INTERNET permission).
- New: feature packages above; `firestore.rules` at repo root; test mirrors under `shared/src/commonTest/`.

## Platform order: iOS Simulator first
- **At the end of each phase, run and verify on the iOS Simulator first.** Steps: `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64`, build `iosApp` with `xcodebuild` (or the simulator build tool), then launch it in the Simulator, take screenshots, and step through the flows with tap and inspect. Android (`:androidApp:assembleDebug` plus an emulator) is checked after iOS passes.
- **Firebase for iOS:** the user will provide `GoogleService-Info.plist`. It goes in `iosApp/iosApp/`, is added to the Xcode target's resources, and is added to `.gitignore`. Add the Firebase iOS SDK (FirebaseAuth, FirebaseFirestore) through SPM in `iosApp.xcodeproj`, and call `FirebaseApp.configure()` in `iOSApp.swift` `init()`. Phases 1–3 (and 1b) don't need Firebase and can go ahead before the plist arrives. **Phase 4 waits for the plist.**
- iOS-specific work to verify in the Simulator: the `NWPathMonitor` connectivity observer (test offline with Network Link Conditioner or by turning off the Mac's Wi-Fi), `BGTaskScheduler` registration in `Info.plist` (`BGTaskSchedulerPermittedIdentifiers`), the Room bundled SQLite driver, reduce motion (turn it on in the Simulator's Accessibility settings), and swipe-back working together with the shared-element transitions.

## Verification
- Offline: an **airplane-mode E2E** run. Fresh install with the network off, check the seed catalog appears, search works locally, add to cart, spin the wheel, check out, and see the order as `PENDING_SYNC`. Turn the network on and check the order becomes `CONFIRMED`, the coins match Firestore, and the leaderboard updates. Also test with a device clock that was changed, and an outbox retry after the server rejects a write (the debit is reversed). Unit-test `SyncEngine` with a fake remote that fails N times, and check idempotency (a retry doesn't create a duplicate order).
- Motion: Compose UI tests that use `mainClock.autoAdvance = false` to step through key animations (fly-to-cart, spin settles on the target segment). Also check that reduce-motion falls back to fades, and profile the frame rate on an emulator for janky frames.
- MVI: every ViewModel is tested by sending Intents and asserting the sequence of States and Effects with Turbine, using fake use cases. Reducers get pure-function tests. Konsist architecture tests run in `:shared:testAndroidHostTest`.
- Unit (commonTest, TDD-first): mappers for each source (with DummyJSON and CheapShark JSON fixtures), `CartCalculator`, `LevelCurve`, `SpinRewardTable`, `AchievementEngine`, ViewModels with fake repos + Turbine, `DummyJsonApi` with Ktor `MockEngine`, `UsernameValidator`.
- Integration: Room DAO tests (androidHostTest); Firestore repos against **Firebase Emulator Suite** (`firebase emulators:start`) incl. security-rules tests.
- Commands: `./gradlew :shared:allTests`, `./gradlew :shared:koverVerify` (≥80%), `./gradlew :androidApp:assembleDebug`, iOS build via Xcode / simulator tool.
- E2E flow, **iOS Simulator first**, then the Android emulator: register → browse → search → add to cart → spin wheel for coins → checkout with coins → order appears in history → XP rises → user shows on leaderboard (verify with 2 accounts).
- Run code-reviewer + security-reviewer agents after phases 4, 5, 7 (auth, wallet, rules).

## Prerequisites from user
- Create a Firebase project (Spark/free plan), enable **Email/Password** auth + **Firestore**, register Android app `com.cikup.amazgone` and iOS bundle id; download config files into the repo (gitignored).

---

## Implementation notes (as built, 2026-09-18)

Deviations from the plan above, with reasons:
- **Firebase via REST, not GitLive.** GitLive targets Kotlin 2.2 + Firebase iOS 11.8 (CocoaPods); this project is Kotlin 2.4 + SPM.
  Auth (Identity Toolkit + Secure Token) and Firestore (documents/commit/transactions/runQuery) are called with Ktor from
  `commonMain` (`core/remote`). Registration's uniqueness comes from Firebase Auth itself (synthetic email = username).
- **Celebrations drawn with Canvas, not Lottie/Compottie** — no third-party animation assets needed; respects reduce-motion.
- **Outbox exhaustion parks instead of rejecting.** Only an explicit server rejection compensates (refund/reversal);
  parked entries retry on "Sync now". Revoked logins *expire* the session (sync pauses, nothing is refunded).
- **JVM target added for tests only** (Room/Ktor on the host) so data/ViewModel tests run on JVM **and** iOS (`src/dbTest`)
  and Kover measures real coverage. `TestGraph` runs the production Koin graph with an in-memory DB + scripted Firebase.
- **Security rules** split user updates into exclusive activities (purchase / spin / scratch / daily visit) with per-activity
  bounds, server-time cooldowns, order↔debit coupling and level↔XP consistency (after an independent security review).

Verified: iOS Simulator end-to-end (browse → search → detail → fly-to-cart → checkout hold-to-pay → order; spin wheel;
scratch card; achievements; register against real Firebase Auth). Android: APK builds + host tests; not run on an emulator.
