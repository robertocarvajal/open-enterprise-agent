import org.scoverage.coveralls.Imports.CoverallsKeys.*
import sbtbuildinfo.BuildInfoPlugin.autoImport.*

// externalResolvers += "ScalaLibrary packages" at "https://maven.pkg.github.com/input-output-hk/anoncreds-rs" // use plugin"sbt-github-packages"

inThisBuild(
  Seq(
    organization := "io.iohk.atala",
    scalaVersion := "3.3.1",
    fork := true,
    run / connectInput := true,
    releaseUseGlobalVersion := false,
    versionScheme := Some("semver-spec"),
    githubOwner := "input-output-hk",
    githubRepository := "atala-prism-building-blocks",
    resolvers += "Local Maven Repository" at "file://" + Path.userHome.absolutePath + "/.m2/repository",
    resolvers += "jitpack" at "https://jitpack.io",
  )
)

// Fixes a bug with concurrent packages download from GitHub registry
Global / concurrentRestrictions += Tags.limit(Tags.Network, 1)

coverageDataDir := target.value / "coverage"
coberturaFile := target.value / "coverage" / "coverage-report" / "cobertura.xml"

inThisBuild(
  Seq(
    scalacOptions ++= Seq(
      "-encoding",
      "UTF-8",
      "-feature",
      "-deprecation",
      "-unchecked",
      "-Dquill.macro.log=false", // disable quill macro logs
      "-Wunused:all",
      "-Wconf:any:warning" // TODO: change unused imports to errors, Wconf configuration string is different from scala 2, figure out how!
      // TODO "-feature",
      // TODO "-Xfatal-warnings",
      // TODO "-Yexplicit-nulls",
      // "-Ysafe-init",
    )
  )
)

lazy val V = new {
  val munit = "1.0.0-M8" // "0.7.29"
  val munitZio = "0.2.0"

  // https://mvnrepository.com/artifact/dev.zio/zio
  val zio = "2.0.18"
  val zioConfig = "3.0.7"
  val zioLogging = "2.1.16"
  val zioJson = "0.3.0"
  val zioHttp = "3.0.0-RC4"
  val zioCatsInterop = "3.3.0"
  val zioMetricsConnector = "2.1.0"
  val zioMock = "1.0.0-RC11"
  val mockito = "3.2.16.0"
  val monocle = "3.2.0"

  // https://mvnrepository.com/artifact/io.circe/circe-core
  val circe = "0.14.6"

  val tapir = "1.9.10"

  val typesafeConfig = "1.4.3"
  val protobuf = "3.1.9"
  val testContainersScala = "0.41.3"
  val testContainersJavaKeycloak = "3.0.0" // scala-steward:off

  val doobie = "1.0.0-RC2"
  val quill = "4.7.3"
  val flyway = "9.8.3"
  val postgresDriver = "42.2.27"
  val logback = "1.4.14"
  val slf4j = "2.0.7"

  val prismSdk = "1.4.1" // scala-steward:off
  val scalaUri = "4.0.3"

  val jwtCirceVersion = "9.4.6"
  val zioPreludeVersion = "1.0.0-RC16"

  val bouncyCastle = "1.70"

  val jsonSchemaValidator = "1.3.3"

  val vaultDriver = "6.2.0"
  val micrometer = "1.11.2"

  val nimbusJwt = "10.0.0"
  val keycloak = "22.0.4" // scala-steward:off

}

