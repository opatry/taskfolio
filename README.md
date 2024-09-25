[![Build & Test](https://github.com/opatry/tasks-app/actions/workflows/Build.yml/badge.svg)](https://github.com/opatry/tasks-app/actions/workflows/Build.yml)

# Tasks app

A basic TODO list application based on [Google Tasks REST API](https://developers.google.com/tasks/reference/rest) to showcase [KMP](https://kotlinlang.org/docs/multiplatform.html) app capabilities.

## Tech stack

- [Material Design Components](https://developer.android.com/develop/ui/compose/designsystems/material3)
- [Kotlin](https://kotlinlang.org/)
- [Kotlin coroutines](https://kotlinlang.org/docs/reference/coroutines/coroutines-guide.html)
- [Kotlin multiplatform](https://kotlinlang.org/docs/multiplatform.html) (aka KMP)
- [Ktor client](https://ktor.io/) (+ [Kotlinx serialization](https://kotlinlang.org/docs/serialization.html))
- [Room](https://developer.android.com/training/data-storage/room)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Coil](https://coil-kt.github.io/coil/)

## Local development

Decrypt `*.gpg` files needed for development:

```bash
export PLAYSTORE_SECRET_PASSPHRASE=...
./_ci/decrypt_secrets.sh
```

## License

```
The MIT License (MIT)

Copyright (c) 2024 Olivier Patry

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
