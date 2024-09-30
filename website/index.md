---
layout: home
---

**Taskfolio** is a personal project designed to showcase my Android development skills.
The app is a lightweight task manager that integrates with [Google Tasks](https://developers.google.com/tasks) through its REST API,
adhering to a clean and modern MVVM architecture. I’ve used [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html),
[Jetpack Compose](https://developer.android.com/compose) and
[Material Design 3](https://developer.android.com/develop/ui/compose/designsystems/material3) to create a smooth and visually appealing UI,
while also incorporating Room for local data persistence and OAuth 2.0 for secure authentication.

This project highlights the breadth of my Android knowledge, from API integration and UI design to database management and CI/CD setup.
It’s designed not just as a functioning task manager, but as a demonstration of my ability to deliver well-structured, maintainable,
and scalable Android apps.

[![Taskfolio Android Application]({{ site.baseurl }}/assets/GetItOnGooglePlay_Badge_Web_color_English.png)](https://play.google.com/store/apps/details?id=net.opatry.tasks.app)

| --------------------------------------- |--------------------------------------- | ---------------------------------- |
| ![]({{ site.baseurl }}/assets/screens/task_lists_light.png) | ![]({{ site.baseurl }}/assets/screens/groceries_light.png) | ![](assets/screens/home_dark.png)  |

## 🛠️ Tech stack

- [Kotlin](https://kotlinlang.org/), [Multiplatform (KMP)](https://kotlinlang.org/docs/multiplatform.html) (currently Desktop & Android are supported)
  - iOS & Web are not planned any time soon (contribution are welcome 🤝)
- [Kotlin coroutines](https://kotlinlang.org/docs/reference/coroutines/coroutines-guide.html)
- [Kotlin multiplatform](https://kotlinlang.org/docs/multiplatform.html) (aka KMP)
- [Ktor client](https://ktor.io/) (+ [Kotlinx serialization](https://kotlinlang.org/docs/serialization.html))
- [Room](https://developer.android.com/training/data-storage/room) (local persistance)
- [Koin](https://insert-koin.io/) for dependency injection
- [Material Design Components](https://developer.android.com/develop/ui/compose/designsystems/material3)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- Kinda follows [Google architecture guidelines](https://developer.android.com/topic/architecture)
- [Coil](https://coil-kt.github.io/coil/)
- [GitHub Actions](https://docs.github.com/en/actions) for CI
  - build Android & Desktop apps
  - run tests
  - publish app on Play Store
  - publish companion website on [Github pages](https://pages.github.com/)

## 🔗 Links

- <span style="fill: currentcolor;" class="icon icon--github">{% include icon-github.svg %}</span> [opatry/taskfolio](https://github.com/opatry/taskfolio)
- [Privacy Policy]({{ site.baseurl }}{% link legal/privacy-policy.md %})
