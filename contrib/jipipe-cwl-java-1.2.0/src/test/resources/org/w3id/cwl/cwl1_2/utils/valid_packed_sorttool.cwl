baseCommand: sort
class: CommandLineTool
cwlVersion: v1.2
doc: Sort lines using the `sort` command
inputs:
- id: reverse
  inputBinding: {position: 1, prefix: -r}
  type: boolean
- id: input
  inputBinding: {position: 2}
  type: File
outputs:
- id: output
  outputBinding: {glob: output.txt}
  type: File
requirements:
- {class: InlineJavascriptRequirement}
stdout: output.txt
