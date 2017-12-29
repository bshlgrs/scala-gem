package workspace

import cas.{EquationParser, Expression, RationalNumber, Variable}

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSExportAll}

@JSExportAll
trait Equation {
  def expr: Expression[String]
  def display(f: String => BuckTex): BuckTex
  def staticDimensions: Map[String, Dimension]
  def varName(varSymbol: String): Option[String]
  def varNameJs(varSymbol: String): String = varName(varSymbol).orNull
  def showNaked: BuckTex = display((varName: String) => CompileToBuckTex.makeVariableSpan(VarId(-1, varName), None))
  def vars: Set[String] = expr.vars
  def varsJs: js.Array[String] = js.Array(vars.toList :_*)
  def solve(varName: String, selfEqId: Int): Expression[VarId] = {
    // TODO: check on the `head` here
    expr.solve(varName).head.mapVariables((name) => VarId(selfEqId, name))
  }
  def exprWithEquationId(id: Int): Expression[VarId] = expr.mapVariables((name) => VarId(id, name))
}

@JSExportAll
case class LibraryEquation(name: String,
                    expr: Expression[String],
                    displayF: (String => BuckTex) => BuckTex,
                    staticDimensions: Map[String, Dimension],
                    varNamesMap: Map[String, String],
                    tags: Set[String]
                   )  extends Equation {
  assert(expr.vars == staticDimensions.keys)
  assert(varNamesMap.keys == staticDimensions.keys)

  def varName(symbol: String): Option[String] = varNamesMap.get(symbol)
  def staticDimensionsJs: js.Dictionary[Dimension] = js.Dictionary(staticDimensions.toSeq :_*)
  def display(f: String => BuckTex): BuckTex = displayF(f)
}

@JSExportAll
case class CustomEquation(lhs: Expression[String], rhs: Expression[String]) extends Equation {
  def expr: Expression[String] = lhs - rhs

  def varName(varSymbol: String): Option[String] = None

  def display(f: String => BuckTex): BuckTex =
    CompileToBuckTex.centeredBox(List(CompileToBuckTex.compileExpression(lhs.mapVariables(f)),
                                     Text("="),
                                     CompileToBuckTex.compileExpression(rhs.mapVariables(f))))

  def staticDimensions = Map()
}

object Equation {
  def buildQuickly(name: String,
                   lhs: (String, String, Dimension),
                   rhsVars: Map[String, (Int, String, Dimension)],
                   tags: String,
                   constant: RationalNumber[String] = RationalNumber[String](1)): LibraryEquation = {
    val rhs = rhsVars.map({
      case (symbol, (power, _, _)) => Expression.makePower(Variable(symbol), RationalNumber(power))
    }).reduce(_ * _)
    val expr = (constant * rhs) / Variable(lhs._1) - 1
    def display(f: (String => BuckTex)): BuckTex = {
      CompileToBuckTex.centeredBox(List(f(lhs._1), Text(" = "),
        CompileToBuckTex.compileExpression((constant * rhs).mapVariables(f))))
    }

    LibraryEquation(name, expr, display,
      rhsVars.mapValues(_._3) + (lhs._1 -> lhs._3),
      rhsVars.mapValues(_._2) + (lhs._1 -> lhs._2),
      tags.split(' ').toSet)
  }

  def buildFaster(name: String, equationString: String, varNamesAndDimensions: Map[String, (String, Dimension)], tags: String): LibraryEquation = {
    val nakedEquation = EquationParser.parseEquation(equationString).get

    def display(f: (String => BuckTex)): BuckTex = {
      CompileToBuckTex.centeredBox(List(
        CompileToBuckTex.compileExpression(nakedEquation.lhs.mapVariables(f)), Text(" = "),
        CompileToBuckTex.compileExpression((nakedEquation.rhs).mapVariables(f))))
    }

    LibraryEquation(
      name,
      nakedEquation.expr,
      display,
      varNamesAndDimensions.mapValues(_._2),
      varNamesAndDimensions.mapValues(_._1),
      tags.split(' ').toSet
    )
  }
}
