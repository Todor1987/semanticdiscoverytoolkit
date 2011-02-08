/*
    Copyright 2009 Semantic Discovery, Inc. (www.semanticdiscovery.com)

    This file is part of the Semantic Discovery Toolkit.

    The Semantic Discovery Toolkit is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    The Semantic Discovery Toolkit is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with The Semantic Discovery Toolkit.  If not, see <http://www.gnu.org/licenses/>.
*/
package org.sd.atn;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.sd.token.CategorizedToken;
import org.sd.token.Token;
import org.sd.util.tree.Tree;

/**
 * Container class for a processing state to pair a grammar rule step with
 * an input token and generate successive states.
 * <p>
 * @author Spence Koehler
 */
public class AtnState {
  
  private Token inputToken;
  public Token getInputToken() {
    return inputToken;
  }

  private AtnRule rule;
  public AtnRule getRule() {
    return rule;
  }

  private int stepNum;
  int getStepNum() {
    return stepNum;
  }

  private int repeatNum;
  public int getRepeatNum() {
    return repeatNum;
  }


  private Tree<AtnState> parentStateNode;
  public Tree<AtnState> getParentStateNode() {
    return parentStateNode;
  }

  AtnParseOptions parseOptions;
  int skipNum;


  private MatchResult matchResult;
  public boolean getMatched() {
    return matchResult == null ? false : matchResult.matched();
  }
  MatchResult getMatchResult() {
    return matchResult;
  }
  void setMatchResult(MatchResult matchResult) {
    this._nextToken = null;
    this.matchResult = matchResult;
  }

  private AtnState pushState;
  public AtnState getPushState() {
    return pushState;
  }

  private boolean isPoppedState;
  boolean isPoppedState() {
    return isPoppedState;
  }

  private int popCount;
  int getPopCount() {
    return popCount;
  }

  public boolean isRepeat() {
    return repeatNum > 0;
  }

  private boolean _isSkipped;
  public boolean isSkipped() {
    return _isSkipped;
  }

  boolean isRuleEnd() {
    boolean result = (rule != null) ? rule.isTerminal(stepNum) : false;

    if (result) {
      result = rule.verifyPop(this.inputToken, this);
    }

    return result;
  }

  private AtnRuleStep _ruleStep;
  public AtnRuleStep getRuleStep() {
    if (_ruleStep == null) {
      _ruleStep = rule.getSteps().get(stepNum);
    }
    return _ruleStep;
  }

  private boolean computedNextToken;
  private Token _nextToken;
  private List<AtnGrammar.Bracket> activeBrackets;


  /**
   * Information used for verifying and incrementing a considered state
   * (token with rule step) match.
   */
  AtnState(Token inputToken, AtnRule rule, int stepNum, Tree<AtnState> parentStateNode,
           AtnParseOptions parseOptions, int repeatNum, int numSkipped, AtnState pushState) {
    this.inputToken = inputToken;
    this.rule = rule;
    this.stepNum = stepNum;
    this.parentStateNode = parentStateNode;
    this.parseOptions = parseOptions;
    this.repeatNum = repeatNum;
    this.skipNum = numSkipped;
    this.matchResult = null;
    this.pushState = pushState;
    this.popCount = 0;
    this._isSkipped = false;
  }

  /** Copy constructor */
  AtnState(AtnState other) {
    this.inputToken = other.inputToken;
    this.rule = other.rule;
    this.stepNum = other.stepNum;
    this.parentStateNode = other.parentStateNode;
    this.parseOptions = other.parseOptions;
    this.repeatNum = other.repeatNum;
    this.skipNum = other.skipNum;
    this.matchResult = other.matchResult;
    this.pushState = other.pushState;
    this.isPoppedState = other.isPoppedState;
    this.popCount = other.popCount;
    this._isSkipped = other._isSkipped;
    this._ruleStep = other._ruleStep;
    this.computedNextToken = other.computedNextToken;
    this._nextToken = other._nextToken;
  }

  /**
   * Determine whether this instance (if verified to match) is a valid end.
   */
  boolean isValidEnd(Set<Integer> stopList) {
    boolean result = false;

    if (getMatched() && isRuleEnd() && isPushEnd()) {
      final Token nextToken = getNextToken(stopList);
      result = (nextToken == null) || !parseOptions.getConsumeAllText();
    }

    return result;
  }

