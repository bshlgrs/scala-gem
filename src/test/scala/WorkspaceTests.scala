import cas.{EquationParser, Expression, RationalNumber, Variable}
import org.scalatest.FunSpec
import workspace._

class WorkspaceTests extends FunSpec {
  import RationalNumber._
  describe("sequences of actions") {
    it("can do the ball-down-a-slide one") {
      val ws = Workspace.empty
        .addEquation(EquationLibrary.getByEqId("ke_def"))
        .addEquation(EquationLibrary.getByEqId("pe_def"))
        .addEquality(VarId(0, "m"), VarId(1, "m"))
        .addEquality(VarId(0, "E_K"), VarId(1, "E_P"))
        .addExpression(VarId(0, "v"))

      assert(ws.expressions(VarId(0, "v")) == (RationalNumber(2) * Variable(VarId(0, "E_K")) / Variable(VarId(0, "m"))).sqrt)

      val ws2 = ws.rewriteExpression(VarId(0, "v"), VarId(0, "E_K"), 1)
//      assert(ws2.exprs((0, "v")) == (RationalNumber(2) * Variable(1 -> "g") * Variable(1 -> "h")).sqrt)
    }
  }

  describe("listed actions are exactly correct") {
    val ws = Workspace.empty
      .addEquation(EquationLibrary.getByEqId("ke_def"))
      .addEquation(EquationLibrary.getByEqId("pe_def"))
      .addEquality(VarId(0, "m"), VarId(1, "m"))

    it("knows about allowed rewrites") {
      val ws2 = ws.addExpression(VarId(0, "v"))

      assert(ws2.possibleRewritesForExpr(VarId(0, "v")) == Set((VarId(0, "m"), 1)))
    }
  }

  describe("substitution") {
    it("works") {
      val ws = Workspace.empty
        .addEquation(EquationLibrary.getByEqId("ke_def"))
        .addEquation(EquationLibrary.getByEqId("pe_def"))
        .addEquality(VarId(0, "m"), VarId(1, "m"))
        .addEquality(VarId(0, "E_K"), VarId(1, "E_P"))
        .addExpression(VarId(0, "v"))
        .rewriteExpression(VarId(0, "v"), VarId(0, "E_K"), 1)

      println(ws.expressions(VarId(0, "v")))
    }

  }

  describe("custom equations") {
    it("works") {
      val ws = Workspace.empty
        .addEquation(EquationParser.parseEquation("KE = 1/2 * m * v**2").get)
        .addEquation(EquationParser.parseEquation("PE = m * g * h").get)
        .addEquality(VarId(0, "m"), VarId(1, "m"))
        .addEquality(VarId(0, "KE"), VarId(1, "PE"))
        .addExpression(VarId(0, "v"))
        .rewriteExpression(VarId(0, "v"), VarId(0, "KE"), 1)

      assert(ws.expressions(VarId(0, "v")) == (Variable(VarId(1, "g")) * Variable(VarId(1, "h")) * 2).sqrt)
    }
  }

  describe("regressions") {
    it("works") {
      val E_T = VarId(2, "E_T")
      val ws = Workspace.empty
        .addEquation(EquationParser.parseEquation("KE = 1/2 * m * v**2").get)
        .addEquation(EquationParser.parseEquation("PE = m * g * h").get)
        .addEquation(EquationParser.parseEquation("E_T = KE + PE").get)
        .addEquality(VarId(0, "KE"), VarId(2, "KE"))
        .addEquality(VarId(1, "PE"), VarId(2, "PE"))
        .addExpression(E_T)

      assert(ws.expressions(VarId(2, "E_T")) == Variable(VarId(2, "KE")) + Variable(VarId(2, "PE")))

      val ws2 = ws.rewriteExpression(E_T, VarId(0, "KE"), 0)
      assert(ws2.expressions(VarId(2, "E_T")) == Variable(VarId(0, "m")) * (Variable(VarId(0, "v")) ** 2) / 2 + Variable(VarId(1, "PE")))
    }

    it("compares stuff reasonably") {

    }
  }
}
