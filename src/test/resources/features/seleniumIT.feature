@web
Feature: Selenium run test

  Background: Open session and navigate to default site
    Given My app is running in 'www.google.com:80'
    When I browse to '/'

  Scenario: Dummy scenario
    When '1' elements exists with 'id:lst-ib'
    And I click on the element on index '0'
    Then I type 'stratio' on the element on index '0'
    Then I send 'ENTER' on the element on index '0'
    And I wait '1' seconds


  Scenario: Given steps test

