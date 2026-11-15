# JIPipe Workflow RO-Crate profile (work item #1360)

**Status:** Approved design · **Date:** 2026-11-15
**Issue:** https://asb-git.hki-jena.de/RGerst/jipipe/-/work_items/1360
**Related:** #753 (JIPX project format, design spec 2026-10-23-jipx-project-format-design.md)

## 1. Motivation

JIPX files are valid RO-Crates that follow the Workflow RO-Crate profile.
Third parties must be able to read, write, and execute JIPipe workflows
without reading JIPipe source code, which requires:

1. Assurance that JIPX containers really follow the Workflow RO-Crate
   spec (automated, not just by construction).
2. A published, semantically versioned "JIPipe Workflow RO-Crate" profile
   spec at a stable permalink that generated files can link to.

This is the follow-up that the JIPX design reserved in its section 6
("the derived profile JIPipe Workflow RO-Crate gets a permalink ...
Publishing the profile page + Profile Crate is a separate work item").

## 2. Current state (facts established during investigation)

- `JIPipeProjectCrate` (jipipe-core, `api/project/jipx/`) builds the
  crate of a JIPX container with the contrib library
  `contrib/jipipe-ro-crate-java-2.1.0` (targets RO-Crate 1.1, the
  compatible base for Workflow RO-Crate 1.0).
- The generated metadata already satisfies every Workflow RO-Crate 1.0
  MUST: zipped container with `ro-crate-metadata.json` at the archive
  root; Metadata File Descriptor `conformsTo` array containing
  `https://w3id.org/ro/crate/1.1` and
  `https://w3id.org/workflowhub/workflow-ro-crate/1.0`; a Main Workflow
  data entity `project/graph.json` typed
  `["File", "SoftwareSourceCode", "ComputationalWorkflow"]` referenced
  via root `mainEntity`; `programmingLanguage` referencing a
  `ComputerLanguage` contextual entity `https://jipipe.org/jipipe`
  (name "JIPipe", version = writing JIPipe version); a root `license`
  (metadata license, else settings default, else MIT).
- The root `hasPart` currently lists exactly two data entities
  (`project/graph.json`, `README.md`); `jipx.json` and the other part
  files are not listed.
- The JIPX save-time compliance checklist
  (`JIPipeProjectCrate.checkCompliance`) covers license, mainEntity,
  README, programmingLanguage, Bioschemas conformsTo.
- The publish-time `.crate.zip` flow (`CreateROCrateRun`,
  `plugins/publish/rocrate/`) builds a different flavor (CWL wrapper as
  Main Workflow, no `jipx.json`). Out of scope for the new profile
  (decision below).
- The website (`RGerst/jipipe-cms-website`, Spring Boot + Thymeleaf,
  `layout :: base(...)` decorator, `@WebMvcTest` controller tests) has
  no spec pages today.
- `jipipe.org` (CNAME `www.jipipe.org`) is served by GitHub Pages from
  the `applied-systems-biology/jipipe` `gh-pages` branch (local clone:
  `/fastdata/src/jipipe-github`). That branch redirects legacy paths to
  the CMS with meta-refresh pages (pattern: `download/index.html`),
  and `404.html` redirects to the site root. GitHub Pages cannot emit
  HTTP 301/302; meta-refresh HTML is the established pattern.
- The live Workflow RO-Crate profile is 1.1; version 1.0 remains
  published and is the compatible profile for RO-Crate 1.1 crates
  (official compatibility table). JIPX claims 1.0 and stays there in
  this work item.

## 3. Profile identity

| | |
|---|---|
| Title | JIPipe Workflow RO-Crate profile 1.0 |
| Permalink (this version) | `https://jipipe.org/ro-crate/jipipe-workflow/1.0` |
| Permalink (latest) | `https://jipipe.org/ro-crate/jipipe-workflow/` |
| Version | 1.0.0 (semantic versioning; the permalink carries major.minor, the page and Profile Crate state the full version) |
| Publisher | Leibniz-Institut für Naturstoff-Forschung und Infektionsbiologie e. V. - Hans-Knöll-Institut (HKI), ROR `https://ror.org/055s37c97`, website `https://www.leibniz-hki.de/` |
| Spec license | CC-BY-4.0 (spec page and Profile Crate; distinct from code licensing) |
| Profile Crate | served from `https://jipipe.hki-jena.de/ro-crate/jipipe-workflow/1.0/ro-crate-metadata.json`, linked from the spec page |

- The canonical URIs live on `jipipe.org` (the project's own domain,
  served from gh-pages) so they survive CMS hosting changes. This
  restores the URIs the JIPX design originally reserved.
- Content lives on the CMS host (`jipipe.hki-jena.de`); `jipipe.org`
  deep links meta-refresh to it. This mirrors the reference setup
  (Workflow RO-Crate: w3id.org permalink, content at
  about.workflowhub.eu).
- No `ro-crate-metadata.json` path is advertised under `jipipe.org`:
  a meta-refresh page would shadow machine fetches, so the Profile
  Crate is only served (and linked) on the CMS host.

## 4. Normative requirements (profile 1.0)

A JIPX container conforms to the JIPipe Workflow RO-Crate profile 1.0
when it satisfies all of the following. MUST/SHOULD per RFC 2119.

1. **Base.** The container MUST be a valid Workflow RO-Crate 1.0 (hence
   a valid RO-Crate 1.1), zipped, with `ro-crate-metadata.json` at the
   archive root. (Inherited; restated for readability.)
2. **Profile declaration.** The Metadata File Descriptor `conformsTo`
   MUST be an array containing `https://w3id.org/ro/crate/1.1`,
   `https://w3id.org/workflowhub/workflow-ro-crate/1.0`, and
   `https://jipipe.org/ro-crate/jipipe-workflow/1.0`. The Root Data
   Entity `conformsTo` SHOULD additionally contain the workflow profile
   and the JIPipe profile (modern tooling discovers profiles there;
   JIPipe writes both).
3. **Main workflow.** The root `mainEntity` MUST reference
   `project/graph.json`, a data entity typed
   `["File", "SoftwareSourceCode", "ComputationalWorkflow"]` with
   `encodingFormat` `application/json`. This file is the executable
   workflow a third party reads or writes.
4. **Workflow language.** The Main Workflow's `programmingLanguage`
   MUST reference a `ComputerLanguage` contextual entity with
   `@id https://jipipe.org/jipipe`, `name` "JIPipe", and `version` set
   to the JIPipe version that wrote the container. The entity SHOULD
   carry `url` `https://jipipe.hki-jena.de/jipipe`.
5. **Manifest.** The container MUST contain `jipx.json` with:
   `format-version` (integer, 1 in this version), `jipipe-version`
   (string), `main-graph` (zip entry path of the Main Workflow, here
   `project/graph.json`), and a parts table mapping zip entry paths to
   part keys. Part files are JSON documents (the split top-level fields
   of the JIPipe project format); a reader resolves the Main Workflow
   and parts through the manifest. Writers MUST preserve unknown
   manifest entries and unknown container files verbatim.
6. **README.** The container MUST contain a root `README.md` (JIPipe
   always generates it; tighter than the base profile's SHOULD). It
   SHOULD be `about` `./` with `encodingFormat` `text/markdown`.
7. **Container inventory.** The manifest and every part file SHOULD be
   listed as data entities referenced from the root `hasPart` (JIPipe
   lists them all; the SHOULD leaves room for exempting large payload
   entries in later versions).
8. **Reserved payload slots.** The directories `data/`, `graphs/`, and
   `history/` MAY appear as payload (Dataset entities in `hasPart` when
   present). They are reserved extension points for later profile
   versions and carry no defined meaning in 1.0.

### Example (abbreviated)

```json
{
  "@context": "https://w3id.org/ro/crate/1.1/context",
  "@graph": [
    {
      "@id": "ro-crate-metadata.json",
      "@type": "CreativeWork",
      "about": { "@id": "./" },
      "conformsTo": [
        { "@id": "https://w3id.org/ro/crate/1.1" },
        { "@id": "https://w3id.org/workflowhub/workflow-ro-crate/1.0" },
        { "@id": "https://jipipe.org/ro-crate/jipipe-workflow/1.0" }
      ]
    },
    {
      "@id": "./",
      "@type": "Dataset",
      "name": "My JIPipe project",
      "description": "...",
      "datePublished": "2026-11-15T12:00:00",
      "license": { "@id": "https://spdx.org/licenses/MIT" },
      "conformsTo": [
        { "@id": "https://w3id.org/workflowhub/workflow-ro-crate/1.0" },
        { "@id": "https://jipipe.org/ro-crate/jipipe-workflow/1.0" }
      ],
      "mainEntity": { "@id": "project/graph.json" },
      "hasPart": [
        { "@id": "project/graph.json" },
        { "@id": "jipx.json" },
        { "@id": "README.md" }
      ]
    },
    {
      "@id": "project/graph.json",
      "@type": ["File", "SoftwareSourceCode", "ComputationalWorkflow"],
      "programmingLanguage": { "@id": "https://jipipe.org/jipipe" },
      "conformsTo": { "@id": "https://bioschemas.org/profiles/ComputationalWorkflow/1.0-RELEASE" },
      "encodingFormat": "application/json",
      "name": "JIPipe project graph"
    },
    {
      "@id": "jipx.json",
      "@type": "File",
      "encodingFormat": "application/json",
      "name": "JIPipe project manifest"
    },
    {
      "@id": "README.md",
      "@type": "File",
      "encodingFormat": "text/markdown",
      "about": { "@id": "./" }
    },
    {
      "@id": "https://jipipe.org/jipipe",
      "@type": "ComputerLanguage",
      "name": "JIPipe",
      "version": "6.0.0",
      "url": { "@id": "https://jipipe.hki-jena.de/jipipe" }
    }
  ]
}
```

## 5. Spec page and Profile Crate (jipipe-cms-website)

Branch: `1360-ro-crate-spec-page` (from master).

- New controller `RoCrateSpecController` (style of the existing
  `PagesController`):
    - `GET /ro-crate/jipipe-workflow/1.0` (+ trailing slash) renders
      the spec page.
    - `GET /ro-crate/jipipe-workflow` and `/ro-crate/jipipe-workflow/`
      redirect to the current version page (the "latest" permalink).
    - `GET /jipipe` renders a small language page: name, one-paragraph
      description, link to the site home. Target of the
      `ComputerLanguage` entity id (via the gh-pages redirect).
- Template `ro_crate_spec.html` (existing `layout :: base(...)`
  decorator): metadata header (permalinks, version 1.0.0, publisher HKI
  with ROR and website links, spec license CC-BY-4.0), abstract, the
  normative requirements 1-8, the example `ro-crate-metadata.json`, a
  link to the Profile Crate, the versioning policy, and the reserved
  slots section. Hand-maintained HTML, no CMS/Squidex dependency. Site
  navigation unchanged in 1.0.
- Profile Crate as a static resource
  `src/main/resources/static/ro-crate/jipipe-workflow/1.0/ro-crate-metadata.json`,
  mirroring the Workflow RO-Crate 1.0 Profile Crate structure:
    - Descriptor `conformsTo`: RO-Crate 1.1 + the RO-Crate profiles
      draft guide.
    - Root Dataset `./`: `name`, `description`, `version` "1.0.0",
      `identifier`/`sameAs` = `https://jipipe.org/ro-crate/jipipe-workflow/1.0`,
      `datePublished`, `license` CC-BY-4.0, `publisher` -> HKI
      `Organization` entity (`@id https://ror.org/055s37c97`, name,
      `url https://www.leibniz-hki.de/`), `author` -> `Person` (Ruman
      Gerst, `@id https://orcid.org/0000-0002-0723-6038`).
    - A `Guide` entity whose `@id` is the version permalink with
      `hasPart` -> `Recommendation` entities (one per requirement,
      `category` MUST/SHOULD, `reviewBody` = normative text,
      `itemReviewed` -> `DefinedTerm` entities).
    - Contextual entities for RO-Crate 1.1, workflow-ro-crate/1.0, the
      Bioschemas ComputationalWorkflow profile, and the JIPipe
      `ComputerLanguage`.
    - No `ro-crate-preview.html` in 1.0.
- Versioning practice: a git tag `jipipe-workflow-ro-crate-1.0.0` per
  release; old version pages stay published; the "latest" URL always
  points at the current version.

## 6. jipipe.org redirects (applied-systems-biology/jipipe, gh-pages)

Branch off `gh-pages`; three meta-refresh pages following the existing
`download/index.html` pattern:

| gh-pages path | Refresh target |
|---|---|
| `ro-crate/jipipe-workflow/1.0/index.html` | `https://jipipe.hki-jena.de/ro-crate/jipipe-workflow/1.0` |
| `ro-crate/jipipe-workflow/index.html` | `https://jipipe.hki-jena.de/ro-crate/jipipe-workflow/1.0` |
| `jipipe/index.html` | `https://jipipe.hki-jena.de/jipipe` |

Pushing gh-pages requires explicit user confirmation.

## 7. jipipe code changes

Branch: `1360-jipipe-workflow-ro-crate-profile` (from master). All in
jipipe-core `api/project/jipx/` unless noted.

`JIPipeProjectCrate`:

- Extract the inline workflow-ro-crate URI into
  `WORKFLOW_RO_CRATE_PROFILE_URI`; add
  `JIPIPE_WORKFLOW_RO_CRATE_PROFILE_URI =
  "https://jipipe.org/ro-crate/jipipe-workflow/1.0"`. Stable string
  literals.
- `LANGUAGE_ENTITY_ID` stays `https://jipipe.org/jipipe`; its javadoc
  loses the "placeholder" caveat (the language page becomes real).
- Descriptor `conformsTo` array grows to the three URIs (requirement 2).
- Root data entity gains
  `conformsTo: [workflow-ro-crate/1.0, jipipe-workflow/1.0]`.
- New `File` data entities for `jipx.json` (name "JIPipe project
  manifest", `encodingFormat` `application/json`) and every part file
  (paths from `JIPipeProjectParts`), so the root `hasPart` inventories
  the whole container (requirement 7). The library auto-wires
  `hasPart` on `addDataEntity`.
- The language entity builder adds `url` (requirement 4 SHOULD).
- `checkCompliance` adds the MUST-level check: descriptor contains the
  JIPipe profile URI.

`JIPipeXProjectWriter`: unchanged structurally (it already writes all
listed files; the new entities are metadata-only).

## 8. Testing strategy

- jipipe: new `JIPipeProjectCrateConformanceTest` (jipipe-launcher,
  existing jipx test pattern with `JIPipe.ensureInstance()`): writes a
  JIPX container, parses its `ro-crate-metadata.json`, and asserts the
  full conformance table:
    - Workflow RO-Crate 1.0 MUSTs: descriptor `conformsTo` pair, Main
      Workflow types, `mainEntity`, `programmingLanguage`, `license`.
    - JIPipe profile MUSTs: third descriptor URI, `mainEntity` =
      `project/graph.json`, language entity id/name/version,
      `jipx.json` presence and manifest fields, `README.md`.
    - Emitted SHOULDs: root `conformsTo` pair, full `hasPart`
      inventory, language `url`, Bioschemas `conformsTo`.
- jipipe: `JIPipeXProjectWriterTest` expectations updated (third
  descriptor URI, new `hasPart` members; the language id assertion
  stays as is).
- Website: `RoCrateSpecControllerTest` in the existing `@WebMvcTest`
  style: versioned route renders, latest route redirects, unknown
  version 404, Profile Crate JSON served.
- Round-trip/corpus tests are unaffected (they compare project JSON,
  not crate metadata); confirmed during implementation.
- The Profile Crate is checked once with an external RO-Crate validator
  (`rocrate` Python tool) as a local sanity check, not wired into CI.
- No Swing-structure tests (project policy); the spec page is verified
  by eye in the running app.

## 9. Versioning and extension policy (published on the spec page)

- Semantic versioning. MAJOR: changes that break readers or writers of
  existing containers. MINOR: new optional features or relaxations.
  PATCH: editorial changes.
- Version permalinks carry major.minor (`.../1.0`); the page and
  Profile Crate state the full version (1.0.0). Old version pages
  remain published unchanged.
- Reserved extension points: the `data/`, `graphs/`, `history/` slots,
  unknown part files, and unknown manifest entries.

## 10. Out of scope (follow-up work)

- Announcing the spec (news entry, resources-page link).
- FormalParameter / `input` / `output` descriptions (future MINOR).
- The publish-time `.crate.zip` flow declaring the profile (rejected in
  scoping: the JIPX-specific requirements do not apply to it).
- Profile Crate preview HTML.
- HTTP-level (301/302) redirects on jipipe.org (GitHub Pages cannot;
  meta-refresh is the accepted pattern).