  /**
   * Determine whether all push states would pop to rule ends.
   */
  private boolean isPushEnd() {
    boolean result = true;

    for (AtnState curPushState = pushState; curPushState != null; curPushState = curPushState.pushState) {
      if (!curPushState.isRuleEnd()) {
        result = false;
        break;
      }
    }

    return result;
  }

  /**
   * Get a "pop" state based on this instance's push state and this state as
   * the end state, where a "pop" state is a temporary state used to generate
   * the next states in a parent rule after completing a 'pushed' rule.
   * 
   * The pop state also acts as a marker when constructing a parse tree
   * from the states tree to identify when to move back up to a parent
   * in the parse tree for adding subsequent children from later matched
   * states.
   */
  AtnState popState(Tree<AtnState> parentStateNode) {
    AtnState result = null;

    if (pushState != null) {
      // verify rule (constituent) with the (pushState.)rule test
      result = new AtnState(inputToken, pushState.rule, pushState.stepNum,
                            parentStateNode, pushState.parseOptions,
                            pushState.repeatNum, pushState.skipNum,
                            pushState.pushState);
      result.isPoppedState = true;
      result.popCount = 1;
    }

    return result;
  }

  /**
   * Get the next state for a repeat of the step if the step could
   * repeat.
   */
  AtnState getNextRepeatState(Tree<AtnState> curStateNode, AtnState referenceState, boolean incToken, Set<Integer> stopList) {
    AtnState result = null;

    if (referenceState == null) referenceState = this;

    if (referenceState.getRuleStep().repeats()) {
      if (incToken && !getRuleStep().consumeToken()) incToken = false;

      final Token nextToken = incToken ? getNextToken(stopList) : this.inputToken;
      if (nextToken != null) {
        result =
          new AtnState(
            nextToken, rule, stepNum,
            curStateNode, parseOptions, repeatNum + (incToken ? 1 : 0), 0, pushState);
      }
    }

    return result;
  }

  /**
   * Get the next state for incrementing to the next rule step if
   * incrementing is possible.
   */
  AtnState getNextStepState(Tree<AtnState> curStateNode, boolean incToken, Set<Integer> stopList) {
    AtnState result = null;

    if (!rule.isLast(stepNum)) {
      final int nextStepNum = getNextStepNum();
      if (nextStepNum >= 0) {

        if (incToken && !getRuleStep().consumeToken()) incToken = false;

        final Token nextToken = incToken ? getNextToken(stopList) : this.inputToken;
        if (nextToken != null) {
          result =
            new AtnState(
              nextToken, rule, nextStepNum,
              curStateNode, parseOptions, 0, 0, pushState);
        }
      }
    }

    return result;
  }

  /**
   * Get the next state for retrying this info's step with a revised
   * input token.
   */
  AtnState getNextRevisedState() {
    AtnState result = null;

    if (getRuleStep().consumeToken()) {
      final Token nextToken = computeRevisedToken();

      if (nextToken != null) {
        result = new AtnState(
          nextToken, rule, stepNum,
          parentStateNode, parseOptions, repeatNum, skipNum, pushState);
      }
    }

    return result;
  }

  AtnState getSkipOptionalState() {
    AtnState result = null;

    if (getRuleStep().isOptional()) {
      if (!rule.isLast(stepNum)) {
        final int nextStepNum = getNextStepNum();
        if (nextStepNum >= 0) {
          // increment step
          result =
            new AtnState(
              inputToken, rule, nextStepNum,
              parentStateNode, parseOptions, 0, 0, pushState);
        }
      }
      // else, return null and let caller add Pop state
    }

    return result;
  }

  private boolean canBeSkipped() {
    boolean result = (skipNum + inputToken.getWordCount() - 1) < Math.max(parseOptions.getSkipTokenLimit(), getRuleStep().getSkip());

    if (result && !parseOptions.getConsumeAllText()) {
      // when not consuming all text,
      // disable skip functionality for first matching rule step in a rule
      final AtnState parentState = (parentStateNode != null) ? parentStateNode.getData() : null;
      if (parentState == null || !(parentState.getMatched() || parentState.isPoppedState() || parentState.isSkipped())) {
        result = false;
      }
    }

    return result;
  }