/** Dependencies */
lazy val D = new {
  val zio: ModuleID = "dev.zio" %% "zio" % V.zio
  val zioStreams: ModuleID = "dev.zio" %% "zio-streams" % V.zio
  val zioLog: ModuleID = "dev.zio" %% "zio-logging" % V.zioLogging
  val zioSLF4J: ModuleID = "dev.zio" %% "zio-logging-slf4j" % V.zioLogging
  val zioJson: ModuleID = "dev.zio" %% "zio-json" % V.zioJson
  val zioHttp: ModuleID = "dev.zio" %% "zio-http" % V.zioHttp
  val zioCatsInterop: ModuleID = "dev.zio" %% "zio-interop-cats" % V.zioCatsInterop
  val zioMetricsConnectorMicrometer: ModuleID = "dev.zio" %% "zio-metrics-connectors-micrometer" % V.zioMetricsConnector
  val tapirPrometheusMetrics: ModuleID = "com.softwaremill.sttp.tapir" %% "tapir-prometheus-metrics" % V.tapir
  val micrometer: ModuleID = "io.micrometer" % "micrometer-registry-prometheus" % V.micrometer
  val micrometerPrometheusRegistry = "io.micrometer" % "micrometer-core" % V.micrometer

  val zioConfig: ModuleID = "dev.zio" %% "zio-config" % V.zioConfig
  val zioConfigMagnolia: ModuleID = "dev.zio" %% "zio-config-magnolia" % V.zioConfig
  val zioConfigTypesafe: ModuleID = "dev.zio" %% "zio-config-typesafe" % V.zioConfig

  val circeCore: ModuleID = "io.circe" %% "circe-core" % V.circe
  val circeGeneric: ModuleID = "io.circe" %% "circe-generic" % V.circe
  val circeParser: ModuleID = "io.circe" %% "circe-parser" % V.circe

  val jwtCirce = "com.github.jwt-scala" %% "jwt-circe" % V.jwtCirceVersion

  // https://mvnrepository.com/artifact/org.didcommx/didcomm/0.3.2
  val didcommx: ModuleID = "org.didcommx" % "didcomm" % "0.3.1"
  val peerDidcommx: ModuleID = "org.didcommx" % "peerdid" % "0.5.0"
  val didScala: ModuleID = "app.fmgp" %% "did" % "0.0.0+113-61efa271-SNAPSHOT"

  // Customized version of numbus jose jwt
  // from https://github.com/goncalo-frade-iohk/Nimbus-JWT_Fork/commit/8a6665c25979e771afae29ce8c965c8b0312fefb
  val nimbusJwt: ModuleID = "io.iohk.atala" % "nimbus-jose-jwt" % V.nimbusJwt

  val typesafeConfig: ModuleID = "com.typesafe" % "config" % V.typesafeConfig
  val scalaPbRuntime: ModuleID =
    "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"
  val scalaPbGrpc: ModuleID = "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion

  val testcontainersPostgres: ModuleID =
    "com.dimafeng" %% "testcontainers-scala-postgresql" % V.testContainersScala % Test
  val testcontainersVault: ModuleID = "com.dimafeng" %% "testcontainers-scala-vault" % V.testContainersScala % Test
  val testcontainersKeycloak: ModuleID =
    "com.github.dasniko" % "testcontainers-keycloak" % V.testContainersJavaKeycloak % Test

  val doobiePostgres: ModuleID = "org.tpolecat" %% "doobie-postgres" % V.doobie
  val doobieHikari: ModuleID = "org.tpolecat" %% "doobie-hikari" % V.doobie
  val flyway: ModuleID = "org.flywaydb" % "flyway-core" % V.flyway

  // For munit https://scalameta.org/munit/docs/getting-started.html#scalajs-setup
  val munit: ModuleID = "org.scalameta" %% "munit" % V.munit % Test
  // For munit zio https://github.com/poslegm/munit-zio
  val munitZio: ModuleID = "com.github.poslegm" %% "munit-zio" % V.munitZio % Test

  val zioTest: ModuleID = "dev.zio" %% "zio-test" % V.zio % Test
  val zioTestSbt: ModuleID = "dev.zio" %% "zio-test-sbt" % V.zio % Test
  val zioTestMagnolia: ModuleID = "dev.zio" %% "zio-test-magnolia" % V.zio % Test
  val zioMock: ModuleID = "dev.zio" %% "zio-mock" % V.zioMock
  val mockito: ModuleID = "org.scalatestplus" %% "mockito-4-11" % V.mockito % Test
  val monocle: ModuleID = "dev.optics" %% "monocle-core" % V.monocle % Test
  val monocleMacro: ModuleID = "dev.optics" %% "monocle-macro" % V.monocle % Test

  // LIST of Dependencies
  val doobieDependencies: Seq[ModuleID] =
    Seq(doobiePostgres, doobieHikari, flyway)
}

lazy val D_Shared = new {
  lazy val dependencies: Seq[ModuleID] =
    Seq(
      D.typesafeConfig,
      D.scalaPbGrpc,
      D.zio,
      // FIXME: split shared DB stuff as subproject?
      D.doobieHikari,
      D.doobiePostgres,
      D.zioCatsInterop
    )
}

lazy val D_SharedTest = new {
  lazy val dependencies: Seq[ModuleID] =
    D_Shared.dependencies ++ Seq(
      D.testcontainersPostgres,
      D.testcontainersVault,
      D.testcontainersKeycloak,
      D.zioCatsInterop,
      D.zioJson,
      D.zioHttp,
      D.zioTest,
      D.zioTestSbt,
      D.zioTestMagnolia,
      D.zioMock
    )
}

