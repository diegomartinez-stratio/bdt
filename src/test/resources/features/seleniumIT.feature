@web
Feature: Selenium run test

  Background: Open session and navigate to default site
    Given My app is running in 'www.google.com:80'
    When I browse to '/'

  Scenario: Dummy scenario
    When '1' elements exists with 'id:lst-ib'
    Then I type 'stratio' on the element on index '0'
    When '1' elements exists with 'id:lst-ib'
    And I click on the element on index '0'
    And I wait '1' seconds

  Scenario: Assert error scenario
    When '1' elements exists with 'xpath:/html/body/header/div/div[4]/div/div/ul/li[1]/a/img'
    And I click on the element on index '0'
    When '1' elements exists with 'xpath:/html/body/div[5]/div/div[2]/div[3]/div[1]/div/article/div[1]/div[1]/hgroup/h2/a'
    Then a text 'hola' exists