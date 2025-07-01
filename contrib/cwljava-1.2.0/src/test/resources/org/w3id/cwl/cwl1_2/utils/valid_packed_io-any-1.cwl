baseCommand: 'true'
class: CommandLineTool
cwlVersion: v1.2
inputs:
- {id: bar, type: Any}
outputs:
- id: t1
  outputBinding: {outputEval: $(inputs.bar.class || inputs.bar)}
  type: Any
requirements:
- {class: InlineJavascriptRequirement}
