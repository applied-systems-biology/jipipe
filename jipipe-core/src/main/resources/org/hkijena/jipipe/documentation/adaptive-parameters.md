# Adaptive parameters

*Summary:* Specific parameters are <i>adapted</i> to the properties (e.g., annotations) of the currently processed iteration step.

To make a parameter adapt to annotations, click the ⏷ menu button next to the parameter help and select "Make parameter adaptive".
Alternatively, you can click **Configure** and add custom entries into the list of overrides. 

To remove an adaptive parameter, click the ⏷ menu button next to the parameter help and select "Make parameter static". Alternatively, 
remove the override in the **Configure** menu.

Please note that the expression must return a compatible value or a string in JSON format.

Custom parameter values are attached as annotations to generated outputs. You can find all settings in the <strong>Configure</strong> menu.

## Parameter precedence

Parameters are applied in a specific order. In the following list, the **latter** items override the **earlier** items.

1. Standard parameters (parameters that are edited in the JIPipe UI)
2. CLI/meta run parameter overrides
3. External parameter sets
4. Adaptive parameters