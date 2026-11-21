# Zume

Zume is an Android social-network starter project built with Kotlin and Jetpack Compose.

## Included

- Android application module and launcher screen
- Blue Zume welcome and sign-in/register interface
- Gradle build configuration
- GitHub Actions workflow for building a debug APK

## Build status

The APK build is being triggered through GitHub Actions. The workflow creates a Gradle Wrapper in the runner, builds the debug APK, and uploads it as an artifact if successful.

## Planned next features

- Connect registration and sign-in to Firebase Authentication
- User profiles
- Photo and video posts
- Private messaging
