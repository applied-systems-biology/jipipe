# JIPipe

A graphical batch processing language for image analysis.

https://www.jipipe.org/

Zoltán Cseresnyés, Ruman Gerst, Marc Thilo Figge

Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge\
https://www.leibniz-hki.de/en/applied-systems-biology.html \
HKI-Center for Systems Biology of Infection\
Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)\
Adolf-Reichwein-Straße 23, 07745 Jena, Germany

The project code is licensed under BSD 2-Clause.\
See the LICENSE file provided with the code for the full license.

[![](https://github.com/applied-systems-biology/jipipe/actions/workflows/build-main.yml/badge.svg)](https://github.com/applied-systems-biology/jipipe/actions/workflows/build-main.yml)

## Project structure

The project consists of following parts:

* **JIPipe-Core** provides all basic functionality, such as the graph model, projects, and the GUI
* **JIPipe-Annotation** provides algorithms and data types for handling annotation data.
* **JIPipe-Filesystem** provides algorithms and data types for handling filesystem data.
* **JIPipe-IJ** integrates ImageJ data types.
* **JIPipe-IJ-Algorithms** integrates ImageJ algorithms.
* **JIPipe-IJ-Multi-Template-Matching** integrates the ImageJ multi-template matching plugin.
* **JIPipe-IJ-OMERO** integrates OMERO.
* **JIPipe-Multiparameters** provides algorithms and data types for handling parameter data. It contains the data source algorithms to define parameters.
* **JIPipe-Plots** provides data types and algorithms for generating plots.
* **JIPipe-Python** provides a Python-scripting node.
* **JIPipe-Tables** provides algorithms and data types to handle table data.
* **JIPipe-Strings** provides algorithms and data types for handling string data.
* **JIPipe-Utils** provides some helpful utility extensions.
* **JIPipe-Forms** provides core functions to allow user interaction during pipeline runs.
* **JIPipe-Cellpose** provides integration of [Cellpose](https://cellpose.org/)
* **JIPipe-Launcher** provides a JAR file that launches JIPipe from outside of ImageJ
* **IJ-Updater-CLI** is an alternative way to trigger ImageJ updates. This is used within the installer tools.

You can use the **JIPipe-Launcher** project to setup a development environment, as this project depends on all
libraries.
Due to internal dependencies, some data types are present in the **JIPipe-Core** library, but not registered into 
the JIPipe runtime.

## Building JIPipe

You will need following packages:

* Java 8 (newer versions do **not** work until supported by SciJava)
* Maven (please make sure Maven runs with Java 8)

### Generate packages

```bash
mvn package
```

You will find the generated \*.jar files in `./*/target` folders. Copy them into the ImageJ `plugins` or `jar` directory.
JIPipe requires some dependency libraries that need to be installer. These dependencies also have to be provided.
This repository comes with automated tools that downloaded dependencies and to create packages that are ready-to-install.

### Generating a package with dependencies

You can find a script `build.sh` in `dist/zip`. It will package the output of an existing `mvn package` run with all 
necessary dependencies, README files, and licenses into a zip file.

This requires Linux or MacOS. On Windows you can install MSYS2, Cygwin, or use WSL. Also ensure that the `zip` utility is installed.

```bash
# Ensure that the project is built
mvn package

# Navigate into the dist folder
cd dist/zip

# Run the package script
./build.sh
```

### Generating installers

This repository comes with scripts to generate installers for Linux, Mac, and Windows. These installers assume that 
you have built JIPipe (`mvn package`).

Following requirements are needed for these installers to work:

* Windows: The installer utilizes [NSIS](https://sourceforge.net/projects/nsis/) and requires some additional libraries (see README in `dist/jipipe-windows-installer`)
* Mac: The installer requires [Platypus](https://sveinbjorn.org/platypus)
* Linux: You need to `mvn package` the `jipipe-installer-linux` project

## Running JIPipe in an IDE

This repository comes with a project `jipipe-launcher` that allows you to run and debug JIPipe inside your IDE.
You just have to run the `main()` function inside this project.

## Generate JavaDocs (Optional)

```bash
mvn javadoc:aggregate
```

The JavaDoc will be put into the `target/site` folder.

## Troubleshooting

### Missing Maven dependencies

Sometimes Maven fails to download certain dependencies if run from CLI. We have experienced that using an IDE 
can resolve this.

### Maven complains about Java version

Only Java 8 is supported. This means that you have to run Maven with Java 8. Other versions will not work.

## Nullpointer exception on launching in IDE

Clean and compile Maven. Then compile inside the IDE, again.