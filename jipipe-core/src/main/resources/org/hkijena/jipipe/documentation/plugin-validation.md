# Plugin validation

Plugins can have various dependencies that are provided by other plugins.
JIPipe will notify you about such missing dependencies via this user interface.

On the left hand side you see a table with all reported problems, such as missing
plugins, algorithms, or annotation types.

## Resolving issues

The most common issue is at location `Extensions > jipipe:json-extension-loader > Unregistered JSON extensions`
and indicates that a JSON extension cannot be loaded because a dependency plugin is missing.
The ID of this dependency is given in the message.

It can happen that dependencies have been renamed or merged with existing plugins - meaning
that all required contents actually exist. To resolve those issues, open the JSON extension 
in the extension builder and confirm loading it. After loading the extension, save it to 
update the dependency list.
