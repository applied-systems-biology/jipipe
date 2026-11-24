This directory is a KDB loader fixture: a corpus root that intentionally
contains no .yaml entry files, so loading it must produce an EMPTY_CORPUS
warning finding with zero entries. This placeholder keeps the directory
in git and doubles as a check that non YAML files are ignored.
