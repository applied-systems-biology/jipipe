#!/usr/bin/env python3

import json
import os

with open("dist-info.json", "r") as f:
    json_data = json.load(f)

jipipe_version = json_data["jipipe-version"]
# opencv_download = "https://github.com/bytedeco/javacv/releases/download/1.5.8/javacv-platform-1.5.8-bin.zip"
# opencv_dir = "javacv-platform-1.5.8-bin"

# Generate ZIP shell script
print("Generating ZIP script")
with open("zip/build.sh", "w") as f:
    def wl(text="", tab=0):
        f.write(tab * "\t" + text + "\n")
    wl("#!/bin/bash")
    wl()
    wl('JIPIPE_VERSION="Development"')
    wl("PROJECT_DIR=../..")
    wl()
    wl('pushd $PROJECT_DIR || exit')
    wl('JIPIPE_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout | grep -Po "\\d\\.\\d+\\.\\d+")')
    # wl('OPENCV_DOWNLOAD="' + opencv_download + '"')
    # wl('OPENCV_DIR=' + opencv_dir)
    wl('popd || exit')
    wl()

    # Download dependencies
    for (name, url) in json_data["dependencies"].items():
        wl('if [ ! -e "./dependencies/' + name + '" ]; then')
        rel_path = "./dependencies/" + name 
        re_dir = os.path.dirname(rel_path)
        wl("mkdir -p " + '"' + re_dir + '"', tab=1)
        wl("wget -O ./dependencies/" + name + " " + url, tab=1)
        wl("fi")

    # Download OpenCV
    # wl('if [ ! -e "$OPENCV_DIR" ]; then')
    # wl('wget -O opencv.zip "$OPENCV_DOWNLOAD"')
    # wl('unzip opencv.zip')
    # wl("rm opencv.zip")
    # wl("rm -rv $OPENCV_DIR/samples")
    # wl("rm -rv $OPENCV_DIR/*.md")
    # wl("rm -rv $OPENCV_DIR/*-android-*")
    # wl("rm -rv $OPENCV_DIR/*-ios-*")
    # wl("rm -rv $OPENCV_DIR/*-x86.jar")
    # wl("rm -rv $OPENCV_DIR/*-arm.jar")
    # wl("rm -rv $OPENCV_DIR/*-arm64.jar")
    # wl("rm -rv $OPENCV_DIR/*-armhf.jar")
    # wl("rm -rv $OPENCV_DIR/*-ppc64le.jar")
    # wl("fi")

    # Cleaning up old package
    wl()
    wl("rm -r package")
    wl("mkdir package")

    # Copy component jars
    wl("for component in " + " ".join(json_data["jipipe-modules"]) + "; do")
    wl("cp -v ../../$component/target/$component-$JIPIPE_VERSION-SNAPSHOT.jar package/$component-$JIPIPE_VERSION.jar", tab=1)
    wl("cp -v ../../$component/target/$component-$JIPIPE_VERSION.jar package/$component-$JIPIPE_VERSION.jar", tab=1)
    wl("cp -v ../../plugins/$component/target/$component-$JIPIPE_VERSION-SNAPSHOT.jar package/$component-$JIPIPE_VERSION.jar", tab=1)
    wl("cp -v ../../plugins/$component/target/$component-$JIPIPE_VERSION.jar package/$component-$JIPIPE_VERSION.jar", tab=1)
    wl("done")
    wl()

    # Copy dependencies
    wl("cp -rv ./dependencies ./package/dependencies")
    # wl("cp -rv $OPENCV_DIR/*.jar ./package/dependencies")

    # Copy other things
    wl("cp -v README.txt package")
    wl("cp -v ../../LICENSE package/LICENSE_JIPipe.txt")
    # wl("cp -v $OPENCV_DIR/LICENSE.txt package/LICENSE_OpenCV.txt")

    # ZIP package
    wl("rm -r JIPipe-$JIPIPE_VERSION.zip")
    wl("pushd package || exit")
    wl("zip -r ../JIPipe-$JIPIPE_VERSION.zip .", tab=1)
    wl("popd || exit")
    

