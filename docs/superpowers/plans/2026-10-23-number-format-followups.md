# Number-Format Converter Follow-ups Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:
> executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add three expression functions that are the "similar converters" from issue #1336: `TO_ROMAN` / `FROM_ROMAN`
(Roman numerals), `SCIENTIFIC_NOTATION` (E-notation string), `BASE_CONVERT` (number base conversion).

**Architecture:** Three new `ExpressionFunction` subclasses following the exact `NUM_TO_LETTERS` pattern (already on
this branch): `Object` first arg with auto-convert, optional args with defaults, fail-closed `IllegalArgumentException`,
one registration line each in `ExpressionPlugin.register()` in the `convert` block (the `functions.convert.*` wildcard
import covers all of them). Each task is TDD with its own test class.

**Tech Stack:** Java 21, JIPipe expression framework, JUnit 5, Maven (jipipe-core).

**Spec:** `docs/superpowers/specs/2026-10-23-number-format-followups-design.md`

## Global Constraints

- **Build:** run Maven from the repo root with network (do **not** use `-o`). Canonical test command (verified working):
  `mvn test -pl jipipe-core -Dtest=ClassName`.
- New registration names are **stable string literals**: exactly `TO_ROMAN`, `FROM_ROMAN`, `SCIENTIFIC_NOTATION`,
  `BASE_CONVERT` (verified: no existing collision).
- All new classes live in package `org.hkijena.jipipe.plugins.expressions.functions.convert` with the verbatim HKI
  copyright header (copy from `NumToLettersFunction.java`).
- `Object` first-arg auto-convert semantics (same as `NUM_TO_LETTERS`): `Number` → direct; `String` → trim + parse;
  anything else → `IllegalArgumentException`. Each class keeps its own private `toLong`/`toDouble` helper
  (self-contained functions, no shared util — established pattern).
- Out-of-range / invalid input → `IllegalArgumentException` with a clear message (fail-closed).
- Branch `1336-num-to-letters`, same MR !173; one commit per task.

## Review Focus

The algorithms are simple; the failure modes a reasonable user would actually hit:

- **Exponent normalization edges (SCIENTIFIC_NOTATION):** raw Java format is `0.0E+00` / `1.0E+25` — a naive strip can
  yield `0.0E` or keep the `+`. Pin `0 → "0.0E0"`, `1e12 → "1.0E12"`, `1500 → "1.5E3"` (no `+`).
- **BASE_CONVERT number/string asymmetry:** a `Number` arg IS the value (`BASE_CONVERT(255, 16, 10)` → 255, NOT 37);
  only `String` args are parsed in `fromBase`. Pin the string case `"ff", 16 → 255`; the number asymmetry is documented
  in the parameter info.
- **Fractional `decimals` (SCIENTIFIC_NOTATION):** `SCIENTIFIC_NOTATION(1500, 2.5)` must error, not silently floor. Pin
  it.
- **FROM_ROMAN canonical validation:** must accept valid non-obvious numerals (`"MCMXC"` = 1990, lowercase `"mcmxciv"`)
  and reject non-canonical (`"IIII"`, `"VV"`, `"XM"`, `"IC"`). Pin both sides.
- **Large inputs:** `TO_ROMAN(1e30)` truncates via `longValue` but the 1–3999 range check converts that to a clear
  error; `BASE_CONVERT(1e30)` silently truncates (documented limitation, no test — unrealistic input).

---

### Task 1: Roman numerals — `TO_ROMAN` and `FROM_ROMAN`

**Files:**

- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/expressions/functions/convert/ToRomanFunction.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/expressions/functions/convert/FromRomanFunction.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/expressions/ExpressionPlugin.java` (two registration
  lines, `convert` block, after `NumToLettersFunction`)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/plugins/expressions/functions/convert/ToRomanFunctionTest.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/plugins/expressions/functions/convert/FromRomanFunctionTest.java`

**Interfaces:**

- Consumes: same as `NumToLettersFunction` (`ExpressionFunction`, `ParameterInfo`, `SetJIPipeDocumentation`,
  `JIPipeExpressionVariablesMap`).
