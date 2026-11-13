# Investigation 1361: UI hang persists after logging overhaul

Status: root cause identified and measured (2026-11-13).

## Symptom (report)

Large workflow, no ROI-overlap node. UI lags, then hangs fully, recovers when the run
finishes. Reproduced on Windows 11 (and here on Linux) with commit `3d3320b6`.

## Root cause

`IJLogToJIPipeProgressInfoPump` (jipipe-core, `utils/IJLogToJIPipeProgressInfoPump.java`)
runs a **250 ms Swing timer on the EDT** while ImageJ1 code executes. Each tick calls
`ij.text.TextPanel.getText()`, which rebuilds the **entire** ImageJ log as one `String`
(ImageJ1 never truncates it), then `substring`s the new part and `split`s it.

Cost per tick therefore grows **linearly with the total ImageJ log volume of the run**,
while the timer keeps firing every 250 ms. Every ImageJ1 plugin / macro that emits
`IJ.log()` output drives the EDT into saturation: ticks queue up behind each other, and
queued application events (mouse, paint, clicks) wait behind them - exactly the
"buttons dead, recovers when the run stops" behaviour.

The pump is created **per algorithm execution** (`try (IJLogToJIPipeProgressInfoPump ...)`),
i.e. for iterating nodes once per data row, and one pump exists per executing node.
Each pump keeps its own timer, so **N concurrent pumps multiply the per-tick cost**.

Note this is independent of the progress system overhaul (#1352). The overhaul fixed the
JIPipe-side per-record event flood (#1349), but the ImageJ log pump path was not part of it.

## Measurements

### Microbenchmark (real `ij.text.TextPanel` + real `IJ.log()`, pump logic copied 1:1)

Producer at 30k lines/s (140-char lines):

| run time | accumulated ImageJ log | avg tick (EDT) | max tick | EDT busy, 1 pump |
|---|---|---|---|---|
| 15 s | 64 MB | 24 ms | 56 ms | 9.6% |
| 30 s | 129 MB | 61 ms | 112 ms | 24.5% |
| 45 s | 195 MB | 93 ms | 140 ms | 36.7% |
| 60 s | 260 MB | 127 ms | 346 ms | 49.8% |
| 76 s | 326 MB | 160 ms | 452 ms | 58.6% |

4 concurrent pumps, 15k lines/s: 15.3% -> 40.4% -> 66.2% -> **94.0%** EDT busy at 130 MB
(ticks already queueing: 49 of 60 per window).

Linear regression on the single-pump data: ~0.49 ms of EDT time per MB of log per tick;
saturation (tick = 250 ms) is reached at roughly **500 MB** of ImageJ log text for one pump,
or ~130 MB with four pumps.

### In-app reproduction (JFR + thread dumps)

Synthetic workflow: `Generate table rows from expressions` (60 rows) -> `Split table into rows`
-> two `Run ImageJ Macro` nodes (per-row, 30,000 `print()` calls each, import/export delay 0).

- 3/3 thread dumps during the run: `AWT-EventQueue-0` inside
  `ij.text.TextPanel.getText` <- `IJLogToJIPipeProgressInfoPump.copyLog`.
- JFR (`jdk.ExecutionSample`, profile settings) over the run window:
  - EDT busy overall: **51.8%** of wall time
  - of that busy time, **99.1%** was inside `copyLog()` -> `TextPanel.getText()`
  - per-second view shows the EDT **saturated (>50 samples/s)** in bursts, 100% attributable
    to the pump
- Top self-time EDT frames: `AbstractStringBuilder.needsNewBuffer`,
  `AbstractStringBuilder.ensureCapacitySameCoder`, `Arrays.copyOf`, `String.<init>` -
  i.e. the growing string copy in `getText()`.

## Fix (implemented on this branch)

`IJLogToJIPipeProgressInfoPump` was rewritten (contract-tested in
`jipipe-core/src/test/java/org/hkijena/jipipe/utils/IJLogToJIPipeProgressInfoPumpTest.java`):

1. **No EDT involvement.** The 250 ms Swing timer is replaced by a poll task on one shared
   daemon executor thread (`IJ-Log-Pump`). The pump does no UI work, so nothing requires the EDT.
2. **Line cursor instead of full-text copy.** Each poll reads only the lines appended since the
   previous poll (`TextPanel.getLineCount()` / `getLine(i)`), at most 20,000 lines per pass;
   larger backlogs are delivered over subsequent polls. ImageJ1 has no log listener API (verified
   against the 1.54p sources: the complete listener inventory covers colors/tools/window
   lifecycle/commands only), so the panel remains the observation point - read incrementally.
3. **Clear handling.** A `\Clear` resets the cursor via count-shrink detection plus two anchor
   lines (last read line and the line at half the cursor) for clears that were already refilled
   past the old cursor. A poll whose read fails mid-pass (the clear race) is discarded entirely.
4. **Exhaustive close drain.** Iterating algorithms create one pump per data row; a row can
   finish faster than the first poll, so `close()` drains the whole backlog in capped passes
   (a single capped pass orphaned the remainder to the next row's attach cursor - caught by the
   in-app verification below and covered by `closeDrainsBacklogsLargerThanThePollCap`).

### Verification (in-app, same synthetic workflow: 60 rows -> 2 macro nodes x 30,000 prints)

| | before fix | after fix |
|---|---|---|
| EDT busy during run (JFR) | 51.8% of wall time | ~1.2% |
| EDT busy inside the pump | 99.1% of EDT busy time | 0 samples |
| captured lines | 20,001 per row execution (cap bug) | 30,001 per row execution, strictly sequential, no duplicates (120 executions, 3.6M lines) |

The pump thread (`IJ-Log-Pump`) appears in JFR samples parked in the executor; the EDT's top
frames during the run are ordinary UI work (painting, thumbnails, dock layout).

Harness (throwaway, not committed): `/tmp/opencode/1361-bench/` -
`PumpBench.java` (microbenchmark), `InstrumentationDriver.java` + `BuildAndRun.java`
(instrumentation client), `hammer-workflow.jip` (synthetic project),
`JfrReport.java` / `JfrRunWindow.java` / `JfrWindows.java` (JFR analysis),
`run-app.sh` (launch with `-javaagent:ij1-patcher` + JFR).

## Direction of the fix (not yet designed)

The pump should not copy the whole log per tick:

1. Read only the appended lines. ImageJ1's `TextPanel` has `getLineCount()`, `getLine(i)`
   and `appendLine(...)`; a line cursor (`fromLine = lastLine + 1`) avoids the full copy.
   (`\Clear` / `\Update:` commands can move or shorten lines, so the cursor needs a cheap
   validity check - e.g. compare line count going down, then rescan.)
2. Poll on a background timer and only hop to the EDT for the target's `log()` calls,
   with coalescing (one EDT runnable per tick instead of work-on-EDT).
3. Bound the work per tick (max lines per pass), so a huge backlog cannot saturate the EDT.
4. Optionally reuse a single global pump/timer for all concurrent run() bodies instead of
   one timer per algorithm execution.