  AtnState getNextSkippedState(Tree<AtnState> curStateNode, Set<Integer> stopList) {
    AtnState result = null;

    if (canBeSkipped()) {
      // increment the token, not the rule step
      final Token nextToken = getNextSmallestToken(stopList);
      if (nextToken != null) {
        markAsSkipped();
        result = new AtnState(nextToken, rule, stepNum, curStateNode, parseOptions, repeatNum, skipNum, pushState);
      }
    }

    return result;
  }


  /**
   * Get the (possibly cached) next token following this instance's considered state.
   */
  Token getNextToken(Set<Integer> stopList) {
    if (!computedNextToken) {
      _nextToken = computeNextToken(inputToken);

      if (_nextToken != null && stopList != null && stopList.contains(_nextToken.getStartIndex())) {
        _nextToken = null;
      }

      computedNextToken = true;
    }
    return _nextToken;
  }

  private Token getNextSmallestToken(Set<Integer> stopList) {
    Token result = inputToken.getNextSmallestToken();

    if (result != null) {
      result = rule.getGrammar().getAcceptedToken(rule.getTokenFilterId(), result, false, inputToken, false, false, this);

      if (result != null && stopList != null && stopList.contains(result.getStartIndex())) {
        result = null;
      }
    }

    return result;
  }

  /**
   * Get the next step num or -1 if there isn't another step after all.
   */
  private final int getNextStepNum() {
    int result = stepNum + 1;

    // check the step's 'require' attribute
    while (true) {
      final AtnRuleStep step = rule.getStep(result);
      if (step == null) {
        result = -1;
        break;
      }

      // Check for 'unless' to see if we can skip considering this next step
      // Note that we won't fully know if we have met 'require' constraints
      // until we get to that state, so we won't rule out 'requires' just yet.
      // Also, the 'unless' constraint will need to be checked again once
      // we've parsed through the current constituent.

      final String unless = step.getUnless();
      if (unless == null || !haveRequired(unless, true)) {
        break;
      }
      // if requirements aren't met, increment and loop
      else {
        ++result;
      }
    }

    return result;
  }

  // Double-Check require and unless with new information of considering this state in context
  private final boolean meetsRequirements() {

    final AtnRuleStep step = rule.getStep(stepNum);
    final String require = step.getRequire();
    boolean result = (require == null || haveRequired(require, false));

    if (result) {
      final String unless = step.getUnless();
      if (unless != null) {
        result = !haveRequired(unless, false);
      }
    }
    

    return result;
  }

  private final boolean haveRequired(String require, boolean includeThisState) {
    boolean result = false;

    if (includeThisState) {
      result = AtnStateUtil.matchesCategory(this, require);
    }

    if (!result) {
      // haven't verified yet, look back in state history
      final int[] levelDiff = new int[]{0};
      final AtnState priorMatch = AtnStateUtil.findPriorMatch(this, require, levelDiff);

      if (priorMatch != null) {
        // can find match 'down' (pushed), but not up (popped)
        result = (levelDiff[0] <= 0);
      }
    }

    return result;
  }

  private String showStateTree() {
    return AtnStateUtil.showStateTree(parentStateNode);
  }

  private String showStatePath() {
    return AtnStateUtil.showStatePath(this);
  }

  public final AtnState getParentState() {
    AtnState result = null;

    if (parentStateNode != null) {
      result = parentStateNode.getData();
    }

    return result;
  }

  private final Token computeNextToken(Token inputToken) {
    Token result = null;

    if (matchResult != null && !matchResult.inc()) {
      result = inputToken;
    }
    else {
      // get the next token without crossing a hard break boundary
      final Token nextToken = inputToken.getNextToken();
      if (nextToken != null) {
        result = rule.getGrammar().getAcceptedToken(rule.getTokenFilterId(), nextToken, false, inputToken, false, true, this);
      }
    }

    return result;
  }

