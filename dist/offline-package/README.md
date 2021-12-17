# Offline packages

## Prerequisites

Add full installations of Fiji into the fiji-win, fiji-linux, and fiji-osx folders.
Rename the installations to JIPipe.app
These installations must come with **all dependencies** installed, including **dependency libraries**,
and **dependency plugins** (plugin manager).

## Generating the packages

Run `./generate-packages.sh` in bash. zip must be available.

On Windows, use Cygwin, MSYS2, or WSL.
