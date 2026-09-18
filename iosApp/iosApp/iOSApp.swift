import SwiftUI
import Shared

@main
struct iOSApp: App {
    @Environment(\.scenePhase) private var scenePhase

    init() {
        KoinInit_iosKt.startAmazgone()
        BackgroundSyncScheduler.register()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .background { BackgroundSyncScheduler.schedule() }
        }
    }
}
