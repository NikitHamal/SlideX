# SlideS AI - AI-Powered Slide Creation App

A modern Android application that creates beautiful presentations using Google Gemini AI. Transform simple prompts into professional slides with Material 3 design.

## ✨ Features

### 🎨 **Modern UI/UX**
- Complete Material 3 design system implementation
- Dark/Light theme support with system default option
- Beautiful animations and transitions
- Intuitive slide management with stacks

### 🔑 **Advanced API Management**
- Secure, encrypted storage of multiple Gemini API keys
- Beautiful bottom sheet interface for key management
- Visibility toggle and easy deletion
- Automatic key rotation and fallback

### 🎯 **Smart Slide Creation**
- AI-powered slide generation using Google Gemini
- JSON-based slide structure for maximum flexibility
- Advanced element customization (text, images, shapes, tables)
- Real-time editing and preview

### 📱 **Enhanced Functionality**
- Slide stacks for organizing presentations
- Multiple export formats (PNG, JPG, PDF)
- Import/Export JSON slide definitions
- Local data storage with SharedPreferences

## 🚀 Getting Started

### Prerequisites
- Android Studio Arctic Fox or newer
- Android SDK API 21+ (Android 5.0+)
- Java 17 or newer
- Gemini API key from [Google AI Studio](https://aistudio.google.com/app/apikey)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-username/slides-ai.git
   cd slides-ai
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Select "Open an existing project"
   - Navigate to the cloned directory

3. **Build the project**
   ```bash
   ./gradlew assembleRelease
   ```

4. **Install on device**
   ```bash
   ./gradlew installRelease
   ```

### Setup API Keys

1. Get your Gemini API key from [Google AI Studio](https://aistudio.google.com/app/apikey)
2. Open the app and go to Settings
3. Tap on "API Keys" section
4. Add your API key with a memorable label
5. Start creating slides!

## 🏗️ Architecture

### Project Structure
```
app/src/main/java/com/slides/ai/
├── MainActivity.java           # Slide stacks management
├── SlideActivity.java         # Slide editing interface  
├── SettingsActivity.java     # App settings and preferences
├── ApiKeyManager.java        # Secure API key management
├── ThemeManager.java         # Theme switching logic
├── NetworkManager.java       # API communication
├── SlideRenderer.java        # Canvas-based slide rendering
├── CustomizationManager.java # Element customization
└── SlideApplication.java     # Application class
```

### Key Components

- **MainActivity**: Grid view of slide stacks with empty states
- **SlideActivity**: Canvas-based slide editor with Material 3 UI
- **ApiKeyManager**: Encrypted storage and management of API keys
- **ThemeManager**: System-aware theme switching
- **SlideRenderer**: High-performance canvas rendering with touch handling

## 🎨 Material 3 Implementation

The app fully implements Google's Material 3 design system:

- **Dynamic Colors**: Supports Material You color extraction
- **Typography**: Uses Material 3 type scale
- **Components**: Latest Material 3 components (FABs, Cards, Sheets)
- **Themes**: Complete light/dark theme support
- **Motion**: Smooth animations and transitions

## 🔧 Build Configuration

### GitHub Actions CI/CD

The project includes a complete CI/CD pipeline:

```yaml
- Automatic building on push
- APK signing with debug keystore
- Artifact upload with commit hash naming
- Support for multiple build configurations
```

### Local Development

```bash
# Debug build
./gradlew assembleDebug

# Release build (signed)
./gradlew assembleRelease

# Run tests
./gradlew test

# Check code quality
./gradlew lint
```

## 📋 Planned Features

### 🚀 **10 Enhancement Ideas for Existing Features**

1. **Enhanced Slide Customization**: Better UI controls with real-time preview
2. **Improved Element Selection**: Multi-element selection and bulk operations
3. **Advanced Image Loading**: Progress indicators and retry mechanisms
4. **Extended Export Options**: PPTX, SVG formats and batch export
5. **Better JSON Handling**: Validation and auto-completion
6. **API Key Analytics**: Usage tracking and rate limiting
7. **Custom Themes**: User-defined color palettes
8. **Advanced Elements**: More shapes, charts, and templates
9. **Performance Optimization**: Viewport culling and smooth animations
10. **Cloud Integration**: Backup and sync across devices

### 🆕 **10 New Feature Ideas**

1. **AI Template Suggestions**: Smart template recommendations
2. **Voice-to-Slide**: Speech recognition for slide creation
3. **Content Recommendations**: AI-powered content improvements
4. **Collaboration**: Real-time sharing and editing
5. **Slide Animations**: Entrance/exit animations and transitions
6. **QR Code Integration**: Auto-generated QR codes and links
7. **Presentation Mode**: Full-screen with speaker notes
8. **Smart Image Search**: Integration with free image APIs
9. **Template Library**: Pre-built professional templates
10. **Analytics Dashboard**: Presentation performance insights

## 🔐 Security

- **API Key Encryption**: AES-256 encryption for stored API keys
- **Local Storage**: All data stored locally on device
- **No Cloud Dependencies**: Complete offline operation after API calls
- **Open Source**: Transparent security through open development

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👨‍💻 Developer

**Nikit Hamal**
- Email: [your-email@example.com]
- GitHub: [@your-username]

## 🙏 Acknowledgments

- Google for the Gemini AI API
- Material Design team for the design system
- Android development community for inspiration and guidance

---

## 🔧 Build Status

![Android CI](https://github.com/your-username/slides-ai/workflows/Android%20CI/badge.svg)

**Note**: This application requires an Android development environment with Android SDK installed for building. The GitHub Actions workflow automatically handles building and signing in the CI environment.