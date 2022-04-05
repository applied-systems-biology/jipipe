#!/bin/bash

JIPIPE_VERSION=1.71.0

if [ ! -e "./dependencies" ]; then
  mkdir dependencies
  pushd dependencies
  wget "https://maven.scijava.org/service/local/repositories/central/content/com/github/vatbub/mslinks/1.0.5/mslinks-1.0.5.jar"
  wget "https://maven.scijava.org/service/local/repositories/central/content/org/reflections/reflections/0.9.12/reflections-0.9.12.jar"
  wget "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util/0.62.2/flexmark-util-0.62.2.jar"
  wget "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-data/0.62.2/flexmark-util-data-0.62.2.jar"
  wget "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-ast/0.62.2/flexmark-util-ast-0.62.2.jar"
  wget "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-misc/0.62.2/flexmark-util-misc-0.62.2.jar"
  wget "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-dependency/0.62.2/flexmark-util-dependency-0.62.2.jar"
  wget "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-format/0.62.2/flexmark-util-format-0.62.2.jar"
  wget "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-sequence/0.62.2/flexmark-util-sequence-0.62.2.jar"
  wget "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-builder/0.62.2/flexmark-util-builder-0.62.2.jar"
  wget "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-visitor/0.62.2/flexmark-util-visitor-0.62.2.jar"
  wget "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-options/0.62.2/flexmark-util-options-0.62.2.jar"
  wget "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-html/0.62.2/flexmark-util-html-0.62.2.jar"
  wget "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-collection/0.62.2/flexmark-util-collection-0.62.2.jar"
  wget "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-pdf-converter/0.62.2/flexmark-pdf-converter-0.62.2.jar"
  wget "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-ext-toc/0.62.2/flexmark-ext-toc-0.62.2.jar"
  wget "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-ext-autolink/0.62.2/flexmark-ext-autolink-0.62.2.jar"
  wget "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark/0.62.2/flexmark-0.62.2.jar"
  wget "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-ext-tables/0.62.2/flexmark-ext-tables-0.62.2.jar"
  wget "https://maven.scijava.org/service/local/repositories/releases/content/sc/fiji/Image_5D/2.0.2/Image_5D-2.0.2.jar"
  wget "https://maven.scijava.org/service/local/repositories/sonatype/content/com/fasterxml/jackson/core/jackson-databind/2.12.5/jackson-databind-2.12.5.jar"
  wget "https://maven.scijava.org/service/local/repositories/sonatype/content/com/fasterxml/jackson/core/jackson-core/2.12.5/jackson-core-2.12.5.jar"
  wget "https://maven.scijava.org/service/local/repositories/sonatype/content/com/fasterxml/jackson/core/jackson-annotations/2.12.5/jackson-annotations-2.12.5.jar"
  wget "https://maven.scijava.org/service/local/repositories/central/content/org/jgrapht/jgrapht-core/1.4.0/jgrapht-core-1.4.0.jar"
  wget "https://maven.scijava.org/service/local/repositories/central/content/org/nibor/autolink/autolink/0.10.0/autolink-0.10.0.jar"
  wget "https://maven.scijava.org/service/local/repositories/central/content/org/apache/pdfbox/fontbox/2.0.4/fontbox-2.0.4.jar"
  wget "https://maven.scijava.org/service/local/repositories/central/content/com/openhtmltopdf/openhtmltopdf-jsoup-dom-converter/1.0.0/openhtmltopdf-jsoup-dom-converter-1.0.0.jar"
  wget "https://maven.scijava.org/service/local/repositories/central/content/com/openhtmltopdf/openhtmltopdf-core/1.0.4/openhtmltopdf-core-1.0.4.jar"
  wget "https://maven.scijava.org/service/local/repositories/central/content/org/apache/pdfbox/pdfbox/2.0.4/pdfbox-2.0.4.jar"
  wget "https://maven.scijava.org/service/local/repositories/central/content/com/openhtmltopdf/openhtmltopdf-rtl-support/1.0.4/openhtmltopdf-rtl-support-1.0.4.jar"
  wget "https://maven.scijava.org/service/local/repositories/central/content/com/openhtmltopdf/openhtmltopdf-pdfbox/1.0.4/openhtmltopdf-pdfbox-1.0.4.jar"
  wget "https://maven.scijava.org/service/local/repositories/central/content/org/jgrapht/jgrapht-core/1.4.0/jgrapht-core-1.4.0.jar"
  wget "https://maven.scijava.org/service/local/repositories/central/content/com/fathzer/javaluator/3.0.3/javaluator-3.0.3.jar"
  wget "https://maven.scijava.org/service/local/repositories/central/content/org/apache/commons/commons-exec/1.3/commons-exec-1.3.jar"