  private Token computeRevisedToken() {
    Token result = null;

    if (!isSkipped()) {
      result = rule.getGrammar().getAcceptedToken(rule.getTokenFilterId(), inputToken.getRevisedToken(), true, inputToken, true, false, this);
    }

    return result;
  }

  /**
   * Determine whether this instance's token matches the step category
   * according to the grammar.
   */
  MatchResult tokenMatchesStepCategory(AtnGrammar grammar) {
    MatchResult result = null;
    boolean matched = false;

    final AtnRuleStep ruleStep = getRuleStep();
    if (ruleStep.getIgnoreToken()) {
      matched = ruleStep.verify(inputToken, this);
    }
    else {
      String category = ruleStep.getCategory();

      if (grammar.getCat2Classifiers().containsKey(category)) {
        for (AtnStateTokenClassifier classifier : grammar.getCat2Classifiers().get(category)) {
          final MatchResult matchResult = classifier.classify(inputToken, this);
          if (matchResult.matched() && ruleStep.verify(inputToken, this)) {
            result = matchResult;
            break;
          }
        }
      }
      else {
        if (!grammar.getCat2Rules().containsKey(category)) {
          // use an "identity" classifier for literal grammar tokens.
          matched = category.equals(inputToken.getText());
        }

        // check for a feature that matches the category
        if (!matched) {
          matched = inputToken.getFeature(category, null) != null;
        }

        if (matched) {
          matched = ruleStep.verify(inputToken, this);
        }
      }
    }

    if (result == null) {
      result = MatchResult.getInstance(matched);
    }

    return result;
  }

  private final boolean applyTests() {
    return getRuleStep().verify(inputToken, this);
  }

  void markAsSkipped() {
    this._isSkipped = true;
    ++this.skipNum;
  }


  public String toString() {
    final StringBuilder result = new StringBuilder();

    result.append(rule.getRuleName());

    if (rule.getRuleId() != null) {
      result.append('[').append(rule.getRuleId()).append(']');
    }

    result.
      append('-').
      append(getRuleStep().getCategory()).
      append('(').
      append(inputToken).
      append(')');

    return result.toString();
  }


  private final boolean matchesRulePath(AtnState other) {
    if (other == null) return false;
    if (this == other) return true;

    boolean result = false;

    if (rule == other.getRule() &&
        stepNum == other.getStepNum() &&
        repeatNum == other.getRepeatNum()) {

      if (parentStateNode == other.getParentStateNode()) {
        result = true;
      }
      else if (parentStateNode != null && other.getParentStateNode() != null) {
        final AtnState parentState = parentStateNode.getData();
        final AtnState otherParentState = other.getParentStateNode().getData();

        if (parentState == otherParentState) {
          result = true;
        }
        else if (parentState != null) {
          result = parentState.matchesRulePath(otherParentState);
        }
      }
    }

    return result;
  }

  private final void addActiveBracket(AtnGrammar.Bracket bracket) {
    if (this.activeBrackets == null) this.activeBrackets = new ArrayList<AtnGrammar.Bracket>();
    this.activeBrackets.add(bracket);
  }

  private final void addStartBracket(AtnGrammar.Bracket startBracket) {
    if (pushState != null) {
      pushState.addActiveBracket(startBracket);
    }
  }

  private final AtnGrammar.Bracket findStartBracket() {
    return rule.getGrammar().findStartBracket(inputToken);
  }

  private final AtnGrammar.Bracket findEndBracket(AtnState[] bracketPushState) {
    AtnGrammar.Bracket result = null;

    for (AtnState state = pushState; state != null; state = state.getPushState()) {
      if (state.activeBrackets != null) {
        final int num = state.activeBrackets.size();
        for (int i = num - 1; i >= 0; --i) {
          final AtnGrammar.Bracket bracket = state.activeBrackets.get(i);
          if (bracket.matchesEnd(inputToken)) {
            result = bracket;
            bracketPushState[0] = state;
            break;
          }
        }
        if (result != null) break;
      }
    }

    return result;
  }

