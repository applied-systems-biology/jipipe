baseCommand: 'true'
class: CommandLineTool
cwlVersion: v1.2
inputs:
- {default: 1, id: length, type: int}
- id: bar
  type:
    fields:
    - {name: length, type: int}
    name: bar_record
    type: record
- id: baz
  type:
    fields:
    - name: length
      type:
        fields:
        - {name: bap, type: int}
        name: length_record
        type: record
    name: baz_record
    type: record
outputs:
- id: output1
  outputBinding: {outputEval: $(inputs.length)}
  type: int
- id: output2
  outputBinding: {outputEval: $(inputs.bar.length)}
  type: int
- id: output3
  outputBinding: {outputEval: $(inputs.baz.length.bap)}
  type: int
requirements: []
