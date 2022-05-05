 # JIPipe distribution

 This directory contains various scripts useful for creating JIPipe packages.

 * `online-installer` contains online-installers . They are experimental and currently not in use.
 * `zip` contains files for the distribution as ZIP package
 * `offline-package` contains files with offline (ready-to-use distributions)

## Additional files

* `dist-info.json` contains information used by scripts to know about dependencies and other metadata
* `generate-dist-scripts.py` generates scripts for generating distributions based on dist-info.json

If you want to include a new JIPipe plugin into the standard distribution or add a dependency into the packages, use `dist-info.json`