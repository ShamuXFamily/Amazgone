import FirebaseAnalytics
import FirebaseCore
import Shared

/// iOS side of the shared `AnalyticsSink`: forwards events to the Firebase Analytics SDK.
/// Fully qualified (`FirebaseAnalytics.Analytics`) because the shared module also exports an `Analytics` class.
final class FirebaseAnalyticsSink: AnalyticsSink {
    func logEvent(name: String, params: [String: Any]) {
        FirebaseAnalytics.Analytics.logEvent(name, parameters: params)
    }

    func setUserId(id: String?) {
        FirebaseAnalytics.Analytics.setUserID(id)
    }

    func setUserProperty(name: String, value: String?) {
        FirebaseAnalytics.Analytics.setUserProperty(value, forName: name)
    }

    /// Configures Firebase from GoogleService-Info.plist (gitignored). Without it the app runs without analytics.
    static func startIfConfigured() -> FirebaseAnalyticsSink? {
        guard Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil else { return nil }
        if FirebaseApp.app() == nil { FirebaseApp.configure() }
        return FirebaseAnalyticsSink()
    }
}