lazy val D_Connect = new {

  private lazy val logback = "ch.qos.logback" % "logback-classic" % V.logback % Test

  // Dependency Modules
  private lazy val baseDependencies: Seq[ModuleID] =
    Seq(D.zio, D.zioTest, D.zioTestSbt, D.zioTestMagnolia, D.zioMock, D.testcontainersPostgres, logback)

  // Project Dependencies
  lazy val coreDependencies: Seq[ModuleID] =
    baseDependencies
  lazy val sqlDoobieDependencies: Seq[ModuleID] =
    baseDependencies ++ D.doobieDependencies ++ Seq(D.zioCatsInterop)
}

lazy val D_Castor = new {

  val scalaUri = "io.lemonlabs" %% "scala-uri" % V.scalaUri

  // We have to exclude bouncycastle since for some reason bitcoinj depends on bouncycastle jdk15to18
  // (i.e. JDK 1.5 to 1.8), but we are using JDK 11
  val prismCrypto = "io.iohk.atala" % "prism-crypto-jvm" % V.prismSdk excludeAll
    ExclusionRule(
      organization = "org.bouncycastle"
    )
  val prismIdentity = "io.iohk.atala" % "prism-identity-jvm" % V.prismSdk

  // Dependency Modules
  val baseDependencies: Seq[ModuleID] =
    Seq(
      D.zio,
      D.zioTest,
      D.zioMock,
      D.zioTestSbt,
      D.zioTestMagnolia,
      D.circeCore,
      D.circeGeneric,
      D.circeParser,
      prismIdentity,
      scalaUri
    )

  // Project Dependencies
  val coreDependencies: Seq[ModuleID] = baseDependencies
}

lazy val D_Pollux = new {
  val logback = "ch.qos.logback" % "logback-classic" % V.logback % Test
  val slf4jApi = "org.slf4j" % "slf4j-api" % V.slf4j % Test
  val slf4jSimple = "org.slf4j" % "slf4j-simple" % V.slf4j % Test

  val doobiePostgres = "org.tpolecat" %% "doobie-postgres" % V.doobie
  val doobieHikari = "org.tpolecat" %% "doobie-hikari" % V.doobie

  val flyway = "org.flywaydb" % "flyway-core" % V.flyway

  val quillJdbcZio = "io.getquill" %% "quill-jdbc-zio" %
    V.quill exclude ("org.scala-lang.modules", "scala-java8-compat_3")

  val quillDoobie = "io.getquill" %% "quill-doobie" %
    V.quill exclude ("org.scala-lang.modules", "scala-java8-compat_3")

  // We have to exclude bouncycastle since for some reason bitcoinj depends on bouncycastle jdk15to18
  // (i.e. JDK 1.5 to 1.8), but we are using JDK 11
  val prismCrypto = "io.iohk.atala" % "prism-crypto-jvm" % V.prismSdk excludeAll
    ExclusionRule(
      organization = "org.bouncycastle"
    )

  // Dependency Modules
  val baseDependencies: Seq[ModuleID] = Seq(
    D.zio,
    D.zioJson,
    D.zioHttp,
    D.zioTest,
    D.zioTestSbt,
    D.zioTestMagnolia,
    D.zioMock,
    D.munit,
    D.munitZio,
    prismCrypto,
    // shared,
    logback,
    slf4jApi,
    slf4jSimple
  )

  val doobieDependencies: Seq[ModuleID] = Seq(
    D.zioCatsInterop,
    D.doobiePostgres,
    D.doobieHikari,
    D.testcontainersPostgres,
    flyway,
    quillDoobie,
    quillJdbcZio,
  )

  // Project Dependencies
  val coreDependencies: Seq[ModuleID] = baseDependencies
  val sqlDoobieDependencies: Seq[ModuleID] = baseDependencies ++ doobieDependencies
}