- Produces: `ToRomanFunction` (no-arg ctor, name `TO_ROMAN`, `evaluate` → `String`) and `FromRomanFunction` (no-arg
  ctor, name `FROM_ROMAN`, `evaluate` → `Long`). `FromRomanFunctionTest` may call `ToRomanFunction` for round-trip.

- [ ] **Step 1: Write the failing tests**

`ToRomanFunctionTest` (package `…functions.convert`, HKI header, call `evaluate` directly):

- `mappings`: `1→"I"`, `4→"IV"`, `9→"IX"`, `14→"XIV"`, `40→"XL"`, `90→"XC"`, `400→"CD"`, `900→"CM"`, `1994→"MCMXCIV"`,
  `2024→"MMXXIV"`, `3999→"MMMCMXCIX"` (Long args).
- `doubleInputAutoConverts`: `2024.0 → "MMXXIV"`.
- `stringInputParses`: `"2024" → "MMXXIV"`.
- `outOfRangeThrows`: `0`, `-1`, `4000` each → `IllegalArgumentException`.

`FromRomanFunctionTest`:

- `mappings`: `"I"→1L`, `"IV"→4L`, `"IX"→9L`, `"XIV"→14L`, `"XL"→40L`, `"XC"→90L`, `"CD"→400L`, `"CM"→900L`,
  `"MCMXC"→1990L` (valid canonical), `"MCMXCIV"→1994L`, `"MMXXIV"→2024L`, `"MMMCMXCIX"→3999L`.
- `lowercaseAccepted`: `"mcmxciv" → 1994L`.
- `nonCanonicalThrows`: `"IIII"`, `"VV"`, `"XM"`, `"IC"` → `IllegalArgumentException`.
- `emptyThrows`: `""` → `IllegalArgumentException`.
- `invalidCharThrows`: `"ABC"` → `IllegalArgumentException`.
- `numberInputThrows`: `5L` → `IllegalArgumentException` (a number is not a Roman numeral).
- `roundTrip`: for each `n` in `{1, 4, 9, 1994, 3999}`: `FROM_ROMAN(TO_ROMAN(n)) == n`.

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn test -pl jipipe-core -Dtest=ToRomanFunctionTest,FromRomanFunctionTest`
Expected: FAIL — compile error, `ToRomanFunction` / `FromRomanFunction` do not exist.

- [ ] **Step 3: Implement both functions**

`ToRomanFunction` —
`@SetJIPipeDocumentation(name = "Number to Roman numeral", description = "Converts an integer (1–3999) to a Roman numeral, e.g. 1994 = MCMXCIV. Fractional input is truncated to an integer.")` —
`super("TO_ROMAN", 1)`, signature `TO_ROMAN(number)`, param 0
`("number", "The integer to convert (1 to 3999).", Object.class)`.

Body:

```java
long n = toLong(parameters.get(0));              // same auto-convert helper as NumToLettersFunction
if (n < 1 || n > 3999) throw new IllegalArgumentException("Roman numerals are defined for 1 to 3999, got " + n + ".");
long[][] VALUES = {{1000,900,500,400,100,90,50,40,10,9,5,4,1}};
String[] SYMBOLS = {"M","CM","D","CD","C","XC","L","XL","X","IX","V","IV","I"};
StringBuilder sb = new StringBuilder();
for (int i = 0; i < VALUES.length; i++) {
    while (n >= VALUES[i][0]) { sb.append(SYMBOLS[i]); n -= VALUES[i][0]; }
}
return sb.toString();
```

`FromRomanFunction` —
`@SetJIPipeDocumentation(name = "Roman numeral to number", description = "Parses a Roman numeral (standard notation) to its number, e.g. MCMXCIV = 1994. Uppercase and lowercase are accepted; non-canonical numerals are rejected.")` —
`super("FROM_ROMAN", 1)`, signature `FROM_ROMAN(text)`, param 0 `("text", "The Roman numeral to parse.", Object.class)`.

Body:

```java
Object value = parameters.get(0);
if (!(value instanceof String)) throw new IllegalArgumentException("A Roman numeral must be a string, got: " + value);
String s = ((String) value).trim().toUpperCase(Locale.ROOT);
if (s.isEmpty()) throw new IllegalArgumentException("Roman numeral must not be empty.");
int total = 0;
for (int i = 0; i < s.length(); i++) {
    int v = valueOf(s.charAt(i));          // I,V,X,L,C,D,M -> 1,5,10,50,100,500,1000; else IAE "not a Roman numeral character"
    int next = (i + 1 < s.length()) ? valueOf(s.charAt(i + 1)) : 0;
    total += v < next ? -v : v;
}
// validate canonical form: re-encode and compare (rejects IIII, VV, XM, IC, ...)
if (!ToRomanFunction.toRomanCanonical(total).equals(s))
    throw new IllegalArgumentException("Not a valid Roman numeral: \"" + value + "\"");
