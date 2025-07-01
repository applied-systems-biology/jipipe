baseCommand: rev
class: CommandLineTool
cwlVersion: v1.2
doc: Reverse each line using the `rev` command
inputs:
- id: input
  inputBinding: {}
  type: File
outputs:
- id: output
  outputBinding: {glob: output.txt}
  type: File
requirements:
- {class: InlineJavascriptRequirement}
stdout: output.txt
