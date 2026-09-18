import BackgroundTasks
import Shared

/// Registers and schedules the BGAppRefreshTask that drains the Kotlin outbox in the background.
enum BackgroundSyncScheduler {
    static let identifier = "com.cikup.amazgone.sync"
    private static let minimumInterval: TimeInterval = 15 * 60

    static func register() {
        BGTaskScheduler.shared.register(forTaskWithIdentifier: identifier, using: nil) { task in
            guard let refresh = task as? BGAppRefreshTask else { return }
            handle(refresh)
        }
    }

    static func schedule() {
        let request = BGAppRefreshTaskRequest(identifier: identifier)
        request.earliestBeginDate = Date(timeIntervalSinceNow: minimumInterval)
        do {
            try BGTaskScheduler.shared.submit(request)
        } catch {
            // Simulator and Low Power Mode may refuse; foreground sync still covers correctness.
            print("E/BackgroundSync: could not schedule refresh: \(error)")
        }
    }

    private static func handle(_ task: BGAppRefreshTask) {
        schedule() // keep the chain going
        let cancel = BackgroundSync.shared.run { success in
            task.setTaskCompleted(success: success.boolValue)
        }
        task.expirationHandler = { cancel() }
    }
}
