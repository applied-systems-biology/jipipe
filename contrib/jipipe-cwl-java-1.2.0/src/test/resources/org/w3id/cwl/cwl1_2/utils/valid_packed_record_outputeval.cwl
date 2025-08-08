arguments: [touch GRC$(inputs.organism_prefix)38.primary_assembly.genome.fa ; touch  gencode.v$(inputs.gtf_version).primary_assembly.annotation.gtf]
baseCommand: [bash, -c]
class: CommandLineTool
cwlVersion: v1.2
inputs:
- {default: M21, id: gtf_version, type: string}
- {default: mouse, id: organism, type: string}
- {default: m, id: organism_prefix, type: string}
outputs:
- id: references
  outputBinding: {outputEval: '$({ "genome_fa": { "class": "File", "path": runtime.outdir+"/"+"GRC"
      + inputs.organism_prefix + "38.primary_assembly.genome.fa" }, "annotation_gtf":
      { "class": "File", "path": runtime.outdir+"/"+"gencode.v" + inputs.gtf_version
      + ".primary_assembly.annotation.gtf" } })'}
  type:
    fields:
    - {name: genome_fa, type: File}
    - {name: annotation_gtf, type: File}
    name: References
    type: record
requirements:
- {class: InlineJavascriptRequirement}
