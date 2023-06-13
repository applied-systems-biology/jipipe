# Adaptive parameters

*Summary:* Specific parameters are <i>adapted</i> to the properties (e.g., annotations) of the currently processed data batch.

To make a parameter adapt to annotations, add an entry into the <strong>Overridden parameters</strong> list below by clicking <strong>Add adaptive parameter &gt; Pick existing parameter</strong>. Adapt the expression accordingly.

Please note that the expression must return a compatible value or a string in JSON format.

Custom parameter values are attached as annotations to generated outputs. You can find all settings in the <strong>Configure</strong> menu.

## Parameter precedence

Parameters are applied in a specific order. In the following list, the **latter** items override the **earlier** items.

1. Standard parameters (parameters that are edited in the JIPipe UI)
2. CLI/meta run parameter overrides
3. External parameter sets
4. Adaptive parameters