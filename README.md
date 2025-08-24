# Todo App - Onboarding Flow

This Android app demonstrates a comprehensive onboarding flow built with Kotlin and XML layouts.

## Features

### 1. Onboarding Slides (ViewPager2 + TabLayout)
- **Dynamic Content**: Loads 4 onboarding slides from `res/raw/onboarding.json`
- **Swipe Navigation**: Users can swipe through slides or use tab indicators
- **Get Started Button**: Appears only on the last slide to proceed to next step

### 2. Name Entry Screen
- **User Input**: EditText for entering user's name
- **Validation**: Ensures name is not empty before proceeding
- **Data Persistence**: Saves name using DataStore preferences

### 3. Permissions Screen
- **Notification Permission**: Requests POST_NOTIFICATIONS permission (Android 13+)
- **Graceful Handling**: Works on older Android versions where permission is auto-granted
- **Skip Option**: Users can skip with a warning message

## Technical Implementation

### Architecture
- **Activities**: Clean separation with dedicated activities for each step
- **DataStore**: Modern preferences storage using DataStore
- **ViewBinding**: Type-safe view binding for all layouts
- **Coroutines**: Asynchronous operations for data persistence

### Key Components

#### Data Classes
- `OnboardingItem`: Represents each onboarding slide with title, description, and image

#### Utilities
- `JsonParser`: Parses onboarding data from JSON file
- `PermissionUtils`: Handles notification permission requests
- `UserPreferences`: DataStore wrapper for user data

#### Activities
- `LauncherActivity`: Entry point that checks onboarding status
- `OnboardingActivity`: ViewPager2 with 4 slides
- `NameEntryActivity`: Name input and validation
- `PermissionsActivity`: Permission request handling
- `MainActivity`: Final destination with welcome message

### Dependencies
- **ViewPager2**: For swipeable onboarding slides
- **TabLayout**: For slide indicators
- **DataStore**: For preferences storage
- **Gson**: For JSON parsing
- **Material Design**: For modern UI components

## File Structure

```
app/src/main/
├── java/com/projects/todos/
│   ├── data/
│   │   ├── OnboardingItem.kt
│   │   └── UserPreferences.kt
│   ├── adapter/
│   │   └── OnboardingAdapter.kt
│   ├── utils/
│   │   ├── JsonParser.kt
│   │   └── PermissionUtils.kt
│   ├── ui/
│   │   ├── LauncherActivity.kt
│   │   ├── OnboardingActivity.kt
│   │   ├── NameEntryActivity.kt
│   │   └── PermissionsActivity.kt
│   └── MainActivity.kt
├── res/
│   ├── raw/
│   │   └── onboarding.json
│   ├── layout/
│   │   ├── activity_onboarding.xml
│   │   ├── activity_name_entry.xml
│   │   ├── activity_permissions.xml
│   │   ├── item_onboarding_slide.xml
│   │   └── activity_main.xml
│   ├── drawable/
│   │   ├── tab_selector.xml
│   │   ├── ic_onboarding_tasks.xml
│   │   ├── ic_onboarding_plan.xml
│   │   ├── ic_onboarding_stats.xml
│   │   └── ic_onboarding_notifications.xml
│   └── values/
│       └── strings.xml
└── AndroidManifest.xml
```

## Usage

1. **First Launch**: App starts with onboarding slides
2. **Name Entry**: User enters their name
3. **Permissions**: User grants or skips notification permission
4. **Main App**: Welcome screen with personalized message
5. **Subsequent Launches**: Direct to main app (onboarding completed)

## Best Practices Implemented

- **Modular Design**: Each step is a separate activity for maintainability
- **Error Handling**: Graceful fallbacks for JSON parsing and permission requests
- **User Experience**: Smooth transitions and clear navigation
- **Data Persistence**: Reliable storage using DataStore
- **Permission Safety**: Proper handling for different Android versions
- **Clean Code**: Separation of concerns with utility classes

## Customization

To modify the onboarding content:
1. Edit `res/raw/onboarding.json` to change slide content
2. Add corresponding drawable resources for images
3. Update strings in `res/values/strings.xml`
4. Modify layouts in `res/layout/` for UI changes
