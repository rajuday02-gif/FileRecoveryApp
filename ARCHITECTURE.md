# FileRecoveryApp Architecture

## Overview

The app is built using a **3-layer architecture**:

### Layer 1: UI Layer (Kotlin)
- MainActivity.kt - Main UI
- BillingManager.kt - In-app purchases
- DeveloperSettings check

### Layer 2: JNI Bridge
- CarverEngine.kt - Java interface to native code
- JNI native-lib.cpp - Bridge between Java and C++

### Layer 3: Native Layer (C++)
- carver.cpp - Main byte carving logic
- signatures.cpp - File signature database
- Memory-efficient chunk processing

## Byte Carving Algorithm

```
1. Read raw storage dump in 10MB chunks
2. For each chunk:
   a. Search for known file signatures (JPEG, PNG, PDF, etc.)
   b. When signature found:
      - Look for footer signature (if exists)
      - Extract byte range between header and footer
      - Write to recovered_files directory
   c. Continue scanning for more files
3. Return list of recovered files
```

## Supported File Types

| Format | Header | Footer | Max Size |
|--------|--------|--------|----------|
| JPEG | FFD8FF | FFD9 | 10MB |
| PNG | 89504E47... | - | 50MB |
| PDF | 25504446 | - | 100MB |
| MP4 | 00000020... | - | 500MB |
| GIF | 47494638 | 003B | 5MB |
| BMP | 424D | - | 100MB |
| ZIP | 504B0304 | - | 500MB |

## Tier Limits

### Free Tier
- Max partition size: 500MB
- File types: JPEG, PNG only
- Recoveries/day: 5
- Features: Basic recovery

### Pro Tier ($99/mo or $499/yr)
- Max partition size: 10GB
- File types: All supported
- Recoveries/day: Unlimited
- Features: Advanced filters, priority support

### Premium Tier ($199/mo or $1299/yr)
- Max partition size: Unlimited
- File types: All supported
- Recoveries/day: Unlimited
- Features: Custom signatures, cloud backup, analytics

## Security Considerations

1. **Developer Mode Check**: App only functions when Developer Options are enabled
2. **Permissions**: Requires explicit user approval for storage access
3. **No Root Required**: Uses emulated storage access instead
4. **Data Privacy**: Recovered files stored locally only
5. **Billing Security**: Google Play Billing API with purchase verification

## Performance

- **Chunk Processing**: 10MB chunks to avoid memory overflow
- **Multi-threading**: Coroutines for non-blocking UI
- **Native Code**: C++ for byte-level operations
- **Memory Efficient**: Streaming file reading instead of loading entire dump

## File Structure

```
FileRecoveryApp/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── cpp/
│   │   │   │   ├── native-lib.cpp
│   │   │   │   ├── carver.cpp
│   │   │   │   ├── signatures.cpp
│   │   │   │   └── include/
│   │   │   ├── java/com/recovery/filecarver/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── CarverEngine.kt
│   │   │   │   └── BillingManager.kt
│   │   │   ├── res/
│   │   │   └── AndroidManifest.xml
│   │   └��─ build.gradle.kts
│   └── CMakeLists.txt
├── README.md
└── ARCHITECTURE.md
```

## Data Flow

```
User selects storage dump
        ↓
MainActivity calls CarverEngine.startCarving()
        ↓
JNI bridge calls native-lib.cpp
        ↓
Carver.cpp reads dump in chunks
        ↓
Searches for file signatures
        ↓
Extracts and writes recovered files
        ↓
Returns recovered file list
        ↓
UI displays results
```

## Future Enhancements

1. Custom signature editor
2. Cloud backup integration
3. Video preview
4. Batch recovery scheduling
5. Machine learning file type detection
6. Fragment reassembly
7. Encrypted partition support
