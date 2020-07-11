# JIPipe

A graphical batch processing language for image analysis.

https://applied-systems-biology.github.io/jipipe

Zoltán Cseresnyés, Ruman Gerst

Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge\
https://www.leibniz-hki.de/en/applied-systems-biology.html\
HKI-Center for Systems Biology of Infection\
Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)\
Adolf-Reichwein-Straße 23, 07745 Jena, Germany\

The project code is licensed under BSD 2-Clause.\
See the LICENSE file provided with the code for the full license.

## Project structure

The project consists of following parts:

* **JIPipe-Core** provides all basic functionality, such as the graph model, projects, and the GUI
* **JIPipe-Annotation** provides algorithms and data types for handling annotation data.
* **JIPipe-Filesystem** provides algorithms and data types for handling filesystem data.
* **JIPipe-IJ** integrates ImageJ data types and algorithms.
* **JIPipe-Multiparameters** provides algorithms and data types for handling parameter data. It contains the data source algorithms to define parameters.
* **JIPipe-Plots** provides data types and algorithms for generating plots.
* **JIPipe-Python** provides a Python-scripting node.
* **JIPipe-Tables** provides algorithms and data types to handle table data.

You can use the **JIPipe-Debug** project to setup a development environment, as this project depends on all
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

You will find the generated \*.jar files in `./*/target` folders.

### Generate JavaDocs

```bash
mvn javadoc:aggregate
```

The JavaDoc will be put into the `target/site` folder.