
<p align="center">
![image](https://github.com/user-attachments/assets/87d03783-c26d-4b4b-baf6-12f55e996ac3)
</p>

# MANI - METONIC ALGORITHMIC NIGHTCYCLE INTERFACE (Android)

An Android calendar application displaying a lunisolar calendar based on reconstructed Germanic traditions, calculated using the Metonic cycle and astronomical events.

This application provides a visual calendar interface showing traditional lunar months alongside Gregorian dates, moon phases, and significant astronomical markers like solstices and equinoxes. Built on the foundation of Simple Calendar Pro but extensively modified for lunisolar functionality.

## Features

### Core Calendar Functionality
- **Lunisolar Calendar System**: Displays lunar months aligned with astronomical events (Winter Solstice, New/Full Moons)
- **42-Day Consistent Grid**: Always shows 6 weeks × 7 days with previous/next month previews
- **Astronomical Integration**: Calculates and displays lunar dates corresponding to Gregorian dates
- **Moon Phase Display**: Shows accurate moon phases for each day with visual icons
- **Eld Year Calculation**: Displays Germanic epoch-based years alongside traditional dates

### Germanic Holidays & Traditions
- **Four Major Holidays**: Yule, Sumarmal, Midsummer, Winter Nights
- **Custom Holiday Names**: 3-day celebrations with meaningful day designations:
  - **Yule**
  - **Sumarmal**
  - **Midsummer**
  - **Winter Nights**
- **Seasonal Color Coding**: 
  - Yule: Red (winter)
  - Sumarmal: Green (spring)
  - Midsummer: Orange (summer)
  - Winter Nights: Blue (fall)

### Modern Calendar Integration
- **Event System**: Full integration with Android calendar events
- **Visual Event Indicators**: Dots showing event count (●, ●●, ●●●) with contrasting colors
- **Clickable Days**: Tap days with events to view detailed information
- **Multi-day Event Support**: Properly handles events spanning multiple days

### User Interface
- **Swipe Navigation**: Swipe left/right to change months
- **Navigation Drawer**: Hamburger menu for future feature expansion
- **Responsive Design**: Adapts to different screen sizes and orientations
- **Theme Integration**: Follows system dark/light theme preferences
- **Accessibility**: Proper contrast ratios and navigation support

### Astronomical Events
- **Solstices & Equinoxes**: Visual indicators for seasonal transitions
- **Moon Phase Tracking**: Accurate lunar phase calculations
- **Metonic Cycle Integration**: 19-year cycle calculations for lunar calendar accuracy

## Building

### Prerequisites
- **Android Studio**: Latest stable version (Arctic Fox or newer recommended)
- **Android SDK**: API level 24+ (Android 7.0) minimum, target API 34+
- **Kotlin**: Integrated with Android Studio
- **JDK**: 17 or newer

### Setup
1. **Clone the repository**:
   ```bash
   git clone https://github.com/starkadr9/MANI-App.git
   cd MANI-App
   ```

2. **Open in Android Studio**:
   - Launch Android Studio
   - Select "Open an existing project"
   - Navigate to the cloned directory
   - Wait for Gradle sync to complete

3. **Build the project**:
   ```bash
   ./gradlew assembleDebug
   ```

### Build Variants
- **Core**: Basic functionality without proprietary features
- **F-Droid**: Open-source compatible build
- **Prepaid**: Version with premium features

### APK Generation
```bash
# Debug APK
./gradlew assembleDebug

# Release APK (requires signing configuration)
./gradlew assembleRelease
```

The generated APK will be located in `app/build/outputs/apk/`

## Installation

### From Source
1. Build the APK using the instructions above
2. Enable "Install from Unknown Sources" in Android settings
3. Install the APK file on your device

### Requirements
- **Android 7.0** (API level 24) or higher
- **Storage**: ~20MB for app installation
- **Permissions**: 
  - Calendar access (for event integration)
  - Storage access (for backup/restore)

## Configuration

The application includes extensive customization options:

### Lunisolar Settings
- **Epoch Configuration**: Adjustable Germanic epoch base year
- **Month Names**: Customizable lunar month names
- **Eld Year Display**: Toggle between traditional and Eld year formats
- **Holiday Names**: Configurable celebration day designations

### Calendar Display
- **Event Integration**: Control event visibility and indicators
- **Color Schemes**: Seasonal holiday color customization
- **Grid Options**: Week number display, highlighting options
- **Navigation**: Arrow buttons alongside swipe gestures

## Technical Details

### Architecture
- **Base**: Modified Simple Calendar Pro codebase
- **Language**: Kotlin with Android SDK
- **UI Framework**: Android Views with Material Design components
- **Calendar Logic**: Custom lunisolar implementation with Metonic cycle calculations
- **Database**: Room database for local event storage

### Astronomical Calculations
Based on algorithms from "Astronomical Algorithms" by Jean Meeus:
- **Lunar Phase Calculation**: Accurate moon phase determination
- **Solstice/Equinox Timing**: Precise seasonal transition dates
- **Metonic Cycle**: 19-year lunar-solar alignment calculations
- **Germanic Calendar Reconstruction**: Historical astronomical event alignment

### Performance Optimizations
- **Event Caching**: Efficient loading for 42-day grid coverage
- **Lazy Loading**: On-demand calculation of complex astronomical data
- **Memory Management**: Optimized for Android lifecycle and memory constraints

## License

This project is licensed under the GNU General Public License v3.0 (GPL-3.0). See the LICENSE file for details.

The original Simple Calendar Pro codebase is also licensed under GPL-3.0, allowing for this derivative work.

## Acknowledgments

- **Simple Mobile Tools**: Original Simple Calendar Pro codebase foundation
- **Jean Meeus**: "Astronomical Algorithms" for core calculations
