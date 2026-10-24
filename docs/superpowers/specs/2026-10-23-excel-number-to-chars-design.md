# Design: Excel-like number → letters expression function (+ converter family)

**Issue:** [#1336 "Excel-like number to char(s)"](https://asb-git.hki-jena.de/RGerst/jipipe/-/work_items/1336)
**Date:** 2026-10-23 · **Author of decision:** Ruman Gerst (via 1336 conversation)

## Intent & why

Users who write expressions need to turn an index/counter into a spreadsheet-style
column label (1 → A, 2 → B, …, 26 → Z, 27 → AA, 702 → ZZ, 703 → AAA) — e.g. for
naming generated columns, sample/element labels, or file-name fragments. There is
currently no expression function that does this (verified: no `TO_CHAR`/`EXCEL`/
column-name function exists).

## Goals / non-goals

**In scope (this change):**
- One expression function `NUM_TO_LETTERS(number, [start])` producing
  bijective base-26 letters (spreadsheet-style column labels), **classic
  Excel default**: `start = 1`, so 1→A, 26→Z, 27→AA. Named generically — not
  "EXCEL" — to avoid trademarked naming; "bijective base-26" is the general
  mathematical name for the scheme.
- First parameter is typed `Object`: accepts any `Number`; if a `String`,
  parses it (trim → integer, then `long` fallback). Non-numeric input → error.
- Second parameter `start` is optional (`minArg=1, maxArg=2`), default **1**.
  Same Object/parse handling as the number arg.
- Inputs below `start` (and, after conversion, values ≤ 0) → `IllegalArgumentException`
  with a clear message (fail-closed, consistent with `SequenceFunction` throwing
  on step 0 and the histogram functions throwing on bad bins).

**Non-goals / later (follow-up family — tracked as checklist on #1336, built
incrementally, same registration pattern):**
- `ROMAN_NUMBER(number, [mode])` — integer ↔ Roman numeral (mode: `to`/`from`).
- `SCIENTIFIC_NOTATION(number, [decimals])` — E-notation string, e.g.
  0.00012 → "1.2E-4" / 1500 → "1.5E3".
- Optional: `BASE_CONVERT(number, [fromBase], [toBase])` (2–36, letter digits).

These are intentionally **not** built in this change; each gets its own
conversation when picked up (their exact output format is still open).

## Constraints

- New registration names must be **stable string literals** (persisted in
  projects) — never code-derived.
- Must follow the existing `ExpressionFunction` pattern:
  `super(name, minArg, maxArg)`, `@SetJIPipeDocumentation`, `getParameterInfo`,
  `getSignature`, `evaluate(List<Object>, JIPipeExpressionVariablesMap)`.
- Register in `ExpressionPlugin.register()` next to the other `convert`
  functions (`TO_STRING`, `TO_NUMBER`, …).
- No existing test pins the expression-function name set, so adding a name is
  safe.

## Approaches (considered)

| # | Approach | Trade-off | Decision |
|---|----------|-----------|----------|
| A | **Single `NUM_TO_LETTERS(number, [start])`**, `start` optional default 1 | Exactly matches the issue + requested param shape; `start` generalizes (0-indexed, etc.) | **Chosen** |
| B | Two functions, `NUM_TO_LETTERS` (1-based) + a separate 0-based variant | More names, more clutter; `start` already covers both | Rejected — `start` is enough |
| C | One function, `start` **required** (always pass 1) | Slightly less ergonomic; forces users to know the default | Rejected — issue asks for optional default 1 |

Chosen approach: **A**.

## The how (this change)

- **Location:** new file
  `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/expressions/functions/convert/NumToLettersFunction.java`
  (package `…functions.convert`, alongside `ToStringFunction`).
- **Name / docs:**
  - internal name: `NUM_TO_LETTERS`
  - `@SetJIPipeDocumentation(name = "Number to letters", description = "Converts a number to a string of letters, like spreadsheet column labels (1 = A, 2 = B, …, 26 = Z, 27 = AA, …) — bijective base-26. The optional start parameter is the number that maps to the first letter (A); default 1.")`
  - signature: `NUM_TO_LETTERS(number, start)`
  - `getParameterInfo(0)` → `("number", "The number to convert", Object.class)`
  - `getParameterInfo(1)` → `("start", "The number that maps to the first letter (A). Default 1.", Object.class)`
- **Algorithm** (bijective base-26 with a `start` offset; classic Excel when `start=1`):
  ```
  n = toLong(number)
  start = (start omitted) ? 1 : toLong(start)
  n0 = n - start                   // 0-based: `start` maps to 0 (letter A)
  if (n0 < 0) throw IllegalArgumentException("…below start…")
  b = n0 + 1                       // bijective base-26 value (A = 1)
  sb = ""
  while (b > 0) {
      b -= 1
      sb.append('A' + (int)(b % 26))
      b /= 26
  }
  return sb.reverse().toString()
  ```
  Verified by simulation: start=1 → 1=A, 25=Y, 26=Z, 27=AA, 28=AB, 52=AZ, 53=BA, 702=ZZ, 703=AAA;
  start=0 → 0=A, 25=Z, 26=AA.
- **`toLong(Object)`** helper: `Number` → `longValue()`; `String` → trim,
  `Long.parseLong` with `Integer.parseInt` fallback; else throw
  `IllegalArgumentException("Cannot convert to a number: …")`.
- **Registration:** add `registerExpressionFunction(new NumToLettersFunction());`
  in `ExpressionPlugin.register()`.
- **Test:** new
  `jipipe-core/src/test/java/org/hkijena/jipipe/plugins/expressions/functions/convert/NumToLettersFunctionTest.java`
  pinning the mappings above, plus:
  - string number input `"5"` → `E`
  - `start = 0` → 0=A, 25=Z, 26=AA (the 0-indexed variant)
  - below-start and non-numeric inputs throw `IllegalArgumentException`

  (This is non-trivial bijective-base logic — a real regression target — so it
  warrants a test per the test policy.)

## What's left to the builder

- Nothing on this change; the follow-up family is explicitly deferred and will
  be re-scoped per function when it's picked up.
