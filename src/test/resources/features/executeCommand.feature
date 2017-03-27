Feature: Simple run test

  Background: Connect to bootstrap machine
    Given I open a ssh connection to '${SSH}' with user 'root' and password 'stratio'

  Scenario: Default exit status - Successful (0)
    Then I run 'ls /tmp' in the ssh connection

  Scenario: Custom exit status - Wrong (not 0)
    Then I run 'lss /tmp' in the ssh connection with exit status '127'

  Scenario: Default exit status - Store/retrieve in/from environment variable
    When I run 'ls -la /tmp' in the ssh connection and save the value in environment variable 'DEFEXSTAT'
    Then '!{DEFEXSTAT}' contains 'total'

  Scenario: Custom exit status - Store/retrieve in/from environment variable
    When I run 'lss /tmp' in the ssh connection with exit status '127' and save the value in environment variable 'CUSEXSTAT'
    Then '!{CUSEXSTAT}' contains 'lss'


  Scenario: Outbound Inbound file
    When I outbound copy 'exampleJSON.conf' through a ssh connection to '/tmp/test/exampleJSON.conf'
    Then I inbound copy '/tmp/test/exampleJSON.conf' through a ssh connection to 'fileFromSsh.conf'

#  Scenario: Outbound directory
#    When I outbound copy 'schemas' through a ssh connection to '/tmp'

  Scenario: Default exit status - Check output
    When I run 'ls -la /tmp' in the ssh connection
    Then the command output contains 'total'