lazy val D_Pollux_VC_JWT = new {

  private lazy val circeJsonSchema = ("net.reactivecore" %% "circe-json-schema" % "0.3.0")
    .cross(CrossVersion.for3Use2_13)
    .exclude("io.circe", "circe-core_2.13")
    .exclude("io.circe", "circe-generic_2.13")
    .exclude("io.circe", "circe-parser_2.13")

  val zio = "dev.zio" %% "zio" % V.zio
  val zioPrelude = "dev.zio" %% "zio-prelude" % V.zioPreludeVersion

  val networkntJsonSchemaValidator = "com.networknt" % "json-schema-validator" % V.jsonSchemaValidator

  val zioTest = "dev.zio" %% "zio-test" % V.zio % Test
  val zioTestSbt = "dev.zio" %% "zio-test-sbt" % V.zio % Test
  val zioTestMagnolia = "dev.zio" %% "zio-test-magnolia" % V.zio % Test

  val scalaTest = "org.scalatest" %% "scalatest" % "3.2.16" % Test

  // Dependency Modules
  val zioDependencies: Seq[ModuleID] = Seq(zio, zioPrelude, zioTest, zioTestSbt, zioTestMagnolia)
  val circeDependencies: Seq[ModuleID] = Seq(D.circeCore, D.circeGeneric, D.circeParser)
  val baseDependencies: Seq[ModuleID] =
    circeDependencies ++ zioDependencies :+ D.jwtCirce :+ circeJsonSchema :+ networkntJsonSchemaValidator :+ D.nimbusJwt :+ scalaTest

  // Project Dependencies
  lazy val polluxVcJwtDependencies: Seq[ModuleID] = baseDependencies
}

lazy val D_EventNotification = new {
  val zio = "dev.zio" %% "zio" % V.zio
  val zioConcurrent = "dev.zio" %% "zio-concurrent" % V.zio
  val zioTest = "dev.zio" %% "zio-test" % V.zio % Test
  val zioTestSbt = "dev.zio" %% "zio-test-sbt" % V.zio % Test
  val zioTestMagnolia = "dev.zio" %% "zio-test-magnolia" % V.zio % Test

  val zioDependencies: Seq[ModuleID] = Seq(zio, zioConcurrent, zioTest, zioTestSbt, zioTestMagnolia)
  val baseDependencies: Seq[ModuleID] = zioDependencies
}

lazy val D_Pollux_AnonCreds = new {
  val baseDependencies: Seq[ModuleID] = Seq(D.zio, D.zioJson)
}

lazy val D_PrismAgent = new {

  // Added here to make prism-crypto works.
  // Once migrated to apollo, re-evaluate if this should be removed.
  val bouncyBcpkix = "org.bouncycastle" % "bcpkix-jdk15on" % V.bouncyCastle
  val bouncyBcprov = "org.bouncycastle" % "bcprov-jdk15on" % V.bouncyCastle

  val logback = "ch.qos.logback" % "logback-classic" % V.logback

  val tapirSwaggerUiBundle = "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % V.tapir
  val tapirJsonZio = "com.softwaremill.sttp.tapir" %% "tapir-json-zio" % V.tapir

  val tapirZioHttpServer = "com.softwaremill.sttp.tapir" %% "tapir-zio-http-server" % V.tapir
  val tapirHttp4sServerZio = "com.softwaremill.sttp.tapir" %% "tapir-http4s-server-zio" % V.tapir
  val http4sBlazeServer = "org.http4s" %% "http4s-blaze-server" % "0.23.12"

  val tapirRedocBundle = "com.softwaremill.sttp.tapir" %% "tapir-redoc-bundle" % V.tapir

  val tapirSttpStubServer =
    "com.softwaremill.sttp.tapir" %% "tapir-sttp-stub-server" % V.tapir % Test
  val sttpClient3ZioJson = "com.softwaremill.sttp.client3" %% "zio-json" % "3.8.16" % Test

  val quillDoobie =
    "io.getquill" %% "quill-doobie" % V.quill exclude ("org.scala-lang.modules", "scala-java8-compat_3")
  val postgresql = "org.postgresql" % "postgresql" % V.postgresDriver
  val quillJdbcZio =
    "io.getquill" %% "quill-jdbc-zio" % V.quill exclude ("org.scala-lang.modules", "scala-java8-compat_3")

  val flyway = "org.flywaydb" % "flyway-core" % V.flyway

  val vaultDriver = "io.github.jopenlibs" % "vault-java-driver" % V.vaultDriver
  val keycloakAuthz = "org.keycloak" % "keycloak-authz-client" % V.keycloak

  // Dependency Modules
  val baseDependencies: Seq[ModuleID] = Seq(
    D.zio,
    D.zioTest,
    D.zioTestSbt,
    D.zioTestMagnolia,
    D.zioConfig,
    D.zioConfigMagnolia,
    D.zioConfigTypesafe,
    D.zioJson,
    logback,
    D.zioHttp,
    D.zioMetricsConnectorMicrometer,
    D.tapirPrometheusMetrics,
    D.micrometer,
    D.micrometerPrometheusRegistry
  )
  val bouncyDependencies: Seq[ModuleID] = Seq(bouncyBcpkix, bouncyBcprov)
  val tapirDependencies: Seq[ModuleID] =
    Seq(
      tapirSwaggerUiBundle,
      tapirJsonZio,
      tapirRedocBundle,
      tapirSttpStubServer,
      sttpClient3ZioJson,
      tapirZioHttpServer,
      tapirHttp4sServerZio,
      http4sBlazeServer
    )

  val postgresDependencies: Seq[ModuleID] =
    Seq(quillDoobie, quillJdbcZio, postgresql, flyway, D.testcontainersPostgres, D.zioCatsInterop)

  // Project Dependencies
  lazy val keyManagementDependencies: Seq[ModuleID] =
    baseDependencies ++ bouncyDependencies ++ D.doobieDependencies ++ Seq(D.zioCatsInterop, D.zioMock, vaultDriver)

  lazy val iamDependencies: Seq[ModuleID] = Seq(keycloakAuthz, D.jwtCirce)

  lazy val serverDependencies: Seq[ModuleID] =
    baseDependencies ++ tapirDependencies ++ postgresDependencies ++ Seq(
      D.zioMock,
      D.mockito,
      D.monocle,
      D.monocleMacro
    )
}