  private boolean bracketsMatch(AtnState[] bracketPushState) {
    boolean result = true;

    AtnGrammar.Bracket endBracket = null;
    bracketPushState[0] = null;

    // check for start bracket match
    AtnGrammar.Bracket startBracket = findStartBracket();

    if (startBracket != null) {
      // verify conditions for starting a bracket: must be first matching step for constituent
      if (this.pushState != this.getParentState()) {
        // can't start!
        startBracket = null;
        result = false;
      }
    }

    if (result) {
      if (startBracket != null) {
        // unless immediate end to startBracket
        if (!startBracket.matchesEnd(inputToken)) {
          // add start bracket to this state
          addStartBracket(startBracket);
        }
        else {
          bracketPushState[0] = pushState;
        }
      }
      else {
        endBracket = findEndBracket(bracketPushState);
      }
    }

    return result;
  }


  private final boolean applyAllPops(Tree<AtnState> nextStateNode, LinkedList<AtnState> states, LinkedList<AtnState> skipStates, Set<Integer> stopList, AtnState bracketPushState) {
    boolean result = true;

    if (isRuleEnd()) {
      final int statesSize = states.size();
      final int skipStatesSize = skipStates.size();

      final AtnGrammar grammar = rule.getGrammar();
      Tree<AtnState> popStateNode = nextStateNode;
      AtnState popState = popState(popStateNode);
      if (bracketPushState != null && popStateNode.getData().getPushState() == bracketPushState) {
        bracketPushState = null;
      }
      while (popState != null) {
        // apply popState test
        result = popState.applyTests();
        if (!result) {
          // back out of popping
          while (states.size() > statesSize) states.removeLast();
          while (skipStates.size() > skipStatesSize) skipStates.removeLast();

          break;
        }

        popStateNode = popStateNode.addChild(popState);
        if (addNextStates(grammar, states, skipStates, popState, popStateNode, true, true, stopList, true, bracketPushState)) {
          if (!popState.isRuleEnd()) popState = null;
          else {
            if (bracketPushState != null && popStateNode.getData().getPushState() == bracketPushState) {
              // no longer need to prevent forward branching (due to end bracket)
              bracketPushState = null;
            }
            popState = popState.popState(popStateNode);
          }
        }
        else break;
      }

      if (result) {
        // remove now unnecessary skipped states
        for (AtnState parentState = this; skipStates.size() > 0 && parentState != null; parentState = parentState.getParentState()) {
          if (parentState == this || parentState.getMatched()) {
            for (Iterator<AtnState> skipIter = skipStates.iterator(); skipIter.hasNext(); ) {
              final AtnState skipState = skipIter.next();
              if (parentState.encompassesToken(skipState.getInputToken())) {
                skipIter.remove();
              }
            }
          }
        }
      }
    }

    return result;
  }

  /**
   * Determine whether this state's input token encompasses the given token.
   */
  private final boolean encompassesToken(Token token) {
    return inputToken.encompasses(token);
  }


  private static boolean trace = false;

  public static final void setTrace(boolean traceValue) {
    trace = traceValue;
  }

  static List<CategorizedToken> computeTokens(Tree<AtnState> stateNode) {
    final List<CategorizedToken> result = new ArrayList<CategorizedToken>();
    final LinkedList<Tree<AtnState>> stateNodes = stateNode.getRootPath();

    for (int pathIndex = 1; pathIndex < stateNodes.size(); ++pathIndex) {
      final Tree<AtnState> pathStateNode = stateNodes.get(pathIndex);
      final AtnState pathState = pathStateNode.getData();
      if (pathState.getMatched() && pathState.getRuleStep().consumeToken()) {
        result.add(new CategorizedToken(pathState.getInputToken(), pathState.getRuleStep().getCategory()));
      }
    }

    return result;
  }

