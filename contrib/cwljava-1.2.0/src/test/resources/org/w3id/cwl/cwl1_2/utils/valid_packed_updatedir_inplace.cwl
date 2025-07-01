arguments: [touch, inp/blurb]
class: CommandLineTool
cwlVersion: v1.2
inputs:
- {id: r, type: Directory}
outputs:
- id: out
  outputBinding: {glob: inp}
  type: Directory
requirements:
- class: InitialWorkDirRequirement
  listing:
  - {entry: $(inputs.r), entryname: inp, writable: true}
- {class: InplaceUpdateRequirement, inplaceUpdate: true}
- {class: InlineJavascriptRequirement}
