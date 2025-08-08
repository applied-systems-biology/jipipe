class: Workflow
cwlVersion: v1.2
inputs:
- {default: me, id: first, type: string}
outputs:
- {id: last, outputSource: first, type: string}
requirements: []
steps: []
