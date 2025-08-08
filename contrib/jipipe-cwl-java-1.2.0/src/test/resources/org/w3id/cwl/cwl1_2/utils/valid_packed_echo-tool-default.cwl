baseCommand: [echo, -n]
class: CommandLineTool
cwlVersion: v1.2
inputs:
- default: tool_default
  id: in
  inputBinding: {position: 1}
  type: string
outputs:
- id: out
  outputBinding: {glob: out.txt, loadContents: true, outputEval: '$(self[0].contents)'}
  type: string
requirements:
- {class: InlineJavascriptRequirement}
stdout: out.txt
