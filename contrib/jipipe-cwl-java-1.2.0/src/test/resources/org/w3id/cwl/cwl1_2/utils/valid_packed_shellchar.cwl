arguments:
- {shellQuote: false, valueFrom: foo 1>&2}
baseCommand: echo
class: CommandLineTool
cwlVersion: v1.2
doc: "Ensure that arguments containing shell directives are not interpreted and\n\
  that `shellQuote: false` has no effect when ShellCommandRequirement is not in\n\
  effect.\n"
inputs: []
outputs:
- {id: stdout_file, type: stdout}
- {id: stderr_file, type: stderr}
requirements:
- {class: InlineJavascriptRequirement}
