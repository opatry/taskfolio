import SwiftUI
import TasksAppShared

@main
struct TaskfolioApp: App {
    init() {
        InitKoinKt.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
