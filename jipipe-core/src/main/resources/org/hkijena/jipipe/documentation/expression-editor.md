# Expression editor

## What are expressions?
Expressions allow to to write simple to complex functions that can test for one or multiple conditions or act as input
for a generator node. 

There are always four components you will interact with:

* Literals, for example numbers (`0.5`), strings (`"hello world"`), and boolean values (`TRUE`, `FALSE`)
* Variables that are supplied from the node itself like `x`, `y`, or `Area`. Please note that variables with special characters (e.g., spaces) can be inserted with the `$` operator, for example: `$"My variable"`
* Functions that process literals or variables including `MIN(x, 5)`, `STRING_EQUALS("abc", "abc")`. Parameters are separated by commas.
* Operators that do something with the literals, variables, or function results, like subtraction, concatentation, or conditions

The expression language is interpreted as one line and you cannot write own variables.

More information can be found here: https://www.jipipe.org/documentation/create-pipelines/expressions/

## Using the expression editor

At the bottom of the editor you will find the currently edited expression.
At the left of the text box there are buttons to create brackets and insert variables while ensuring that 
the correct syntax is used. 
On the right side of the window there is a list of all known variables, constants, operators, and functions. 
Select an item from the list to open an editor that guides you through the parameters and documentation.

If you select a function or operator, there will be a list of parameters where you can fill in the values.