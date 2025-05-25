---
layout: home
title: Taskfolio
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

[![Taskfolio Android Application](assets/GetItOnGooglePlay_Badge_Web_color_English.png)](https://play.google.com/store/apps/details?id=net.opatry.tasks.app)

| --------------------------------------- |--------------------------------------- |--------------------------------------- | ---------------------------------- |
| ![](assets/screens/task_lists_light.png) | ![](assets/screens/groceries_light.png) | ![](assets/screens/add_task_light.png) | ![](assets/screens/home_dark.png)  |

## 🎯 Project intentions

- [x] Showcase my expertise in Android application development
- [x] Demonstrate UI development using Jetpack Compose with Material Design 3.
- [x] Include local-first capabilities for local data storage using Room.
- [x] OAuth 2.0 authentication.
- [x] Provide sync capabilities with Google Tasks for seamless task management.
- [x] Illustrate my ability to set up CI/CD pipelines and publish apps to the Play Store.

## ❌ Out of scope

This project is not intended as a comprehensive task manager for public use.
I do not aim to implement advanced features beyond what is supported by the Google Tasks REST API.

- no task list reordering
- no starred task
- no task priority
- only due date, no custom time support
- no task recurrence
- limited hierarchy (2 levels)

## 🚧 Known Limitations

- Authentication flow isn't 100% reliable yet ([#34](https://github.com/opatry/taskfolio/issues/34)).
- Local-first support with Google Tasks sync is limited, in particular sorting & conflict management is barely implemented ([#140](https://github.com/opatry/taskfolio/issues/140)).
- No indentation support (ongoing) (#129)
- Task completion state toggle doesn't honor indentation properly (ongoing) (#175)
- No drag'n'drop to re-order tasks nor move them between lists ([#133](https://github.com/opatry/taskfolio/issues/133)).
- Task deletion undo is not implemented ([#149](https://github.com/opatry/taskfolio/issues/149)).
- Local action sync failure might not be synced again ([#150](https://github.com/opatry/taskfolio/issues/150)).
- Setting due date isn't properly supported ([#155](https://github.com/opatry/taskfolio/issues/155)).

## 🛠️ Tech stack

- [Kotlin](https://kotlinlang.org/), [Multiplatform (KMP)](https://kotlinlang.org/docs/multiplatform.html) (currently Desktop & Android are supported)
  - iOS & Web are not planned any time soon (contribution are welcome 🤝)
- [Kotlin coroutines](https://kotlinlang.org/docs/reference/coroutines/coroutines-guide.html)
- [Kotlin multiplatform](https://kotlinlang.org/docs/multiplatform.html) (aka KMP)
- [Ktor client](https://ktor.io/) (+ [Kotlinx serialization](https://kotlinlang.org/docs/serialization.html))
- [Room](https://developer.android.com/training/data-storage/room) for local persistance
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

- <span class="svg-icon">{% include lucide-icon-github-1px.svg %}</span> [opatry/taskfolio](https://github.com/opatry/taskfolio)
- <span class="svg-icon">{% include lucide-icon-twitter-1px.svg %}</span> [o_patry](https://twitter.com/opatry)
- <span class="svg-icon">{% include lucide-icon-shield-check-1px.svg %}</span> [Privacy Policy]({% link legal/privacy-policy.md %})
