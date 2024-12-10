export default class SICurve {
  public ageRange: string
  public species: string
  public curveName: string

  constructor(blob: any) {
    this.ageRange = blob && blob.ageRange ? blob.ageRange.toString() : ''
    this.species = blob && blob.species ? blob.species.toString() : ''
    this.curveName = blob && blob.curveName ? blob.curveName.toString() : ''
  }
}
