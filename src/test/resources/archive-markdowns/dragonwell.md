---
ARCHIVE_FORMAT_VERSION: 1.0
ARCHIVE_TYPE: conversation_summary
CREATED_DATE: 2025-10-02
ORIGINAL_PLATFORM: Claude (Anthropic)

INSTRUCTIONS_FOR_AI: |
  ## Purpose
  This is an archived conversation that has been summarized and preserved for future reference.
  The conversation has been condensed to capture only the meaningful phases and outcomes.

  ## File Structure
  1. This metadata header (YAML front matter)
  2. Conversation summary sections (Initial Query, Key Insights, Follow-up Explorations, References)
  3. Artifacts section containing code/scripts created during conversation
  4. Attachments section (if any)
  5. Workarounds Used section (if applicable)
  6. Archive Metadata section

  ## Artifact Format
  Artifacts are valuable outputs created during the conversation (scripts, code, documents).
  Each artifact uses the structure shown in the Artifacts section below.

  ## Attachment Format
  All attachments are located at the bottom of this file in a wrapped format.
  Each attachment uses this exact structure:

  :::attachment filename="example.md"
  [content here]
  :::

  Important notes about attachments:
  - ALL attachments have been converted to markdown format, regardless of original type
  - Images are embedded as base64-encoded data URIs in markdown image syntax
  - The filename in the wrapper preserves the original filename for reference
  - Check for ⚠️ WARNING markers for summarized content

  ## Archive Completeness
  Check the ARCHIVE_COMPLETENESS field:
  - COMPLETE: All attachments/artifacts are fully preserved
  - PARTIAL: Some content was summarized or simplified
  - SUMMARIZED: Most/all content required summarization

  ## How to Process This Archive
  1. Read this entire file to understand the full context
  2. The summarized sections contain the core knowledge
  3. Artifacts contain the working scripts and code created
  4. When referenced by name, locate artifacts/attachments in their respective sections
  5. All content is directly readable - no extraction needed

  ## When User Uploads This File
  - Confirm you've loaded the archive and understood the topic
  - Be ready to continue from where it left off
  - Reference both summary and artifacts/attachments as needed
  - Treat archived information as established context
---

# Building Dragonwell JDK 21 on macOS with Compact Object Headers

**Date:** 2025-10-02
**Tags:** [java, dragonwell, jdk21, macos, compact-object-headers, build-from-source, jep-519]

---

## Initial Query

The user was investigating Java 21 builds that have the Compact Object Headers JEP (JEP 519) backported, beyond the known Alibaba Dragonwell implementation. They wanted to know if any other JDK distributions had this performance-enhancing feature available for Java 21.

After confirming that only Dragonwell and Amazon's internal builds had this backport, the conversation pivoted to building Dragonwell 21 from source on macOS to gain access to Compact Object Headers.

**Initial challenges identified:**
- Dragonwell only provides pre-built binaries for Linux (x86_64 and aarch64)
- No official macOS builds available
- User would need to compile from source

**Artifacts referenced:** build-osx.sh (final build script)

---

## Key Insights

**About Compact Object Headers availability:**
- Dragonwell JDK 21 has Compact Object Headers (JEP 519) backported
- Amazon has internal backports to JDK 21 and JDK 17, tested across hundreds of production services
- Compact Object Headers became experimental in Java 24 (JEP 450) and production-ready in Java 25 (JEP 519)
- Provides 10-30% memory reduction and CPU improvements for applications with many small objects
- No other public Java 21 distributions were found with this backport

**Building Dragonwell on macOS - Critical requirements:**
1. **Full Xcode installation required** - Command Line Tools alone are insufficient
2. **Metal Toolchain component** - Must be installed via `xcodebuild -downloadComponent MetalToolchain`
3. **Explicit tool paths needed** - Metal compiler locations must be specified using `xcrun --find metal` and `xcrun --find metallib`
4. **Compiler flag adjustments** - Modern Xcode/Clang is stricter; requires `--disable-warnings-as-errors` and `-Wno-vla-cxx-extension`
5. **Version string configuration** - Use `--with-version-opt=""` with `--with-version-pre=""` to avoid `-internal` suffix
6. **macOS bundle structure** - Built JDK needs manual `Contents/Home` wrapper for proper system integration

**Key build configuration:**
```bash
bash configure \
  --disable-warnings-as-errors \
  --with-extra-cxxflags="-Wno-vla-cxx-extension" \
  --with-vendor-name="Alibaba" \
  --with-vendor-version-string="Dragonwell" \
  --with-version-pre="" \
  --with-version-opt="" \
  METAL="$(xcrun --find metal)" \
  METALLIB="$(xcrun --find metallib)"
```

**Installation on macOS:**
- Standard location: `/Library/Java/JavaVirtualMachines/dragonwell-21.jdk`
- Proper bundle structure with `Contents/Home` required for `/usr/libexec/java_home` recognition
- Info.plist metadata with version information enables IDE and tool integration
- Bundle identifier format: `com.alibaba.dragonwell.21` (major version only)
- Can use with jenv for multi-JDK management

