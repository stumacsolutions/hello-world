This is a simple hello world application built using Spring Boot. It's 
main function is to act as a testing area for gradle, Spring Boot and 
the use of Snap CI to create a continuous deployment pipeline into an 
AWS environment orchestrated by Docker Cloud.

# Build Pipeline Status
[![Build Status](https://snap-ci.com/stumacsolutions/hello-world/branch/master/build_image)](https://snap-ci.com/stumacsolutions/hello-world/branch/master)

The Snap CI pipeline for this project can be found here:
https://snap-ci.com/stumacsolutions/hello-world/branch/master

Each stage in the pipeline is a single gradle wrapper command. 
The tasks run in each command are listed below in the order they are
executed.
* classes testClasses
* checkStyle
* findBugs
* test jacocoTestReport
* mutationTest
* acceptanceTest
* publishContainer

# Live Environment
The pipeline creates and pushes a Docker container to a Docker Hub
registry which can be found here:
https://hub.docker.com/r/stumacsolutions/hello-world-container/

Docker Cloud monitors this registry and when it detects a new version 
of the container it deploys it automatically. There is a short period
of downtime at present when this happens. When the deployment is done
the application can be seen in all its glory here (user/password):
http://lb.hello-world.fb305fe4.svc.dockerapp.io/

The .gradle and build folders are collected as artifacts on every stage
of the pipeline. These are automatically made available to all subsequent
stages automatically by Snap CI. The advantage of this approach is that
it leverages the incremental building capabilities of Gradle, without
feeling the need to squash multiple activities into a single stage of the 
pipeline to avoid the repetition of tasks such as compilation.

# Gradle Commands
This section documents custom Gradle commands built into this project.
Those tasks provided automatically by the various plugins which have 
been incorporated are not documented here.

## ./gradlew acceptanceTest
This command will run the acceptance tests for the project. Cucumber has
been used to implement acceptance tests. The Gherkin feature files are
located under src/test/resources/features. The Java code implementing 
the steps is located under src/test/java/acceptance/steps.

## ./gradlew assembleContainer
This command will build a Docker container holding the Spring Boot 
application. This command relies on Docker being available on the host
running the command.

## ./gradlew checkstyle
This command is a simple convenience command which wraps up the tasks 
provided by the checkstyle plugin.

## ./gradlew findbugs
This command is a simple convenience command which wraps up the tasks 
provided by the findbugs plugin.

## ./gradlew mutationTest
This command will perform mutation testing on the project. This ensures
that the tests are of a high standard and are performing meaningful
assertions.

## ./gradlew publishContainer
This command will publish the Docker container produced by the 
assembleContainer task. This command relies on Docker being available 
on the host running the command. The following environment variables are 
also required
* DOCKER_EMAIL = The email address for the registry into which to publish.
* DOCKER_USER  = The username for the registry into which to publish.
* DOCKER_PASS  = The password for the registry into which to publish.