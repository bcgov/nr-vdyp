export class ValidationBase {
  public validateRange(value: number, min: number, max: number): boolean {
    return value >= min && value <= max
  }

  public validateRequired(value: any): boolean {
    return !!value
  }
}
