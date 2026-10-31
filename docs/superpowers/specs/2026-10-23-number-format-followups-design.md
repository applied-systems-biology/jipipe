# Design: number-format converter follow-ups (ROMAN / SCIENTIFIC / BASE)

**Issue:** [#1336](https://asb-git.hki-jena.de/RGerst/jipipe/-/work_items/1336) (follow-up checklist) · **Date:**
2026-10-23 **Builds on:** `NUM_TO_LETTERS` (branch `1336-num-to-letters`, MR !173) - same pattern, same package.

## Intent

Issue #1336 asked for "similar converters that fall into such usecases eg roman numerals,
scientific notation etc". This adds three small `ExpressionFunction`s, each following the
exact `NUM_TO_LETTERS` pattern (class in the matching package, `Object` first arg with
auto-convert, optional args with defaults, fail-closed `IllegalArgumentException`, TDD test).

## Goals / non-goals

**In scope (this change):** the three functions below, each with a passing test class. **Non-goals:** no UI work, no new
parameters, no i18n, no locale-specific digit symbols.

## Constraints (inherited)

- New registration names are **stable string literals**, never code-derived.
- First arg typed `Object` (auto-convert: `Number` direct; `String` parsed) - consistent
  with `NUM_TO_LETTERS`.
- Fail-closed: invalid/out-of-range input → `IllegalArgumentException` with a clear message.
- Register in `ExpressionPlugin.register()` in the matching block; `convert.*` wildcard
  import already covers the `convert` package (no import edit there).
- Follow-up names must not collide with any existing function (verified none do).

## Approaches

Single approach, no real fork: each is a thin, well-known algorithm wrapped in the existing
`ExpressionFunction` pattern (the pattern is proven by `NUM_TO_LETTERS`). The only design
choice per function is the **argument list + defaults**, pinned below.

## The three functions

### 1. Roman numerals - `TO_ROMAN(number)` and `FROM_ROMAN(text)`

> **Naming note:** the #1336 checklist sketched a single `ROMAN_NUMBER(number, [mode])`.
> On design, a `mode` string arg is non-idiomatic for JIPipe's typed-arg style, so this
> becomes a **typed pair** matching the `TO_*` / `FROM_*` family. This renames the
> checklist item; the checklist will be updated to match.

- **`TO_ROMAN(number)`** - internal name `TO_ROMAN`, display "Number to Roman numeral".
  1 arg. Integer 1–3999 (standard subtractive notation: 4=IV, 9=IX, 40=XL, 90=XC,
  400=CD, 900=CM; e.g. 1994=MCMXCIV, 2024=MMXXIV, 3999=MMMCMXCIX). Out of range (0,
  <1, >3999) → `IllegalArgumentException`. Fractional input floors to int (documented).
- **`FROM_ROMAN(text)`** - internal name `FROM_ROMAN`, display "Roman numeral to number".
  1 arg, typed `Object` (a string; a `Number` is rejected with a clear error since a
  number is not a Roman numeral). Parses + **validates** standard notation: rejects
  non-canonical strings (e.g. `IIII`, `VV`, `XM`, `IC`) → `IllegalArgumentException`.
  Uppercase- and lowercase-input both accepted.

### 2. Scientific (E) notation - `SCIENTIFIC_NOTATION(number, [decimals])`

- Internal name `SCIENTIFIC_NOTATION`, display "Number to scientific notation".
  1–2 args; `decimals` optional, **default 1** (matches the issue examples).
- Output is a **normalized** `mantissa E exponent` string: exponent **not zero-padded**,
  no `+` sign for positive exponents, `-` kept for negative. This matches the issue's
  examples, not raw `String.format("%..E")`:
    - `0.00012` → `"1.2E-4"` (raw would be `1.2E-04`)
    - `1500` → `"1.5E3"` (raw would be `1.5E+03`)
    - `1000` → `"1.0E3"`; `1234.5678` with `decimals=3` → `"1.235E3"`
- Implementation: `String.format("%.<d>E", value)` then post-process the exponent part (after `E`): strip a leading `+`,
  strip leading zeros (keep at least one digit, keep `-`).

### 3. Base conversion - `BASE_CONVERT(value, [toBase], [fromBase])`

- Internal name `BASE_CONVERT`, display "Convert number base". 1–3 args.
- **Defaults:** `toBase = 16` (hex is the most common target), `fromBase = 10`. So
  `BASE_CONVERT(255)` → `"ff"`; `BASE_CONVERT(8, 2)` → `"1000"`.
- `value` is `Object`: a `Number` is taken as its long value (`fromBase` is ignored for
  numbers); a `String` is parsed **in `fromBase`** (so `BASE_CONVERT("ff", 10, 16)` → `255`).
  A non-decimal string without an explicit `fromBase` fails closed (default `fromBase = 10`).
- Both bases must be in `[2, 36]`; digits `a–z` / `A–Z` accepted for bases > 10.
  Output is **lowercase** (e.g. `"ff"`, not `"FF"`).
- Signs: negative integers are supported - output prefixed `-`; negative `value` strings
  parsed with `-`. (Bases are unsigned, so this is the conventional sign-prefix form.)
- **Return type:** `toBase == 10` → `Long`; otherwise a lowercase `String`.
- Errors: base out of [2,36] → `IllegalArgumentException`; unparseable value → same.

## What's left to the builder

- Nothing on the algorithms (all standard, all verified by simulation below).
- Each function gets its own `*Test` class pinning the documented mappings + error cases,
  mirroring `NumToLettersFunctionTest`.

## Simulation-verified references

- Roman: 1=I, 4=IV, 9=IX, 14=XIV, 40=XL, 90=XC, 400=CD, 900=CM, 1994=MCMXCIV,
  2024=MMXXIV, 3999=MMMCMXCIX; round-trip gen→parse is identity; `IIII`/`VV`/`XM`/`IC`
  rejected on re-encode check.
- Scientific: Java `%.1E` → `1500→"1.5E+03"`, `0.00012→"1.2E-04"`, `1e12→"1.0E+12"`,
  `1e25→"1.0E+25"`; normalized by stripping `+` and exponent leading zeros.
- Base: Java `Integer.toString(255,16)→"ff"`, `Long.toString(8,8)→"10"`,
  `Long.parseLong("ff",16)→255`, max supported radix 36 (`35`→`"z"`).
