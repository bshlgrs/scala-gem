import React, { Component } from 'react';
import './App.css';
import Gem from './Gem'
import katex from 'katex'

const latex = (latex, displayMode) => {
  try {
    return <span
     dangerouslySetInnerHTML={{__html: katex.renderToString(latex, {displayMode: displayMode})}}/>;
  } catch (e) {
    return <span>oh dear: {latex}</span>
  }
};


class App extends Component {
  constructor () {
    super();
    this.state = {
      workspace: Gem.Workspace()
    };
  }
  setWs(newWs) {
    this.setState({ workspace: newWs });
  }
  render() {
    const ws = this.state.workspace;

    return (
      <div className="App">
        <h3>Equations</h3>
        {ws.equationIdList.map((equationId, idx) =>
          <p key={idx}>{latex(ws.equationLatex(idx), true)} ({equationId})</p>
        )}

        <button onClick={() => { this.setWs(ws.addEquationFromLibrary("ke_def")) }}>
          Add KE equation</button>
        <button onClick={() => { this.setWs(ws.addEquationFromLibrary("pe_def")) }}>
          Add PE equation</button>

        <h3>Equalities</h3>
        {ws.equalityList.map((list, idx) =>
          <p key={idx}>{list.map((varId, varIdIdx) => {
            if (varIdIdx === list.length - 1) {
              return <span key={varIdIdx}>
              {latex(ws.showVar(varId), false)}
              </span>
            } else {
              return <span key={varIdIdx}>
                {latex(ws.showVar(varId), false)}
                =
              </span>;
            }
          })}</p>
        )}

        {ws.addableEqualities.map((tuple, idx) =>
          <button
            key={idx}
            onClick={() => this.setWs(ws.declareEqual(tuple[0], tuple[1]))}>
            Set {latex(ws.showVar(tuple[0]) + "=" + ws.showVar(tuple[1]))}
          </button>
        )}

        <h3>Expressions</h3>

        {ws.expressionList.map((x, idx) =>
          <div key={idx} className="expression">{latex(ws.exprLatex(x), true)}
            {ws.possibleRewritesForExpr(x).map((rewrite, idx) =>
              <button key={idx}
                onClick={() => this.setWs(ws.rewriteExpression(x, rewrite[0], rewrite[1]))}>
                Sub in equation {rewrite[1]} to replace {latex(ws.showVar(rewrite[0]))}
              </button>
            )}
          </div>
        )}

        {ws.addableExpressionList.map((x, idx) =>
          <button key={idx} onClick={() => this.setWs(ws.addExpression(x))}>
            Add {latex(ws.showVar(x))}
          </button>
        )}
      </div>
    );
  }
}

export default App;