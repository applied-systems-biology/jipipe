# Build adaptive parameters

First, select the parameter that should be made adaptive. 
Add new conditions into the list by clicking the *Add* button at the top right.
JIPipe will check each condition expression in order. If it matches, then 
the value assigned to the condition will be set as parameter.

The condition is an expression that has access to all annotations of the current data batch (stored as variables), as well 
as to the default value (stored in a variable `default`).