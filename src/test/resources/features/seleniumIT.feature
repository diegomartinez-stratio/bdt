@web
Feature: Selenium run test

  Scenario: Dummy scenario
    Given My app is running in 'www.google.com:80'
    When I browse to '/'
    When '1' elements exists with 'id:lst-ib'
    And I click on the element on index '0'
    Then I type 'stratio' on the element on index '0'
    Then I send 'ENTER' on the element on index '0'
    And I wait '1' seconds

  Scenario: Dummy scenario with iframes
    Given My app is running in 'jenkins.stratio.com'
    When I browse to '/'
    Then I maximize the browser
    When I switch to iframe with 'id:_yuiResizeMonitor'
    Then I wait '1' seconds
    And I switch to a parent frame
    When '1' elements exists with 'id:_yuiResizeMonitor'
    Then I switch to the iframe on index '0'

  Scenario: Dummy scenario with HTTPS
    Given My app is running in 'www.google.com'
    When I securely browse to '/'