return (long) total;
```

`ToRomanFunction` needs a package-private static `toRomanCanonical(long)` (the greedy table above, no range check) so
the validator can re-encode. (Range check stays in `TO_ROMAN`'s public path: `FROM_ROMAN` of a canonical string can
never exceed 3999.)

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn test -pl jipipe-core -Dtest=ToRomanFunctionTest,FromRomanFunctionTest`
Expected: PASS — all green.

- [ ] **Step 5: Register both functions in `ExpressionPlugin`**

In `register(...)`, after `registerExpressionFunction(new NumToLettersFunction());`:

```java
registerExpressionFunction(new ToRomanFunction());
registerExpressionFunction(new FromRomanFunction());
```

- [ ] **Step 6: Verify the module compiles**

Run: `mvn test-compile -pl jipipe-core`
Expected: BUILD SUCCESS.

- [ ] **Step 7: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/plugins/expressions/functions/convert/ToRomanFunction.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/plugins/expressions/functions/convert/FromRomanFunction.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/plugins/expressions/ExpressionPlugin.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/plugins/expressions/functions/convert/ToRomanFunctionTest.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/plugins/expressions/functions/convert/FromRomanFunctionTest.java
git commit -m "feat: TO_ROMAN / FROM_ROMAN expression functions (Roman numerals) (1336)"
```

---

### Task 2: `SCIENTIFIC_NOTATION`

**Files:**

- Create:
  `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/expressions/functions/convert/ScientificNotationFunction.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/expressions/ExpressionPlugin.java` (one registration
  line after `FromRomanFunction`)
- Test:
  `jipipe-core/src/test/java/org/hkijena/jipipe/plugins/expressions/functions/convert/ScientificNotationFunctionTest.java`

**Interfaces:**

- Consumes: same as Task 1.
- Produces: `ScientificNotationFunction` (no-arg ctor, name `SCIENTIFIC_NOTATION`, `minArg=1, maxArg=2`, `evaluate` →
  `String`).

- [ ] **Step 1: Write the failing test**

`ScientificNotationFunctionTest`:

- `defaultDecimals`: `0.00012 → "1.2E-4"`, `1500 → "1.5E3"`, `1000 → "1.0E3"` (double args, single-element list).
- `customDecimals`: `1234.5678` with `3 → "1.235E3"`; `1500.0` with `0L → "2E3"`.
- `zero`: `0.0 → "0.0E0"`.
- `negative`: `-1500.0 → "-1.5E3"`.
- `largeExponent`: `1.0e12 → "1.0E12"`.
- `negativeExponent`: `3.14e-7 → "3.1E-7"`.
- `nonNumericThrows`: `"abc"` → `IllegalArgumentException`.
- `fractionalDecimalsThrows`: `1500.0` with `2.5` → `IllegalArgumentException`.
- `negativeDecimalsThrows`: `1500.0` with `-1L` → `IllegalArgumentException`.

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl jipipe-core -Dtest=ScientificNotationFunctionTest`
Expected: FAIL — compile error, class does not exist.

- [ ] **Step 3: Implement `ScientificNotationFunction`**

`@SetJIPipeDocumentation(name = "Number to scientific notation", description = "Formats a number as a scientific-notation string (e.g. 0.00012 = 1.2E-4, 1500 = 1.5E3). The exponent is not zero-padded and carries no plus sign. The optional decimals parameter sets the mantissa precision (default 1).")` —
`super("SCIENTIFIC_NOTATION", 1, 2)`, signature `SCIENTIFIC_NOTATION(number, decimals)`, params:
`("number", "The number to format.", Object.class)`,
`("decimals", "Mantissa precision (whole number ≥ 0). Default 1.", Object.class)`.

