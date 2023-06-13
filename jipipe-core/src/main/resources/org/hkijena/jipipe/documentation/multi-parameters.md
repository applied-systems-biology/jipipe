# External parameter sets

*Summary:* You can supply parameter sets via the <strong>Parameters</strong> input.

*Important:* Parameter sets are independent of how JIPipe generates data batches. If you provide 10 parameter sets, the node will run 10 times, regardless of annotations. If you wish to **adapt** parameters to annotations, use **Adaptive parameters**.

To create multiple parameter sets, utilize the node <strong>Add data &gt; Parameters &gt; Define multiple parameters</strong> that comes with an interactive editor.
Alternatively, you can use <strong>Generate parameters from expression</strong> if you prefer creating parameter sets automatically.<br/><br/>
For each parameter set, outputs are labeled by non-<code>#</code> annotations based on the parameter values. If you want to change this behavior, scroll down to the <strong>Multi-parameter settings</strong>.

