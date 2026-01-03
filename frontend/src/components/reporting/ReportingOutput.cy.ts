import ReportingOutput from './ReportingOutput.vue'

describe('<ReportingOutput />', () => {
  const defaultProps = {
    data: [
      'Line 1: Output data',
      'Line 2: Additional output',
      'Line 3: Final output',
    ],
  }

  it('renders with default props', () => {
    cy.mount(ReportingOutput, { props: defaultProps })

    // Check if the component is rendered
    cy.get('div').should('exist')

    // Check if the data is rendered correctly
    cy.get('div').should('contain', 'Line 1: Output data')
    cy.get('div').should('contain', 'Line 2: Additional output')
    cy.get('div').should('contain', 'Line 3: Final output')
  })

  it('renders an empty state if data is empty', () => {
    cy.mount(ReportingOutput, { props: { data: [] } })

    // Check if the div is empty
    cy.get('div').should('be.empty')
  })

  it('supports long data and is scrollable', () => {
    const longData = new Array(50)
      .fill(null)
      .map((_, i) => `Line ${i + 1}: Long data entry`)

    cy.mount(ReportingOutput, { props: { data: longData } })

    // Check if the first and last lines are rendered
    cy.get('div.ml-2').should('contain', 'Line 1: Long data entry')
    cy.get('div.ml-2').should(
      'contain',
      `Line ${longData.length}: Long data entry`,
    )

    // Verify scrollability
    cy.get('div.ml-2').scrollTo('bottom')
  })
})
