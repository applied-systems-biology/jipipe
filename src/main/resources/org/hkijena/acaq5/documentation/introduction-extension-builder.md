<p style="text-align:center;"><img src="image://logo-400.png" width="400" height="146"/></p>

# ACAQ5 Extension Builder

ACAQ5 supports two types of extensions:

<table>
<tr><td><img src="image://icons/module-java-32.png"/></td><td>Java extensions</td></tr>
<tr><td><img src="image://icons/module-json-32.png"/></td><td>JSON extensions</td></tr>
</table>

While Java extensions need to be developed as ImageJ java plugin, JSON extensions can be created without
programming via this user interface.

On opening the extension builder, you will see three additional tabs:

<table>
<tr><td><img src="image://icons/wrench.png"/></td><td>Extension settings</td></tr>
<tr><td><img src="image://icons/module.png"/></td><td>Extension contents</td></tr>
<tr><td><img src="image://icons/connect.png"/></td><td>Annotations</td></tr>
</table>

## Extension settings

This tab allows you to change some extension metadata. The most important setting is the `ID` that 
uniquely identifies your extension to allow other projects to trace back dependencies.

## Extension contents

This tab contains all algorithms and annotation types defined in the extension. You can use it to 
add more content, or delete entries.

## Annotations

A graphical editor to create annotation types, and setup a hierarchy of annotation types.

## Installation

If you save your extension into the `Fiji.app/plugins` folder, it is automatically loaded just like any ImageJ plugin.