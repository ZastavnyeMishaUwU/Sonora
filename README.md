# Sonora

**Sonora** is an Android music application built in Java. It allows users to discover music through the Jamendo API, play tracks, manage favorites, download music for offline use, and view detailed information about selected tracks.

The project combines API integration, local storage, authentication, session management, music playback, downloads, and multi-screen Android navigation in one application.

## Features

- User registration and login
- Persistent user session
- Music search using the Jamendo API
- Search by track or artist
- Track list with selectable results
- Music streaming and playback
- Play, pause, resume, stop, and seek controls
- Track cover loading
- Track details screen
- Add and remove tracks from favorites
- Favorites screen
- Track downloading
- Offline playback using downloaded files
- Download status tracking
- User settings screen
- Logout functionality
- Back navigation between application screens
- Background execution for network and database operations

## Application Flow

```text
Login / Register
       |
       v
      Home
       |
       +---- Search
       |       |
       |       v
       |     Player
       |       |
       |       v
       |  Track Details
       |
       +---- Favorites
       |       |
       |       v
       |     Player
       |       |
       |       v
       |  Track Details
       |
       +---- Settings
```

The Android back stack is preserved so users can naturally return to the previous screen.

## Main Screens

### Home

The main screen gives users access to music search and the primary features of the application.

Search results can be opened directly in the player.

### Login

Allows an existing user to sign in.

After successful authentication, Sonora stores the active session so the user can remain logged in between application launches.

### Registration

Allows a new user to create an account.

User information is stored locally using Room Database.

### Search

Allows users to search for tracks and artists through the Jamendo API.

Search results are displayed as a list. Selecting a track opens it in the player.

### Player

The player screen displays information about the currently selected track and provides music playback controls.

Available controls include:

- Play
- Pause
- Resume
- Stop
- Seek

The screen also displays:

- Track title
- Artist name
- Track artwork
- Current playback position
- Total track duration

From the player, users can open the Track Details screen.

### Track Details

Displays additional information about the current track, including:

- Track name
- Artist
- Album
- Duration
- Favorite status
- Download status

The user can also:

- Play the track
- Add or remove the track from favorites
- Download the track

Favorite and download actions use compact icon-based controls.

### Favorites

Displays tracks saved by the currently logged-in user.

Users can open a favorite track in the player or remove it from their favorites.

### Downloaded Tracks

Provides access to tracks that have been downloaded locally.

Downloaded files can be used for offline playback.

### Settings

Displays information about the current user account.

The settings screen also provides logout functionality.

After logout, authenticated screens are removed from the back stack.

## Technology Stack

Sonora uses:

- **Java**
- **Android SDK**
- **Android Studio**
- **Gradle**
- **Room Database**
- **SharedPreferences**
- **Jamendo API**
- **Material Components**
- **Glide**
- **RecyclerView**
- **ListView**
- **MediaPlayer**
- **ExecutorService**
- **Android Intents**

## Architecture

The project is divided into logical areas responsible for UI, authentication, local storage, API communication, playback, downloading, and repositories.

A simplified structure looks like this:

```text
app
└── src
    └── main
        ├── java
        │   └── ...
        │       ├── api
        │       ├── auth
        │       ├── database
        │       ├── downloader
        │       ├── models
        │       ├── musicplayback
        │       ├── repositories
        │       └── tracks
        │
        └── res
            ├── drawable
            ├── layout
            ├── mipmap
            ├── values
            └── xml
```

## API Integration

Sonora uses the Jamendo API to retrieve music data.

The API provides information such as:

- Track ID
- Track title
- Artist name
- Album name
- Track duration
- Artwork URL
- Audio streaming URL

Network requests are performed outside the Android main thread.

## Local Database

The application uses **Room Database** for local persistence.

Room is used to store data required by the application, including user-related and locally managed information.

Using Room provides a structured abstraction over SQLite and keeps database operations organized.

## User Sessions

Sonora uses **SharedPreferences** to maintain the active user session.

