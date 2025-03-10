#!/bin/bash

JIPIPE_VERSION="Development"
PROJECT_DIR=../..

pushd $PROJECT_DIR || exit
JIPIPE_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout | grep -Po "\d\.\d+\.\d+")
popd || exit

if [ ! -e "./dependencies/mslinks-1.0.5.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/mslinks-1.0.5.jar https://maven.scijava.org/service/local/repositories/central/content/com/github/vatbub/mslinks/1.0.5/mslinks-1.0.5.jar
fi
if [ ! -e "./dependencies/reflections-0.9.12.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/reflections-0.9.12.jar https://maven.scijava.org/service/local/repositories/central/content/org/reflections/reflections/0.9.12/reflections-0.9.12.jar
fi
if [ ! -e "./dependencies/flexmark-util-0.62.2.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/flexmark-util-0.62.2.jar https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util/0.62.2/flexmark-util-0.62.2.jar
fi
if [ ! -e "./dependencies/flexmark-util-data-0.62.2.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/flexmark-util-data-0.62.2.jar https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-data/0.62.2/flexmark-util-data-0.62.2.jar
fi
if [ ! -e "./dependencies/flexmark-util-ast-0.62.2.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/flexmark-util-ast-0.62.2.jar https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-ast/0.62.2/flexmark-util-ast-0.62.2.jar
fi
if [ ! -e "./dependencies/flexmark-util-misc-0.62.2.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/flexmark-util-misc-0.62.2.jar https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-misc/0.62.2/flexmark-util-misc-0.62.2.jar
fi
if [ ! -e "./dependencies/flexmark-util-dependency-0.62.2.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/flexmark-util-dependency-0.62.2.jar https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-dependency/0.62.2/flexmark-util-dependency-0.62.2.jar
fi
if [ ! -e "./dependencies/flexmark-util-format-0.62.2.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/flexmark-util-format-0.62.2.jar https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-format/0.62.2/flexmark-util-format-0.62.2.jar
fi
if [ ! -e "./dependencies/flexmark-util-sequence-0.62.2.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/flexmark-util-sequence-0.62.2.jar https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-sequence/0.62.2/flexmark-util-sequence-0.62.2.jar
fi
if [ ! -e "./dependencies/flexmark-util-builder-0.62.2.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/flexmark-util-builder-0.62.2.jar https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-builder/0.62.2/flexmark-util-builder-0.62.2.jar
fi
if [ ! -e "./dependencies/flexmark-util-visitor-0.62.2.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/flexmark-util-visitor-0.62.2.jar https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-visitor/0.62.2/flexmark-util-visitor-0.62.2.jar
fi
if [ ! -e "./dependencies/flexmark-util-options-0.62.2.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/flexmark-util-options-0.62.2.jar https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-options/0.62.2/flexmark-util-options-0.62.2.jar
fi
if [ ! -e "./dependencies/flexmark-util-html-0.62.2.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/flexmark-util-html-0.62.2.jar https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-html/0.62.2/flexmark-util-html-0.62.2.jar
fi
if [ ! -e "./dependencies/flexmark-util-collection-0.62.2.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/flexmark-util-collection-0.62.2.jar https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-collection/0.62.2/flexmark-util-collection-0.62.2.jar
fi
if [ ! -e "./dependencies/flexmark-pdf-converter-0.62.2.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/flexmark-pdf-converter-0.62.2.jar https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-pdf-converter/0.62.2/flexmark-pdf-converter-0.62.2.jar
fi
if [ ! -e "./dependencies/flexmark-ext-toc-0.62.2.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/flexmark-ext-toc-0.62.2.jar https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-ext-toc/0.62.2/flexmark-ext-toc-0.62.2.jar
fi
if [ ! -e "./dependencies/flexmark-ext-autolink-0.62.2.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/flexmark-ext-autolink-0.62.2.jar https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-ext-autolink/0.62.2/flexmark-ext-autolink-0.62.2.jar
fi
if [ ! -e "./dependencies/flexmark-0.62.2.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/flexmark-0.62.2.jar https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark/0.62.2/flexmark-0.62.2.jar
fi
if [ ! -e "./dependencies/flexmark-ext-tables-0.62.2.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/flexmark-ext-tables-0.62.2.jar https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-ext-tables/0.62.2/flexmark-ext-tables-0.62.2.jar
fi
if [ ! -e "./dependencies/jgrapht-core-1.4.0.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/jgrapht-core-1.4.0.jar https://maven.scijava.org/service/local/repositories/central/content/org/jgrapht/jgrapht-core/1.4.0/jgrapht-core-1.4.0.jar
fi
if [ ! -e "./dependencies/jgrapht-io-1.4.0.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/jgrapht-io-1.4.0.jar https://maven.scijava.org/service/local/repositories/central/content/org/jgrapht/jgrapht-io/1.4.0/jgrapht-io-1.4.0.jar
fi
if [ ! -e "./dependencies/autolink-0.10.0.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/autolink-0.10.0.jar https://maven.scijava.org/service/local/repositories/central/content/org/nibor/autolink/autolink/0.10.0/autolink-0.10.0.jar
fi
if [ ! -e "./dependencies/openhtmltopdf-jsoup-dom-converter-1.0.0.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/openhtmltopdf-jsoup-dom-converter-1.0.0.jar https://maven.scijava.org/service/local/repositories/central/content/com/openhtmltopdf/openhtmltopdf-jsoup-dom-converter/1.0.0/openhtmltopdf-jsoup-dom-converter-1.0.0.jar
fi
if [ ! -e "./dependencies/openhtmltopdf-core-1.0.4.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/openhtmltopdf-core-1.0.4.jar https://maven.scijava.org/service/local/repositories/central/content/com/openhtmltopdf/openhtmltopdf-core/1.0.4/openhtmltopdf-core-1.0.4.jar
fi
if [ ! -e "./dependencies/openhtmltopdf-rtl-support-1.0.4.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/openhtmltopdf-rtl-support-1.0.4.jar https://maven.scijava.org/service/local/repositories/central/content/com/openhtmltopdf/openhtmltopdf-rtl-support/1.0.4/openhtmltopdf-rtl-support-1.0.4.jar
fi
if [ ! -e "./dependencies/openhtmltopdf-pdfbox-1.0.4.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/openhtmltopdf-pdfbox-1.0.4.jar https://maven.scijava.org/service/local/repositories/central/content/com/openhtmltopdf/openhtmltopdf-pdfbox/1.0.4/openhtmltopdf-pdfbox-1.0.4.jar
fi
if [ ! -e "./dependencies/javaluator-3.0.3.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/javaluator-3.0.3.jar https://maven.scijava.org/service/local/repositories/central/content/com/fathzer/javaluator/3.0.3/javaluator-3.0.3.jar
fi
if [ ! -e "./dependencies/commons-exec-1.3.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/commons-exec-1.3.jar https://maven.scijava.org/service/local/repositories/central/content/org/apache/commons/commons-exec/1.3/commons-exec-1.3.jar
fi
if [ ! -e "./dependencies/jna-platform-4.5.2.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/jna-platform-4.5.2.jar https://maven.scijava.org/service/local/repositories/central/content/net/java/dev/jna/jna-platform/4.5.2/jna-platform-4.5.2.jar
fi
if [ ! -e "./dependencies/ij_ridge_detect-1.4.1.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/ij_ridge_detect-1.4.1.jar https://maven.scijava.org/service/local/repositories/releases/content/de/biomedical-imaging/imagej/ij_ridge_detect/1.4.1/ij_ridge_detect-1.4.1.jar
fi
if [ ! -e "./dependencies/poi-5.2.0.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/poi-5.2.0.jar https://maven.scijava.org/service/local/repositories/central/content/org/apache/poi/poi/5.2.0/poi-5.2.0.jar
fi
if [ ! -e "./dependencies/poi-ooxml-5.2.0.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/poi-ooxml-5.2.0.jar https://maven.scijava.org/service/local/repositories/central/content/org/apache/poi/poi-ooxml/5.2.0/poi-ooxml-5.2.0.jar
fi
if [ ! -e "./dependencies/poi-ooxml-lite-5.2.0.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/poi-ooxml-lite-5.2.0.jar https://repo1.maven.org/maven2/org/apache/poi/poi-ooxml-lite/5.2.0/poi-ooxml-lite-5.2.0.jar
fi
if [ ! -e "./dependencies/xmlbeans-5.0.3.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/xmlbeans-5.0.3.jar https://maven.scijava.org/service/local/repositories/sonatype/content/org/apache/xmlbeans/xmlbeans/5.0.3/xmlbeans-5.0.3.jar
fi
if [ ! -e "./dependencies/log4j-api-2.17.1.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/log4j-api-2.17.1.jar https://maven.scijava.org/service/local/repositories/central/content/org/apache/logging/log4j/log4j-api/2.17.1/log4j-api-2.17.1.jar
fi
if [ ! -e "./dependencies/balloontip-1.2.4.1.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/balloontip-1.2.4.1.jar https://maven.scijava.org/service/local/repositories/bedatadriven/content/net/java/balloontip/balloontip/1.2.4.1/balloontip-1.2.4.1.jar
fi
if [ ! -e "./dependencies/OrientationJ_.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/OrientationJ_.jar https://bigwww.epfl.ch/demo/orientation/OrientationJ_.jar
fi
if [ ! -e "./dependencies/LICENSE_Javaluator.html" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/LICENSE_Javaluator.html https://opensource.org/licenses/lgpl-3.0.html
fi
if [ ! -e "./dependencies/LICENSE_JGraphT.txt" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/LICENSE_JGraphT.txt https://raw.githubusercontent.com/jgrapht/jgrapht/master/license-EPL.txt
fi
if [ ! -e "./dependencies/LICENSE_OpenHTMLToPDF.txt" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/LICENSE_OpenHTMLToPDF.txt https://raw.githubusercontent.com/danfickle/openhtmltopdf/open-dev-v1/LICENSE
fi
if [ ! -e "./dependencies/LICENSE_Image5D.txt" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/LICENSE_Image5D.txt https://raw.githubusercontent.com/fiji/Image_5D/master/LICENSE.txt
fi
if [ ! -e "./dependencies/LICENSE_Flexmark.txt" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/LICENSE_Flexmark.txt https://raw.githubusercontent.com/vsch/flexmark-java/master/LICENSE.txt
fi
if [ ! -e "./dependencies/LICENSE_Reflections.txt" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/LICENSE_Reflections.txt https://raw.githubusercontent.com/ronmamo/reflections/master/COPYING.txt
fi
if [ ! -e "./dependencies/LICENSE_mslinks.txt" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/LICENSE_mslinks.txt https://raw.githubusercontent.com/DmitriiShamrikov/mslinks/master/LICENSE
fi
if [ ! -e "./dependencies/LICENSE_OrientationJ.txt" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/LICENSE_OrientationJ.txt https://raw.githubusercontent.com/Biomedical-Imaging-Group/OrientationJ/master/LICENSE
fi

rm -r package
mkdir package
for component in jipipe-core jipipe-desktop jipipe-cli jipipe-plugin-clij jipipe-plugin-filesystem jipipe-plugin-ij jipipe-plugin-ij2 jipipe-plugin-ij-omero jipipe-plugin-ij-algorithms jipipe-plugin-ij-weka jipipe-plugin-ij-multi-template-matching jipipe-plugin-ij-trackmate jipipe-plugin-ij-3d jipipe-plugin-ij-filaments jipipe-plugin-ij-ocr jipipe-plugin-python jipipe-plugin-plots jipipe-plugin-tables jipipe-plugin-annotations jipipe-plugin-utils jipipe-plugin-strings jipipe-plugin-forms jipipe-plugin-r jipipe-plugin-cellpose jipipe-plugin-omnipose jipipe-plugin-scene-3d jipipe-plugin-imp jipipe-plugin-ilastik jipipe-plugin-opencv; do
	cp -v ../../$component/target/$component-$JIPIPE_VERSION-SNAPSHOT.jar package/$component-$JIPIPE_VERSION.jar
	cp -v ../../$component/target/$component-$JIPIPE_VERSION.jar package/$component-$JIPIPE_VERSION.jar
	cp -v ../../plugins/$component/target/$component-$JIPIPE_VERSION-SNAPSHOT.jar package/$component-$JIPIPE_VERSION.jar
	cp -v ../../plugins/$component/target/$component-$JIPIPE_VERSION.jar package/$component-$JIPIPE_VERSION.jar
done

cp -rv ./dependencies ./package/dependencies
cp -v README.txt package
cp -v ../../LICENSE package/LICENSE_JIPipe.txt
rm -r JIPipe-$JIPIPE_VERSION.zip
pushd package || exit
	zip -r ../JIPipe-$JIPIPE_VERSION.zip .
popd || exit
