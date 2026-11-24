# Node usage prior (#1404)

The shipped usage prior ranks commonly used nodes up in the node search.
This directory holds the generator for the committed resource
`jipipe-core/src/main/resources/org/hkijena/jipipe/search/usage-prior.json`.

## Regenerating

The corpus (real-world pipelines) lives outside the repository; the
generator takes its path as an argument. The registry dump restricts the
prior to ids that are registered node types in the current build (ids
from other plugin versions are dropped; canvas notes, comments, groups,
and compartment I/O are real registered node types and stay in the
prior).

```bash
# 1. Dump the live node registry (all plugins)
mvn -q -pl jipipe-launcher -am test -o -Dtest=NodeRegistryDumpToolTest \
    -Djipipe.dump.path=/tmp/opencode/node-registry-dump.json \
    -Dsurefire.failIfNoSpecifiedTests=false

# 2. Mine the corpus and write the resource
python3 utils/search-usage/generate-usage-prior.py \
    --corpus "/path/to/Example workflows" \
    --registry-dump /tmp/opencode/node-registry-dump.json \
    --out jipipe-core/src/main/resources/org/hkijena/jipipe/search/usage-prior.json
```

The output is deterministic (sorted keys, corpus content hash). After
regenerating, `UsagePriorResourceTest` must be updated if the pinned
entries' counts changed, and must stay green.

## Format

```json
{
  "source": "...",
  "generated": "YYYY-MM-DD",
  "corpus": {"files": N, "sha256": "..."},
  "counts": {"<node-id>": {"instances": N, "pipelines": M}}
}
```

`instances` is the ranking unit (times the node type occurs across the
corpus); `pipelines` (distinct projects using the type) is informational.
