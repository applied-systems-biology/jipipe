#!/bin/bash

JIPIPE_VERSION="$1"

if [ ! -e "./dependencies/mslinks-1.0.5.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/mslinks-1.0.5.jar https://repo1.maven.org/maven2/com/github/vatbub/mslinks/1.0.5/mslinks-1.0.5.jar || exit 1
fi
if [ ! -e "./dependencies/reflections-0.9.12.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/reflections-0.9.12.jar https://repo1.maven.org/maven2/org/reflections/reflections/0.9.12/reflections-0.9.12.jar || exit 1
fi
if [ ! -e "./dependencies/flexmark-util-data-0.62.2.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/flexmark-util-data-0.62.2.jar https://repo1.maven.org/maven2/com/vladsch/flexmark/flexmark-util-data/0.62.2/flexmark-util-data-0.62.2.jar || exit 1
fi
if [ ! -e "./dependencies/flexmark-util-ast-0.62.2.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/flexmark-util-ast-0.62.2.jar https://repo1.maven.org/maven2/com/vladsch/flexmark/flexmark-util-ast/0.62.2/flexmark-util-ast-0.62.2.jar || exit 1
fi
if [ ! -e "./dependencies/flexmark-util-misc-0.62.2.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/flexmark-util-misc-0.62.2.jar https://repo1.maven.org/maven2/com/vladsch/flexmark/flexmark-util-misc/0.62.2/flexmark-util-misc-0.62.2.jar || exit 1
fi
if [ ! -e "./dependencies/flexmark-util-dependency-0.62.2.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/flexmark-util-dependency-0.62.2.jar https://repo1.maven.org/maven2/com/vladsch/flexmark/flexmark-util-dependency/0.62.2/flexmark-util-dependency-0.62.2.jar || exit 1
fi
if [ ! -e "./dependencies/flexmark-util-format-0.62.2.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/flexmark-util-format-0.62.2.jar https://repo1.maven.org/maven2/com/vladsch/flexmark/flexmark-util-format/0.62.2/flexmark-util-format-0.62.2.jar || exit 1
fi
if [ ! -e "./dependencies/flexmark-util-sequence-0.62.2.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/flexmark-util-sequence-0.62.2.jar https://repo1.maven.org/maven2/com/vladsch/flexmark/flexmark-util-sequence/0.62.2/flexmark-util-sequence-0.62.2.jar || exit 1
fi
if [ ! -e "./dependencies/flexmark-util-builder-0.62.2.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/flexmark-util-builder-0.62.2.jar https://repo1.maven.org/maven2/com/vladsch/flexmark/flexmark-util-builder/0.62.2/flexmark-util-builder-0.62.2.jar || exit 1
fi
if [ ! -e "./dependencies/flexmark-util-visitor-0.62.2.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/flexmark-util-visitor-0.62.2.jar https://repo1.maven.org/maven2/com/vladsch/flexmark/flexmark-util-visitor/0.62.2/flexmark-util-visitor-0.62.2.jar || exit 1
fi
if [ ! -e "./dependencies/flexmark-util-options-0.62.2.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/flexmark-util-options-0.62.2.jar https://repo1.maven.org/maven2/com/vladsch/flexmark/flexmark-util-options/0.62.2/flexmark-util-options-0.62.2.jar || exit 1
fi
if [ ! -e "./dependencies/flexmark-util-html-0.62.2.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/flexmark-util-html-0.62.2.jar https://repo1.maven.org/maven2/com/vladsch/flexmark/flexmark-util-html/0.62.2/flexmark-util-html-0.62.2.jar || exit 1
fi
if [ ! -e "./dependencies/flexmark-util-collection-0.62.2.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/flexmark-util-collection-0.62.2.jar https://repo1.maven.org/maven2/com/vladsch/flexmark/flexmark-util-collection/0.62.2/flexmark-util-collection-0.62.2.jar || exit 1
fi
if [ ! -e "./dependencies/flexmark-pdf-converter-0.62.2.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/flexmark-pdf-converter-0.62.2.jar https://repo1.maven.org/maven2/com/vladsch/flexmark/flexmark-pdf-converter/0.62.2/flexmark-pdf-converter-0.62.2.jar || exit 1
fi
if [ ! -e "./dependencies/flexmark-ext-toc-0.62.2.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/flexmark-ext-toc-0.62.2.jar https://repo1.maven.org/maven2/com/vladsch/flexmark/flexmark-ext-toc/0.62.2/flexmark-ext-toc-0.62.2.jar || exit 1
fi
if [ ! -e "./dependencies/flexmark-ext-autolink-0.62.2.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/flexmark-ext-autolink-0.62.2.jar https://repo1.maven.org/maven2/com/vladsch/flexmark/flexmark-ext-autolink/0.62.2/flexmark-ext-autolink-0.62.2.jar || exit 1
fi
if [ ! -e "./dependencies/flexmark-0.62.2.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/flexmark-0.62.2.jar https://repo1.maven.org/maven2/com/vladsch/flexmark/flexmark/0.62.2/flexmark-0.62.2.jar || exit 1
fi
if [ ! -e "./dependencies/flexmark-ext-tables-0.62.2.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/flexmark-ext-tables-0.62.2.jar https://repo1.maven.org/maven2/com/vladsch/flexmark/flexmark-ext-tables/0.62.2/flexmark-ext-tables-0.62.2.jar || exit 1
fi
if [ ! -e "./dependencies/jgrapht-io-1.4.0.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/jgrapht-io-1.4.0.jar https://repo1.maven.org/maven2/org/jgrapht/jgrapht-io/1.4.0/jgrapht-io-1.4.0.jar || exit 1
fi
if [ ! -e "./dependencies/autolink-0.10.0.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/autolink-0.10.0.jar https://repo1.maven.org/maven2/org/nibor/autolink/autolink/0.10.0/autolink-0.10.0.jar || exit 1
fi
if [ ! -e "./dependencies/openhtmltopdf-jsoup-dom-converter-1.0.0.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/openhtmltopdf-jsoup-dom-converter-1.0.0.jar https://repo1.maven.org/maven2/com/openhtmltopdf/openhtmltopdf-jsoup-dom-converter/1.0.0/openhtmltopdf-jsoup-dom-converter-1.0.0.jar || exit 1
fi
if [ ! -e "./dependencies/openhtmltopdf-core-1.0.4.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/openhtmltopdf-core-1.0.4.jar https://repo1.maven.org/maven2/com/openhtmltopdf/openhtmltopdf-core/1.0.4/openhtmltopdf-core-1.0.4.jar || exit 1
fi
if [ ! -e "./dependencies/openhtmltopdf-rtl-support-1.0.4.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/openhtmltopdf-rtl-support-1.0.4.jar https://repo1.maven.org/maven2/com/openhtmltopdf/openhtmltopdf-rtl-support/1.0.4/openhtmltopdf-rtl-support-1.0.4.jar || exit 1
fi
if [ ! -e "./dependencies/openhtmltopdf-pdfbox-1.0.4.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/openhtmltopdf-pdfbox-1.0.4.jar https://repo1.maven.org/maven2/com/openhtmltopdf/openhtmltopdf-pdfbox/1.0.4/openhtmltopdf-pdfbox-1.0.4.jar || exit 1
fi
if [ ! -e "./dependencies/javaluator-3.0.3.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/javaluator-3.0.3.jar https://repo1.maven.org/maven2/com/fathzer/javaluator/3.0.3/javaluator-3.0.3.jar || exit 1
fi
if [ ! -e "./dependencies/commons-exec-1.3.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/commons-exec-1.3.jar https://repo1.maven.org/maven2/org/apache/commons/commons-exec/1.3/commons-exec-1.3.jar || exit 1
fi
if [ ! -e "./dependencies/jna-platform-4.5.2.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/jna-platform-4.5.2.jar https://repo1.maven.org/maven2/net/java/dev/jna/jna-platform/4.5.2/jna-platform-4.5.2.jar || exit 1
fi
if [ ! -e "./dependencies/ij_ridge_detect-1.4.1.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/ij_ridge_detect-1.4.1.jar https://maven.scijava.org/service/local/repositories/releases/content/de/biomedical-imaging/imagej/ij_ridge_detect/1.4.1/ij_ridge_detect-1.4.1.jar || exit 1
fi
if [ ! -e "./dependencies/poi-5.2.0.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/poi-5.2.0.jar https://repo1.maven.org/maven2/org/apache/poi/poi/5.2.0/poi-5.2.0.jar || exit 1
fi
if [ ! -e "./dependencies/poi-ooxml-5.2.0.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/poi-ooxml-5.2.0.jar https://repo1.maven.org/maven2/org/apache/poi/poi-ooxml/5.2.0/poi-ooxml-5.2.0.jar || exit 1
fi
if [ ! -e "./dependencies/poi-ooxml-lite-5.2.0.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/poi-ooxml-lite-5.2.0.jar https://repo1.maven.org/maven2/org/apache/poi/poi-ooxml-lite/5.2.0/poi-ooxml-lite-5.2.0.jar || exit 1
fi
if [ ! -e "./dependencies/xmlbeans-5.0.3.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/xmlbeans-5.0.3.jar https://repo1.maven.org/maven2/org/apache/xmlbeans/xmlbeans/5.0.3/xmlbeans-5.0.3.jar || exit 1
fi
if [ ! -e "./dependencies/log4j-api-2.17.1.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/log4j-api-2.17.1.jar https://repo1.maven.org/maven2/org/apache/logging/log4j/log4j-api/2.17.1/log4j-api-2.17.1.jar || exit 1
fi
if [ ! -e "./dependencies/balloontip-1.2.4.1.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/balloontip-1.2.4.1.jar https://repo1.maven.org/maven2/net/java/balloontip/balloontip/1.2.4.1/balloontip-1.2.4.1.jar || exit 1
fi
if [ ! -e "./dependencies/OrientationJ_.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/OrientationJ_.jar https://bigwww.epfl.ch/demo/orientation/OrientationJ_.jar || exit 1
fi
if [ ! -e "./dependencies/jcefmaven-135.0.20.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/jcefmaven-135.0.20.jar https://repo1.maven.org/maven2/me/friwi/jcefmaven/135.0.20/jcefmaven-135.0.20.jar || exit 1
fi
if [ ! -e "./dependencies/jcef-api-jcef-ca49ada+cef-135.0.20+ge7de5c3+chromium-135.0.7049.85.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/jcef-api-jcef-ca49ada+cef-135.0.20+ge7de5c3+chromium-135.0.7049.85.jar https://repo1.maven.org/maven2/me/friwi/jcef-api/jcef-ca49ada+cef-135.0.20+ge7de5c3+chromium-135.0.7049.85/jcef-api-jcef-ca49ada+cef-135.0.20+ge7de5c3+chromium-135.0.7049.85.jar || exit 1
fi
if [ ! -e "./dependencies/jackson-datatype-jsr310-2.18.0.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/jackson-datatype-jsr310-2.18.0.jar https://repo1.maven.org/maven2/com/fasterxml/jackson/datatype/jackson-datatype-jsr310/2.18.0/jackson-datatype-jsr310-2.18.0.jar || exit 1
fi
if [ ! -e "./dependencies/zip4j-2.11.5.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/zip4j-2.11.5.jar https://repo1.maven.org/maven2/net/lingala/zip4j/zip4j/2.11.5/zip4j-2.11.5.jar || exit 1
fi
if [ ! -e "./dependencies/json-compare-7.0.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/json-compare-7.0.jar https://repo1.maven.org/maven2/com/github/fslev/json-compare/7.0/json-compare-7.0.jar || exit 1
fi
if [ ! -e "./dependencies/commons-validator-1.9.0.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/commons-validator-1.9.0.jar https://repo1.maven.org/maven2/commons-validator/commons-validator/1.9.0/commons-validator-1.9.0.jar || exit 1
fi
if [ ! -e "./dependencies/titanium-json-ld-1.6.0.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/titanium-json-ld-1.6.0.jar https://repo1.maven.org/maven2/com/apicatalog/titanium-json-ld/1.6.0/titanium-json-ld-1.6.0.jar || exit 1
fi
if [ ! -e "./dependencies/json-schema-validator-1.5.7.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/json-schema-validator-1.5.7.jar https://repo1.maven.org/maven2/com/networknt/json-schema-validator/1.5.7/json-schema-validator-1.5.7.jar || exit 1
fi
if [ ! -e "./dependencies/jakarta.json-2.0.1.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/jakarta.json-2.0.1.jar https://repo1.maven.org/maven2/org/glassfish/jakarta.json/2.0.1/jakarta.json-2.0.1.jar || exit 1
fi
if [ ! -e "./dependencies/jte-3.2.1.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/jte-3.2.1.jar https://repo1.maven.org/maven2/gg/jte/jte/3.2.1/jte-3.2.1.jar || exit 1
fi
if [ ! -e "./dependencies/freemarker-2.3.34.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/freemarker-2.3.34.jar https://repo1.maven.org/maven2/org/freemarker/freemarker/2.3.34/freemarker-2.3.34.jar || exit 1
fi
if [ ! -e "./dependencies/LICENSE_Javaluator.html" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/LICENSE_Javaluator.html https://opensource.org/licenses/lgpl-3.0.html || exit 1
fi
if [ ! -e "./dependencies/LICENSE_JGraphT.txt" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/LICENSE_JGraphT.txt https://raw.githubusercontent.com/jgrapht/jgrapht/master/license-EPL.txt || exit 1
fi
if [ ! -e "./dependencies/LICENSE_OpenHTMLToPDF.txt" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/LICENSE_OpenHTMLToPDF.txt https://raw.githubusercontent.com/danfickle/openhtmltopdf/open-dev-v1/LICENSE || exit 1
fi
if [ ! -e "./dependencies/LICENSE_Image5D.txt" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/LICENSE_Image5D.txt https://raw.githubusercontent.com/fiji/Image_5D/master/LICENSE.txt || exit 1
fi
if [ ! -e "./dependencies/LICENSE_Flexmark.txt" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/LICENSE_Flexmark.txt https://raw.githubusercontent.com/vsch/flexmark-java/master/LICENSE.txt || exit 1
fi
if [ ! -e "./dependencies/LICENSE_Reflections.txt" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/LICENSE_Reflections.txt https://raw.githubusercontent.com/ronmamo/reflections/master/COPYING.txt || exit 1
fi
if [ ! -e "./dependencies/LICENSE_mslinks.txt" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/LICENSE_mslinks.txt https://raw.githubusercontent.com/DmitriiShamrikov/mslinks/master/LICENSE || exit 1
fi
if [ ! -e "./dependencies/LICENSE_OrientationJ.txt" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/LICENSE_OrientationJ.txt https://raw.githubusercontent.com/Biomedical-Imaging-Group/OrientationJ/master/LICENSE || exit 1
fi

rm -r package
mkdir -p package/plugins/JIPipe
mkdir -p package/jars
for component in jipipe-core jipipe-desktop jipipe-cli jipipe-plugin-clij jipipe-plugin-filesystem jipipe-plugin-ij jipipe-plugin-ij2 jipipe-plugin-ij-omero jipipe-plugin-ij-algorithms jipipe-plugin-ij-weka jipipe-plugin-ij-multi-template-matching jipipe-plugin-ij-trackmate jipipe-plugin-ij-3d jipipe-plugin-ij-filaments jipipe-plugin-ij-ocr jipipe-plugin-python jipipe-plugin-plots jipipe-plugin-tables jipipe-plugin-annotations jipipe-plugin-utils jipipe-plugin-strings jipipe-plugin-forms jipipe-plugin-r jipipe-plugin-cellpose jipipe-plugin-omnipose jipipe-plugin-scene-3d jipipe-plugin-imp jipipe-plugin-ilastik jipipe-plugin-opencv; do
	cp -v ../../$component/target/$component-$JIPIPE_VERSION-SNAPSHOT.jar package/plugins/JIPipe/$component-$JIPIPE_VERSION.jar
	cp -v ../../$component/target/$component-$JIPIPE_VERSION.jar package/plugins/JIPipe/$component-$JIPIPE_VERSION.jar
	cp -v ../../plugins/$component/target/$component-$JIPIPE_VERSION-SNAPSHOT.jar package/plugins/JIPipe/$component-$JIPIPE_VERSION.jar
	cp -v ../../plugins/$component/target/$component-$JIPIPE_VERSION.jar package/plugin/JIPipe/$component-$JIPIPE_VERSION.jar
done

cp -rv ./dependencies/* ./package/jars/
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
