# Creating a pipeline

The graph editor allows you to create a pipeline of algorithms.
To add an algorithm, select one in the *menu bar* on top of the white *graph area*.
You can also use the *search bar* to find algorithms.
After selecting an algorithm, it will appear in the graph area.

You can freely move algorithms within the area my dragging them by the colored areas.

## Data slots

Algorithms process input data to generate output data. The input and output can be 
addressed as individual *data slots* that are at the top and bottom of 
an algorithm node.

**Input data slots** are located on the left side

**Output data slots** are located on the right side

### Connecting data

To connect slots, click the ![](image://icons/chevron-up.png) or ![](image://icons/chevron-down.png) icon to open a menu of
compatible source or target data slots. On creating a connection, a line will appear between
the two affected slots to indicate a connection.

To disconnect two slots, click ![](image://icons/chevron-up.png) or ![](image://icons/chevron-down.png) and select 
![](image://icons/remove.png) **Disconnect**.

### Modifying slots

Some algorithms allow you to add additional slots. To add another slot, click the ![](image://icons/add.png)
**Add** button and select which data type the slot should have. Depending on the algorithm, 
you can make an output slot inherit the type of an existing input slot.

### Annotating data

You can annotate data slots with information about the data.
Annotations are carried overby algorithms to the next steps. Algorithms also can modify annotations.
You can use the information to find which algorithms fits your data best.

To annotate data, click the ![](image://icons/label.png) **Annotate** button within
a data slot. Then select the annotations.