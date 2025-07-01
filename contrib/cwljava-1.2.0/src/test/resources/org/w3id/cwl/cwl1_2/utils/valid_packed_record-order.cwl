baseCommand: python
class: CommandLineTool
cwlVersion: v1.2
hints:
- {class: DockerRequirement, dockerPull: 'docker.io/python:3-slim'}
inputs:
- default: {class: File, location: args.py}
  id: args.py
  inputBinding: {position: -1}
  type: File
- id: a
  inputBinding: {position: 5, prefix: -a}
  type:
    fields:
    - inputBinding: {position: 1, prefix: -b}
      name: b
      type: int
    - inputBinding: {position: 3, prefix: -c}
      name: c
      type: int
    type: record
- id: d
  inputBinding: {position: 6, prefix: -d}
  type:
    fields:
    - inputBinding: {position: 2, prefix: -e}
      name: e
      type: int
    - inputBinding: {position: 4, prefix: -f}
      name: f
      type: int
    type: record
outputs:
- id: args
  type: {items: string, type: array}
requirements: []
