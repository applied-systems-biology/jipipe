#!/bin/bash

JIPIPE_VERSION="Development"
PROJECT_DIR=$PWD/../../

pushd $PROJECT_DIR || exit
JIPIPE_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout | sed "s/-SNAPSHOT//g")
popd || exit

mkdir zip

function process_directory() {
    APP_DIR=$1
    OUTPUT_ZIP=$3
    rm -r tmp
    mkdir tmp
    cp -rv $APP_DIR/JIPipe.app tmp
    FIJI_DIR=tmp/$2

    pushd $FIJI_DIR || exit
    rm -rv ./jipipe
    rm -rv ./plugins/JIPipe/*
    mkdir -p ./plugins/JIPipe
    for component in jipipe-core jipipe-clij jipipe-multiparameters jipipe-filesystem jipipe-ij jipipe-ij2 jipipe-ij-omero jipipe-ij-algorithms jipipe-ij-multi-template-matching jipipe-python jipipe-plots jipipe-tables jipipe-annotation jipipe-utils jipipe-strings jipipe-forms jipipe-r jipipe-cellpose jipipe-launcher jipipe-ij-updater-cli; do
        cp -v $PROJECT_DIR/$component/target/$component-$JIPIPE_VERSION-SNAPSHOT.jar ./plugins/JIPipe/$component-$JIPIPE_VERSION.jar
    done
    popd || exit

    pushd tmp || exit
    rm $OUTPUT_ZIP
    zip -r $OUTPUT_ZIP .
    popd || exit

    rm -r tmp
}

process_directory $PWD/fiji-linux/ JIPipe.app $PWD/zip/jipipe-full-$JIPIPE_VERSION-linux.zip
process_directory $PWD/fiji-win/ JIPipe.app $PWD/zip/jipipe-full-$JIPIPE_VERSION-win.zip
process_directory $PWD/fiji-osx/ JIPipe.app/Contents/Resources/Fiji.app $PWD/zip/jipipe-full-$JIPIPE_VERSION-macos.zip
