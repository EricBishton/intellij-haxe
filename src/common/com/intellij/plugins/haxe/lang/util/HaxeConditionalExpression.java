/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2017 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.plugins.haxe.lang.util;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.parser.HaxeAstFactory;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeDummyASTNode;
import com.intellij.plugins.haxe.util.HaxeDebugLogger;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.containers.Stack;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;
import static com.intellij.plugins.haxe.lang.util.HaxeAstUtil.*;

/**
 * Condition that controls #if and #elseif segments
 *
 * Created by ebishton on 3/23/17.
 */
public class HaxeConditionalExpression {

  // Here is a set of rules to parse conditions, if we ever want to run them
  // through a parser:
  //    ppIdentifier ::= identifier
  //    private ppNumberPrefix ::= "-"
  //    ppNumber ::= ppNumberPrefix? (LITINT | LITHEX | LITOCT | LITFLOAT)
  //    private ppNegation ::= "!"
  //    private ppComparisonOperator ::= ("=="|"!="|">"|">="|"<"|"<=")
  //    left ppOperator ::= ("||" | "&&" | ppComparisonOperator)
  //    ppLiteral ::= ppIdentifier | ppNumber | KTRUE | KFALSE
  //    private ppSimpleExpression ::= ppNegation? (ppParenthesizedExpression | ppLiteral)
  //    left ppExpression ::=  ppSimpleExpression (ppOperator ppSimpleExpression)*
  //    private ppParenthesizedExpression ::= '(' ppExpression ')'
  //
  //    private pp_statement_recover ::= !(ppStatement | ';' | '[' | ']' | '{' | '}' | assignOperation | bitOperation)
  //    private ppStatementWithCondition ::= ("#if" | "#elseif") ppExpression {pin=1 recoverWhile="pp_statement_recover"}
  //    private ppStatementWithoutCondition ::= "#else" | "#end"  {recoverWhile="pp_statement_recover"}
  //    private ppStatementWithComment ::= "#line" | "#error"  {pin=1 recoverWhile="!\n"}
  //    ppStatement ::= ppStatementWithCondition | ppStatementWithoutCondition | ppStatementWithComment {extends="com.intellij.psi.PsiComment"}

  static final HaxeDebugLogger LOG = HaxeDebugLogger.getLogger();
  static {      // Take this out when finished debugging.
    LOG.setLevel(org.apache.log4j.Level.DEBUG);
  }
  private final ArrayList<ASTNode> tokens = new ArrayList<ASTNode>();
  private boolean evaluated = false;    // Cleared when dirty.
  private boolean evalResult = false;   // Cleared when dirty.

  public HaxeConditionalExpression(@Nullable ArrayList<ASTNode> startTokens) {
    if (startTokens != null) {
      tokens.addAll(startTokens);
    }
  }

  public boolean isTrue() {
    return tokens.isEmpty() ? false : evaluate();
  }

  public void extend(@NotNull CharSequence chars, @NotNull IElementType tokenType) {
    tokens.add(HaxeAstFactory.leaf(tokenType, chars));
    evaluated = false;
  }

  private boolean areTokensBalanced(IElementType leftToken, IElementType rightToken) {
    LOG.assertLog(leftToken != rightToken, "Cannot balance tokens of the same type.");
    int tokenCount = 0;
    for (ASTNode t : tokens) {
      IElementType type = t.getElementType();
      if (type.equals(leftToken)) {
        tokenCount++;
      }
      else if (type.equals(rightToken)) {
        tokenCount--;
      }
    }
    return tokenCount == 0;
  }

  private boolean areParensBalanced() {
    return areTokensBalanced(PLPAREN, PRPAREN);
  }

  private boolean areStringQuotesBalanced() {
    return areTokensBalanced(OPEN_QUOTE, CLOSING_QUOTE);
  }

  public boolean isComplete() {
    if (tokens.isEmpty()) {
      return false;
    }

    ASTNode first = tokens.get(0);
    if (tokens.size() == 1) {
      return isLiteral(first);
    }
    if (tokens.size() == 2) {
      ASTNode second = tokens.get(1);
      boolean secondIsStandalone = isLiteral(second);
      return isNegation(first) && secondIsStandalone;
    }
    return areParensBalanced() && areStringQuotesBalanced();
  }

  public boolean evaluate() {
    // Evaluation can be expensive, so we cache the result in order to speed parsing.
    if (!evaluated) {
      evalResult = reevaluate();
      evaluated = true;
    }
    return evalResult;
  }

  private boolean reevaluate() {
    boolean ret = false;
    if (isComplete()) {
      try {
        Stack<ASTNode> rpn = infixToRPN();
        LOG.debug(toString() + "-->" + rpn.toString());
        ret = objectIsTrue(calculateRPN(rpn));
        if (!rpn.isEmpty()) {
          throw new CalculationException("Invalid Expression: Tokens left after calculating: " + rpn.toString());
        }
      } catch (CalculationException e) {
        String msg = "Error calculating conditional compiler expression '" + toString() + "'";
        if (LOG.getEffectiveLevel() == Level.DEBUG) {
          // Add stack info if in debug mode.
          LOG.info(msg, e);
        } else {
          LOG.info(msg);
        }
      }
    }
    return ret;
  }

