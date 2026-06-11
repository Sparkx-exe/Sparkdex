# SparkDex (MangaZen)

A high-fidelity modern Manga reader leveraging the public [MangaDex API](https://mangadex.org/), built entirely with Jetpack Compose using modern Android architectural principles.

## Features

- **Modern UI/UX**: Built entirely with declarative UI using Jetpack Compose and Material 3 guidelines.
- **Fast & Responsive**: 120Hz unlocked, high-refresh-rate supported, with buttery smooth screen transitions. 
- **Offline Library & History**: Uses Room local database to safely store your bookmarked and recently read manga locally on your device.
- **Seamless Manga Reading**: A rich, robust reader that loads chapters instantly and supports pre-fetching page data.
- **Search & Filter**: Search to easily find the manga you want based on tags, author, and genres.
- **Dark Mode Support**: A beautiful, completely immersive native dark mode.

## Tech Stack & Architecture

- **Kotlin**: 100% Kotlin codebase.
- **Jetpack Compose**: For all declarative UI development.
- **MVVM Architecture**: Utilizing Jetpack ViewModels to separate UI layer from the domain layer.
- **Retrofit & OkHttp**: Robust and fast network requests for interacting with the MangaDex API.
- **Moshi**: Type-safe JSON serialization and deserialization.
- **Room Database**: Offline first persistent caching of data.
- **Coil**: Smooth image loading right from the network.
- **Coroutines & Flow**: Reactive and asynchronous state management.
- **Navigation Compose**: Type-safe declarative app routing.

## Screenshots

*(Add screenshot images of the app here)*

## Installation

1. Clone this repository locally.
2. Open the project in Android Studio (Giraffe or newer).
3. Connect an Android device (Target API 36 / Minimum SDK 24) or initiate an emulator.
4. Let Gradle sync naturally resolve all dependencies from the `libs.versions.toml`.
5. Run the app directly onto your device.

## API Acknowledgement

All Manga logic and images are actively sourced from the open-source community at **MangaDex**. This app conforms to their guidelines and usage API constraints.

## License

This project is licensed under the MIT License - see the LICENSE file for details.
