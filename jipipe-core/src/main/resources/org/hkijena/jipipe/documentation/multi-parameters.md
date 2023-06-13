# External parameter sets

*Summary:* You can supply parameter sets via the <strong>Parameters</strong> input.

*Important:* Parameter sets are independent of how JIPipe generates data batches. If you provide 10 parameter sets, the node will run 10 times, regardless of annotations. If you wish to **adapt** parameters to annotations, use **Adaptive parameters**.

To create multiple parameter sets, utilize the node <strong>Add data &gt; Parameters &gt; Define multiple parameters</strong> that comes with an interactive editor.
Alternatively, you can use <strong>Generate parameters from expression</strong> if you prefer creating parameter sets automatically.<br/><br/>
For each parameter set, outputs are labeled by non-<code>#</code> annotations based on the parameter values. If you want to change this behavior, scroll down to the <strong>Multi-parameter settings</strong>.

## Parameter precedence

Parameters are applied in a specific order. In the following list, the **latter** items override the **earlier** items.

1. Standard parameters (parameters that are edited in the JIPipe UI)
2. CLI/meta run parameter overrides
3. External parameter sets
4. Adaptive parameters