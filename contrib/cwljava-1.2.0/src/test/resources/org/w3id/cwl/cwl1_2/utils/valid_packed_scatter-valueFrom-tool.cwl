baseCommand: echo
class: CommandLineTool
cwlVersion: v1.2
inputs:
- id: scattered_message
  inputBinding: {position: 2}
  type: string
- id: message
  inputBinding: {position: 1}
  type: string
outputs:
- {id: out_message, type: stdout}
requirements:
- {class: InlineJavascriptRequirement}
