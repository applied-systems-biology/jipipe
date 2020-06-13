# Testing algorithm parameters

Algorithms can have a plethora of different parameters that might need to be tuned depending
on the available data. It can be time-consuming to re-run the whole algorithm graph
to test parameters.

ACAQ5 provides an *Quick run* feature (sometimes referred as Test bench) that creates a **snapshot** of one specified
algorithm input and allows you to repeatedly run the algorithm with different parameters.

## Creating a quick run

To create a testbench for an algorithm, open the algorithm settings, and select *Testbench* on
the right-hand side. Then click ![](image://icons/run.png) *Create* to create a new instance.

## Testing parameters

You can change algorithm parameters within the testbench UI on the left-hand side.
To run the algorithm, click ![](image://icons/run.png) *Add test*. This will run 
the algorithm and add a new *snapshot* in the selection box.

You can go back to previous parameters and their results by selecting them in
the selection box.