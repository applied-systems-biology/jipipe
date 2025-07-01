arguments: [mkdir, $(inputs.dirname)]
class: CommandLineTool
cwlVersion: v1.2
inputs:
- {id: dirname, type: string}
outputs:
- id: out
  outputBinding: {glob: $(inputs.dirname)}
  type: Directory
requirements:
- {class: InlineJavascriptRequirement}
