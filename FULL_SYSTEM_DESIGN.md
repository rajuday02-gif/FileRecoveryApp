# FileRecoveryApp - Complete System Architecture

## 🎯 System Overview

This is an **institutional-grade Android file recovery application** using byte-level carving and file signature detection.

## 🏗️ Architecture

### Layer 1: Android UI (Kotlin)
- **MainActivity** - Main recovery interface
- **RecoveryViewModel** - State management
- **StorageAccessManager** - Storage scanning
- **RecoveryLogger** - Operation logging

### Layer 2: Recovery Engine (C++)
- **RecoveryCore** - Main recovery algorithm
- **RecoveryEngine** - JNI interface
- **FileSignatureDatabase** - 14+ file format signatures

### Layer 3: Storage Access
- Internal storage scanning
- External storage (SDCard)
- Cache directory recovery
- Optional: Root access for partition scanning

## 🔧 Core Features

### ✅ Implemented
- [x] Byte-level file carving
- [x] Multiple file format support
- [x] Real-time progress tracking
- [x] File confidence scoring (0-100%)
- [x] Recovery logging system
- [x] File preview (images, text, hex)
- [x] Log viewer
- [x] 3-Tier monetization
- [x] Root detection

### 📋 File Formats Supported

| Format | Header | Footer | Max Size |
|--------|--------|--------|----------|
| JPEG | FF D8 FF | FF D9 | 10MB |
| PNG | 89 50 4E 47... | - | 50MB |
| GIF | 47 49 46 38 | 00 3B | 5MB |
| PDF | 25 50 44 46 | - | 100MB |
| MP4 | ftyp box | - | 500MB |
| ZIP | 50 4B 03 04 | - | 500MB |
| BMP | 42 4D | - | 100MB |
| WEBP | RIFF | - | 50MB |
| DOC | D0 CF 11 E0... | - | 50MB |
| DOCX | 50 4B 03 04 | - | 50MB |
| TIFF | 49 49 2A 00 | - | 100MB |
| WAV | RIFF | - | 500MB |
| SQLite | SQLite format | - | 1GB |

## 📊 Tier System

### Free Tier 🔓
- 500MB scan limit
- JPEG + PNG only
- 5 recoveries/day
- Basic logging

### Pro Tier 💎
- 10GB scan limit
- All formats
- Unlimited recoveries
- Advanced filters
- Priority support

### Premium Tier 👑
- Unlimited scan
- Custom signatures
- Cloud backup
- Advanced analytics
- 24/7 support

## 🔄 Scan Process

1. **User selects storage dump** (.img file)
2. **Engine initializes** signatures
3. **Chunk-based scanning** (5MB chunks)
4. **Signature matching** for each format
5. **Footer detection** (if applicable)
6. **Confidence scoring** based on file integrity
7. **Results aggregation** and logging
8. **File preview** and recovery

## 🧠 Technical Highlights

### C++ Native Layer
- Memory-efficient chunk processing
- Multi-threaded scanning capability
- Atomic operations for thread safety
- JNI bridge for Java integration

### Kotlin/Android
- MVVM architecture
- Coroutines for async operations
- StateFlow for reactive UI updates
- Material Design 3 UI

### Database
- Room for recovered files history
- SharedPreferences for user settings
- File logging to external storage

## 🚀 Build & Run

```bash
# Build
./gradlew build

# Run on device
./gradlew installDebug

# Test
./gradlew test
```

## 📱 Device Requirements

- Android 7.0+ (API 24)
- 2GB RAM minimum
- 100MB free storage
- Developer Mode enabled (for full features)

## 🔒 Security & Privacy

- No cloud upload of recovered files (local only)
- Encrypted local storage of logs
- Root access detection and warning
- HTTPS only for any network requests
- Regular permission audits

## 🎓 Development Roadmap

### Phase 1: Core (✅ Complete)
- [x] Byte carving engine
- [x] File signature detection
- [x] Basic UI
- [x] Logging system

### Phase 2: Enhancement (🔄 In Progress)
- [ ] Fragment reassembly
- [ ] EXT4 filesystem parser
- [ ] AI confidence prediction
- [ ] Performance optimization

### Phase 3: Advanced (📋 Planned)
- [ ] Cloud integration
- [ ] Custom signature editor
- [ ] Batch recovery scheduling
- [ ] Video preview
- [ ] Network recovery (remote devices)

## 📝 License

Private - All Rights Reserved

## 👨‍💻 Author

rajuday02-gif (GitHub)