  /**
   * Converts an infix expression into an RPN expression.  (Re-orders and removes parenthesis.)
   * For example: !(cpp && js) -> cpp js && !
   *         and: (( cpp || js ) && (haxe-ver < 3))  -> cpp js || haxe-ver 3 < &&
   * See https://en.wikipedia.org/wiki/Reverse_Polish_notation
   * @return
   * @throws CalculationException
   */
  private Stack<ASTNode> infixToRPN() throws CalculationException {
    // This is a simplified shunting-yard algorithm: http://https://en.wikipedia.org/wiki/Shunting-yard_algorithm
    Stack<ASTNode> rpnOutput = new Stack<ASTNode>();
    Stack<ASTNode> operatorStack = new Stack<ASTNode>();

    try {
      for (ASTNode token : tokens) {
        if (isLiteral(token)) {
          rpnOutput.push(token);
        }
        else if (isStringQuote(token)) {
          // Ignore it for calculations.  The REGULAR_STRING_PART is the part we keep.
        }
        else if (isLeftParen(token)) {
          operatorStack.push(token);
        }
        else if (isRightParen(token)) {
          boolean foundLeftParen = false;
          while (!operatorStack.isEmpty()) {
            ASTNode op = operatorStack.pop();
            if (!isLeftParen(op)) {
              rpnOutput.push(op);
            }
            else {
              foundLeftParen = true;
              break;
            }
          }
          if (operatorStack.isEmpty() && !foundLeftParen) {
            // mismatched parens.
            // TODO: Report errors back through a reporter class.
            throw new CalculationException("Mismatched right parenthesis.");
          }
        }
        else if (isCCOperator(token)) {
          while (!operatorStack.isEmpty()
                 && HaxeOperatorPrecedenceTable.shuntingCompare(token.getElementType(), operatorStack.peek().getElementType())) {
            rpnOutput.push(operatorStack.pop());
          }
          operatorStack.push(token);
        }
        else {
          throw new CalculationException("Couldn't process token '" + token.toString() + "' when converting to RPN.");
        }
      }
    } catch (HaxeOperatorPrecedenceTable.OperatorNotFoundException e) {
      LOG.warn("IntelliJ-Haxe plugin internal error: Unknown operator encountered while calculating compiler conditional exression:"
               + toString(), e);
      throw new CalculationException(e.toString());
    }

    // Anything left in the operator stack means an error.
    while(!operatorStack.isEmpty()) {
      ASTNode node = operatorStack.pop();
      if (isLeftParen(node)) {
        // Mismatched parens.
        // TODO: Report errors back through a reporter class.
        throw new CalculationException("Mismatched left parenthesis.");
      } else {
        rpnOutput.push(node);
      }
    }

    return rpnOutput;
  }

  private Object calculateRPN(Stack<ASTNode> rpn) throws CalculationException {
    while (!rpn.isEmpty()) {
      ASTNode node = rpn.tryPop();
      if (isCCOperator(node)) {
        switch (getArity(node)) {
          case UNARY: {
            Object rhs = calculateRPN(rpn);
            return applyUnary(node, rhs);
          }
          case BINARY: {
            Object rhs = calculateRPN(rpn);
            Object lhs = calculateRPN(rpn);
            return applyBinary(node, lhs, rhs);
          }
        }
      } else if (isLiteral(node)) {
        return literalValue(node);
      } else if (isIdentifier(node)) {
        return lookupIdentifier(node);
      } else {
        throw new CalculationException("Unexpected AST Node type " + node.getElementType().toString());
      }
    }
    return false; // TODO: Return the actual value.
  }

  @NotNull
  private HaxeOperatorPrecedenceTable.Arity getArity(@NotNull ASTNode node)
    throws CalculationException {
    HaxeOperatorPrecedenceTable.Arity arity = HaxeOperatorPrecedenceTable.getArity(node.getElementType());
    if (null == arity) {
      throw new CalculationException("NULL arity from node: '" + node.toString() + "'.");
    }

    // This could just as well be done in the routine above... Doing it here makes that one more understandable.
    switch(arity) {
      case UNARY:
      case BINARY:
        break;
      default:
        String msg = "Unexpected arity of " + arity.toString() + " from operator '" + node.toString() + "'.";
        throw new CalculationException(msg);
    }
    return arity;
  }

  @NotNull
  private Object literalValue(ASTNode node) throws CalculationException {
    if (isTrueKeyword(node))        { return new Boolean(true); }
    if (isFalseKeyword(node))       { return new Boolean(false); }
    if (isRegularString(node))      { return new String(node.getText()); }
    if (isNumber(node))             { return new Float(node.getText()); }

    throw new CalculationException("Unrecognized value token: " + node.toString());
  }

