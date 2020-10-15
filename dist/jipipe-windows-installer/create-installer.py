#!/usr/bin/env python3

from pathlib import Path
import urllib.request
from zipfile import ZipFile
import shutil

fiji_download_url = "https://downloads.imagej.net/fiji/latest/fiji-win64.zip"
fiji_zip_file = "fiji-win64.zip"
dependency_urls = [
    "https://maven.scijava.org/service/local/repositories/central/content/com/github/vatbub/mslinks/1.0.5/mslinks-1.0.5.jar",
    "https://maven.scijava.org/service/local/repositories/central/content/org/reflections/reflections/0.9.12/reflections-0.9.12.jar",
    "https://maven.scijava.org/service/local/repositories/central/content/org/javassist/javassist/3.25.0-GA/javassist-3.25.0-GA.jar",
    "https://maven.scijava.org/service/local/repositories/central/content/org/jfree/jfreesvg/3.3/jfreesvg-3.3.jar",
    "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-util/0.50.50/flexmark-util-0.50.50.jar",
    "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-pdf-converter/0.50.50/flexmark-pdf-converter-0.50.50.jar",
    "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-formatter/0.50.50/flexmark-formatter-0.50.50.jar",
    "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-ext-toc/0.50.50/flexmark-ext-toc-0.50.50.jar",
    "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-ext-autolink/0.50.50/flexmark-ext-autolink-0.50.50.jar",
    "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark/0.50.50/flexmark-0.50.50.jar",
    "https://maven.scijava.org/service/local/repositories/central/content/com/vladsch/flexmark/flexmark-ext-tables/0.50.50/flexmark-ext-tables-0.50.50.jar",
    "https://maven.scijava.org/service/local/repositories/releases/content/sc/fiji/imagescience/3.0.0/imagescience-3.0.0.jar",
    "https://maven.scijava.org/service/local/repositories/releases/content/sc/fiji/Image_5D/2.0.2/Image_5D-2.0.2.jar",
    "https://maven.scijava.org/service/local/repositories/sonatype/content/com/fasterxml/jackson/core/jackson-databind/2.7.3/jackson-databind-2.7.3.jar",
    "https://maven.scijava.org/service/local/repositories/sonatype/content/com/fasterxml/jackson/core/jackson-core/2.7.3/jackson-core-2.7.3.jar",
    "https://maven.scijava.org/service/local/repositories/sonatype/content/com/fasterxml/jackson/core/jackson-annotations/2.7.3/jackson-annotations-2.7.3.jar",
    "https://maven.scijava.org/service/local/repositories/central/content/org/jgrapht/jgrapht-core/1.3.1/jgrapht-core-1.3.1.jar",
    "https://maven.scijava.org/service/local/repositories/central/content/org/nibor/autolink/autolink/0.10.0/autolink-0.10.0.jar",
    "https://maven.scijava.org/service/local/repositories/central/content/org/apache/pdfbox/fontbox/2.0.4/fontbox-2.0.4.jar",
    "https://maven.scijava.org/service/local/repositories/central/content/com/openhtmltopdf/openhtmltopdf-jsoup-dom-converter/1.0.0/openhtmltopdf-jsoup-dom-converter-1.0.0.jar",
    "https://maven.scijava.org/service/local/repositories/central/content/com/openhtmltopdf/openhtmltopdf-core/1.0.4/openhtmltopdf-core-1.0.4.jar",
    "https://maven.scijava.org/service/local/repositories/central/content/org/apache/pdfbox/pdfbox/2.0.4/pdfbox-2.0.4.jar",
    "https://maven.scijava.org/service/local/repositories/central/content/com/openhtmltopdf/openhtmltopdf-rtl-support/1.0.4/openhtmltopdf-rtl-support-1.0.4.jar",
    "https://maven.scijava.org/service/local/repositories/central/content/com/openhtmltopdf/openhtmltopdf-pdfbox/1.0.4/openhtmltopdf-pdfbox-1.0.4.jar"
]
jipipe_components = ["core", "clij", "multiparameters", "filesystem", "ij", "ij-algorithms",
                     "ij-multi-template-matching", "python", "plots", "tables", "annotation", "utils", "strings", "launcher"]
jipipe_version = "2020.10"

if not Path(fiji_zip_file).exists():
    print("Downloading Fiji distribution ...")
    urllib.request.urlretrieve(fiji_download_url, fiji_zip_file)

if not Path("dependencies").exists():
    Path("dependencies").mkdir()

for dependency_url in dependency_urls:
    file_name = dependency_url.split("/")[-1]
    target_file = Path("dependencies") / file_name
    if not target_file.exists():
        print("Downloading " + file_name + " ...")
        urllib.request.urlretrieve(dependency_url, target_file)

if Path("Fiji.app").exists():
    print("Deleting old package ...")
    shutil.rmtree(Path("Fiji.app"))

print("Extracting vanilla package ...")
with ZipFile(fiji_zip_file, 'r') as zipObj:
   zipObj.extractall()

print("Merging dependencies ...")

for dependency_url in dependency_urls:
    file_name = dependency_url.split("/")[-1]
    source_file = Path("dependencies") / file_name
    target_file = Path("Fiji.app") / "jars" / file_name
    if target_file.exists():
        continue
    print("Copying " + file_name + " ...")
    shutil.copyfile(source_file, target_file)

print("Installing JIPipe ...")

for component in jipipe_components:
    print("Installing component " + component + " ...")
    source_file = Path("../../") / ("jipipe-" + component) / "target" / ("jipipe-" + component + "-" + jipipe_version + ".jar")
    if not source_file.exists():
        raise Exception("JIPipe component binary '" + component + "' not located at " + str(source_file) + ". Aborting.")
    target_file = Path("Fiji.app") / "plugins" / ("jipipe-" + component + "-" + jipipe_version + ".jar")
    shutil.copyfile(source_file, target_file)

print("Copying JIPipe icon ...")
shutil.copy(Path("../../jipipe-core/src/main/resources/org/hkijena/jipipe/icon.ico"), Path("Fiji.app") / "jipipe-icon.ico")

print("Windows installation is ready.")