#!/bin/bash

JIPIPE_VERSION="Development"
PROJECT_DIR=../..

pushd $PROJECT_DIR || exit
JIPIPE_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout | grep -Po "\d\.\d+\.\d+")
popd || exit


rm -r package
mkdir -p package/plugins/JIPipe
mkdir -p package/jars
for component in jipipe-core jipipe-launcher jipipe-plugin-clij jipipe-plugin-filesystem jipipe-plugin-ij jipipe-plugin-ij2 jipipe-plugin-ij-omero jipipe-plugin-ij-algorithms jipipe-plugin-ij-weka jipipe-plugin-ij-multi-template-matching jipipe-plugin-ij-trackmate jipipe-plugin-ij-3d jipipe-plugin-ij-filaments jipipe-plugin-ij-ocr jipipe-plugin-python jipipe-plugin-plots jipipe-plugin-tables jipipe-plugin-annotations jipipe-plugin-utils jipipe-plugin-strings jipipe-plugin-forms jipipe-plugin-r jipipe-plugin-cellpose jipipe-plugin-omnipose jipipe-plugin-scene-3d jipipe-plugin-imp jipipe-plugin-ilastik jipipe-plugin-opencv; do
	cp -v ../../$component/target/$component-$JIPIPE_VERSION-SNAPSHOT.jar package/plugins/JIPipe/$component-$JIPIPE_VERSION.jar
	cp -v ../../$component/target/$component-$JIPIPE_VERSION.jar package/plugins/JIPipe/$component-$JIPIPE_VERSION.jar
	cp -v ../../plugins/$component/target/$component-$JIPIPE_VERSION-SNAPSHOT.jar package/plugins/JIPipe/$component-$JIPIPE_VERSION.jar
	cp -v ../../plugins/$component/target/$component-$JIPIPE_VERSION.jar package/plugin/JIPipe/$component-$JIPIPE_VERSION.jar
done

for component in jipipe-ro-crate-java-2.1.0; do
	cp -v ../../contrib/$component/target/$component-SNAPSHOT.jar ./package/jars/$component.jar
	cp -v ../../contrib/$component/target/$component.jar ./package/jars/$component.jar
done

cp -v README.txt package
cp -v ../../LICENSE package/LICENSE_JIPipe.txt
rm -r JIPipe-$JIPIPE_VERSION.zip
pushd package || exit
	zip -r ../JIPipe-$JIPIPE_VERSION.zip .
popd || exit