  @NotNull
  private Object identifierValue(String s) throws CalculationException {
    if (KTRUE.toString().equals(s))   { return new Boolean(true); }
    if (KFALSE.toString().equals(s))  { return new Boolean(false); }

    Float result = new Float(0);
    if (isFloat(s,result))            { return result; }

    // XXX: Need to de-quote strings? (and recurse?)

    return s;
  }

  @NotNull
  private Object lookupIdentifier(ASTNode identifier) {
    if (identifier == null) {
      return new Boolean(false);
    }
    if (myProject == null) {
      return SDK_DEFINES.contains(name);
    }
    String[] definitions = null;
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      final Object userData = myProject.getUserData(DEFINES_KEY);
      if (userData instanceof String) {
        definitions = ((String)userData).split(",");
      }
    }
    else {
      definitions = HaxeProjectSettings.getInstance(myProject).getUserCompilerDefinitions();
    }
    return definitions != null && Arrays.asList(definitions).contains(name);



    // TODO: Implement
    return identifierValue(definition);
  }


  // Parodies Haxe parser is_true function
  // https://github.com/HaxeFoundation/haxe/blob/development/src/syntax/parser.mly#L1596
  private boolean objectIsTrue(Object o) {
    if (o == null)            { return false; }
    if (o instanceof Boolean) { return (Boolean)o; }
    if (o instanceof Float)   { return !((Float)o == 0.0); }
    if (o instanceof String)  { return !((String)o).isEmpty(); }
    return true;
  }

  // Parodies Haxe parser cmp function
  // https://github.com/HaxeFoundation/haxe/blob/development/src/syntax/parser.mly#L1600
  private int objectCompare(Object lhs, Object rhs) throws CalculationException {
    if (lhs == null && rhs == null) { return 0; }
    if (lhs instanceof Boolean && rhs instanceof Boolean) { return ((Boolean)lhs).compareTo((Boolean)rhs); }
    if (lhs instanceof String && rhs instanceof String)   { return ((String)lhs).compareTo((String)rhs); }

    // For String vs Float, convert the strings to floats.  Errors converting are thrown past this function.
    if (lhs instanceof String  && rhs instanceof Float)  { lhs = Float.valueOf((String)lhs); }
    if (lhs instanceof Float   && rhs instanceof String) { rhs = Float.valueOf((String)rhs); }

    if (lhs instanceof Float && rhs instanceof Float) {
      // To get the same behavior as OCaml, NaN needs to be treated as less than all other numbers,
      // rather than larger, as Java likes to do it.
      int result = ((Float)lhs).compareTo((Float)rhs);
      if (((Float)lhs).isNaN()) { result = -result; }
      if (((Float)rhs).isNaN()) { result = -result; }
      return result;
    }

    // No other combinations are allowed.
    throw new CalculationException("Invalid value comparison between '"
                                   + lhs.toString() + "' and '" + rhs.toString() + "'.");
  }

  // Parodies Haxe parser eval function
  // https://github.com/HaxeFoundation/haxe/blob/development/src/syntax/parser.mly#L1619
  @NotNull
  private Object applyUnary(ASTNode op, Object value) throws CalculationException {
    IElementType optype = op.getElementType();
    if (optype.equals(ONOT))  { return !objectIsTrue(value); }
    throw new CalculationException("Unexpected unary operator encountered: " + op.toString());
  }

  // Parodies Haxe parser eval function at lines 1617, 1618, and 1621-1634
  // https://github.com/HaxeFoundation/haxe/blob/development/src/syntax/parser.mly#L1617
  @NotNull
  private Object applyBinary(ASTNode op, Object lhs, Object rhs) throws CalculationException {
    IElementType optype = op.getElementType();
    if (optype.equals(LOGIC_AND_EXPRESSION)) { return objectIsTrue(lhs) && objectIsTrue(rhs); }
    if (optype.equals(LOGIC_OR_EXPRESSION))  { return objectIsTrue(lhs) || objectIsTrue(rhs); }
    if (optype.equals(OEQ))                  { return objectCompare(lhs, rhs) == 0; }
    if (optype.equals(ONOT_EQ))              { return objectCompare(lhs, rhs) != 0; }
    if (optype.equals(OGREATER))             { return objectCompare(lhs, rhs) >  0; }
    if (optype.equals(OGREATER_OR_EQUAL))    { return objectCompare(lhs, rhs) >= 0; }
    if (optype.equals(OLESS_OR_EQUAL))       { return objectCompare(lhs, rhs) <= 0; }
    if (optype.equals(OLESS))                { return objectCompare(lhs, rhs) <  0; }
    throw new CalculationException("Unexpected operator when comparing '"
                                   + lhs.toString() + " " + optype.toString() + " " + rhs.toString() + "'.");
  }

  public String toString() {
    StringBuilder s = new StringBuilder();
    boolean first = true;
    for (ASTNode t : tokens) {
      if (!first) s.append(" ");
      s.append(t.getText());
    }
    return s.toString();
  }

  public static class CalculationException extends Exception {
    private CalculationException(String message) {
      super(message);
    }
  }


}
