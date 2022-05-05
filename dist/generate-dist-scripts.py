#!/usr/bin/env python3

import json
import os

with open("dist-info.json", "r") as f:
    json_data = json.load(f)

jipipe_version = json_data["jipipe-version"]

# Generate ZIP shell script
print("Generating ZIP script")
with open("zip/build.sh", "w") as f:
    def wl(text="", tab=0):
        f.write(tab * "\t" + text + "\n")
    wl("#!/bin/bash")
    wl()
    wl("JIPIPE_VERSION=" + jipipe_version)
    wl()

    # Download dependencies
    for (name, url) in json_data["dependencies"].items():
        wl('if [ ! -e "./dependencies/' + name + '" ]; then')
        rel_path = "./dependencies/" + name 
        re_dir = os.path.dirname(rel_path)
        wl("mkdir -p " + '"' + re_dir + '"', tab=1)
        wl("wget -O ./dependencies/" + name + " " + url, tab=1)
        wl("fi")

    # Cleaning up old package
    wl()
    wl("rm -r package")
    wl("mkdir package")

    # Copy component jars
    wl("for component in " + " ".join(json_data["jipipe-modules"]) + "; do")
    wl("cp -v ../../$component/target/$component-$JIPIPE_VERSION.jar package", tab=1)
    wl("done")
    wl()

    # Copy dependencies
    wl("cp -rv ./dependencies ./package/dependencies")

    # Copy other things
    wl("cp -v README.txt package")
    wl("cp -v ../../LICENSE package/LICENSE_JIPipe.txt")

    # ZIP package
    wl("rm -r JIPipe-$JIPIPE_VERSION.zip")
    wl("pushd package")
    wl("zip -r ../JIPipe-$JIPIPE_VERSION.zip .", tab=1)
    wl("popd")
    

