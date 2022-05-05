#!/bin/bash

JIPIPE_VERSION=1.72.0

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
if [ ! -e "./dependencies/jackson-databind-2.12.5.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/jackson-databind-2.12.5.jar https://maven.scijava.org/service/local/repositories/sonatype/content/com/fasterxml/jackson/core/jackson-databind/2.12.5/jackson-databind-2.12.5.jar
fi
if [ ! -e "./dependencies/jackson-core-2.12.5.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/jackson-core-2.12.5.jar https://maven.scijava.org/service/local/repositories/sonatype/content/com/fasterxml/jackson/core/jackson-core/2.12.5/jackson-core-2.12.5.jar
fi
if [ ! -e "./dependencies/jackson-annotations-2.12.5.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/jackson-annotations-2.12.5.jar https://maven.scijava.org/service/local/repositories/sonatype/content/com/fasterxml/jackson/core/jackson-annotations/2.12.5/jackson-annotations-2.12.5.jar
fi
if [ ! -e "./dependencies/jgrapht-core-1.4.0.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/jgrapht-core-1.4.0.jar https://maven.scijava.org/service/local/repositories/central/content/org/jgrapht/jgrapht-core/1.4.0/jgrapht-core-1.4.0.jar
fi
if [ ! -e "./dependencies/autolink-0.10.0.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/autolink-0.10.0.jar https://maven.scijava.org/service/local/repositories/central/content/org/nibor/autolink/autolink/0.10.0/autolink-0.10.0.jar
fi
if [ ! -e "./dependencies/fontbox-2.0.4.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/fontbox-2.0.4.jar https://maven.scijava.org/service/local/repositories/central/content/org/apache/pdfbox/fontbox/2.0.4/fontbox-2.0.4.jar
fi
if [ ! -e "./dependencies/openhtmltopdf-jsoup-dom-converter-1.0.0.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/openhtmltopdf-jsoup-dom-converter-1.0.0.jar https://maven.scijava.org/service/local/repositories/central/content/com/openhtmltopdf/openhtmltopdf-jsoup-dom-converter/1.0.0/openhtmltopdf-jsoup-dom-converter-1.0.0.jar
fi
if [ ! -e "./dependencies/openhtmltopdf-core-1.0.4.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/openhtmltopdf-core-1.0.4.jar https://maven.scijava.org/service/local/repositories/central/content/com/openhtmltopdf/openhtmltopdf-core/1.0.4/openhtmltopdf-core-1.0.4.jar
fi
if [ ! -e "./dependencies/pdfbox-2.0.4.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/pdfbox-2.0.4.jar https://maven.scijava.org/service/local/repositories/central/content/org/apache/pdfbox/pdfbox/2.0.4/pdfbox-2.0.4.jar
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
if [ ! -e "./dependencies/omero/omero-gateway-5.6.7.jar" ]; then
	mkdir -p "./dependencies/omero"
	wget -O ./dependencies/omero/omero-gateway-5.6.7.jar https://maven.scijava.org/service/local/repositories/ome-releases/content/org/openmicroscopy/omero-gateway/5.6.7/omero-gateway-5.6.7.jar
fi
if [ ! -e "./dependencies/omero/omero-blitz-5.5.8.jar" ]; then
	mkdir -p "./dependencies/omero"
	wget -O ./dependencies/omero/omero-blitz-5.5.8.jar https://maven.scijava.org/service/local/repositories/ome-releases/content/org/openmicroscopy/omero-blitz/5.5.8/omero-blitz-5.5.8.jar
fi
if [ ! -e "./dependencies/omero/omero-server-5.5.6.jar" ]; then
	mkdir -p "./dependencies/omero"
	wget -O ./dependencies/omero/omero-server-5.5.6.jar https://maven.scijava.org/service/local/repositories/ome-releases/content/org/openmicroscopy/omero-server/5.5.6/omero-server-5.5.6.jar
fi
if [ ! -e "./dependencies/omero/omero-renderer-5.5.5.jar" ]; then
	mkdir -p "./dependencies/omero"
	wget -O ./dependencies/omero/omero-renderer-5.5.5.jar https://maven.scijava.org/service/local/repositories/ome-releases/content/org/openmicroscopy/omero-renderer/5.5.5/omero-renderer-5.5.5.jar
fi
if [ ! -e "./dependencies/omero/omero-romio-5.6.0.jar" ]; then
	mkdir -p "./dependencies/omero"
	wget -O ./dependencies/omero/omero-romio-5.6.0.jar https://maven.scijava.org/service/local/repositories/ome-releases/content/org/openmicroscopy/omero-romio/5.6.0/omero-romio-5.6.0.jar
fi
if [ ! -e "./dependencies/omero/omero-common-5.5.5.jar" ]; then
	mkdir -p "./dependencies/omero"
	wget -O ./dependencies/omero/omero-common-5.5.5.jar https://maven.scijava.org/service/local/repositories/ome-releases/content/org/openmicroscopy/omero-common/5.5.5/omero-common-5.5.5.jar
fi
if [ ! -e "./dependencies/omero/omero-model-5.6.0.jar" ]; then
	mkdir -p "./dependencies/omero"
	wget -O ./dependencies/omero/omero-model-5.6.0.jar https://maven.scijava.org/service/local/repositories/ome-releases/content/org/openmicroscopy/omero-model/5.6.0/omero-model-5.6.0.jar
fi
if [ ! -e "./dependencies/omero/icegrid-3.6.5.jar" ]; then
	mkdir -p "./dependencies/omero"
	wget -O ./dependencies/omero/icegrid-3.6.5.jar https://maven.scijava.org/service/local/repositories/central/content/com/zeroc/icegrid/3.6.5/icegrid-3.6.5.jar
fi
if [ ! -e "./dependencies/omero/glacier2-3.6.5.jar" ]; then
	mkdir -p "./dependencies/omero"
	wget -O ./dependencies/omero/glacier2-3.6.5.jar https://maven.scijava.org/service/local/repositories/central/content/com/zeroc/glacier2/3.6.5/glacier2-3.6.5.jar
fi
if [ ! -e "./dependencies/omero/ice-3.6.5.jar" ]; then
	mkdir -p "./dependencies/omero"
	wget -O ./dependencies/omero/ice-3.6.5.jar https://maven.scijava.org/service/local/repositories/central/content/com/zeroc/ice/3.6.5/ice-3.6.5.jar
fi
if [ ! -e "./dependencies/jna-platform-4.5.2.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/jna-platform-4.5.2.jar https://maven.scijava.org/service/local/repositories/central/content/net/java/dev/jna/jna-platform/4.5.2/jna-platform-4.5.2.jar
fi
if [ ! -e "./dependencies/ij_ridge_detect-1.4.1.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/ij_ridge_detect-1.4.1.jar https://maven.scijava.org/service/local/repositories/releases/content/de/biomedical-imaging/imagej/ij_ridge_detect/1.4.1/ij_ridge_detect-1.4.1.jar
fi
if [ ! -e "./dependencies/commons-compress-1.20.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/commons-compress-1.20.jar https://maven.scijava.org/service/local/repositories/central/content/org/apache/commons/commons-compress/1.20/commons-compress-1.20.jar
fi
if [ ! -e "./dependencies/jackson-dataformat-yaml-2.12.5.jar" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/jackson-dataformat-yaml-2.12.5.jar https://maven.scijava.org/service/local/repositories/central/content/com/fasterxml/jackson/dataformat/jackson-dataformat-yaml/2.12.5/jackson-dataformat-yaml-2.12.5.jar
fi
if [ ! -e "./dependencies/LICENSE_OMERO.txt" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/LICENSE_OMERO.txt https://github.com/ome/omero-insight/blob/master/LICENSE.txt
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
if [ ! -e "./dependencies/LICENSE_PDFBox.txt" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/LICENSE_PDFBox.txt https://www.apache.org/licenses/LICENSE-2.0.txt
fi
if [ ! -e "./dependencies/LICENSE_FontBox.txt" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/LICENSE_FontBox.txt https://www.apache.org/licenses/LICENSE-2.0.txt
fi
if [ ! -e "./dependencies/LICENSE_Jackson.txt" ]; then
	mkdir -p "./dependencies"
	wget -O ./dependencies/LICENSE_Jackson.txt https://www.apache.org/licenses/LICENSE-2.0.txt
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

rm -r package
mkdir package
for component in jipipe-core jipipe-clij jipipe-multiparameters jipipe-filesystem jipipe-ij jipipe-ij2 jipipe-ij-omero jipipe-ij-algorithms jipipe-ij-multi-template-matching jipipe-python jipipe-plots jipipe-tables jipipe-annotation jipipe-utils jipipe-strings jipipe-forms jipipe-r jipipe-cellpose jipipe-launcher jipipe-ij-updater-cli; do
	cp -v ../../$component/target/$component-$JIPIPE_VERSION.jar package
done

cp -rv ./dependencies ./package/dependencies
cp -v README.txt package
cp -v ../../LICENSE package/LICENSE_JIPipe.txt
rm -r JIPipe-$JIPIPE_VERSION.zip
pushd package
	zip -r ../JIPipe-$JIPIPE_VERSION.zip .
popd