**Testing Compact Object Headers:**
```bash
java -XX:+UnlockExperimentalVMOptions -XX:+UseCompactObjectHeaders -version
```

**Artifacts referenced:** build-osx.sh

---

## Follow-up Explorations

**Metal compiler discovery issues:**
Explored multiple Metal-related build failures:
- Initial "metal command not found" error despite Xcode installation
- Subsequent "metallib not found" error after installing Metal Toolchain
- Root cause: Metal tools not in PATH during make subprocess execution
- Solution: Dynamic discovery using `xcrun --find` and explicit path passing to configure

**VLA (Variable Length Array) compiler errors:**
Newer Xcode versions (15+) treat VLA in C++ as strict errors:
- Error: `variable length arrays in C++ are a Clang extension [-Werror,-Wvla-cxx-extension]`
- Occurred in `PLATFORM_API_MacOSX_Ports.cpp` with non-const array sizes
- Solution: Combined `--disable-warnings-as-errors` with specific `-Wno-vla-cxx-extension` flag
- This allows build to proceed without modifying Dragonwell source code

**Version string customization:**
Investigated multiple approaches to remove `-internal` suffix:
- Initial attempt: `--with-version-build=7` - suggested but user found alternative
- Attempted: `--without-version-opt` - doesn't work in OpenJDK build system
- **Final solution**: `--with-version-opt=""` combined with `--with-version-pre=""`
- This produces clean version strings in jenv like `dragonwell64-21.0.8`

**macOS bundle structure requirements:**
Discovered build doesn't auto-generate macOS-specific bundle structure:
- Built JDK contains `/bin`, `/lib` directly (Unix-style layout)
- macOS expects `/Contents/Home/bin`, `/Contents/Home/lib` (bundle-style layout)
- Manual wrapper creation needed with proper Info.plist
- Info.plist version fields populated dynamically from built JDK
- Proper structure enables recognition by macOS tools and IDEs

**Script automation development:**
Iteratively developed comprehensive build script with 10 automated steps:
1. Environment setup with Xcode developer tools in PATH
2. Build folder cleanup
3. Dynamic Metal tool discovery using xcrun
4. Configuration with all necessary flags
5. Image building via make
6. Version extraction from built java binary
7. macOS bundle directory creation
8. Contents/Home folder structure generation
9. Info.plist creation with extracted version metadata
10. Archive packaging as distributable .tar.gz

**Artifacts referenced:** build-osx.sh (evolved through multiple iterations addressing each issue)

---

## References/Links

**Dragonwell Project:**
- GitHub Repository: https://github.com/dragonwell-project/dragonwell21
- Official Website: https://dragonwell-jdk.io/
- Release Downloads: https://github.com/dragonwell-project/dragonwell21/releases

**JEP Documentation:**
- JEP 450 (Experimental in Java 24): https://openjdk.org/jeps/450
- JEP 519 (Production in Java 25): https://openjdk.org/jeps/519
- OpenJDK Build Documentation: https://openjdk.org/groups/build/doc/building.html

**Related Articles:**
- InfoQ on Compact Object Headers: https://www.infoq.com/news/2025/06/java-25-compact-object-headers/
- HappyCoders Compact Headers Guide: https://www.happycoders.eu/java/compact-object-headers/

**Related Projects:**
- Amazon Corretto: https://aws.amazon.com/corretto/
- Alibaba Cloud Dragonwell Deployment: https://www.alibabacloud.com/help/en/ecs/use-cases/deploy-alibaba-dragonwell-jdk

---

## Artifacts

:::artifact type="script" language="bash" title="Dragonwell macOS Build Script" version="final"
#!/bin/bash
set -e

echo "======================================"
echo "Dragonwell macOS Build Script"
echo "======================================"

# Setup environment
export DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer
export PATH="$DEVELOPER_DIR/Toolchains/XcodeDefault.xctoolchain/usr/bin:$PATH"
export PATH="$DEVELOPER_DIR/usr/bin:$PATH"
export SDKROOT=$(xcrun --show-sdk-path)

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# 1. Clean up the build folder
echo ""
echo "Step 1: Cleaning up build folder..."
if [ -d "build" ]; then
    rm -rf build/
    echo "✓ Build folder cleaned"
else
    echo "✓ No build folder to clean"
fi

# 2. Find Metal tools
echo ""
echo "Step 2: Locating Metal tools..."
METAL_PATH=$(xcrun --find metal)
METALLIB_PATH=$(xcrun --find metallib)
echo "✓ Metal found at: $METAL_PATH"
echo "✓ Metallib found at: $METALLIB_PATH"

# 3. Configure with proper flags
echo ""
echo "Step 3: Configuring build..."
bash configure \
  --disable-warnings-as-errors \
  --with-extra-cxxflags="-Wno-vla-cxx-extension" \
  --with-vendor-name="Alibaba" \
  --with-vendor-version-string="Dragonwell" \
  --with-version-pre="" \
  --with-version-opt="" \
  METAL="$METAL_PATH" \
  METALLIB="$METALLIB_PATH"

