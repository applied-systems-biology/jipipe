#!/bin/bash

JIPIPE_VERSION="1.68.0"
PROJECT_DIR=$PWD/../../

mkdir zip

function process_directory() {
    APP_DIR=$1
    OUTPUT_ZIP=$3
    rm -r tmp
    mkdir tmp
    cp -rv $APP_DIR/JIPipe.app tmp
    FIJI_DIR=tmp/$2

    pushd $FIJI_DIR
    rm -rv ./jipipe
    rm -rv ./plugins/JIPipe/*
    for component in jipipe-core jipipe-clij jipipe-multiparameters jipipe-filesystem jipipe-ij jipipe-ij2 jipipe-ij-omero jipipe-ij-algorithms jipipe-ij-multi-template-matching jipipe-python jipipe-plots jipipe-tables jipipe-annotation jipipe-utils jipipe-strings jipipe-forms jipipe-r jipipe-cellpose jipipe-launcher jipipe-ij-updater-cli; do
        cp -v $PROJECT_DIR/$component/target/$component-$JIPIPE_VERSION.jar ./plugins/JIPipe/
    done
    popd

    pushd tmp
    rm $OUTPUT_ZIP
    zip -r $OUTPUT_ZIP .
    popd

    rm -r tmp
}

process_directory $PWD/fiji-linux/ JIPipe.app $PWD/zip/jipipe-full-$JIPIPE_VERSION-linux.zip
process_directory $PWD/fiji-win/ JIPipe.app $PWD/zip/jipipe-full-$JIPIPE_VERSION-win.zip
process_directory $PWD/fiji-osx/ JIPipe.app/Contents/Resources/Fiji.app $PWD/zip/jipipe-full-$JIPIPE_VERSION-macos.zip
