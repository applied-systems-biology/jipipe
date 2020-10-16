#!/bin/bash
JIPIPE_VERSION=2020.10
JDK_DIR=jdk-11.0.8+10

rm -r AppDir
mkdir AppDir

# Copy JIPipe components
mkdir AppDir/jipipe-bin
for component in jipipe-core jipipe-clij jipipe-multiparameters jipipe-filesystem jipipe-ij jipipe-ij-algorithms jipipe-ij-multi-template-matching jipipe-python jipipe-plots jipipe-tables jipipe-annotation jipipe-utils jipipe-strings jipipe-launcher ij-updater-cli; do
    cp -v ../../$component/target/$component-$JIPIPE_VERSION.jar AppDir/jipipe-bin
done

# Copy installer 
cp ../../jipipe-installer-linux/target/jipipe-installer-linux-$JIPIPE_VERSION.jar AppDir

