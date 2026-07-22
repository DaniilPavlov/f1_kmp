import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    init() {
        // Ставим AppleLanguages до первого чтения preferredLanguages / Compose Locale.current.
        // Иначе на английском устройстве UI остаётся на EN, хотя LocaleController = ru.
        let defaults = UserDefaults.standard
        let saved = defaults.string(forKey: "app_locale")
        let language = (saved == "en" || saved == "ru") ? saved! : "ru"
        defaults.set([language], forKey: "AppleLanguages")
        defaults.synchronize()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