  static boolean matchTokenToRule(AtnGrammar grammar, LinkedList<AtnState> states, LinkedList<AtnState> skipStates, Set<Integer> stopList) {
    boolean result = false;

    AtnState[] bracketPushState = new AtnState[]{null};

    while ((states.size() + skipStates.size() > 0) && !result) {
      final AtnState curstate = states.size() > 0 ? states.removeFirst() : skipStates.removeFirst();

      boolean success = false;
      bracketPushState[0] = null;
      final boolean meetsRequirements = curstate.meetsRequirements();
      MatchResult matchResult = null;
      boolean matches = meetsRequirements;
      if (matches) {
        matchResult = curstate.tokenMatchesStepCategory(grammar);
        matches = matchResult.matched();

        // check bracket conditions
        if (matches) {
          matches = curstate.bracketsMatch(bracketPushState);

          if (!matches) matchResult = null;
        }
      }
      final Tree<AtnState> nextStateNode = curstate.parentStateNode.addChild(curstate);

      if (trace) {
        System.out.println(curstate + "\t" + matches + "\t" + (matches ? AtnStateUtil.showStateTree(nextStateNode) : ""));
      }

      if (matches) {
        matches = curstate.applyAllPops(nextStateNode, states, skipStates, stopList, bracketPushState[0]);
      }

      if (matches) {
        success = true;
        curstate.setMatchResult(matchResult);

        if (curstate.isValidEnd(stopList)) {
          // found a valid full parse
          result = true;
        }
      }
      else {
        matchResult = null;
      }

      if (bracketPushState[0] == null) {
        success = addNextStates(grammar, states, skipStates, curstate, nextStateNode,
                                false, matches, stopList, meetsRequirements, null);
      }
    }

    return result;
  }

  private static boolean addNextStates(AtnGrammar grammar, LinkedList<AtnState> states, LinkedList<AtnState> skipStates, AtnState curstate, Tree<AtnState> nextStateNode, boolean isPop, boolean inc, Set<Integer> stopList, boolean meetsRequirements, AtnState bracketPushState) {
    if (curstate == null) return false;

    boolean foundOne = inc || isPop;
    AtnState nextstate = null;

    // increment token
    if (foundOne) {
      if (bracketPushState == null) {
        nextstate = curstate.getNextRepeatState(nextStateNode, isPop ? curstate : null, inc, stopList);

        if (nextstate != null) {
          addState(states, nextstate);
        }
      }

      if (bracketPushState == null) {
        nextstate = curstate.getNextStepState(nextStateNode, inc, stopList);
        if (nextstate != null) {
          addState(states, nextstate);
        }
      }

      // revise token
      nextstate = curstate.getNextRevisedState();
      if (nextstate != null) { addState(states, nextstate); foundOne = true; }

      return foundOne;
    }

    // revise token
    nextstate = curstate.getNextRevisedState();
    if (nextstate != null) { addState(states, nextstate); foundOne = true; }

    // account for optional step.
    if (curstate.getRuleStep().isOptional() && !curstate.isRepeat()) {
      nextstate = curstate.getSkipOptionalState();
      if (nextstate != null) {
        addState(states, nextstate); foundOne = true;
      }
    }

    // apply (push) rules
    if (meetsRequirements && bracketPushState == null) {
      final String category = curstate.getRuleStep().getCategory();
      if (grammar.getCat2Rules().containsKey(category)) {
        foundOne = true;
        for (AtnRule rule : grammar.getCat2Rules().get(category)) {
          addState(states, new AtnState(curstate.getInputToken(), rule, 0, nextStateNode, curstate.parseOptions, 0, 0, curstate));
        }

        // skip constituents
        if (curstate.canBeSkipped()) {
          final AtnState dupstate = new AtnState(curstate);
          final Tree<AtnState> dupstateNode = new Tree<AtnState>(dupstate);
          nextstate = dupstate.getNextSkippedState(dupstateNode, stopList);
          if (nextstate != null) {
            nextStateNode.getParent().addChild(dupstateNode);
            dupstate.parentStateNode = nextStateNode.getParent();
            addState(skipStates, nextstate);
          }
        }
      }
    }

    // skip tokens
    if (!foundOne && bracketPushState == null) {
      nextstate = curstate.getNextSkippedState(nextStateNode, stopList);
      if (nextstate != null) addState(skipStates, nextstate);
    }

    return foundOne;
  }

  private static final void addState(LinkedList<AtnState> states, AtnState nextstate) {
    boolean isDup = false;

    // check for duplicates
    final Token token = nextstate.getInputToken();
    for (AtnState state : states) {
      if (token == state.getInputToken() && nextstate.matchesRulePath(state)) {
        isDup = true;
        break;
      }
    }

    if (!isDup) states.addLast(nextstate);
  }
}
