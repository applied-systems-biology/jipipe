#!/bin/bash

FIJI_DOWNLOAD="https://downloads.imagej.net/fiji/latest/fiji-macosx.zip"

function download_url {
  echo "Downloading $1 ..."
  curl -O -L -s $1
}

if [[ ! -e $PWD/Fiji.app ]]; then
  echo "ALERT:JIPipe installer|This app will now download all necessary files for JIPipe to run. This will take a couple of minutes and only has to be done once."

  # Download and install Fiji
  #curl "$FIJI_DOWNLOAD" -o "fiji-macosx.zip"
  cp ~/Downloads/fiji-macosx.zip $PWD
  unzip "fiji-macosx.zip"
  rm "fiji-macosx.zip"
  echo "PROGRESS:25"

  # Install JIPipe dependencies
  pushd $PWD/Fiji.app/jars
  download_url "https://maven.scijava.org/service/local/repositories/central/content/com/github/vatbub/mslinks/1.0.5/mslinks-1.0.5.jar"
  download_url "https://maven.scijava.org/service/local/repositories/central/content/org/reflections/reflections/0.9.12/reflections-0.9.12.jar"
  download_url "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util/0.62.2/flexmark-util-0.62.2.jar"
  download_url "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-data/0.62.2/flexmark-util-data-0.62.2.jar"
  download_url "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-ast/0.62.2/flexmark-util-ast-0.62.2.jar"
  download_url "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-misc/0.62.2/flexmark-util-misc-0.62.2.jar"
  download_url "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-dependency/0.62.2/flexmark-util-dependency-0.62.2.jar"
  download_url "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-format/0.62.2/flexmark-util-format-0.62.2.jar"
  download_url "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-sequence/0.62.2/flexmark-util-sequence-0.62.2.jar"
  download_url "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-builder/0.62.2/flexmark-util-builder-0.62.2.jar"
  download_url "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-visitor/0.62.2/flexmark-util-visitor-0.62.2.jar"
  download_url "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-options/0.62.2/flexmark-util-options-0.62.2.jar"
  download_url "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-html/0.62.2/flexmark-util-html-0.62.2.jar"
  download_url "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util-collection/0.62.2/flexmark-util-collection-0.62.2.jar"
  download_url "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-pdf-converter/0.62.2/flexmark-pdf-converter-0.62.2.jar"
  download_url "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-ext-toc/0.62.2/flexmark-ext-toc-0.62.2.jar"
  download_url "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-ext-autolink/0.62.2/flexmark-ext-autolink-0.62.2.jar"
  download_url "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark/0.62.2/flexmark-0.62.2.jar"
  download_url "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-ext-tables/0.62.2/flexmark-ext-tables-0.62.2.jar"
  download_url "https://maven.scijava.org/service/local/repositories/releases/content/sc/fiji/Image_5D/2.0.2/Image_5D-2.0.2.jar"
  download_url "https://maven.scijava.org/service/local/repositories/sonatype/content/com/fasterxml/jackson/core/jackson-databind/2.11.0/jackson-databind-2.11.0.jar"
  download_url "https://maven.scijava.org/service/local/repositories/sonatype/content/com/fasterxml/jackson/core/jackson-core/2.11.0/jackson-core-2.11.0.jar"
  download_url "https://maven.scijava.org/service/local/repositories/sonatype/content/com/fasterxml/jackson/core/jackson-annotations/2.11.0/jackson-annotations-2.11.0.jar"
  download_url "https://maven.scijava.org/service/local/repositories/central/content/org/jgrapht/jgrapht-core/1.3.1/jgrapht-core-1.4.0.jar"
  download_url "https://maven.scijava.org/service/local/repositories/central/content/org/nibor/autolink/autolink/0.10.0/autolink-0.10.0.jar"
  download_url "https://maven.scijava.org/service/local/repositories/central/content/org/apache/pdfbox/fontbox/2.0.4/fontbox-2.0.4.jar"
  download_url "https://maven.scijava.org/service/local/repositories/central/content/com/openhtmltopdf/openhtmltopdf-jsoup-dom-converter/1.0.0/openhtmltopdf-jsoup-dom-converter-1.0.0.jar"
  download_url "https://maven.scijava.org/service/local/repositories/central/content/com/openhtmltopdf/openhtmltopdf-core/1.0.4/openhtmltopdf-core-1.0.4.jar"
  download_url "https://maven.scijava.org/service/local/repositories/central/content/org/apache/pdfbox/pdfbox/2.0.4/pdfbox-2.0.4.jar"
  download_url "https://maven.scijava.org/service/local/repositories/central/content/com/openhtmltopdf/openhtmltopdf-rtl-support/1.0.4/openhtmltopdf-rtl-support-1.0.4.jar"
  download_url "https://maven.scijava.org/service/local/repositories/central/content/com/openhtmltopdf/openhtmltopdf-pdfbox/1.0.4/openhtmltopdf-pdfbox-1.0.4.jar"
  download_url "https://maven.scijava.org/service/local/repositories/central/content/org/jgrapht/jgrapht-core/1.4.0/jgrapht-core-1.4.0.jar"
  download_url "https://maven.scijava.org/service/local/repositories/central/content/com/fathzer/javaluator/3.0.3/javaluator-3.0.3.jar"
  download_url "https://maven.scijava.org/service/local/repositories/central/content/org/apache/commons/commons-exec/1.3/commons-exec-1.3.jar"
  download_url "https://github.com/ome/omero-insight/releases/download/v5.5.14/omero_ij-5.5.14-all.jar"
  popd
  echo "PROGRESS:50"

  # Install JIPipe
  for file in $PWD/jipipe-bin/*.jar; do
    cp -v $file $PWD/Fiji.app/plugins/
  done
  echo "PROGRESS:75"

  # Download JIPipe dependencies
  pushd $PWD/Fiji.app/Contents/MacOS
    ./ImageJ-macosx --pass-classpath --full-classpath --main-class org.hkijena.ijupdatercli.Main activate clij clij2 IJPB-plugins ImageScience IJ-OpenCV-plugins Multi-Template-Matching
  popd
  echo "PROGRESS:100"

fi

cd $PWD/Fiji.app/Contents/MacOS
nohup ./ImageJ-macosx --pass-classpath --full-classpath --main-class org.hkijena.jipipe.JIPipeLauncher &

echo "QUITAPP"
