package workspace

import cas._
import workspace.dimensions.SiDimension

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSExportAll}

@JSExportAll
sealed trait Equation {
  def expr: Expression[String]
  def display(f: String => BuckTex): BuckTex
  def staticDimensions: Map[String, SiDimension]
  def varName(varSymbol: String): Option[String]
  def varNameJs(varSymbol: String): String = varName(varSymbol).orNull
  def showNaked: BuckTex = display((varName: String) => CompileToBuckTex.makeVariableSpan(VarId(-1, varName), None))
  def vars: Set[String] = expr.vars
  def varsJs: js.Array[String] = js.Array(vars.toList :_*)
  def solve(varName: String, selfEqId: Int): Expression[VarId] = {
    // TODO: check on the `head` here
    expr.solve(varName).head.mapVariables((name) => VarId(selfEqId, name))
  }

  def solutions(varName: String, selfEqId: Int): Set[Expression[VarId]] = {
    expr.solve(varName).map(_.mapVariables((name) => VarId(selfEqId, name)))
  }
  def exprWithEquationId(id: Int): Expression[VarId] = expr.mapVariables((name) => VarId(id, name))

  def toJsObject: js.Object
}


trait EquationJs extends js.Object {
  val symbol: js.UndefOr[String]
  val lhs: js.UndefOr[Expression[String]]
  val rhs: js.UndefOr[Expression[String]]
}

object EquationJs {
  def parse(equationJs: EquationJs): Equation = equationJs.symbol.toOption match {
    case None => CustomEquation(equationJs.lhs.get, equationJs.rhs.get)
    case Some(symbol) => EquationLibrary.getByEqId(symbol)
  }
}

//object PhysicalNumberJs {
//  def parse(physicalNumberJs: PhysicalNumberJs): PhysicalNumber = PhysicalNumber(
//    physicalNumberJs.value,
//    ConcreteSiDimensionJs.parse(physicalNumberJs.siDimension),
//    physicalNumberJs.originalInputValue.toOption match {
//      case None => None
//      case Some(value) => Some(value -> DimensionJs.parse(physicalNumberJs.originalInputDim.toOption.get))
//    })
//}


@JSExportAll
case class LibraryEquation(name: String,
                           expr: Expression[String],
                           displayF: (String => BuckTex) => BuckTex,
                           staticDimensions: Map[String, SiDimension],
                           varNamesMap: Map[String, String],
                           extraTags: Set[String]
                   ) extends Equation {
  assert(expr.vars == staticDimensions.keys.toSet, s"assert 234876 $name ${expr.vars} ${staticDimensions.keys}")
  assert(varNamesMap.keys == staticDimensions.keys, "12387340")

  def varName(symbol: String): Option[String] = varNamesMap.get(symbol)
  def staticDimensionsJs: js.Dictionary[SiDimension] = js.Dictionary(staticDimensions.toSeq :_*)
  def display(f: String => BuckTex): BuckTex = displayF(f)
  def tags: Set[String] = (extraTags ++ varNamesMap.values ++ name.split(' ').toSet).map(_.toLowerCase)

  def toJsObject: js.Object = js.Dynamic.literal(
    "className" -> "LibraryEquation",
    "symbol" -> EquationLibrary.library.find(_._2 == this).map(_._1).get
  )
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

  def toJsObject: js.Object = js.Dynamic.literal(
    "className" -> "CustomEquation",
    "lhs" -> lhs.toJsObject,
    "rhs" -> rhs.toJsObject
  )
}

object Equation {
  def buildQuickly(name: String,
                   lhs: (String, String, SiDimension),
                   rhsVars: Map[String, (Int, String, SiDimension)],
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

  def buildFaster(name: String,
                  equationString: String,
                  varNamesAndDimensions: Map[String, (String, SiDimension)],
                  tags: String = "",
                  constantsUsed: Set[PhysicalConstant] = Set()): LibraryEquation = {
    val nakedEquation = EquationParser.parseEquation(equationString).getOrElse({
      throw new RuntimeException(s"Error thrown while trying to parse $name $equationString")})

    def removeConstants(expr: Expression[String]): Expression[String] = expr.mapVariablesToExpressions[String]((varName: String) => {
      constantsUsed.find(_.namedNumber.name == varName) match {
        case None => Variable(varName)
        case Some(constant) => constant.namedNumber
      }
    })

    val exprWithoutConstants = removeConstants(nakedEquation.expr)

    def display(f: (String => BuckTex)): BuckTex = {
//      val wrappedF: String => BuckTex = (varName: String) => constantsUsed.find(_.namedNumber.name == varName) match {
//        case None => f(varName)
//        case Some(constant) => Text(constant.namedNumber.name)
//      }

      CompileToBuckTex.centeredBox(List(
        CompileToBuckTex.compileExpression(removeConstants(nakedEquation.lhs).mapVariables(f)), Text(" = "),
        CompileToBuckTex.compileExpression(removeConstants(nakedEquation.rhs).mapVariables(f))))
    }

    LibraryEquation(
      name,
      exprWithoutConstants,
      display,
      varNamesAndDimensions.mapValues(_._2),
      varNamesAndDimensions.mapValues(_._1),
      tags.split(' ').toSet
    )
  }
}
