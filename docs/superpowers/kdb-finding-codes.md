# KDB finding codes

The closed set of KDB finding codes (design spec 5.8). A finding reports malformed or
problematic corpus content instead of throwing: the loader and the service collect findings,
and the corpus integrity gate (`KdbCorpusIntegrityTest` in jipipe-launcher) decides what fails
the build. ERROR severity fails the gate; WARNING severity stays visible in the gate's printed
report without failing.

The list is closed and guarded by `KdbFindingCodesTest`: every code must be named in this table
with its severity, so a code may be added, removed, or re-severitied only together with a row
here. The meanings below mirror the Javadoc of `org.hkijena.jipipe.api.knowledge.KdbFindingCode`.

| Code | Severity | Meaning |
|---|---|---|
| `UNKNOWN_KIND` | ERROR | The file cannot be classified into the corpus taxonomy: the kind YAML field or the kind directory segment is unknown, the domain directory segment is unknown, or the path does not match `<domain>/<kind-directory>/<name>.yaml`. The file is skipped. |
| `UNKNOWN_SLOT` | ERROR | The `slots` mapping contains a key that the kind's slot schema does not declare. The unknown value is dropped; the entry still loads. |
| `TYPE_MISMATCH` | ERROR | A value cannot narrow to its declared type: an unparseable YAML file, a non string for a string shaped type, a mapping where a scalar was expected, or a non version string for a version. The defective value is dropped. |
| `CARDINALITY_VIOLATION` | ERROR | A SINGLE slot received a list of values instead of exactly one. The slot's values are dropped. |
| `KIND_PATH_MISMATCH` | ERROR | The `kind` YAML field disagrees with the kind directory segment of the path. The file is skipped because its classification is contradictory. |
| `MISSING_TITLE` | ERROR | The entry has no usable `title` (missing, empty, or not a string). The entry still loads without a title. |
| `DUPLICATE_ENTRY_KEY` | ERROR | Two loaded entries carry the same entry key. The first occurrence is kept, later ones are dropped. |
| `UNRESOLVED_NODE_REF` | ERROR | A NODE_REF value does not resolve against the live node registry. Produced by the registry gates, not by the loader. |
| `UNRESOLVED_ENTRY_REF` | ERROR | An ENTRY_REF value does not resolve to a loaded entry key. A dangling cross-reference is structural breakage: the integrity gate fails the build on it. |
| `PROPERTY_SHADOWS_SLOT` | WARNING | A `properties` key shadows a schema slot of the kind. The property is kept. |
| `EMPTY_CORPUS` | WARNING | The corpus root contains no entry files, does not exist, or could not be walked. |
