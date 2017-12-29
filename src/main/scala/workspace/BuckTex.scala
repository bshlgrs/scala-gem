package workspace

import cas._

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSExportAll}

trait BuckTex {
  @JSExport
  def typeStr: String = this match {
    case _: FlexBox => "FlexBox"
    case _: Sup => "Sup"
    case _: Sub => "Sub"
    case _: Fraction => "Fraction"
    case _: Text => "Text"
    case _: Wrapper => "Wrapper"
  }
}

@JSExportAll
case class FlexDirection(dir: String)
object Column extends FlexDirection("column")
object Row extends FlexDirection("row")

@JSExportAll
case class AlignItemDirection(dir: String)
object FlexEnd extends AlignItemDirection("flex-end")
object Center extends AlignItemDirection("center")

@JSExportAll
case class FlexBox(items: List[BuckTex], flexDirection: FlexDirection, alignItems: AlignItemDirection) extends BuckTex {
  def jsItems: js.Array[BuckTex] = js.Array(items :_*)
}

case class Sup(items: List[BuckTex]) extends BuckTex {
  @JSExport
  def jsItems: js.Array[BuckTex] = js.Array(items :_*)
}
case class Sub(items: List[BuckTex]) extends BuckTex {
  @JSExport
  def jsItems: js.Array[BuckTex] = js.Array(items :_*)
}
case class Fraction(numerator: List[BuckTex], denominator: List[BuckTex]) extends BuckTex {
  @JSExport
  def jsNumerator: js.Array[BuckTex] = js.Array(numerator :_*)
  @JSExport
  def jsDenominator: js.Array[BuckTex] = js.Array(denominator :_*)
}
@JSExportAll
case class Text(text: String) extends BuckTex
@JSExportAll
case class Wrapper(item: BuckTex, data: js.Dictionary[Any] = js.Dictionary()) extends BuckTex

object CompileToBuckTex {
  def horizontalBox(items: List[BuckTex]) = FlexBox(items, Row, FlexEnd)
  def centeredBox(items: List[BuckTex]) = FlexBox(items, Row, Center)

  def compileExpression(expr: Expression[BuckTex]): BuckTex = {
    compileExpressionWithBinding(expr, 0)
  }

  def compileExpressionWithBinding(expr: Expression[BuckTex], strongestPullFromOutside: Int): BuckTex = {
    def wrapIfNeeded(stuff: BuckTex, pullStrengthAtWhichWrappingIsNeeded: Int): BuckTex = {
      if (pullStrengthAtWhichWrappingIsNeeded > strongestPullFromOutside) {
        horizontalBox(List(stuff))
      } else {
        stuff
      }
    }
    expr match {
      case Sum(set) => {
        wrapIfNeeded(centeredBox(set.toList.flatMap((x) => List(Text(" + "), compileExpressionWithBinding(x, 1))).tail),
          1)
      }
      case Product(set) => {
        wrapIfNeeded(centeredBox(set.toList.flatMap((x) => List(Text(" "), compileExpressionWithBinding(x, 2))).tail),
          2)
      }
      case Power(lhs, rhs) => centeredBox(List(compileExpressionWithBinding(lhs, strongestPullFromOutside),
        Sup(List(compileExpressionWithBinding(rhs, 0)))
      ))
      case Variable(buckTex) => buckTex
      case RealNumber(r) => Text(r.toString)
      case RationalNumber(n, 1) => Text(n.toString)
      case RationalNumber(1, 2) => Text("½")
      case RationalNumber(-1, 2) => Text("-½")
      case RationalNumber(n, d) => Fraction(List(Text(n.toString)), List(Text(d.toString)))
    }
  }

  def showEquation(equation: Equation, idx: Int, varSubscripts: Map[String, Int]): BuckTex = {
    equation.display((varName: String) =>
      makeVariableSpan(VarId(idx, varName), varSubscripts.get(varName)))
  }

  def showVariable(varId: VarId, varSubscripts: Map[VarId, Int]): BuckTex = {
    makeVariableSpan(varId, varSubscripts.get(varId))
  }

  def makeVariableSpan(varId: VarId, mbNum: Option[Int]): BuckTex = {
    val name = varId.varName

    def showVarWithStr(numStr: String): BuckTex = {
      if (name.contains("_")) {
        var List(mainText, subscript) = name.split('_').toList
        CompileToBuckTex.horizontalBox(List(Text(mainText), Sub(List(Text(subscript + numStr)))))
      } else {
        if (numStr.isEmpty)
          Text(name)
        else
          CompileToBuckTex.horizontalBox(List(Text(name), Sub(List(Text(numStr)))))
      }
    }

    val list = mbNum match {
      case Some(num) => showVarWithStr(num.toString)
      case None => showVarWithStr("")
    }
    Wrapper(list, js.Dictionary[Any]("varId" -> (varId: Any), "type" -> "Variable")) // todo: remove type annotation?
  }

  def showExpression(varId: VarId,
                     expression: Expression[VarId],
                     varSubscripts: Map[VarId, Int],
                     mbNumericValue: Option[PhysicalNumber]): BuckTex = {
    val numericValueDisplay = mbNumericValue match {
      case None => List()
      case Some(PhysicalNumber(numericValue, dimension)) =>
        List(Text(s" = ${numericValue.toString.take(5)}"), dimension.toBuckTex)
    }

    CompileToBuckTex.centeredBox(List(makeVariableSpan(varId, varSubscripts.get(varId)), Text(" = "),
      this.compileExpression(expression.mapVariables(varId => makeVariableSpan(varId, varSubscripts.get(varId))))) ++
      numericValueDisplay)
  }
}