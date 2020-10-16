#!/bin/bash
JIPIPE_VERSION=2020.10
JDK_DIR=jdk-11.0.8+10

rm -rf AppDir
mkdir AppDir

# Create partial JDK 
# Use ./$JDK_DIR/bin/jdeps --list-deps AppDir/jipipe-installer-linux-2020.10.jar to find the dependencies!
# Here: java.base java.datatransfer java.desktop java.logging java.naming java.prefs java.scripting java.sql java.xml jdk.unsupported
eval "./$JDK_DIR/bin/jlink --no-header-files --no-man-pages --compress=2 --strip-debug --add-modules java.base,java.datatransfer,java.desktop,java.logging,java.naming,java.prefs,java.scripting,java.sql,java.xml,jdk.unsupported --output AppDir/usr"

# Copy JIPipe components
mkdir -p AppDir/usr/share/jipipe-installer/jipipe-bin
for component in jipipe-core jipipe-clij jipipe-multiparameters jipipe-filesystem jipipe-ij jipipe-ij-algorithms jipipe-ij-multi-template-matching jipipe-python jipipe-plots jipipe-tables jipipe-annotation jipipe-utils jipipe-strings jipipe-launcher ij-updater-cli; do
    cp -v ../../$component/target/$component-$JIPIPE_VERSION.jar AppDir/usr/share/jipipe-installer/jipipe-bin
done

# Copy installer 
cp -v ../../jipipe-installer-linux/target/jipipe-installer-linux-$JIPIPE_VERSION.jar AppDir/usr/share/jipipe-installer/jipipe-installer-linux.jar

# Copy AppImage requirements
cp -v jipipe-installer.png AppDir/
cp -v jipipe-installer.desktop AppDir/
cp -v AppRun AppDir/
cp -v jipipe-installer AppDir/usr/bin/
chmod +x AppDir/usr/bin/jipipe-installer

# Build AppImage 
./appimagetool-x86_64.AppImage AppDir