publish / skip := true

val commonSetttings = Seq(
  testFrameworks ++= Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
  // Needed for Kotlin coroutines that support new memory management mode
  resolvers += "JetBrains Space Maven Repository" at "https://maven.pkg.jetbrains.space/public/p/kotlinx-coroutines/maven",
  resolvers += "jitpack" at "https://jitpack.io",
  // Override 'updateLicenses' for all project to inject custom DependencyResolution.
  // https://github.com/sbt/sbt-license-report/blob/9675cedb19c794de1119cbcf46a255fc8dcd5d4e/src/main/scala/sbtlicensereport/SbtLicenseReport.scala#L84
  updateLicenses := {
    import sbt.librarymanagement.DependencyResolution
    import sbt.librarymanagement.ivy.IvyDependencyResolution
    import sbtlicensereport.license

    val ignore = update.value
    val overrides = licenseOverrides.value.lift
    val depExclusions = licenseDepExclusions.value.lift
    val originatingModule = DepModuleInfo(organization.value, name.value, version.value)
    val resolution =
      DependencyResolution(new LicenseReportCustomDependencyResolution(ivyConfiguration.value, ivyModule.value))
    license.LicenseReport.makeReport(
      ivyModule.value,
      resolution,
      licenseConfigurations.value,
      licenseSelection.value,
      overrides,
      depExclusions,
      originatingModule,
      streams.value.log
    )
  }
)

// #####################
// #####  shared  ######
// #####################

lazy val shared = (project in file("shared"))
  // .configure(publishConfigure)
  .settings(commonSetttings)
  .settings(
    organization := "io.iohk.atala",
    organizationName := "Input Output Global",
    buildInfoPackage := "io.iohk.atala.shared",
    name := "shared",
    crossPaths := false,
    libraryDependencies ++= D_Shared.dependencies
  )
  .enablePlugins(BuildInfoPlugin)

lazy val sharedTest = (project in file("shared-test"))
  .settings(commonSetttings)
  .settings(
    organization := "io.iohk.atala",
    organizationName := "Input Output Global",
    buildInfoPackage := "io.iohk.atala.sharedtest",
    name := "sharedtest",
    crossPaths := false,
    libraryDependencies ++= D_SharedTest.dependencies
  )
  .dependsOn(shared)
  .enablePlugins(BuildInfoPlugin)

// #########################
// ### Models & Services ###
// #########################

/** Just data models and interfaces of service.
  *
  * This module must not depend on external libraries!
  */
lazy val models = project
  .in(file("mercury/mercury-library/models"))
  .settings(name := "mercury-data-models")
  .settings(
    libraryDependencies ++= Seq(D.zio),
    libraryDependencies ++= Seq(
      D.circeCore,
      D.circeGeneric,
      D.circeParser
    ), // TODO try to remove this from this module
    // libraryDependencies += D.didScala
  )
  .settings(libraryDependencies += D.nimbusJwt) //FIXME just for the DidAgent

