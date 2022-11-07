// basictest.spec.js created with Cypress
//
// Start writing your Cypress tests below!
// If you're unfamiliar with how Cypress works,
// check out the link below and learn how to write your first test:
// https://on.cypress.io/writing-first-test

it('should pass', () => {
  expect(true).to.equal(true);
});

describe('Google Search', () => {
  it('loads search page', () => {
    cy.visit('https://www.google.com');
  });
});