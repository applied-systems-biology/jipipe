# Creating a pipeline

The graph editor allows you to create a pipeline of algorithms.
To add an algorithm, select one in the *menu bar* on top of the white *graph area*.
You can also use the *search bar* to find and add algorithms.

Once added, you can freely move algorithms within the area my dragging them by the colored areas.
Click the area to select the node. If you hold `Shift` while clicking, you can select multiple algorithms.

You can right-click the graph area or a node to open the context menu. Alternatively, you will also find context 
actions at the top-right corner after selecting one or multiple nodes.

You will notice a ![](resource://icons/actions/target.png) **Cursor** within the graph area that moves to the 
position where you clicked. New nodes appear at its location.

## Data slots

Algorithms process input data to generate output data. The input and output can be 
addressed as individual *data slots* that are at the top and bottom of 
an algorithm node.

**Input data slots** are located at the top (left side in horizontal view)

**Output data slots** are located at the bottom (right side in horizontal view)

### Connecting data

To connect slots, click the ![](resource://icons/emblems/slot-connected-vertical.png) icon to open a menu of
compatible source or target data slots. On creating a connection, a line will appear between
the two affected slots to indicate a connection. Alternatively, you can drag a 
line between the slots with your mouse.

To disconnect two slots, click ![](resource://icons/emblems/slot-connected-vertical.png) and select 
![](resource://icons/actions/cancel.png) **Disconnect**.

The connection type is indicated by its color. <span style="color: #404040">Gray</span> connections 
are regular edges without any meaning. <span style="color: blue">Blue</span> links indicate that 
an automated data conversion is applied. <span style="color: red">Red</span> connections indicate 
that the data types are incompatible.

### Modifying slots

Some algorithms allow you to add additional slots. To add another slot, click the ![](resource://icons/actions/list-add.png)
**Add** button and select which data type the slot should have. Depending on the algorithm, 
you can make an output slot inherit the type of an existing input slot.

