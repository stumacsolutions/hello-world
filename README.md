This is a simple hello world application built using Spring Boot.

# Build Status
[![Build Status](https://snap-ci.com/stumacsolutions/hello-world/branch/master/build_image)](https://snap-ci.com/stumacsolutions/hello-world/branch/master)

# Gradle Commands

## ./gradlew acceptanceTest
This command will run the acceptance tests for the project. Cucumber has
been used to implement acceptance tests. The Gherkin feature files are
located under src/test/resources/features. The Java code implementing 
the steps is located under src/test/java/acceptance/tests.

## ./gradlew check
This command will perform static analysis of the code base using both
Checkstyle and FindBugs.

## ./gradlew pitest
This command will perform mutation testing on the project. This ensures
that the tests are of a high standard and are performing meaningful
assertions.