# Number to Letters (NUM_TO_LETTERS) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:
> executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a JIPipe expression function `NUM_TO_LETTERS(number, [start])` that converts a number to a string of
letters, like spreadsheet column labels (default classic scheme: 1→A, 26→Z, 27→AA, 702→ZZ, 703→AAA).

**Architecture:** One new `ExpressionFunction` subclass in the `functions.convert` package (same home as
`ToStringFunction`), following the existing pattern: `@SetJIPipeDocumentation` + `super(name, minArg, maxArg)` +
`getParameterInfo` + `getSignature` + `evaluate`. Registered in `ExpressionPlugin.register()`. The `convert.*` wildcard
import already in the plugin means no import edit is required. The algorithm is bijective base-26 with a `start` offset.

**Tech Stack:** Java 21, JIPipe expression framework (`ExpressionFunction` from `com.fathzer.soft.javalibrator`), JUnit
5 (junit-jupiter), Maven multi-module (jipipe-core).

**Spec:** `docs/superpowers/specs/2026-10-23-excel-number-to-chars-design.md`

## Global Constraints

- **Build:** run Maven from the repo root with network (do **not** use `-o`; offline fails on the sibling
  `contrib/jipipe-ro-crate-java` artifact, and a pre-existing surefire classloader issue affects `-am`/full-reactor
  runs). If `jipipe-core` reports it cannot resolve `org.hkijena:jipipe-ro-crate-java:jar:2.1.0`, first run
  `mvn install -pl contrib/jipipe-ro-crate-java-2.1.0 -DskipTests`. Canonical test command (verified working in this
  environment): `mvn test -pl jipipe-core -Dtest=NumToLettersFunctionTest`.
- New registration names are **stable string literals**, never code-derived (persisted in saved projects). Internal
  function name must be exactly `NUM_TO_LETTERS`.
- Default mapping is **classic Excel, 1-indexed**: `start` defaults to **1** (1→A, 26→Z, 27→AA, 702→ZZ, 703→AAA).
- First parameter (`number`) is typed `Object`: accepts any `Number`; if a `String`, it is parsed (trim, integer-first
  then `long` fallback). Anything non-numeric → `IllegalArgumentException`.
- Second parameter (`start`) is **optional** (`minArg=1, maxArg=2`), default **1**, with the same `Object`/parse
  handling as `number`.
- Input whose converted value is **below `start`** → `IllegalArgumentException` with a clear message (fail-closed).
- The follow-up family (`ROMAN_NUMBER`, `SCIENTIFIC_NOTATION`, base converter) is **out of scope** for this plan.
- No existing test pins the expression-function name set, so adding `NUM_TO_LETTERS` is safe.

## Review Focus

The bijective-base-26 mapping is the whole feature and the off-by-one is the classic failure mode. Each of these is
pinned to a test in Task 1.

- **Boundary at 26 (Z vs AA):** a person converting 26 expects `Z`, 27 expects `AA`. A naive base-26 (with a `0` digit)
  produces `AZ`/`BA` off-by-one. Pin `26 → Z` and `27 → AA`.
- **Double-letter wrap (52/53):** a person converting 52 expects `AZ`, 53 expects `BA`. This catches wrong carry
  behavior. Pin `52 → AZ`, `53 → BA`.
- **Triple-letter (702/703):** 702 expects `ZZ`, 703 expects `AAA`. Pin both — this catches a loop that drops the final
  carry.
- **`start` offset correctness:** a person passing `start = 0` expects 0→A, 25→Z, 26→AA (the 0-indexed variant). A wrong
  offset silently shifts every label. Pin the `start = 0` triple.
- **Auto-convert / parse of the number arg:** a person typing `NUM_TO_LETTERS("5")` (string) or `NUM_TO_LETTERS(27.0)`
  (double from the evaluator) expects the same result as the integer. Pin `"5" → E` and a double input.
- **Below-`start` input:** a person converting 0 under the default (start=1) expects a clear error, not a silent `Z` or
  crash. Pin that `0` (default) throws `IllegalArgumentException`.
- **Non-numeric input:** a person passing a non-number, non-parseable value expects a clear error. Pin
  `IllegalArgumentException`.

---

### Task 1: Implement and register `NUM_TO_LETTERS`

**Files:**

- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/expressions/functions/convert/NumToLettersFunction.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/expressions/ExpressionPlugin.java` (insert one
  registration line in the `convert` block)
- Test:
  `jipipe-core/src/test/java/org/hkijena/jipipe/plugins/expressions/functions/convert/NumToLettersFunctionTest.java`

**Interfaces:**

- Consumes: `org.hkijena.jipipe.plugins.expressions.ExpressionFunction` (abstract, `super(name, minArg, maxArg)`,
  `getParameterInfo(int)`, `getSignature()`, `evaluate(List<Object>, JIPipeExpressionVariablesMap)`),
  `org.hkijena.jipipe.plugins.expressions.ParameterInfo`
  (`new ParameterInfo(String name, String description, Class<?>... types)`),
  `org.hkijena.jipipe.api.SetJIPipeDocumentation` (annotation, `name`/`description`),
  `org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap`.
- Produces: a public class `NumToLettersFunction` with a no-arg constructor; internal name `NUM_TO_LETTERS`; `evaluate`
  returns `String`. Registered in `ExpressionPlugin` via `registerExpressionFunction(new NumToLettersFunction())`.

- [ ] **Step 1: Write the failing test**

Create
`jipipe-core/src/test/java/org/hkijena/jipipe/plugins/expressions/functions/convert/NumToLettersFunctionTest.java`.

It must contain the standard HKI copyright header (copy the header block verbatim from a sibling file, e.g.
`HistogramThresholdFunctionTest.java`). Package `org.hkijena.jipipe.plugins.expressions.functions.convert`. Instantiate
`new NumToLettersFunction()` and call `evaluate(params, new JIPipeExpressionVariablesMap())` (do **not** route through
the JIPipe expression evaluator).

Tests to include (use `org.junit.jupiter.api.Test`, `Assertions.assertEquals` / `assertThrows`):

- `classicStartOne`: `1→A`, `26→Z`, `27→AA`, `28→AB`, `52→AZ`, `53→BA`, `702→ZZ`, `703→AAA`. Pass each as a `Long` in a
  single-element list.
- `doubleInputAutoConverts`: `evaluate(List.of(27.0d), …) == "AA"`.
- `stringInputParses`: `evaluate(List.of("5"), …) == "E"`.
- `startZeroIsZeroIndexed`: `evaluate(List.of(0L, 0L), …) == "A"`, `evaluate(List.of(25L, 0L), …) == "Z"`,
  `evaluate(List.of(26L, 0L), …) == "AA"`.
- `belowStartThrows`: `assertThrows(IllegalArgumentException.class, () -> evaluate(List.of(0L), …))` (default start is
  1, so 0 is below it).
- `nonNumericThrows`: `assertThrows(IllegalArgumentException.class, () -> evaluate(List.of("not a number"), …))`.

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl jipipe-core -Dtest=NumToLettersFunctionTest`
Expected: FAIL — compile error, `NumToLettersFunction` does not exist yet (the test cannot compile). (If the build first
reports the sibling `jipipe-ro-crate-java` module unresolved, install it per the Build constraint in Global
Constraints.)

- [ ] **Step 3: Implement `NumToLettersFunction`**

Create `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/expressions/functions/convert/NumToLettersFunction.java` in
package `org.hkijena.jipipe.plugins.expressions.functions.convert`. Copy the HKI copyright header verbatim from a
sibling `convert` file (e.g. `ToStringFunction.java`).

Class shape (write the surrounding boilerplate; the two bodies below are the pinned logic):

```java
@SetJIPipeDocumentation(
    name = "Number to letters",
    description = "Converts a number to a string of letters, like spreadsheet column labels " +
            "(1 = A, 2 = B, …, 26 = Z, 27 = AA, 702 = ZZ, 703 = AAA, …) — bijective base-26. " +
            "The optional start parameter is the number that maps to the first letter (A); default 1.")
public class NumToLettersFunction extends ExpressionFunction {
    public NumToLettersFunction() {
        super("NUM_TO_LETTERS", 1, 2);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        switch (index) {
            case 0: return new ParameterInfo("number", "The number to convert to a column label.", Object.class);
            case 1: return new ParameterInfo("start", "The number that maps to the first letter (A). Default 1.", Object.class);
            default: return null;
        }
    }

    @Override
    public String getSignature() {
        return getName() + "(number, start)";
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        long n = toLong(parameters.get(0));
        long start = parameters.size() > 1 ? toLong(parameters.get(1)) : 1;
        long n0 = n - start;                 // 0-based: `start` maps to 0 (letter A)
        if (n0 < 0) {
            throw new IllegalArgumentException(
                "Number " + n + " is below the start value " + start +
                " (the smallest valid input is " + start + ").");
        }
        long b = n0 + 1;                     // bijective base-26 value (A = 1)
        StringBuilder sb = new StringBuilder();
        while (b > 0) {
            b -= 1;
            sb.append((char) ('A' + (int) (b % 26)));
            b /= 26;
        }
        return sb.reverse().toString();
    }

    private long toLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            String s = ((String) value).trim();
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException ignored) {
                // fall through to long
            }
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Cannot convert to a number: \"" + value + "\"", e);
            }
        }
        throw new IllegalArgumentException("Cannot convert to a number: " + value);
    }
}
```

Imports: `org.hkijena.jipipe.api.SetJIPipeDocumentation`, `org.hkijena.jipipe.plugins.expressions.ExpressionFunction`,
`org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap`,
`org.hkijena.jipipe.plugins.expressions.ParameterInfo`, `java.util.List`.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl jipipe-core -Dtest=NumToLettersFunctionTest`
Expected: PASS — all tests green.

- [ ] **Step 5: Register the function in `ExpressionPlugin`**

In `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/expressions/ExpressionPlugin.java`, in the `register(...)`
method, add `registerExpressionFunction(new NumToLettersFunction());` inside the `convert` block — directly after the
line `registerExpressionFunction(new ToBooleanFunction());` (currently line 187). The `convert.*` wildcard import (line
25) already covers the new class, so no import edit is needed.

- [ ] **Step 6: Verify the module compiles with the registration**

Run: `mvn test-compile -pl jipipe-core`
Expected: BUILD SUCCESS (the `ExpressionPlugin` registration line compiles and the test still compiles).

- [ ] **Step 7: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/plugins/expressions/functions/convert/NumToLettersFunction.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/plugins/expressions/ExpressionPlugin.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/plugins/expressions/functions/convert/NumToLettersFunctionTest.java
git commit -m "feat: NUM_TO_LETTERS expression function (number → letters, spreadsheet column labels) (1336)"
```
