package workspace

import cas.{Expression, Variable}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport

case class TriangleDiagram(vars: Map[String, VarId]) {
  // Triangle
  //
  //     C
  //    /|
  //   / |
  //  /  |
  // A---B
  // theta is the angle CAB, phi is the angle ACB
  // H is hypotenuse AC
  // A is AB
  // O is BC

  def diagramVarNames: Set[String] = Set("H", "A", "O", "θ", "φ")

  def usableEquations: Set[CustomEquation] = {
    val O = Variable("O")
    val A = Variable("A")
    val H = Variable("H")
    val theta = Variable("θ")
    val phi = Variable("φ")

    val identities = Set[(Expression[String], Expression[String])](
      (O**2) + (A**2) -> (H**2),
      O -> H * cas.SpecialFunction.sin(theta),
      A -> H * cas.SpecialFunction.cos(theta),
      A -> H * cas.SpecialFunction.sin(phi),
      O -> H * cas.SpecialFunction.cos(phi),
      // tangents
      O -> A * cas.SpecialFunction.tan(theta),
      A -> O * cas.SpecialFunction.tan(phi),
    )

    def getOutputVarName(name: String): String = vars.get(name) match {
      case None => name
      case Some(varId) => varId.varName
    }

    identities
      .map({ case (lhs, rhs) => {
        CustomEquation(lhs.mapVariables(getOutputVarName), rhs.mapVariables(getOutputVarName))
      }})
  }

  def equalitiesToAddWithEquation(customEquation: CustomEquation, newEquationId: Int, diagramId: Int): Set[(VarId, VarId)] = {
    (customEquation.lhs.vars ++ customEquation.rhs.vars).map((varName) =>
      EquationVarId(newEquationId, varName) -> DiagramVarId(diagramId, varName)
    )
  }

  @JSExport
  def usableEquationsJs: js.Array[CustomEquation] = js.Array(usableEquations.toSeq :_*)

  def set(varName: String, mbSetting: Option[VarId]): TriangleDiagram = mbSetting match {
    case None => this.copy(vars - varName)
    case Some(setting) => this.copy(vars + (varName -> setting))
  }
}
