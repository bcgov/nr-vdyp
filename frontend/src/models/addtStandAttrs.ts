export default class AddtStandAttrs {
  public loreyHeight: string
  public wholeStemVol75: string
  public basalArea125: string
  public wholeStemVol125: string
  public cuVol: string
  public cuNetDecayVol: string
  public cuNetDecayWasteVol: string

  constructor(blob: any) {
    this.loreyHeight =
      blob && typeof blob.loreyHeight === 'string' ? blob.loreyHeight : ''
    this.wholeStemVol75 =
      blob && typeof blob.wholeStemVol75 === 'string' ? blob.wholeStemVol75 : ''
    this.basalArea125 =
      blob && typeof blob.basalArea125 === 'string' ? blob.basalArea125 : ''
    this.wholeStemVol125 =
      blob && typeof blob.wholeStemVol125 === 'string'
        ? blob.wholeStemVol125
        : ''
    this.cuVol = blob && typeof blob.cuVol === 'string' ? blob.cuVol : ''
    this.cuNetDecayVol =
      blob && typeof blob.cuNetDecayVol === 'string' ? blob.cuNetDecayVol : ''
    this.cuNetDecayWasteVol =
      blob && typeof blob.cuNetDecayWasteVol === 'string'
        ? blob.cuNetDecayWasteVol
        : ''
  }
}
