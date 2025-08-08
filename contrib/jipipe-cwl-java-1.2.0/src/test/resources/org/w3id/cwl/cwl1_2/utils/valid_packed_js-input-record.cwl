baseCommand: [echo]
class: CommandLineTool
cwlVersion: v1.0
inputs:
- id: message
  type:
    fields:
    - inputBinding: {position: 1}
      name: text
      type: string
    - inputBinding: {position: 0, prefix: -n}
      name: newlines
      type: ['null', boolean]
    name: message_object
    type: record
outputs:
- id: out
  outputBinding: {glob: output.txt, loadContents: true, outputEval: '$(self[0].contents)'}
  type: string
requirements:
- {class: InlineJavascriptRequirement}
stdout: output.txt
