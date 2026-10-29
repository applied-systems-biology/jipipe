# Node Pattern Rendering (Phase 2, revised) — Design

**Date:** 2026-10-20 **Work item:** 1324 (node taxonomy & design)
**Supersedes:** the "Node rendering (Phase 2)" section of
[`2026-10-08-node-taxonomy-redesign-design.md`](2026-10-08-node-taxonomy-redesign-design.md)
(DiceBear port → Java2D formula engine). The Phase 1 data model is unchanged:
`JIPipeNodeTaxonomyFramework.getPatternStyleId()` remains the per-framework
style selection, now pointing at our own style ids.

## Problem

Canvas nodes are currently identified visually by **function hue alone**
(`ThemeUtils.getNodeFillColor`). Two nodes from the same framework (e.g. two
different TrackMate steps in different function branches) look unrelated, and
a framework-heavy pipeline (a TrackMate setup spanning Tracking, Images and
Tables) has no visual thread tying the nodes together.

Phase 2 of the taxonomy redesign was originally specced as a **port of the
DiceBear identicon engine** (contrib module + SVG + Batik rasterization).
Evaluation of the real DiceBear 9.x/10.x output rejected that path:

- Several styles are **centered motifs, not plane-fillers** (`constellation`,
  `blobs`) — they do not extend beyond the node body, which the user flagged
  explicitly ("not infinitely expanding").
- Every style **bakes in its own palette**; making them color-neutral requires
  stripping and recoloring the generated SVG.
- The library **lacks a real organic style** (the user rejected the figurative
  ones, e.g. bigSmile/sprouts).
- The SVG engine + Batik dependency + contrib module + dist packaging is the
  heaviest part of the original plan, for a feature that is ultimately
  "a monochrome tile drawn behind the node text".

The approved pivot: a small **Java2D formula engine** — each style is a pure
`paint(Graphics2D, w, h, Random)` formula (Vanta/KDE-structural inspiration:
isocurves, voronoi membranes, kNN nets, hex tiling, wavy strata, space-filling
curves, recursive subdivision). Color-neutrality and plane-filling are then **properties of the pipeline, not
post-processing**, and are testable.

## Goals

1. Every algorithm node on a framework (Cellpose, TrackMate, cLij, ImageJ,
   OpenCV, …) carries that framework's **signature pattern** as a low-opacity
   background over the whole node body — recognizable at a glance and at
   zoomed-out distance (minimap reuses it).
2. Patterns are **color-neutral** (a white alpha mask) so the theme can recolor
   them freely; the function hue stays the node's primary color.
3. Patterns **fill the whole node body** edge-to-edge (plane-filling); a
   centered motif is a defect, and a test enforces it.
4. Same framework ⇒ identical pattern on every node of it, on every run and
   every machine (seed = framework id).
5. A **badge** (rounded chip with the framework abbreviation, framework hash
   color, tooltip = full framework name) sits in the node title band.
6. All of it is toggleable in the theme editor; all-off ⇒ exactly today's
   look.
7. Zero new dependencies. Everything lives in `jipipe-core`.

## Non-goals