/* TODO move code from agentDidcommx to here
models implementation for didcommx () */
// lazy val modelsDidcommx = project
//   .in(file("models-didcommx"))
//   .settings(name := "mercury-models-didcommx")
//   .settings(libraryDependencies += D.didcommx)
//   .dependsOn(models)

// #################
// ### Protocols ###
// #################

lazy val protocolConnection = project
  .in(file("mercury/mercury-library/protocol-connection"))
  .settings(name := "mercury-protocol-connection")
  .settings(libraryDependencies += D.zio)
  .settings(libraryDependencies ++= Seq(D.circeCore, D.circeGeneric, D.circeParser))
  .settings(libraryDependencies += D.munitZio)
  .dependsOn(models, protocolInvitation)

lazy val protocolCoordinateMediation = project
  .in(file("mercury/mercury-library/protocol-coordinate-mediation"))
  .settings(name := "mercury-protocol-coordinate-mediation")
  .settings(libraryDependencies += D.zio)
  .settings(libraryDependencies ++= Seq(D.circeCore, D.circeGeneric, D.circeParser))
  .settings(libraryDependencies += D.munitZio)
  .dependsOn(models)

lazy val protocolDidExchange = project
  .in(file("mercury/mercury-library/protocol-did-exchange"))
  .settings(name := "mercury-protocol-did-exchange")
  .settings(libraryDependencies += D.zio)
  .settings(libraryDependencies ++= Seq(D.circeCore, D.circeGeneric, D.circeParser))
  .dependsOn(models, protocolInvitation)

lazy val protocolInvitation = project
  .in(file("mercury/mercury-library/protocol-invitation"))
  .settings(name := "mercury-protocol-invitation")
  .settings(libraryDependencies += D.zio)
  .settings(
    libraryDependencies ++= Seq(
      D.circeCore,
      D.circeGeneric,
      D.circeParser,
      D.munit,
      D.munitZio
    )
  )
  .dependsOn(models)

lazy val protocolMercuryMailbox = project
  .in(file("mercury/mercury-library/protocol-mercury-mailbox"))
  .settings(name := "mercury-protocol-mailbox")
  .settings(libraryDependencies += D.zio)
  .dependsOn(models, protocolInvitation, protocolRouting)

lazy val protocolLogin = project
  .in(file("mercury/mercury-library/protocol-outofband-login"))
  .settings(name := "mercury-protocol-outofband-login")
  .settings(libraryDependencies += D.zio)
  .settings(libraryDependencies += D.zio)
  .settings(libraryDependencies ++= Seq(D.circeCore, D.circeGeneric, D.circeParser))
  .settings(libraryDependencies += D.munitZio)
  .dependsOn(models)

lazy val protocolReportProblem = project
  .in(file("mercury/mercury-library/protocol-report-problem"))
  .settings(name := "mercury-protocol-report-problem")
  .settings(libraryDependencies += D.munitZio)
  .dependsOn(models)

lazy val protocolRouting = project
  .in(file("mercury/mercury-library/protocol-routing"))
  .settings(name := "mercury-protocol-routing-2-0")
  .settings(libraryDependencies += D.zio)
  .dependsOn(models)

lazy val protocolIssueCredential = project
  .in(file("mercury/mercury-library/protocol-issue-credential"))
  .settings(name := "mercury-protocol-issue-credential")
  .settings(libraryDependencies += D.zio)
  .settings(libraryDependencies ++= Seq(D.circeCore, D.circeGeneric, D.circeParser))
  .settings(libraryDependencies += D.munitZio)
  .dependsOn(models)

lazy val protocolPresentProof = project
  .in(file("mercury/mercury-library/protocol-present-proof"))
  .settings(name := "mercury-protocol-present-proof")
  .settings(libraryDependencies += D.zio)
  .settings(libraryDependencies ++= Seq(D.circeCore, D.circeGeneric, D.circeParser))
  .settings(libraryDependencies += D.munitZio)
  .dependsOn(models)

lazy val vc = project
  .in(file("mercury/mercury-library/vc"))
  .settings(name := "mercury-verifiable-credentials")
  .dependsOn(protocolIssueCredential, protocolPresentProof) //TODO merge those two modules into this one

lazy val protocolTrustPing = project
  .in(file("mercury/mercury-library/protocol-trust-ping"))
  .settings(name := "mercury-protocol-trust-ping")
  .settings(libraryDependencies += D.zio)
  .settings(libraryDependencies ++= Seq(D.circeCore, D.circeGeneric, D.circeParser))
  .settings(libraryDependencies += D.munitZio)
  .dependsOn(models)