The stored session can include:

- Login state
- Current user ID
- Current user email

When the user logs out, the stored session information is cleared.

## Music Playback

Music playback is handled by the application's playback manager.

The player supports remote streaming and can also use locally downloaded files when available.

Typical playback flow:

1. The user selects a track.
2. Sonora loads the track information.
3. The audio source is prepared.
4. Playback starts.
5. The UI periodically updates the playback position.
6. The seek bar reflects the current position and total duration.

## Track Downloads

Sonora includes track downloading functionality.

The download system can:

- Download a selected track
- Detect whether the track has already been downloaded
- Retrieve the local file path
- Use the local file for playback
- Display the current download status in the UI

## Favorites

Logged-in users can maintain a personal favorites list.

Users can:

- Add tracks to favorites
- Remove tracks from favorites
- View saved tracks
- Open favorite tracks in the player

## Background Operations

Sonora avoids performing expensive work directly on the Android UI thread.

`ExecutorService` is used for operations such as:

- API requests
- Database queries
- Track loading
- Favorite operations
- User registration
- Downloads

UI updates are returned to the main thread after background work is complete.

## Image Loading

The project uses **Glide** for remote track artwork.

Glide provides:

- Efficient image loading
- Caching
- Placeholder support
- Error image support
- Automatic resource management

## Navigation

The application uses Android Activities and Intents.

A simplified navigation structure is:

```text
Main
├── Search
│   └── Player
│       └── Track Details
│
├── Favorites
│   └── Player
│       └── Track Details
│
├── Login
├── Register
├── Downloaded Tracks
└── Settings
```

Nested screens use normal Android back-stack behavior so users return to the screen they previously opened.

## Error Handling

The application includes basic handling for common problems such as:

- Empty search query
- Missing track information
- Track not found
- API request failure
- Network error
- Missing audio URL
- Database error
- Download error
- Missing user session

Whenever possible, the application displays a message or fallback value instead of crashing.

## Requirements

To build and run Sonora, you need:

- Android Studio
- Android SDK
- Java 11
- Gradle
- Android emulator or physical Android device
- Internet connection for Jamendo API functionality

## Installation

Clone the repository:

```bash
git clone https://github.com/ZastavnyeMishaUwU/Sonora.git
```

Open the Android project in Android Studio and wait for Gradle synchronization to complete.

## Build

### Windows

```powershell
.\gradlew assembleDebug
```

### macOS / Linux

```bash
./gradlew assembleDebug
```

A successful build should end with:

```text
BUILD SUCCESSFUL
```

## Run

To run the application:

1. Open the project in Android Studio.
2. Wait for Gradle synchronization.
3. Select an Android emulator or connected physical device.
4. Press **Run**.
5. Wait for the application to install and start.

## Permissions

Sonora requires internet access for API requests, streaming, artwork loading, and other online functionality.

Typical permissions include:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

## Project Goals

Sonora demonstrates practical Android development concepts including:

- Multi-screen Android applications
- REST API integration
- Authentication
- Session management
- Local data persistence
- Repository-based data access
- Music streaming
- Media playback
- File downloading
- Offline playback
- Background execution
- Material UI components
- Android navigation
- Error handling

## Possible Future Improvements

Possible future improvements include:

- Playlists
- Recently played tracks
- Playback queue
- Background playback service
- Media notification controls
- Artist pages
- Album pages
- Better offline mode
- Improved download management
- Dark mode
- Theme switching
- Shuffle
- Repeat
- Previous / next track controls
- Improved search filters
- Better caching
- More advanced profile settings

## Project Status

Sonora currently includes the core functionality required for a basic Android music application:

- Authentication
- Music search
- Playback
- Favorites
- Track details
- Downloads
- Settings
- Local persistence
- API integration

The project was created primarily for educational purposes and demonstrates the development of a complete Android application using Java.

## License

This project was created for educational purposes.

Third-party services, APIs, and libraries used by the application remain subject to their respective licenses and terms of use.