- Animation (the Vanta source material is animated; we render static tiles).
- Per-node pattern variation (seed is the framework, not the node).
- User-editable pattern parameters (per-framework seed salts, tuned at ship
  time if one framework's realization looks unlucky).
- Patterns for non-algorithm objects (compartments, slots, input/output
  markers).
- The DiceBear engine port in any form.

## Architecture

### Package: `org.hkijena.jipipe.desktop.commons.nodepatterns` (jipipe-core)

```java
/** One pattern style: white ink on transparent, antialiased, plane-filling. */
public interface NodePatternFamily {
    void paint(Graphics2D g, int w, int h, Random rng);
}
```

Contract for every family (enforced by tests, §Testing):

- draws only with the current paint (white) onto a transparent canvas;
- reaches all four borders (overshoots the frame where the geometry is
  grid-based — one row/col of elements past each edge — so the motif is
  clipped by the node body exactly like an infinitely continuing texture);
- is a pure function of `(w, h, rng)` — no wall clock, no `Math.random`.

```java
/** Registry of the bundled styles. Ids are stable string literals. */
public final class NodePatternStyles {
    public static NodePatternFamily family(String id);   // null for unknown
    public static Collection<String> ids();
}
```

```java
/** Renders + caches color-neutral mask tiles and paints them onto nodes. */
public final class NodePatternRenderer {
    /** White-on-transparent alpha mask for (style, seed, size bucket). Cached. */
    public static BufferedImage mask(String styleId, String seed, int width, int height);

    /** Paints the mask, tinted with `ink` at `opacity`, into (x, y, w, h). */
    public static void paint(Graphics2D g, int x, int y, int w, int h,
                             String styleId, String seed, Color ink, float opacity);

    public static void invalidateCache(); // on theme/framework re-registration
}
```

- **Mask caching** is keyed by `(styleId, seed, width/8, height/8)` — node
  sizes cluster, so a handful of tiles per (style, framework) serve all zoom
  levels; larger nodes scale the tile up via `drawImage` (a low-opacity
  texture tolerates the interpolation). Mask cap 512×512.
- The mask is **theme-independent** by construction (white ink) — changing
  ink color/opacity only changes the draw call, never the cache.
- **Seed:** `new Random(seed.hashCode())` with `seed` = the framework id (`String.hashCode()` is JLS-specified, `Random`
  's LCG is fixed ⇒
  deterministic across JVMs and machines). Every node of a framework shares
  one signature; families never seed themselves.

### Style catalog (17 families, 12 mapped)

| Id          | Formula (from the validated prototype)                                                            | Mapped to        |
|-------------|---------------------------------------------------------------------------------------------------|------------------|
| `blobs`     | scattered smooth closed blobs (12-point quadratic splines), centers may overshoot the frame       | Cellpose2/3/4    |
| `net`       | jittered-grid kNN graph (k=3, wrap-tolerant), nodes + edges                                       | TrackMate, Comet |
| `honeycomb` | wobbly hexagonal tiling + sparse cell dots (KDE *Canopee/Flow*)                                   | cLij             |
| `triangles` | jittered-grid triangulation mesh (edges)                                                          | ImageJ1, ImageJ2 |
| `waves`     | isocurves of a 3-term periodic sine field (marching squares)                                      | JFreeChart       |
| `patchwork` | recursive BSP subdivision, alternating fill/outline                                               | Core             |
| `weave`     | over/under rounded bands                                                                          | OpenCV           |
| `stripes`   | wavy vertical bands; frame-adjacent bands straight (border guarantee)                             | Python           |
| `cellular`  | voronoi membranes as smooth wobbly closed outlines + border sites (KDE *Cluster* / Vanta *cells*) | Omnipose         |
| `dots`      | dot grid, radius modulated by a periodic field, centers on the border grid                        | Ilastik          |
| `landscape` | stacked wavy strata drawn as **bands between consecutive curves** (no alpha stacking)             | R                |
| `carpet`    | Sierpinski carpet (space-filling fractal)                                                         | JIPipeDL         |
| `loops`     | open arc loops on a jittered grid — *spare*                                                       | —                |
| `disco`     | confetti polygons (3–5 sides) on a jittered grid — *spare*                                        | —                |
| `hilbert`   | space-filling Hilbert curve, order 6 (canonical inside-out `d2xy`) — *spare*                      | —                |
| `ripple`    | radial fan + concentric circles from an interior point (Vanta *ripple*) — *spare*                 | —                |
| `flow`      | flow-field line segments (Vanta *topology*) — *spare*                                             | —                |

Replaced/removed vs. the previous `getPatternStyleId()` values:
`constellation` (sparse, rejected) → `net`; `shape-grid` → `waves`/`carpet`;
`initials` → `patchwork`/`stripes`/`landscape`. `weave`/`triangles`/`blobs`
keep their ids. **`NoFramework.getPatternStyleId()` returns `null`** ⇒ no
pattern, no badge (deliberate behavior change: framework-less nodes stay
plain).

The 17 families are 17 small named classes (`BlobsFamily`, `NetFamily`, …) ported from the validated prototype
(`PatternForge`, kept as the visual-evidence artifact, not shipped).

### Framework mapping (approved 2026-10-20)

| Frameworks       | Style     |
|------------------|-----------|
| Cellpose2/3/4    | blobs     |
| TrackMate, Comet | net       |
| cLij             | honeycomb |
| ImageJ1, ImageJ2 | triangles |
| JFreeChart       | waves     |
| Core             | patchwork |
| OpenCV           | weave     |
| Python           | stripes   |
| Omnipose         | cellular  |
| Ilastik          | dots      |
| R                | landscape |
| JIPipeDL         | carpet    |

Sharing a style across versions (Cellpose 2/3/4 → one silhouette) and across
related trackers (TrackMate/Comet) is the point: the pattern is a family
signature.

### Node UI integration (`JIPipeDesktopGraphNodeUI`)

`paintNode`, after the body fill and **before** the disabled/pass-through
hatch (the hatch therefore covers the pattern — the spec'd "suppressed where
hatching paints" falls out of the paint order):

```java
JIPipeNodeTaxonomyFramework fw = node.getInfo().getFramework();
if (style.isShowNodeFrameworkPattern() && fw != null
        && !NoFramework.ID.equals(fw.getId())) {
    NodePatternRenderer.paint(g, 0, 0, getWidth(), getHeight(),
            fw.getPatternStyleId(), fw.getId(),
            style.getNodePatternInk(), style.getNodePatternOpacity());
}
```

- **Badge** (`paintNodeControls`, after the node name): rounded chip,
  abbreviation text, fill = `fw.getColor()` (the existing 12-color hash
  palette), border in the node border color. The title band's centering
  math (`centerNativeWidth`) gains the chip width when the badge is shown.
- **Tooltip:** when the badge is shown, the node tooltip gains a
  `Framework: <name>` line.
- **Minimap** (`paintMinimap`): the same mask scaled to the minimap rect at
  the theme opacity (framework recognizable zoomed out).
- The `nodeBuffer` caching path is unchanged (it caches the painted node;
  the mask cache is upstream of it).

### Theme settings

`JIPipeDesktopModernThemeStyle` (persisted theme document, Jackson — new
fields deserialize with defaults, old theme files unaffected):

| Setting                    | Default                                                                                     |
|----------------------------|---------------------------------------------------------------------------------------------|
| `showNodeFrameworkPattern` | `true`                                                                                      |
| `nodePatternInk`           | white                                                                                       |
| `nodePatternOpacity`       | `0.4f` (live-tuned for the light base theme; the dark bundled themes pin `0.15` explicitly) |
| `showNodeFrameworkBadge`   | `true`                                                                                      |

Theme editor: a "Node patterns" group in the node section (checkboxes +
opacity slider + color picker). All off ⇒ today's look, per goal 6.

## Error handling & edge cases

- Unknown/`null` style id (a framework registered with a style we don't have)
  ⇒ no pattern, no badge, no exception (fail quiet; the registry test catches
  this for shipped frameworks).
- `NoFramework` ⇒ no pattern, no badge.
- Node smaller than one mask bucket ⇒ smallest bucket scaled down.
- Pattern paint cost: one cached `drawImage` per node paint — the issue's
  caching/performance requirement.

## Testing (per the "do not test trivial things" policy — 4 contracts)

All tests headless (`BufferedImage` only, no Swing interaction; no display
needed), in `jipipe-core/src/test/.../nodepatterns/`
(`NodePatternEngineTest`, no JIPipe boot required):

1. **Determinism** — `mask(style, seed, size)` is byte-identical across two
   independent renderer invocations (fresh `Random` from the same seed), for
   all 17 styles at 3 sizes. Plus one **golden mask** (a small PNG checked in
   as a test resource, seed `jipipe:framework:trackmate`, style `net`,
   128×128) pinned pixel-exactly — the byte-stable idempotency-pin pattern
   the generators already use.
2. **Plane-filling** — for every family, at 3 sizes × 2 seeds: ink coverage
   in the 6px band of **all four** borders ≥ 4%. This is the regression guard
   for the centered-motif defects (constellation/sierpinski/koch class).
3. **Color-neutral** — for every family at one size: every non-transparent
   mask pixel has white color channels (`r==g==b==255`; alpha may vary —
   some styles use translucent fills). Recolorability is structural: the
   mask can be drawn with any tint via `AlphaComposite`.
4. **Registry** — every framework in `JIPipe.getNodes().getFrameworks().all()`
   except `NoFramework` resolves to a non-null style id that `NodePatternStyles`
   knows; `NoFramework` returns null; all 15 mapped framework→style pairs from
   the table above hold (the mapping is the ship contract).

Visual appearance (subtlety, per-hue legibility) is **not** tested — it is
reviewed in the app, per the standing test policy.

## Migration & compatibility

- No project-file impact: the pattern is presentation metadata rebuilt from
  the registry at runtime; saved projects persist node-type ids only.
- `FrameworksTest`'s `getPatternStyleId()` pins update to the new ids (test
  code only).
- Legacy themes without the new fields load with the defaults above.

## Branching & commits

Continue on **`1324-node-taxonomy-core`** (MR !168) — the original plan's
"one branch per phase" is dropped for practicality while the whole 1324
feature set lands in a single MR. (Confirm at spec review.)

Suggested commit shape:

1. `feat: node pattern engine (families, registry, mask renderer, theme settings)`
2. `feat: paint framework pattern + badge on canvas nodes, minimap reuse`
3. `test: pattern contracts (determinism, plane-filling, color-neutral, registry)`
4. `docs: mark the DiceBear Phase-2 section of the taxonomy spec superseded`

## Risks & mitigations

| Risk                                                                                 | Mitigation                                                                                                                                                                                                                       |
|--------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| A family looks bad at some node size/zoom                                            | Mask buckets + `drawImage` scaling; the family formulas are size-parametric (grid counts derive from `w`/`h`), re-tune parameters in one class                                                                                   |
| `String.hashCode()` seed distribution gives an unlucky realization for one framework | Per-framework seed salt (one-line constant) — deterministic, still stable                                                                                                                                                        |
| Pattern hurts node text legibility                                                   | Default opacity 0.4 on light / 0.15 on dark (live-tuned via the theme editor preview; pinned per theme, see `NodePatternThemeSettingsTest.shippedThemesPinPatternOpacity`); theme slider; the title band paints over the pattern |
| 17 families is too much code to maintain                                             | ~40–80 lines each, pure Java2D, no shared mutable state; the 4 contract tests keep them honest without per-pixel test bloat                                                                                                      |

## Decisions log (this design round, 2026-10-20)

1. **Java2D formula engine** instead of the DiceBear port (user-approved).
2. **Style set:** the user's 10 (blobs, disco, loops, patchwork, stripes,
   triangles, waves, weave, constellation, landscape) minus
   `constellation` (rejected: not plane-filling), plus formula-derived
   `net`, `cellular`, `honeycomb`, `ripple`, `carpet`, `hilbert`, `flow`,
   `dots`, `blobs` (re-implementation), `landscape` (re-implementation).
   `sprouts`/`bigSmile`/`glass`/`identicon`/`shape-grid`/`initials` rejected.
3. **Color-neutral by construction** (white alpha mask, tint at draw time).
4. **Plane-filling is a tested contract** (border-band ink coverage).
5. **Organic gap** covered by `cellular` (voronoi membranes) + `blobs`.
6. **Fractal sources** covered by `carpet` (Sierpinski carpet) + `hilbert`
   (space-filling curve); bounded-motif fractals (Sierpinski *triangle*, Koch *snowflake*) excluded for the same reason
   as `constellation`.
7. **Mapping table** as above — approved.
8. **Badge included** in this phase — approved.
9. **No contrib module, no Batik, no dist changes** — consequence of (1).
10. **`NoFramework` loses its pattern** (null style id) — deliberate.
