package workspace

import scala.scalajs.js.annotation.{JSExport, JSExportAll}

@JSExportAll
case class PhysicalNumber(value: Double, dimension: Dimension) {
  @JSExport
  lazy val toBuckTex: BuckTex = Text(value.toString) ++ dimension.toBuckTex
}
