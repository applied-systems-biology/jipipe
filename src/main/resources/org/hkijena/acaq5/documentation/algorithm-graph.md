# Creating a pipeline

ACAQ5 allows you to create a pipeline of various algorithms.
To add an algorithm, select one in the *menu bar* on top of the white *graph area*.
After selecting an algorithm, it will appear in the graph area.

You can freely move algorithms within the area my dragging them by the colored areas.

## Data slots

Algorithms process input data to generate output data. The input and output can be 
addressed as individual *data slots* that are left and right to the colored area of 
an algorithm node. 

**Input data slots** are located on the left side

**Output data slots** are located on the right side

### Connecting data

To connect slots, click the ![](image://icons/chevron-right.png) icon to open a menu of
compatible source or target data slots. On creating a connection, a line will appear between
the two affected slots to indicate a connection.

To disconnect two slots, click ![](image://icons/chevron-right.png) and select 
the ![](image://icons/remove.png) disconnect command.

### Adding slots

Some algorithms allow you to add additional slots. To add another slot, click the ![](image://icons/add.png)
add button and select which data type the slot should have.

### Annotating data

ACAQ5 allows you to annotate data with certain predefined traits. These annotations
allow you to select an optimal algorithm for your data. Annotations are carried over
by algorithms to the next steps, unless an algorithm specifically removes an annotation.

To annotate data, click the ![](image://icons/label.png) *Annotate* button within
a data slot. Then select the annotations.