#  wget "https://github.com/ome/omero-insight/releases/download/v5.5.14/omero_ij-5.5.14-all.jar"
  wget "https://maven.scijava.org/service/local/repositories/ome-releases/content/org/openmicroscopy/omero-gateway/5.6.7/omero-gateway-5.6.7.jar"
  wget "https://maven.scijava.org/service/local/repositories/ome-releases/content/org/openmicroscopy/omero-blitz/5.5.8/omero-blitz-5.5.8.jar"
  wget "https://maven.scijava.org/service/local/repositories/central/content/com/zeroc/icegrid/3.6.5/icegrid-3.6.5.jar"
  wget "https://maven.scijava.org/service/local/repositories/central/content/com/zeroc/glacier2/3.6.5/glacier2-3.6.5.jar"
  wget "https://maven.scijava.org/service/local/repositories/central/content/net/java/dev/jna/jna-platform/4.5.2/jna-platform-4.5.2.jar"
  wget "https://maven.scijava.org/service/local/repositories/releases/content/de/biomedical-imaging/imagej/ij_ridge_detect/1.4.1/ij_ridge_detect-1.4.1.jar"
  wget "https://maven.scijava.org/service/local/repositories/central/content/org/apache/commons/commons-compress/1.20/commons-compress-1.20.jar"
  wget "https://maven.scijava.org/service/local/repositories/central/content/com/fasterxml/jackson/dataformat/jackson-dataformat-yaml/2.12.5/jackson-dataformat-yaml-2.12.5.jar"
  wget -O "LICENSE_OMERO.txt" https://github.com/ome/omero-insight/blob/master/LICENSE.txt
  wget -O "LICENSE_Javaluator.html" https://opensource.org/licenses/lgpl-3.0.html
  wget -O "LICENSE_JGraphT.txt" https://raw.githubusercontent.com/jgrapht/jgrapht/master/license-EPL.txt
  wget -O "LICENSE_OpenHTMLToPDF.txt" https://raw.githubusercontent.com/danfickle/openhtmltopdf/open-dev-v1/LICENSE
  wget -O "LICENSE_PDFBox.txt" https://www.apache.org/licenses/LICENSE-2.0.txt
  wget -O "LICENSE_FontBox.txt" https://www.apache.org/licenses/LICENSE-2.0.txt
  wget -O "LICENSE_Jackson.txt" https://www.apache.org/licenses/LICENSE-2.0.txt
  wget -O "LICENSE_Image5D.txt" https://raw.githubusercontent.com/fiji/Image_5D/master/LICENSE.txt
  wget -O "LICENSE_Flexmark.txt" https://raw.githubusercontent.com/vsch/flexmark-java/master/LICENSE.txt
  wget -O "LICENSE_Reflections.txt" https://raw.githubusercontent.com/ronmamo/reflections/master/COPYING.txt
  wget -O "LICENSE_mslinks.txt" https://raw.githubusercontent.com/DmitriiShamrikov/mslinks/master/LICENSE
  popd
fi

rm -r package
mkdir -p package/dependencies

for component in jipipe-core jipipe-clij jipipe-multiparameters jipipe-filesystem jipipe-ij jipipe-ij2 jipipe-ij-omero jipipe-ij-algorithms jipipe-ij-multi-template-matching jipipe-python jipipe-plots jipipe-tables jipipe-annotation jipipe-utils jipipe-strings jipipe-forms jipipe-r jipipe-cellpose jipipe-launcher jipipe-ij-updater-cli; do
    cp -v ../../$component/target/$component-$JIPIPE_VERSION.jar package
done
for dependency in dependencies/*.jar; do
  cp -v "$dependency" package/dependencies
done

cp -v README.txt package
cp -v ../../LICENSE package/LICENSE_JIPipe.txt

rm -r JIPipe-$JIPIPE_VERSION.zip
pushd package
  zip -r ../JIPipe-$JIPIPE_VERSION.zip .
popd

