import SwiftUI
import TasksAppShared

@main
struct TaskfolioApp: App {
    init() {
        InitKoinKt.doInitKoin(googleAuthenticator: IOSGoogleAuthenticator())
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