Body:

```java
double value = toDouble(parameters.get(0));
int decimals = 1;
if (parameters.size() > 1) {
    double d = toDouble(parameters.get(1));
    if (d < 0 || d != Math.floor(d)) throw new IllegalArgumentException("decimals must be a whole number ≥ 0, got: " + d);
    decimals = (int) d;
}
String raw = String.format(Locale.ROOT, "%." + decimals + "E", value);   // e.g. "1.5E+03", "1.2E-04", "0.0E+00" (Locale.ROOT: locale-neutral digits; precision is a validated integer, safe to embed literally)
int e = raw.indexOf('E');
String mantissa = raw.substring(0, e);
String exp = raw.substring(e + 1);                       // e.g. "+03", "-04", "+00"
boolean neg = exp.startsWith("-");
String digits = exp.replaceFirst("^[+-]", "");
digits = digits.replaceFirst("^0+(?=.)", "");            // strip leading zeros, keep ≥1 digit
return mantissa + "E" + (neg ? "-" : "") + digits;
```

`toDouble(Object)`: `Number` → `doubleValue()`; `String` → trim, `Double.parseDouble`, on `NumberFormatException` →
`IllegalArgumentException("Cannot convert to a number: …")` (wrapping); else IAE.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl jipipe-core -Dtest=ScientificNotationFunctionTest`
Expected: PASS.

- [ ] **Step 5: Register in `ExpressionPlugin`**

After `registerExpressionFunction(new FromRomanFunction());`:

```java
registerExpressionFunction(new ScientificNotationFunction());
```

- [ ] **Step 6: Verify the module compiles**

Run: `mvn test-compile -pl jipipe-core`
Expected: BUILD SUCCESS.

- [ ] **Step 7: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/plugins/expressions/functions/convert/ScientificNotationFunction.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/plugins/expressions/ExpressionPlugin.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/plugins/expressions/functions/convert/ScientificNotationFunctionTest.java
git commit -m "feat: SCIENTIFIC_NOTATION expression function (E-notation string) (1336)"
```

---

### Task 3: `BASE_CONVERT`

> **Ruling (recorded in ledger):** the arg order is `BASE_CONVERT(value, [toBase], [fromBase])`
> (target first), with `fromBase` default **10**. This swapped from the original spec sketch
> (`[fromBase], [toBase]`, fromBase first): for `Number` inputs the leading `fromBase` arg is
> meaningless (numbers are already values), so the 2-arg form couldn't express a target base and
> silently no-opped — a fail-open defect. `toBase`-first makes `BASE_CONVERT(8, 2)` → `"1000"`
> behave as a reasonable person expects; a non-decimal string without an explicit `fromBase`
> fails closed instead of silently round-tripping in base 16.

**Files:**

- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/expressions/functions/convert/BaseConvertFunction.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/expressions/ExpressionPlugin.java` (one registration
  line after `ScientificNotationFunction`)
- Test:
  `jipipe-core/src/test/java/org/hkijena/jipipe/plugins/expressions/functions/convert/BaseConvertFunctionTest.java`

**Interfaces:**

- Consumes: same as Task 1.
- Produces: `BaseConvertFunction` (no-arg ctor, name `BASE_CONVERT`, `minArg=1, maxArg=3`, `evaluate` → `String` when
  `toBase ≠ 10`, `Long` when `toBase = 10`).

- [ ] **Step 1: Write the failing test**

`BaseConvertFunctionTest`:

- `defaultsToHex`: `255 → "ff"`, `0 → "0"`, `10 → "a"`.
- `targetBase`: `8L, 2L → "1000"`; `8L, 8L → "10"`; `255L, 16L → "ff"`.
- `stringSourceWithFromBase`: `"ff", 10L, 16L → 255L` (result is a Long); `"1010", 16L, 2L → "a"`;
  `"z", 10L, 36L → 35L`.
- `negative`: `-255L → "-ff"`; `"-ff", 10L, 16L → -255L`.
- `nonDecimalStringFailsClosed`: `"ff"` (default fromBase 10) → `IllegalArgumentException`.
- `invalidBaseThrows`: `255L, 37L → IAE`; `255L, 1L (as toBase) → IAE`; `255L, 10L, 1L → IAE` (fromBase 1).
- `invalidDigitThrows`: `"zz", 10L, 16L → IAE`.
- `nonNumericThrows`: `"not a number"` → IAE.

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl jipipe-core -Dtest=BaseConvertFunctionTest`
Expected: FAIL — compile error, class does not exist.

