# Organizing annotations

Annotation types support inheritance, making it possible to put annotations into higher-order
category annotations.

## Inhertiances

Connections are made between *slots* that are left and right to the colored area of 
a node. 

Each node has one output `This` (right-hand side), that can be connected to an input (left)
to make the input inherit from `This`. 

### Connecting slots

To connect slots, click the ![](resource://icons/chevron-bottom.png) icon to open a menu of
compatible source or target data slots. On creating a connection, a line will appear between
the two affected slots to indicate a connection.

To disconnect two slots, click ![](resource://icons/chevron-bottom.png) and select 
the ![](resource://icons/remove.png) disconnect command.

### Multiple inheritance

Annotation types allow multiple inputs. To add another slot, click the ![](resource://icons/add.png)
add button and select ![](resource://icons/label.png) *Inheritance*.