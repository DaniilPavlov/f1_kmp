import Foundation
import ComposeApp

#if canImport(FirebaseCore)
import FirebaseCore
import FirebaseAnalytics
import FirebaseCrashlytics
import FirebaseRemoteConfig
#endif

#if canImport(AppMetricaCore)
import AppMetricaCore
#endif

/// Bootstrap Firebase + AppMetrica + Remote Config для iOS host.
///
/// Пакеты подключаются через SPM в Xcode (см. README). Без пакетов —
/// RC остаётся на defaults, AppMetrica skip.
enum AnalyticsBootstrap {
    private static let notificationsKey = "local_notifications_enabled"
    private static let minVersionKey = "min_app_version"

    static func configure() {
        configureFirebase()
        activateAppMetrica()
        fetchRemoteConfig()
    }

    private static func configureFirebase() {
        #if canImport(FirebaseCore)
        if FirebaseApp.app() == nil {
            FirebaseApp.configure()
        }
        #if DEBUG
        Analytics.setAnalyticsCollectionEnabled(false)
        Crashlytics.crashlytics().setCrashlyticsCollectionEnabled(false)
        #else
        Analytics.setAnalyticsCollectionEnabled(true)
        Crashlytics.crashlytics().setCrashlyticsCollectionEnabled(true)
        #endif
        #endif
    }

    private static func activateAppMetrica() {
        guard let apiKey = Bundle.main.object(forInfoDictionaryKey: "AppMetricaApiKey") as? String,
              !apiKey.isEmpty else {
            return
        }
        #if canImport(AppMetricaCore)
        guard let config = AppMetricaConfiguration(apiKey: apiKey) else { return }
        config.areCrashReportingEnabled = false
        config.areLocationTrackingEnabled = false
        AppMetrica.activate(with: config)
        #else
        _ = apiKey
        #endif
    }

    private static func fetchRemoteConfig() {
        #if canImport(FirebaseRemoteConfig)
        let remoteConfig = RemoteConfig.remoteConfig()
        let settings = RemoteConfigSettings()
        #if DEBUG
        settings.minimumFetchInterval = 0
        #else
        settings.minimumFetchInterval = 3600
        #endif
        settings.fetchTimeout = 10
        remoteConfig.configSettings = settings
        remoteConfig.setDefaults([
            notificationsKey: true as NSObject,
            minVersionKey: "0.0.0" as NSObject,
        ])
        remoteConfig.fetchAndActivate { _, _ in
            let enabled = remoteConfig.configValue(forKey: notificationsKey).boolValue
            let minVersion = remoteConfig.configValue(forKey: minVersionKey).stringValue ?? "0.0.0"
            IosRemoteConfigBridge.shared.apply(
                localNotificationsEnabled: enabled,
                minAppVersion: minVersion
            )
        }
        #else
        IosRemoteConfigBridge.shared.apply(
            localNotificationsEnabled: true,
            minAppVersion: "0.0.0"
        )
        #endif
    }
}
