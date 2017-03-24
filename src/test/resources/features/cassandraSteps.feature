Feature: Cassandra steps test

  Scenario: Connect to Cassandra
    Given I connect to 'Cassandra' cluster at '${CASSANDRA_HOST}'

  Scenario: Create a keyspace in Cassandra
    Given I create a Cassandra keyspace named 'cassandrakeyspace'
    Then a Cassandra keyspace 'cassandrakeyspace' exists

  Scenario: Create a table in Cassandra
    Given I create a Cassandra table named 'tabletest' using keyspace 'cassandrakeyspace' with:
    | description     | location  |
    | UUID            | text      |
    | PK              |           |
    Then a Cassandra keyspace 'cassandrakeyspace' contains a table 'tabletest'
    And a Cassandra keyspace 'cassandrakeyspace' contains a table 'tabletest' with '0' rows

  Scenario: Create a table with null PK in Cassandra
    Given I create a Cassandra table named 'tabletest_nopk' using keyspace 'cassandrakeyspace' with:
      | description     | location  |
      | UUID            | text      |

  Scenario: Exception captured when insert value in cassandra
    Given I insert in keyspace 'cassandrakeyspace' and table 'tabletest' with:
      | description     | location  |
      | UUID            | text      |

  Scenario: Querying table in Cassandra
    When a Cassandra keyspace 'cassandrakeyspace' contains a table 'tabletest' with values:
      |  description-uuid |

  Scenario: Truncate table in Cassandra
    Given I truncate a Cassandra table named 'tabletest' using keyspace 'cassandrakeyspace'

  Scenario: Drop table in Cassandra
    Given I drop a Cassandra table named 'tabletest' using keyspace 'cassandrakeyspace'

  Scenario: Drop keyspace in Cassandra
    Given I drop a Cassandra keyspace 'cassandrakeyspace'