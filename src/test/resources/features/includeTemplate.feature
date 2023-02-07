Feature: Include Template

  Scenario:includeAspect with params
    Then I run 'echo <paramtest>' locally
    And the command output contains '1'

  Scenario:testInclude (test) blablabla
    Then I run 'echo 2' locally
    And the command output contains '1'

  Scenario:testInclude (test) blablabla withParams
    Then I run 'echo <paramtest>' locally
    And the command output contains '1'

  Scenario:prueba test include con comas y params, dos
    Then I run 'echo <paramtest>' locally
    And the command output contains '1'