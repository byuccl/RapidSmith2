# Testing

RapidSmith2 is automatically tested by a collection of JUnit tests, gradle
wrappers, and Travis-CI. Contributers to the RapidSmith2 project are strongly
encouraged to include JUnit tests in their pull requests. This will greatly
shorten the review process, and it helps to ensure quality of new and existing
code.

## Gradle

Gradle is used to build and test RapidSmith2. This process has been automated in
the provided scripts and configuration files. In addition, many IDEs support
gradle integrations. To run the build from a bash shell, type:
```
./gradlew build
```
After the build finishes, a summary of all tests will be displayed. Be sure to
check that all of them passed.

## JUnit

Many RapidSmith2 structures and classes are tested by JUnit. These tests can be
found in the src/test/ directory. They are written in Java - the same language
as RapidSmith2 - to make the learning curve easier for new developers. Kotlin
tests can also be found, but Java is preferred.

## Travis-CI

The Travis-CI github plugin is used to automatically run the gradle/JUnit build
as discussed above. This process occurs whenever a collaborator pushes a branch
or opens a pull request, and it is configured in .travis.yml

### The Process

Whenever a collaborator pushes a branch, github notifies Travis-CI which will
then start up a build slave to pull and test that branch. Once Travis-CI has
tested the branch, it will report back to github whether or not the build was a
success. The same process also occurs for pull requests. The results of each
test can be seen as either a green checkmark or a red X on the [branch
page](https://github.com/byuccl/RapidSmith2/branches), the [pull request
page](https://github.com/byuccl/RapidSmith2/pulls), or the [Travis-CI
RapidSmith2 page](https://travis-ci.org/byuccl/RapidSmith2). Additionally, the
README.md in the root of the repository contains a badge that indicates the
health of the master branch tests.

### Configuration

The Travis-CI build is configured with the .travis.yml file in the root of the
repository. If you ever need to change the configuration, please see the
[official documentation](https://docs.travis-ci.com/user/customizing-the-build/)
