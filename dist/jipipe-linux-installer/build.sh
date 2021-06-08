#!/bin/bash
JIPIPE_VERSION=2021.6
APPIMAGE_TOOL_URL="https://github.com/AppImage/AppImageKit/releases/download/12/appimagetool-x86_64.AppImage"

rm -rf AppDir
mkdir AppDir

# Copy JIPipe components
mkdir -p AppDir/usr/share/jipipe-installer/jipipe-bin
for component in jipipe-core jipipe-clij jipipe-multiparameters jipipe-filesystem jipipe-ij jipipe-ij-algorithms jipipe-ij-multi-template-matching jipipe-python jipipe-plots jipipe-tables jipipe-annotation jipipe-utils jipipe-strings jipipe-forms jipipe-r jipipe-cellpose jipipe-deep-learning jipipe-launcher ij-updater-cli; do
    cp -v ../../$component/target/$component-$JIPIPE_VERSION.jar AppDir/usr/share/jipipe-installer/jipipe-bin
done

# Copy installer 
cp -v ../../jipipe-installer-linux/target/jipipe-installer-linux-$JIPIPE_VERSION.jar AppDir/usr/share/jipipe-installer/jipipe-installer-linux.jar

# Copy AppImage requirements
cp -v jipipe-installer.png AppDir/
cp -v jipipe-installer.desktop AppDir/
cp -v AppRun AppDir/
mkdir -p AppDir/usr/bin/
cp -v jipipe-installer AppDir/usr/bin/
chmod +x AppDir/usr/bin/jipipe-installer

# Build AppImage 
if [[ ! -e "./appimagetool-x86_64.AppImage" ]]; then
    wget $APPIMAGE_TOOL_URL
fi
chmod +x ./appimagetool-x86_64.AppImage
ARCH=x86_64 ./appimagetool-x86_64.AppImage AppDir
