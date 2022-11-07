Feature: Basic cypress tests

  @cypress
  Scenario: Test cypress
    When I run Cypress testcase 'basictest.cy.js'
      | notused       | notused                       |

  @cypress
  Scenario: Test cypress
    When I run all Cypress tests with host 'google.es' and token 'test'
