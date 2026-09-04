interface AuthSession {
  accessToken: string
}

const requiredStringEnv = (name: string): string => {
  const value: unknown = Cypress.env(name)

  expect(value, `Cypress environment variable ${name}`).to.be.a('string')
  expect(value, `Cypress environment variable ${name}`).not.to.equal('')
  return value as string
}

const originOf = (value: string, name: string): string => {
  const url = new URL(value)
  expect(url.protocol, `${name} must use HTTPS`).to.equal('https:')
  return url.origin
}

describe('ZAP Business BCeID authentication', () => {
  it('writes a short-lived VDYP access token to the runner', () => {
    const frontendUrl = requiredStringEnv('frontendUrl')
    const ssoOrigin = originOf(requiredStringEnv('ssoOrigin'), 'ssoOrigin')
    const bceidOrigin = originOf(
      requiredStringEnv('bceidOrigin'),
      'bceidOrigin',
    )
    const username = requiredStringEnv('bceidUsername')
    const password = requiredStringEnv('bceidPassword')
    const tokenPath = requiredStringEnv('tokenPath')
    const frontendOrigin = originOf(frontendUrl, 'frontendUrl')

    cy.visit(frontendUrl)

    // Cypress 13 injects document.domain and treats these gov.bc.ca hosts as
    // one superdomain. Assert each destination before entering credentials;
    // cy.origin() is only needed here after upgrading to Cypress 14 or later.
    cy.location('origin', { timeout: 60_000 }).should('equal', ssoOrigin)
    cy.get('#social-bceidbusiness', { timeout: 60_000 })
      .should('be.visible')
      .click()

    cy.location('origin', { timeout: 60_000 }).should('equal', bceidOrigin)
    cy.get('#user', { timeout: 60_000 })
      .should('be.visible')
      .type(username, { log: false })
    cy.get('#password').type(password, { log: false })
    cy.get('[name=btnSubmit]').click()

    cy.location('origin', { timeout: 120_000 }).should('equal', frontendOrigin)

    const authSessionKey = window.btoa('vdyp-auth-user')
    cy.window({ timeout: 60_000 })
      .should((appWindow) => {
        expect(
          appWindow.sessionStorage.getItem(authSessionKey),
          'VDYP auth session',
        ).to.be.a('string')
      })
      .then((appWindow) => {
        const encodedSession = appWindow.sessionStorage.getItem(authSessionKey)
        expect(encodedSession, 'VDYP auth session').not.to.be.null

        const authSession = JSON.parse(
          appWindow.atob(encodedSession!),
        ) as AuthSession
        expect(authSession.accessToken, 'access token').to.be.a('string')
        expect(authSession.accessToken, 'access token').not.to.equal('')

        return cy.task(
          'writeZapAccessToken',
          { filePath: tokenPath, token: authSession.accessToken },
          { log: false },
        )
      })
  })
})
