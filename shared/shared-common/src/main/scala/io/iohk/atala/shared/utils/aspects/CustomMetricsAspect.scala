package io.iohk.atala.shared.utils.aspects

import zio.*
import scala.collection.mutable.{Map => MutMap}
import zio.metrics.*
import java.time.{Instant, Clock, Duration}
import io.iohk.atala.shared.utils.DurationOps.toMetricsSeconds

object CustomMetricsAspect {
  private val checkpoints: MutMap[String, Instant] = MutMap.empty
  private val clock = Clock.systemUTC()
  private def now = ZIO.succeed(clock.instant)

  def startRecordingTime(key: String): ZIOAspect[Nothing, Any, Nothing, Any, Nothing, Any] =
    new ZIOAspect[Nothing, Any, Nothing, Any, Nothing, Any] {
      override def apply[R, E, A](
          zio: ZIO[R, E, A]
      )(implicit trace: Trace): ZIO[R, E, A] =
        for {
          res <- zio
          timeAfter <- now
          _ = checkpoints.update(key, timeAfter)
        } yield res
    }

  def endRecordingTime(
      key: String,
      metricsKey: String,
      tags: Set[MetricLabel] = Set.empty
  ): ZIOAspect[Nothing, Any, Nothing, Any, Nothing, Any] =
    new ZIOAspect[Nothing, Any, Nothing, Any, Nothing, Any] {
      override def apply[R, E, A](
          zio: ZIO[R, E, A]
      )(implicit trace: Trace): ZIO[R, E, A] = {
        for {
          res <- zio
          end <- now
          maybeStart = checkpoints.get(key)
          _ = checkpoints.remove(key)
          metricsZio = maybeStart.map(start => Duration.between(start, end)).fold(ZIO.unit) { duration =>
            ZIO.succeed(duration.toMetricsSeconds) @@ Metric.gauge(metricsKey).tagged(tags)
          }
          _ <- metricsZio
        } yield res
      }
    }

}
