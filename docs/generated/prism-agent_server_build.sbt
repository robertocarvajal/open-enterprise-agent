This is an sbt build definition file for a Scala project. It describes the project structure, dependencies, and settings for building, packaging, and deploying the application. The code defines two main subprojects: wallet-api and server.

Key aspects of the build definition:

Custom keys: The apiBaseDirectory key is defined to specify the base directory for PrismAgent API specifications.
In-this-build settings: Global settings for the entire build are specified, including the organization, Scala version, resolvers for GitHub Packages and JetBrains Space Maven Repository, and other settings.
Coverage settings: The coverageDataDir is set to store the code coverage data in the target directory.
root project: This is an aggregate project that includes both wallet-api and server subprojects.
wallet-api project: This subproject has a specific set of dependencies for key management, defined in keyManagementDependencies.
server project: This subproject has its own set of dependencies (serverDependencies) and settings for the main class, OpenAPI generation, Docker image configuration, build information, and more. It also depends on the wallet-api subproject. The server project enables the following sbt plugins:
OpenApiGeneratorPlugin: For generating API classes from an OpenAPI specification.
JavaAppPackaging: For packaging the application as a standalone Java app.
DockerPlugin: For building and publishing a Docker image for the application.
BuildInfoPlugin: For generating build information (e.g., version, Scala version) at compile-time.
Release process: A custom release process is defined using sbt-release plugin, which includes steps like checking snapshot dependencies, setting the release version, running tests, publishing the Docker image, and setting the next version.
In summary, the build definition file organizes the project into two main subprojects, manages their dependencies, and configures the build process, including OpenAPI generation, Docker image creation, and application packaging. The sbt-release plugin is used to automate the release process, providing a streamlined way to handle versioning and deployment.
