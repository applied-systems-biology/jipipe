# JIPipe Extension Builder

The extension builder allows you to publish one or multiple pipelines into a custom algorithm node without the need for
programming.

On opening the extension builder, you will see three additional tabs:

<table>
<tr><td><img src="image://icons/actions/wrench.png"/></td><td>Extension settings</td></tr>
<tr><td><img src="image://icons/actions/module.png"/></td><td>Extension contents</td></tr>
</table>

## Extension settings

This tab allows you to change some extension metadata. The most important setting is the `ID` that 
uniquely identifies your extension to allow other projects to trace back dependencies.

## Extension contents

This tab contains all algorithms types defined in the extension. You can use it to 
add more content, or delete entries.

## Installation

If you save your extension into the `Fiji.app/plugins` folder, it is automatically loaded just like any ImageJ plugin.