// ################
// ### Resolver ###
// ################

// TODO move stuff to the models module
lazy val resolver = project // maybe merge into models
  .in(file("mercury/mercury-library/resolver"))
  .settings(name := "mercury-resolver")
  .settings(
    libraryDependencies ++= Seq(
      D.didcommx,
      D.peerDidcommx,
      D.munit,
      D.munitZio,
      D.nimbusJwt,
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )
  .dependsOn(models)

// ##############
// ### Agents ###
// ##############

lazy val agent = project // maybe merge into models
  .in(file("mercury/mercury-library/agent"))
  .settings(name := "mercury-agent-core")
  .settings(libraryDependencies ++= Seq(D.zioLog, D.zioSLF4J))
  .dependsOn(
    models,
    resolver,
    protocolCoordinateMediation,
    protocolInvitation,
    protocolRouting,
    protocolMercuryMailbox,
    protocolLogin,
    protocolIssueCredential,
    protocolPresentProof,
    vc,
    protocolConnection,
    protocolReportProblem,
    protocolTrustPing,
  )

/** agents implementation with didcommx */
lazy val agentDidcommx = project
  .in(file("mercury/mercury-library/agent-didcommx"))
  .settings(name := "mercury-agent-didcommx")
  .settings(libraryDependencies += D.didcommx)
  .settings(libraryDependencies += D.munitZio)
  .dependsOn(agent) //modelsDidcommx

// ///** TODO Demos agents and services implementation with did-scala */
// lazy val agentDidScala =
//   project
//     .in(file("mercury/mercury-library/agent-did-scala"))
//     .settings(name := "mercury-agent-didscala")
//     .settings(skip / publish := true)
//     .dependsOn(agent)

// ####################
// ###  prismNode  ####
// ####################
val prismNodeClient = project
  .in(file("prism-node/client/scala-client"))
  .settings(
    name := "prism-node-client",
    libraryDependencies ++= Seq(D.scalaPbGrpc, D.scalaPbRuntime),
    coverageEnabled := false,
    // gRPC settings
    Compile / PB.targets := Seq(scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"),
    Compile / PB.protoSources := Seq(
      baseDirectory.value / "api" / "grpc",
      (Compile / resourceDirectory).value // includes scalapb codegen package wide config
    )
  )

// #####################
// #####  castor  ######
// #####################

lazy val castorCore = project
  .in(file("castor/lib/core"))
  .settings(commonSetttings)
  .settings(
    name := "castor-core",
    libraryDependencies ++= D_Castor.coreDependencies
  )
  .dependsOn(shared, prismNodeClient)

// #####################
// #####  pollux  ######
// #####################

lazy val polluxVcJWT = project
  .in(file("pollux/lib/vc-jwt"))
  .settings(commonSetttings)
  .settings(
    name := "pollux-vc-jwt",
    libraryDependencies ++= D_Pollux_VC_JWT.polluxVcJwtDependencies
  )
  .dependsOn(castorCore)

lazy val polluxCore = project
  .in(file("pollux/lib/core"))
  .settings(commonSetttings)
  .settings(
    name := "pollux-core",
    libraryDependencies ++= D_Pollux.coreDependencies
  )
  .dependsOn(shared)
  .dependsOn(prismAgentWalletAPI)
  .dependsOn(polluxVcJWT)
  .dependsOn(vc, resolver, agentDidcommx, eventNotification, polluxAnoncreds)

lazy val polluxDoobie = project
  .in(file("pollux/lib/sql-doobie"))
  .settings(commonSetttings)
  .settings(
    name := "pollux-sql-doobie",
    libraryDependencies ++= D_Pollux.sqlDoobieDependencies
  )
  .dependsOn(polluxCore % "compile->compile;test->test")
  .dependsOn(shared)
  .dependsOn(sharedTest % "test->test")

// ########################
// ### Pollux Anoncreds ###
// ########################

lazy val polluxAnoncreds = project
  .in(file("pollux/lib/anoncreds"))
  .enablePlugins(BuildInfoPlugin)
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "pollux-anoncreds",
    Compile / unmanagedJars += baseDirectory.value / "anoncreds-jvm-1.0-SNAPSHOT.jar",
    Compile / unmanagedResourceDirectories ++= Seq(
      baseDirectory.value / "native-lib" / "NATIVE"
    ),
    // libraryDependencies += "io.iohk.atala.prism.anoncredskmp" % "anoncreds-kmp-jvm" % "0.3.5-M2",
    libraryDependencies ++= D_Pollux_AnonCreds.baseDependencies
  )

lazy val polluxAnoncredsTest = project
  .in(file("pollux/lib/anoncredsTest"))
  .settings(libraryDependencies ++= Seq("org.scalatest" %% "scalatest" % "3.2.15" % Test))
  .dependsOn(polluxAnoncreds % "compile->test")

// #####################
// #####  connect  #####
// #####################

lazy val connectCore = project
  .in(file("connect/lib/core"))
  .settings(commonSetttings)
  .settings(
    name := "connect-core",
    libraryDependencies ++= D_Connect.coreDependencies,
    Test / publishArtifact := true
  )
  .dependsOn(shared)
  .dependsOn(protocolConnection, protocolReportProblem, eventNotification)

lazy val connectDoobie = project
  .in(file("connect/lib/sql-doobie"))
  .settings(commonSetttings)
  .settings(
    name := "connect-sql-doobie",
    libraryDependencies ++= D_Connect.sqlDoobieDependencies
  )
  .dependsOn(shared)
  .dependsOn(sharedTest % "test->test")
  .dependsOn(connectCore % "compile->compile;test->test")

// ############################
// #### Event Notification ####
// ############################

lazy val eventNotification = project
  .in(file("event-notification"))
  .settings(
    name := "event-notification",
    libraryDependencies ++= D_EventNotification.baseDependencies
  )
  .dependsOn(shared)

// #####################
// #### Prism Agent ####
// #####################

lazy val prismAgentWalletAPI = project
  .in(file("prism-agent/service/wallet-api"))
  .settings(commonSetttings)
  .settings(
    name := "prism-agent-wallet-api",
    libraryDependencies ++=
      D_PrismAgent.keyManagementDependencies ++
        D_PrismAgent.iamDependencies ++
        D_PrismAgent.postgresDependencies ++
        Seq(D.zioMock)
  )
  .dependsOn(
    agentDidcommx,
    castorCore,
    eventNotification
  )
  .dependsOn(sharedTest % "test->test")

lazy val prismAgentServer = project
  .in(file("prism-agent/service/server"))
  .settings(commonSetttings)
  .settings(
    name := "prism-agent",
    fork := true,
    libraryDependencies ++= D_PrismAgent.serverDependencies,
    Compile / mainClass := Some("io.iohk.atala.agent.server.MainApp"),
    Docker / maintainer := "atala-coredid@iohk.io",
    Docker / dockerUsername := Some("input-output-hk"),
    Docker / dockerRepository := Some("ghcr.io"),
    dockerExposedPorts := Seq(8080, 8085, 8090),
    // Official docker image for openjdk 21 with curl and bash
    dockerBaseImage := "openjdk:21-jdk",
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "io.iohk.atala.agent.server.buildinfo",
    Compile / packageDoc / publishArtifact := false
  )
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .enablePlugins(BuildInfoPlugin)
  .dependsOn(prismAgentWalletAPI % "compile->compile;test->test")
  .dependsOn(
    agent,
    polluxCore,
    polluxDoobie,
    polluxAnoncreds,
    connectCore,
    connectDoobie,
    castorCore,
    eventNotification
  )
  .dependsOn(sharedTest % "test->test")

// ############################
// ####  Release process  #####
// ############################
import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations.*
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  ReleaseStep(releaseStepTask(prismAgentServer / Docker / stage)),
  setNextVersion
)

lazy val aggregatedProjects: Seq[ProjectReference] = Seq(
  shared,
  sharedTest,
  models,
  protocolConnection,
  protocolCoordinateMediation,
  protocolDidExchange,
  protocolInvitation,
  protocolMercuryMailbox,
  protocolLogin,
  protocolReportProblem,
  protocolRouting,
  protocolIssueCredential,
  protocolPresentProof,
  vc,
  protocolTrustPing,
  resolver,
  agent,
  agentDidcommx,
  castorCore,
  polluxVcJWT,
  polluxCore,
  polluxDoobie,
  polluxAnoncreds,
  polluxAnoncredsTest,
  connectCore,
  connectDoobie,
  prismAgentWalletAPI,
  prismAgentServer,
  eventNotification,
)

lazy val root = project
  .in(file("."))
  .aggregate(aggregatedProjects: _*)
