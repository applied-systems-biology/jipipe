The packaged KDB corpus root.

Entry files live at <domain>/<kind-directory>/<entry-name>.yaml below this
directory (design spec 5.1). This root ships empty until the corpus content
batches land: the integrity gate reports an empty corpus as the EMPTY_CORPUS
warning, which stays visible without failing the build. Non YAML files in
this tree (this README included) are ignored by the loader.
