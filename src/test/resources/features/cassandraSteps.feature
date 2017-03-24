Feature: Cassandra steps test

  Scenario: Connect to Cassandra
    Given I connect to 'Cassandra' cluster at '${CASSANDRA_HOST}'
    When I create a Cassandra keyspace named 'cassandrakeyspace'
    When a Cassandra keyspace 'cassandrakeyspace' exists
    When I create a Cassandra table named 'tabletest' using keyspace 'cassandrakeyspace' with:
    | description     | location  |
    | UUID            | text      |
    | PK              |           |
    When a Cassandra keyspace 'cassandrakeyspace' contains a table 'tabletest'
    When a Cassandra keyspace 'cassandrakeyspace' contains a table 'tabletest' with '0' rows
    When I truncate a Cassandra table named 'tabletest' using keyspace 'cassandrakeyspace'
    When I drop a Cassandra table named 'tabletest' using keyspace 'cassandrakeyspace'
    When I drop a Cassandra keyspace 'cassandrakeyspace'

#    When a Cassandra keyspace 'cassandrakeyspace' contains a table 'tabletest' with values:
#      |  description |
#    When I load a Cassandra script with name '.+' into the keyspace '.+'
#    When I create a Cassandra index named '.+' with schema '.+' of type 'json|string' in table '.+' using magic_column '.+' using keyspace '.+' with:
#    When I create a Cassandra index named '.+' in table '.+' using magic_column '.+' using keyspace '.+'
