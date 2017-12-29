package cas

import fastparse.WhitespaceApi
import fastparse.core.Parsed
import workspace.CustomEquation

object EquationParser {
  def parseEquation(equationString: String): Option[CustomEquation] = for {
    list: List[String] <- Option(equationString.split('=').toList)
    if list.size == 2
    lhs <- parseExpression(list(0))
    rhs <- parseExpression(list(1))
  } yield CustomEquation(lhs, rhs)

  def parseExpression(expressionString: String): Option[Expression[String]] = expr.parse(expressionString) match {
    case Parsed.Success(expression, _) => Some(expression)
    case _ => None
  }

  val White = WhitespaceApi.Wrapper{
    import fastparse.all._
    NoTrace(" ".rep)
  }
  import fastparse.noApi._
  import White._
  def eval(tree: (Expression[String], Seq[(String, Expression[String])])): Expression[String] = {
    val (base, ops) = tree
    ops.foldLeft(base){
      case (left, (op, right)) => op match {
        case "+" => left + right case "-" => left - right
        case "*" => left * right case "/" => left / right
      }
    }
  }

  val number: P[Expression[String]] = P( CharIn('0'to'9').rep(1).!.map(str => RationalNumber[String](str.toInt)))
  val variable: P[Expression[String]] = P( CharIn('a' to 'z', 'A' to 'Z', Seq('_')).rep(1).!.map(str => Variable(str)))
  lazy val parens: P[Expression[String]] = P( "(" ~/ addSub ~ ")" )
  val atom: P[Expression[String]] = P( number | variable | parens )

  val power: P[Expression[String]] = P(atom ~ "**" ~ atom | atom).map({
    case x: Expression[String] => x
    case (x: Expression[String], y: Expression[String]) => Expression.makePower(x, y)
  })
  val divMul: P[Expression[String]] = P( power ~ (CharIn("*/").! ~/ power).rep ).map(eval)
  val addSub: P[Expression[String]] = P( divMul ~ (CharIn("+-").! ~/ divMul).rep ).map(eval)
  val expr: P[Expression[String]]   = P( " ".rep ~ addSub ~ " ".rep ~ End )

  def main(args: Array[String]): Unit = {
    println(expr.parse("1/2 * m * v ** 2"))
    println(EquationParser.parseEquation("E_K = 1/2 * m * v**2"))
    println(EquationParser.parseEquation("E_K = 1/2 * m * g * h"))
  }
}