- [ ] **Step 3: Implement `BaseConvertFunction`**

`@SetJIPipeDocumentation(name = "Convert number base", description = "Converts a number between bases 2–36. A number input is the value itself; a string input is parsed in the fromBase (default 10). The result is a string in the toBase (default 16, lowercase), or a number when toBase is 10. Digits a–z are accepted for bases above 10.")` —
`super("BASE_CONVERT", 1, 3)`, signature `BASE_CONVERT(value, toBase, fromBase)`, params:

- 0:
  `("value", "The number to convert. A number is the value itself; a string is parsed in the fromBase.", Object.class)`
- 1: `("toBase", "Base to convert to (2–36). Default 16.", Object.class)`
- 2: `("fromBase", "Base the value is given in (2–36). Default 10; only applies to string inputs.", Object.class)`

Body:

```java
Object value = parameters.get(0);
int toBase = parameters.size() > 1 ? (int) toLong(parameters.get(1)) : 16;
int fromBase = parameters.size() > 2 ? (int) toLong(parameters.get(2)) : 10;
checkBase(fromBase, "fromBase");
checkBase(toBase, "toBase");
long n;
if (value instanceof Number) {
    n = ((Number) value).longValue();
} else if (value instanceof String) {
    String s = ((String) value).trim();
    if (s.isEmpty()) throw new IllegalArgumentException("Value must not be empty.");
    boolean neg = s.startsWith("-");
    if (neg || s.startsWith("+")) s = s.substring(1);
    try {
        n = Long.parseLong(s, fromBase);
        if (neg) n = -n;
    } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Cannot parse \"" + value + "\" in base " + fromBase + ".", e);
    }
} else {
    throw new IllegalArgumentException("Cannot convert to a number: " + value);
}
if (toBase == 10) return n;
return Long.toString(n, toBase);        // lowercase, sign handled by Java ("-ff")
```

`checkBase(int b, String name)`:
`if (b < 2 || b > 36) throw new IllegalArgumentException(name + " must be between 2 and 36, got: " + b);`.
`toLong(Object)`: same auto-convert helper as `NumToLettersFunction` (private, per class).

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl jipipe-core -Dtest=BaseConvertFunctionTest`
Expected: PASS.

- [ ] **Step 5: Register in `ExpressionPlugin`**

After `registerExpressionFunction(new ScientificNotationFunction());`:

```java
registerExpressionFunction(new BaseConvertFunction());
```

- [ ] **Step 6: Verify the module compiles**

Run: `mvn test-compile -pl jipipe-core`
Expected: BUILD SUCCESS.

- [ ] **Step 7: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/plugins/expressions/functions/convert/BaseConvertFunction.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/plugins/expressions/ExpressionPlugin.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/plugins/expressions/functions/convert/BaseConvertFunctionTest.java
git commit -m "feat: BASE_CONVERT expression function (number base conversion) (1336)"
```

- [ ] **Step 8: Amend the committed spec + plan** (the ruling changed the arg order after they were written; keep the
  committed docs in sync):

```bash
git add docs/superpowers/specs/2026-10-23-number-format-followups-design.md docs/superpowers/plans/2026-10-23-number-format-followups.md
git commit --amend --no-edit
```

### Task 4 (post-implementation, no code): update the #1336 checklist

- [ ] Update the issue #1336 description (via the GitLab API): replace the follow-up checklist with a "Delivered" note
  listing `NUM_TO_LETTERS`, `TO_ROMAN`/`FROM_ROMAN`, `SCIENTIFIC_NOTATION`, `BASE_CONVERT` and their signatures. (The
  checklist item named `ROMAN_NUMBER(number, [mode])` becomes the `TO_ROMAN`/`FROM_ROMAN` pair — note the deliberate
  rename.)