echo "✓ Configuration complete"

# 4. Build images
echo ""
echo "Step 4: Building images..."
make images
echo "✓ Build complete"

# 5. Extract version information
echo ""
echo "Step 5: Extracting version information..."
VERSION_OUTPUT=$(build/macosx-aarch64-server-release/jdk/bin/java -version 2>&1)
echo "$VERSION_OUTPUT"

# Extract major version (e.g., 21)
VERSION_MAJOR=$(echo "$VERSION_OUTPUT" | grep -oE 'version "[0-9]+' | grep -oE '[0-9]+' | head -1)

# Extract full version (e.g., 21.0.8)
VERSION_FULL=$(echo "$VERSION_OUTPUT" | grep -oE 'version "[0-9]+\.[0-9]+\.[0-9]+' | grep -oE '[0-9]+\.[0-9]+\.[0-9]+' | head -1)

echo "✓ Major version: $VERSION_MAJOR"
echo "✓ Full version: $VERSION_FULL"

# 6. Create bundle directory
echo ""
echo "Step 6: Creating bundle structure..."
BUNDLE_NAME="dragonwell-${VERSION_MAJOR}.jdk"
BUNDLE_PATH="build/$BUNDLE_NAME"

rm -rf "$BUNDLE_PATH"
mkdir -p "$BUNDLE_PATH"
echo "✓ Created $BUNDLE_NAME"

# 7. Create Contents and Home folders
echo ""
echo "Step 7: Creating Contents and Home folders..."
mkdir -p "$BUNDLE_PATH/Contents/Home"
mkdir -p "$BUNDLE_PATH/Contents/MacOS"
echo "✓ Folder structure created"

# 8. Create Info.plist
echo ""
echo "Step 8: Creating Info.plist..."
cat > "$BUNDLE_PATH/Contents/Info.plist" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleDevelopmentRegion</key>
    <string>English</string>
    <key>CFBundleExecutable</key>
    <string>libjli.dylib</string>
    <key>CFBundleGetInfoString</key>
    <string>Alibaba Dragonwell ${VERSION_FULL}</string>
    <key>CFBundleIdentifier</key>
    <string>com.alibaba.dragonwell.${VERSION_MAJOR}</string>
    <key>CFBundleInfoDictionaryVersion</key>
    <string>${VERSION_FULL}</string>
    <key>CFBundleName</key>
    <string>Dragonwell ${VERSION_MAJOR}</string>
    <key>CFBundlePackageType</key>
    <string>BNDL</string>
    <key>CFBundleShortVersionString</key>
    <string>${VERSION_FULL}</string>
    <key>CFBundleSignature</key>
    <string>????</string>
    <key>CFBundleVersion</key>
    <string>${VERSION_FULL}</string>
    <key>JavaVM</key>
    <dict>
        <key>JVMCapabilities</key>
        <array>
            <string>CommandLine</string>
        </array>
        <key>JVMMinimumFrameworkVersion</key>
        <string>13.2.9</string>
        <key>JVMMinimumSystemVersion</key>
        <string>10.7.0</string>
        <key>JVMPlatformVersion</key>
        <string>${VERSION_FULL}</string>
        <key>JVMVendor</key>
        <string>Alibaba</string>
        <key>JVMVersion</key>
        <string>${VERSION_FULL}</string>
    </dict>
</dict>
</plist>
EOF
echo "✓ Info.plist created with version ${VERSION_FULL}"

# 9. Copy JDK files to Home
echo ""
echo "Step 9: Copying JDK files..."
cp -r build/macosx-aarch64-server-release/jdk/* "$BUNDLE_PATH/Contents/Home/"
echo "✓ JDK files copied to Contents/Home"

# 10. Create zip archive
echo ""
echo "Step 10: Creating zip archive..."
cd build
ZIP_NAME="dragonwell-${VERSION_FULL}-macos-aarch64.tar.gz"
tar -czf "$ZIP_NAME" "$BUNDLE_NAME"
echo "✓ Archive created: build/$ZIP_NAME"

cd ..

# Summary
echo ""
echo "======================================"
echo "Build Summary"
echo "======================================"
echo "Version: ${VERSION_FULL}"
echo "Bundle: build/$BUNDLE_NAME"
echo "Archive: build/$ZIP_NAME"
echo ""
echo "To install:"
echo "  sudo cp -r build/$BUNDLE_NAME /Library/Java/JavaVirtualMachines/"
echo ""
echo "Or extract and use the archive:"
echo "  tar -xzf build/$ZIP_NAME -C /Library/Java/JavaVirtualMachines/"
echo ""
echo "✅ Build completed successfully!"
:::

---

## Workarounds Used

None - All artifacts were successfully preserved in full format.

---

## Archive Metadata

**Original conversation date:** 2025-10-02
**Archive created:** 2025-10-02
**Archive version:** 1.0
**Archive completeness:** COMPLETE
**Total attachments:** 0
**Total artifacts:** 1
**Attachments with workarounds:** 0
**Estimated reading time:** 15 minutes

---

_End of archived conversation_
