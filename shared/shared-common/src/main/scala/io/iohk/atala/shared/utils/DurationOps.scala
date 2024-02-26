package io.iohk.atala.shared.utils

import java.time.Duration

object DurationOps {

  extension (d: Duration) def toMetricsSeconds: Double = d.toMillis.toDouble / 1000.0

}
