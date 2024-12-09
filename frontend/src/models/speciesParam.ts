export default class SpeciesParam {
  public species: string
  public percentComp: string
  public totAge: string
  public bhAge: string
  public height: string
  public si: string
  public ytbh: string

  constructor(blob: any) {
    this.species = blob && blob.species ? blob.species.toString() : ''
    this.percentComp =
      blob && blob.percentComp ? blob.percentComp.toString() : ''
    this.totAge = blob && blob.totAge ? blob.totAge.toString() : ''
    this.bhAge = blob && blob.bhAge ? blob.bhAge.toString() : ''
    this.height = blob && blob.height ? blob.height.toString() : ''
    this.si = blob && blob.si ? blob.si.toString() : ''
    this.ytbh = blob && blob.ytbh ? blob.ytbh.toString() : ''
  }
}
