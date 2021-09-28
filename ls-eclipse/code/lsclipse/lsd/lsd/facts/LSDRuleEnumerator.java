/* 
*    Logical Structural Diff (LSDiff)  
*    Copyright (C) <2015>  <Dr. Miryung Kim miryung@cs.ucla.edu>
*
*    This program is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
*    (at your option) any later version.
*
*    This program is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package lsd.facts;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import lsclipse.RestrictionOption;
import lsd.io.LSDAlchemyRuleReader;
import lsd.io.LSDTyrubaFactReader;
import lsd.io.LSDTyrubaRuleChecker;
import lsd.rule.LSDBinding;
import lsd.rule.LSDFact;
import lsd.rule.LSDInvalidTypeException;
import lsd.rule.LSDLiteral;
import lsd.rule.LSDPredicate;
import lsd.rule.LSDRule;
import lsd.rule.LSDVariable;
import lsdSimplified.FactClassifier;
import metapackage.MetaInfo;
import tyRuBa.engine.RuleBase;
import tyRuBa.modes.TypeModeError;
import tyRuBa.parser.ParseException;

public class LSDRuleEnumerator {
	private LSDTyrubaRuleChecker ruleChecker;

	private LSDTyrubaRuleChecker remainingRuleChecker;

	private int minMatches = 1;

	private int minMatchesPerLiteral = 0;

	private int maxExceptions = 10;

	private double minAccuracy = 0;

	private int beamSize = 100;

	private ArrayList<LSDFact> read2kbFacts = new ArrayList<LSDFact>();

	private ArrayList<LSDFact> readDeltaFacts = new ArrayList<LSDFact>();

	private ArrayList<LSDRule> winnowingRules = new ArrayList<LSDRule>();

	private ArrayList<LSDFact> workingSet2KB = new ArrayList<LSDFact>();

	private ArrayList<LSDFact> workingSetDeltaKB = new ArrayList<LSDFact>();


	private static ArrayList<LSDPredicate>  getUniquePredicates (Collection<LSDFact> facts, boolean antedecedent) { 
		TreeSet<String> predNames = new TreeSet<String>(); 
		for (LSDFact f : facts) { 
			LSDPredicate p = f.getPredicate(); 
			predNames.add(p.getName()); 
		}
		ArrayList<LSDPredicate> preds = new ArrayList<LSDPredicate>(); 
		
		for (String s: predNames) { 
			LSDPredicate p = LSDPredicate.getPredicate(s); 
			if (antedecedent && p.isAntecedentPredicate()) { 
				preds.add(p);
				// antecedent
			} else {
				if (p.conclusionConstraints()){
					preds.add(p); 
				}
				// consequent// MK changes to limit consequent predicates
			}
		}
		
		return preds;
	}
	private ArrayList<LSDRule> modifiedWinnowingRules = new ArrayList<LSDRule>();

	// MK: added for manipulating 2kb

	final LSdiffDistanceFactBase onDemand2KB;

	final LSdiffHierarchialDeltaKB onDemandDeltaKB;

	// Cumulative numbers
	// These are not in use, although they are not deleted
	public int statsGeneratedPartials = 0;

	public int statsEnqueuedPartials = 0;

	public int statsSavedPartials = 0;

	int statsGeneratedGroundings = 0;

	int statsEnqueuedGroundings = 0;

	int statsSavedGroundings = 0;

	int statsPartialValidQueryCount = 0;

	int statsGroundingConstantsQueryCount = 0;

	int statsGroundingValidQueryCount = 0;

	int statsGroundingExceptionsQueryCount = 0;

	double timeUngroundRuleGeneration;

	double timePartiallyGroundRuleGeneration;

	int numValidRules;

	int numRulesWithException;

	int num2KBSize;

	int numDeltaKBSize;

	int numWinnowDeltaKBSize;

	int numRemainingDeltaKBSize;

	int numFinalRules;

	private long enumerationTimestamp = 0;

	// ///////////

	private LSDFactBase fb;

	static int varNum = 0;

	public BufferedWriter output;

	private String resString;

	private int antecedantSize;

	private static final boolean isConclusion = true;

	private static final boolean isAntecedent = !isConclusion;

	long timer = 0, lastStart = 0;

	static {
		RuleBase.silent = true;
	}

	public LSDRuleEnumerator(File twoKBFile, File deltaKBFile,
			File winnowingRulesFile, File resultsFile, int minConcFact,
			double accuracy, int k, int beamSize2, int maxException,
			File modifiedWinnowingRulesFile, BufferedWriter output)
			throws Exception {
		setMinMatchesPerLiteral(0);
		setMaxExceptions(maxException);
		setBeamSize(beamSize);
		setMinMatches(minConcFact);
		setMinAccuracy(accuracy);
		setAntecedentSize(k);
		this.output = output;
		this.fb = new LSDFactBase();
		// reads input files and builds lists of facts
		startTimer();
		read2kbFacts = new LSDTyrubaFactReader(twoKBFile).getFacts();
		readDeltaFacts = new LSDTyrubaFactReader(deltaKBFile).getFacts();
		winnowingRules = new LSDAlchemyRuleReader(winnowingRulesFile)
				.getRules();

		// set onDemand database manipulators
		onDemand2KB = new LSdiffDistanceFactBase(read2kbFacts, readDeltaFacts);
		onDemandDeltaKB = new LSdiffHierarchialDeltaKB(readDeltaFacts);

		// set the modified winnowing rules
		modifiedWinnowingRules = new LSDAlchemyRuleReader(new File(
				MetaInfo.modifiedWinnowings)).getRules();
		
		
		stopTimer();
	}

	public LSDRuleEnumerator(ArrayList<LSDFact> input2kbFacts, 
			ArrayList<LSDFact> inputDeltaFacts,
			int minConcFact, double accuracy, int k, int beamSize2, int maxException,
			BufferedWriter output) throws Exception {

		setMinMatchesPerLiteral(0);
		setMaxExceptions(maxException);
		setBeamSize(beamSize);
		setMinMatches(minConcFact);
		setMinAccuracy(accuracy);
		setAntecedentSize(k);
		this.output = output;
		this.fb = new LSDFactBase();
		startTimer();
		read2kbFacts = input2kbFacts;
		readDeltaFacts = inputDeltaFacts;
//		winnowingRules = inputWinnowingRules;

		// set onDemand database manipulators
		onDemand2KB = new LSdiffDistanceFactBase(read2kbFacts, readDeltaFacts);
		onDemandDeltaKB = new LSdiffHierarchialDeltaKB(readDeltaFacts);

		// set the modified winnowing rules
		modifiedWinnowingRules = new LSDAlchemyRuleReader(new File(
				MetaInfo.modifiedWinnowings)).getRules();
		stopTimer();
	}

	public void setAntecedentSize(int k) {
		this.antecedantSize = k;
	}

	public void setMinMatches(int minMatches) {
		this.minMatches = minMatches;
	}

	public void setMinMatchesPerLiteral(int minMatchesPerLiteral) {
		this.minMatchesPerLiteral = minMatchesPerLiteral;
	}

	public void setMaxExceptions(int maxExceptions) {
		this.maxExceptions = maxExceptions;
	}

	public void setMinAccuracy(double minAccuracy) {
		this.minAccuracy = minAccuracy;
	}

	public void setBeamSize(int beamSize) {
		this.beamSize = beamSize;
	}

	// Based on the level the function uses appropriate read lists and loads fb
	public void loadFactBases(int hopDistance2KB, LSdiffFilter filter)
			throws Exception {

		// use hop depth 1 for now to filter 2KB facts
		onDemand2KB.expand(hopDistance2KB);
		workingSet2KB = onDemand2KB.getWorking2KBFacts();

		//MK 020611 I tried to turn this option temporarily off 
		TreeSet<LSDFact> workingDelta = new TreeSet<LSDFact>();
		onDemandDeltaKB.filterFacts(null, workingDelta, filter);
		workingSetDeltaKB = new ArrayList<LSDFact>(workingDelta);

		this.fb = new LSDFactBase(); 
		
		fb.load2KBFactBase(workingSet2KB);
		fb.loadDeltaKBFactBase(workingSetDeltaKB);
		fb.loadWinnowingRules(modifiedWinnowingRules);
		for (LSDRule r : modifiedWinnowingRules) { 
			System.out.println("MK 022311: winnowing Rule:\t "+r); 
		}

		List<LSDFact> afterWinnowing = fb.getRemainingFacts(true);

		this.num2KBSize = fb.num2KBFactSize();
		this.numDeltaKBSize = fb.numDeltaKBFactSize();
		this.numWinnowDeltaKBSize = afterWinnowing.size();

		ruleChecker = createRuleChecker();
		remainingRuleChecker = createReducedRuleChecker(new ArrayList<LSDRule>());

		System.out.println("Number of 2kbFacts: " + num2KBSize);
		System.out.println("Number of deltaFacts: " + numDeltaKBSize);
		System.out.println("Number of WinnowdeltaFacts: " + numWinnowDeltaKBSize);

	}


	// Primed fact bases are removed completely
	// Again here we should consider level
	// MK: make this rule checker always work only with respect to working 2KB
	// and working deltak
	private LSDTyrubaRuleChecker createRuleChecker() throws ParseException,
			TypeModeError, IOException {

		LSDTyrubaRuleChecker newRuleChecker = new LSDTyrubaRuleChecker();
		ArrayList<LSDFact> twoKB = workingSet2KB;
		ArrayList<LSDFact> deltaKB = workingSetDeltaKB;
		newRuleChecker.loadAdditionalDB(MetaInfo.included2kb);
		for (LSDFact fact : twoKB)
			newRuleChecker.loadFact(fact);
		newRuleChecker.loadAdditionalDB(MetaInfo.includedDelta);
		for (LSDFact fact : deltaKB)
			newRuleChecker.loadFact(fact);
		return newRuleChecker;
	}

	public LSDTyrubaRuleChecker createReducedRuleChecker(
			Collection<LSDRule> additionalRules) throws IOException,
			TypeModeError, ParseException {
		LSDTyrubaRuleChecker newRuleChecker = new LSDTyrubaRuleChecker();
		newRuleChecker.loadAdditionalDB(MetaInfo.included2kb);
		ArrayList<LSDFact> twoKB = workingSet2KB;
		ArrayList<LSDFact> deltaKB = workingSetDeltaKB;
		ArrayList<LSDRule> winnowing = modifiedWinnowingRules;

		for (LSDFact fact : twoKB)
			newRuleChecker.loadFact(fact);
		newRuleChecker.loadAdditionalDB(MetaInfo.includedDelta);
		LSDFactBase localFB = new LSDFactBase();
		localFB.load2KBFactBase(twoKB);
		localFB.loadDeltaKBFactBase(deltaKB);
		localFB.loadWinnowingRules(winnowing);
		localFB.loadWinnowingRules(additionalRules);

		List<LSDFact> afterWinnowing = localFB.getRemainingFacts(true);
		fb = localFB;
		this.num2KBSize = fb.num2KBFactSize();
		this.numDeltaKBSize = fb.numDeltaKBFactSize();
		this.numWinnowDeltaKBSize = afterWinnowing.size();

		for (LSDFact fact : afterWinnowing)
			newRuleChecker.loadFact(fact);
		return newRuleChecker;
	}

	// Creates a new ruleChecker with all deltaKB facts.
	public LSDTyrubaRuleChecker createRuleChecker(ArrayList<String> cluster)
			throws IOException, TypeModeError, ParseException {
		LSDTyrubaRuleChecker newRuleChecker = new LSDTyrubaRuleChecker();
		newRuleChecker.loadAdditionalDB(MetaInfo.included2kb);
		for (LSDFact fact : read2kbFacts)
			for (String str : cluster) {
				if (fact.toString().contains(str))
					newRuleChecker.loadFact(fact);
			}

		newRuleChecker.loadAdditionalDB(MetaInfo.includedDelta);
		for (LSDFact fact : readDeltaFacts)
			for (String str : cluster) {
				if (fact.toString().contains(str))
					newRuleChecker.loadFact(fact);
			}
		return newRuleChecker;
	}

	// The way timer works is changed
	// The filed timer is a continuous timer that is controlled with startTimer
	// and stopTimer
	private void startTimer() {
		lastStart = new Date().getTime();
	}

	private void stopTimer() {
		long temp = new Date().getTime() - lastStart;
		timer += temp;
	}


	
	private double nextEnumerationTiming() {
		long nowTime = new Date().getTime();
		double delta = (nowTime - enumerationTimestamp) / 1000.;
		enumerationTimestamp = nowTime;
		return delta;
	}

	// MK: copied from optimized diff
	// MK: I am going to skip java library constants.
	// MK: 020511 This code is called by groundRules. This code takes one
	// ungrounded rule and makes multiple partial grounded rules where some
	// variables are bound to constants
	private List<LSDRule> groundRule(LSDRule ungroundedRule) {
		ArrayList<LSDRule> rules = new ArrayList<LSDRule>();
		// Create a Stack of groundings.
		Stack<Grounding> groundings = new Stack<Grounding>();
		// Add the initial empty grounding with just a list of all vars.
		groundings.add(new Grounding(ungroundedRule));
		statsEnqueuedGroundings++;
		statsGeneratedGroundings++;
		// For each head of the stack:
		next: while (!groundings.isEmpty()) {
			// create all the possible groundings with the next var grounded (or
			// not)
			// add them to the queue.
			// Grounding grounding = groundings.remove();gf 

			Grounding grounding = groundings.pop();
			// list all variables that could be replaced with constants. 
			Iterator<LSDVariable> it = grounding.getVariablesToSubtitute().iterator(); 
			if (grounding.getVariablesToSubtitute().size()==0) continue; 
			LSDVariable variable = it.next();
			// MK 020611. This is to prevent picking variables that must be left as a universal quantifiers. 			
			// MK 020511. Perhaps we could only consider variables for replacing with constants 
			// (Assemble list of consts that could sub in for next var)
			startTimer();
			Set<String> constants = ruleChecker.getReplacementConstants(
					grounding.rule, variable);
			statsGroundingConstantsQueryCount++;
			constants.add(null);
			// For each constant of type of next var and for none:
			for (String constant : constants) {
				// Don't reuse constants. Ensures the generation of unique
				// rules.
				if (constant != null
						&& grounding.usedConstants.contains(constant))
					continue;
				
				// MK
				// FIXME: I decided to penalize constants with java library in learned rules. 
				if (constant !=null && constant.indexOf("java.")>0)
					continue;
				// Substitute the constant in to a copy of the rule
				Grounding newGrounding = grounding.addGrounding(variable,
						constant);
				statsGeneratedGroundings++;
				// If rule is still valid and a query has results:
				// Do this in stages to avoid unnecessary queries.
				if (newGrounding.rule.containsFacts())
					continue;
				int minMatchesByLength = minMatchesPerLiteral
						* (newGrounding.rule.getLiterals().size() - 1);
				startTimer();
				int numMatches = countRemainingMatches(newGrounding.rule);
				statsGroundingValidQueryCount++;
				if (numMatches < minMatches || numMatches < minMatchesByLength)
					continue;
				// add new grounding def. to the queue
				// If we can ground more variables, we'll repeat
				if (newGrounding.getVariablesToSubtitute().size() > 0) {
					if (newGrounding.scanned) {
						if (newGrounding.isGrounded() && newGrounding.scanned
								&& newGrounding.rule.isValid() ) {

							// MK 020611: TO ADD
							// I need to check that all free variables in the
							// antecedent must be either a key predicate
							// variable in the conclusion or a connecting
							// variable
							// This check should only work for partially ground
							// rules.

							// MK 020511 adding rules 
							
							rules = addRule(rules, grounding,
									grounding.numMatches);
						} else if (grounding.scanned)
							continue next;
					}
					else {
						statsEnqueuedGroundings++;
						newGrounding.scanned = true;
						newGrounding.numMatches = numMatches;
						groundings.add(newGrounding);
					}
				} else if (newGrounding.rule.isValid()
						&& newGrounding.isGrounded())
					// MK 020511 adding rules
					// MK 020611: TO ADD
					// I need to check that all free variables in the antecedent
					// must be either a key predicate variable in the conclusion
					// or a connecting variable
					// This check should only work for partially ground rules.
					addRule(rules, newGrounding, numMatches);
			}// end for loop that iterates over constants
		}

		return rules;
	}

	// MK: copied from opitmized diff
	// This code is called by enumerateRules by passing ungroundedRules 
	private List<LSDRule> groundRules(List<LSDRule> ungroundedRules) {
	
		// We now have a list of the fully-ungrounded rules. Now we'll add rules
		// to our list of partial groundings.
		ArrayList<LSDRule> originalRules = new ArrayList<LSDRule>();
		// For each ungrounded rule: create all (partial or full) groundings.
		int rulesGrounded = 0;
		for (LSDRule ungroundedRule : ungroundedRules) {
			System.err.println("[unground rule]: "+ ungroundedRule);
			if (rulesGrounded % 10 == 0) {
				System.err
						.println((((float) (rulesGrounded * 100)) / ungroundedRules
								.size())
								+ "% done.");
				System.err.flush();
			}
			originalRules.addAll(groundRule(ungroundedRule));
			rulesGrounded += 1;
		}


		return originalRules;

	}

	// MK: Copied from optimized diff.
	
	// return value: ungrounded rules 
	// 1st arg: old partial rules 
	// 2st arg; new partial rules
	// this is called by bruteforce learning eventually 
	// bruteforcelearning=>enumerateRules(int) => extendUngroundedRules 
	private List<LSDRule> extendUngroundedRules(List<LSDRule> oldPartialUngroundedRules,
			List<LSDRule> newPartialUngroundedRules) {
		Set<LSDRule> ungroundedRules = new LinkedHashSet<LSDRule>();
		
		List<LSDPredicate> predicates = getUniquePredicates(workingSet2KB, true);
		System.out.println("[extendUngroundRules: predicates to add]\t"+predicates); 
		
		for (LSDRule partialUngroundedRule : oldPartialUngroundedRules) {
			List<LSDLiteral> previousLiterals = partialUngroundedRule.getLiterals();
			LSDPredicate conclusionPredicate = partialUngroundedRule.getConclusions()
					.getLiterals().get(0).getPredicate();
			Set<Character> currentTypes = new HashSet<Character>();
			// MK 020611. get a list of free variables from a partial rule 
			for (LSDVariable variable : partialUngroundedRule.getFreeVariables())
				currentTypes.add(variable.getType());
			// For each predicate that could be added to as antecedent:
			for (LSDPredicate predicateToAdd : predicates) {
				LSDPredicate antecedant = null;
				if (partialUngroundedRule.getAntecedents() != null
						&& partialUngroundedRule.getAntecedents().getLiterals().size() > 0) {
					antecedant = partialUngroundedRule.getAntecedents().getLiterals()
							.get(0).getPredicate();
				}
				if (!predicateToAdd.allowedInSameRule(conclusionPredicate,
						antecedant))
					continue;

				// MK: check whether the predicate that we want to add has at least one type overlapping with previously learned rules
				if (!predicateToAdd.typeMatches(currentTypes))
					continue;
				
//				System.out.println("[extendedUngroundedRule]- trying adding a predicate to a partial rule:\t"+ predicate+"\t"+partialRule+ "\t"+currentTypes+"\t"+predicate.typeMatches(currentTypes) );
			
				// Create two lists of lists of bindings, one with [] in it.
				List<List<LSDBinding>> bindingsList = enumerateUngroundedBindings(
						partialUngroundedRule, predicateToAdd);
				// For each now complete list of bindings:
				perBindings: for (List<LSDBinding> bindings : bindingsList) {
					
					statsGeneratedPartials++;
					LSDLiteral newLiteral = null;
					try {
						newLiteral = new LSDLiteral(predicateToAdd, bindings,
								isAntecedent);
					} catch (LSDInvalidTypeException e) {
						System.err
								.println("We're taking types directly from the predicates, so we should never have this type error.");
						System.exit(-7);
					}
					// If the new literal is identical to a previous one, skip
					// it and continue
					for (LSDLiteral oldLiteral : previousLiterals) {
						if (oldLiteral
								.identifiesSameIgnoringNegation(newLiteral))
							continue perBindings;
					}
					// Add that binding to the partial rule
					LSDRule newPartialRule = new LSDRule(partialUngroundedRule);

					// MK 020811: MK added another constraint that at least one constant must appear in each predicate 
					// to prevent a rule like A(x1,x0) ^ A(x1,c1) => B(c0, X0)
					if (RestrictionOption.OPTION_AtLeastOneConstantPerPredicate) {
						if (newLiteral.atLeastOneConstHolder() == false
								|| newLiteral.atLeastOneVariable() == false)
							continue;
					}
					LSDVariable keyConclusionVariable = partialUngroundedRule
							.getKeyConclusionVariable();

					// add a new antecedent literal to the rule
					LSDVariable keyPredicateVariable = newLiteral
							.getKeyPredicateVariable();
					newPartialRule.addLiteral(newLiteral);

					// MK 020611: MK added a key variable match so that the key
					// variable of conclusion predicate is used as a key
					// variable of other predicates as well
					// for example
					// A(x1,x2) => B(x1, x2) instead of 
					// A(x3,x2) => B(x1, x2) 

					boolean keyVariableMatch = true;
					if (RestrictionOption.OPTION_KeyVariableMatchBetweenConsequentandAntecedent) {
						keyVariableMatch = keyPredicateVariable == null
								|| keyConclusionVariable == null
								|| keyPredicateVariable.getName().equals(
										keyConclusionVariable.getName());
					}
					
					// Add to the list of generated rules
					// Can be an invalid rule, but may become one with
					// grounding
					if (newPartialRule.literalsLinked()
							&& newPartialRule.hasValidLinks()
							&& keyVariableMatch)

					{
						int minMatchesByLength = minMatchesPerLiteral
								* (newPartialRule.getLiterals().size() - 1);
						startTimer();
						int numMatches = countRemainingMatches(newPartialRule,
								Math.max(minMatches, minMatchesByLength));
						statsPartialValidQueryCount++;
						if (numMatches >= minMatches
								&& numMatches >= minMatchesByLength) {
							statsSavedPartials++;
							ungroundedRules.add(newPartialRule);
							statsEnqueuedPartials++;
							newPartialUngroundedRules.add(newPartialRule);
						}
					}
				}
			}
		}
		return new ArrayList<LSDRule>(ungroundedRules);
	}

	// MK: copied from optimized diff
	private List<LSDRule> narrowSearch(List<LSDRule> partialRules,
			int currentLength) {
		ArrayList<LSDRule> chosenRules = new ArrayList<LSDRule>();
		ArrayList<LSDRule> sortedRules = sortRules(partialRules);
		int max = Math.min(beamSize, sortedRules.size());
		for (int i = 0; i < max; i++)
			chosenRules.add(sortedRules.get(i));
		return chosenRules;
	}

	public List<LSDFact> getRelevantFacts(LSDRule rule) {
		return fb.getRelevantFacts(rule);
	}
	public List<Map<LSDVariable, String>> getExceptions(LSDRule rule) {
		return fb.getExceptions(rule);
	}
	
	//MK Investigation Feb 4th, Who is calling bruteForceLearning?
	// this is called by doLSDiff 
	public List<LSDRule> bruteForceLearning(int hopDistance2KB,
			LSdiffFilter filter) {
		List<LSDRule> selectedSubset =null;
		
		try {
			long st = new Date().getTime();
			List<LSDRule> rules;
			loadFactBases(hopDistance2KB, filter);

			rules = enumerateRules(antecedantSize);
			numValidRules = rules.size();

			if (rules != null) {
				fb.loadWinnowingRules(rules);
				fb.forceWinnowing();
			}

			List<LSDFact> remainingFacts = fb.getRemainingFacts(true);
			numRemainingDeltaKBSize = remainingFacts.size();

			System.err.println("Found Rules:" +rules.size()); 
			int cnt=1;
			// MK 022311: 
			// remove rules that do not have any constants in the antecedent. 
			ArrayList<LSDRule> toBeRemoved = new ArrayList<LSDRule>(); 
			for (LSDRule r : rules) {
				if (r.hasConstantsInEveryAntecendent()) {
//					System.err.println(r.toString());
					int matches = countMatches(r);
					int exceptions = countExceptions(r);
					if (exceptions > 0)
						numRulesWithException++;
//					System.err.println("#" + cnt++ + "\t(" + matches + "/"
//							+ (matches + exceptions) + ")");
//					System.err.println(r);
//					for (LSDFact pfact : fb.getRelevantFacts(r))
//						System.err.println("#P:\t" + pfact);
				} else {
					if (RestrictionOption.OPTION_MustHaveAtLeastOneConstantInRule) {
						toBeRemoved.add(r);
					}
				}
			}

			rules.removeAll(toBeRemoved);
			selectedSubset = coverSet(rules, true, null);
			
			int cInfo = counttextual(selectedSubset);
			System.err.println("Selected Rules:" +selectedSubset.size()); 
			System.err.println("Remaining Facts:" +remainingFacts.size());
			System.err.println("Contextual Facts:" +cInfo); 
			for (LSDRule r : selectedSubset) {
				System.err.println(r.toString());
				int matches = countMatches(r);
				int exceptions = countExceptions(r);
				System.err.println("#" + cnt++ + "\t(" + matches + "/"
						+ (matches + exceptions) + ")");
				System.err.println(r);
				for (LSDFact pfact : fb.getRelevantFacts(r))
					System.err.println("#P:\t" + pfact);
			}

			for (LSDFact f : remainingFacts) { 
				System.err.print(f); 
			}
			
			long en = new Date().getTime();
			if (output!=null && rules != null && selectedSubset != null) {
				output.write((Double.valueOf((en - st)) / 1000.00) + " \t "
						+ rules.size() + " \t " + selectedSubset.size()
						+ " \t " + numRemainingDeltaKBSize + " \t " + resString
						+ cInfo);

				output.newLine();
			}

			shutdown();
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println(e.getMessage());
		}
		System.out.println("Done");
		return selectedSubset;
	}

	// MK: copied from optimized lsdiff
	List<LSDRule> coverSet(Collection<LSDRule> rules, boolean print,
			File rf) throws TypeModeError, FileNotFoundException,
			ParseException, IOException {
		List<LSDRule> chosenRules = new ArrayList<LSDRule>();
		List<LSDRule> remainingRules = new ArrayList<LSDRule>(rules);
		Collections.sort(remainingRules, new Comparator<LSDRule>(){
			@Override
			public int compare(LSDRule arg0, LSDRule arg1) {
				return (int)((arg0.getScore()-arg1.getScore())*1000);
			}});
		List<LSDFact> remainingFacts;
		HashMap<LSDRule, Integer> alreadyFoundExceptionCounts = new HashMap<LSDRule, Integer>();
		int startingNumFacts = -1;
		do {
			LSDFactBase fb = new LSDFactBase();
			LSDRule bestRule = null;
			int bestCount = 0;
			fb.load2KBFactBase(workingSet2KB);
			fb.loadDeltaKBFactBase(workingSetDeltaKB);
			fb.loadWinnowingRules(modifiedWinnowingRules);
			fb.loadWinnowingRules(chosenRules);
			remainingFacts = fb.getRemainingFacts(true);
			if (startingNumFacts == -1) // Only set this at the very beginning.
				startingNumFacts = remainingFacts.size();
			fb.loadWinnowingRules(remainingRules);
			for (Iterator<LSDRule> i = remainingRules.iterator(); i.hasNext();) {
				LSDRule rule = i.next();
				List<LSDFact> facts = fb.getRelevantFacts(rule);
				facts.retainAll(remainingFacts);
				int count = facts.size();
				if (count == 0) {
					i.remove();
					continue;
				}
				if (count > bestCount) {
					bestCount = count;
					bestRule = rule;
				} else if (count == bestCount) {
					// Compute exception counts, unless they already have been.
					// In that case, get the cached ones.
					Integer preFound = alreadyFoundExceptionCounts
							.get(bestRule);
					int exBestRule;
					if (preFound == null) {
						exBestRule = countExceptions(bestRule);
						alreadyFoundExceptionCounts.put(bestRule, exBestRule);
					} else {
						exBestRule = preFound;
					}
					preFound = alreadyFoundExceptionCounts.get(bestRule);
					int exRule;
					if (preFound == null) {
						exRule = countExceptions(rule);
						alreadyFoundExceptionCounts.put(rule, exRule);
					} else {
						exRule = preFound;
					}

					// Prefer rules with fewer exceptions
					if (exBestRule > exRule)
						bestRule = rule;
					// Given that, prefer more specific rules.
					else if (rule.generalityCompare(bestRule) < 0)
						bestRule = rule;
				}
			}
			if (bestRule != null) {
				chosenRules.add(bestRule);
				remainingRules.remove(bestRule);
				remainingFacts.removeAll(fb.getRelevantFacts(bestRule));
			}
		} while (!remainingFacts.isEmpty() && !remainingRules.isEmpty());

		LSDFactBase fb = new LSDFactBase();
		if (print) {
			fb.load2KBFactBase(workingSet2KB);
			fb.loadDeltaKBFactBase(workingSetDeltaKB);
			fb.loadWinnowingRules(modifiedWinnowingRules);
			fb.loadWinnowingRules(chosenRules);
			fb.forceWinnowing();
		}
		double coverage = Double.valueOf((startingNumFacts - remainingFacts
				.size()))
				/ Double.valueOf(startingNumFacts);
		double conciseness = Double.valueOf(startingNumFacts)
				/ Double.valueOf(chosenRules.size() + remainingFacts.size());
		this.resString = startingNumFacts + " \t " + coverage + " \t "
				+ conciseness + " \t ";
		// LogData(rf,remainingFacts,chosenRules,startingNumFacts);
		return chosenRules;
	}

	// MK 020511: this is used to increment a list of partially grounded rules 
	private ArrayList<LSDRule> addRule(ArrayList<LSDRule> originalRules,
			Grounding grounding, int numMatches) {
		double accuracy = measureAccuracy(grounding.rule, minAccuracy,
				maxExceptions, numMatches);
		statsGroundingExceptionsQueryCount++;
		if (accuracy >= minAccuracy) {
			statsSavedGroundings++;
			grounding.rule.setAccuracy(accuracy);
			grounding.rule.setNumMatches(numMatches);
			grounding.rule.setScore();
			//AL031510: before adding, check if there exists another rule which subsumes this
			//subsumption=same # matches but less unbounded vars
			ArrayList<LSDLiteral> mylits = grounding.rule.getLiterals();
			int myfree = grounding.rule.getFreeVariables().size();
			LSDRule duplicaterule = null;
			next: for (LSDRule otherrule : originalRules) {
				//check if number of matches are the same
				if (otherrule.getNumMatches()!=numMatches)
					continue;
				//check if subsumes each other
				ArrayList<LSDLiteral> otherlits = otherrule.getLiterals();
				if (mylits.size()!=otherlits.size()) 
					continue;	//impossible given that they have same predicate
				for (int i=0; i<mylits.size(); ++i) {
					LSDLiteral mylit = mylits.get(i);
					LSDLiteral otherlit = otherlits.get(i);
					Iterator<LSDBinding> mybinds = mylit.getBindings().iterator();
					Iterator<LSDBinding> otherbinds = otherlit.getBindings().iterator();
					while (mybinds.hasNext()) {
						LSDBinding mybind = mybinds.next();
						LSDBinding otherbind = otherbinds.next();
						if (mybind.isBound() && otherbind.isBound() && 
								!mybind.getGroundConst().equals(otherbind.getGroundConst())) {
							continue next;	//bindings do not match
						}
					}
				}
				//everything matches
				duplicaterule = otherrule;
				break;
			}
			if (duplicaterule!=null) {
				//pick only the rule with less free variables to admit
				int otherfree = duplicaterule.getFreeVariables().size();
				if (otherfree<=myfree) {
					//skip this rule completely
				} else {
					//replace old rule with this one 
					originalRules.remove(duplicaterule);
					originalRules.add(grounding.rule);
				}
			} else {
				originalRules.add(grounding.rule);
			}
			 
		}
		return originalRules;
	}

	// MK 020611: try to understand this method. given a partial ungrounded rule and a new predicate to add. 
	List<List<LSDBinding>> enumerateUngroundedBindings(LSDRule partialRule,
			LSDPredicate predicateToAdd) {
		List<List<LSDBinding>> bindingsList = new ArrayList<List<LSDBinding>>();
		bindingsList.add(new ArrayList<LSDBinding>());

		// This is where I need to identify a key free variable of the
		// conclusion predicate, and make sure that the key free variable appears
		// as a primary variable of each antecedent literal

		Set<LSDVariable> existingFreeVars = new HashSet<LSDVariable>(partialRule
				.getFreeVariables());
		// For each variable spot:
		for (int i = 0; i < predicateToAdd.getTypes().length; i++) {
			char type = predicateToAdd.getTypes()[i];
			List<List<LSDBinding>> newBindingsList = new ArrayList<List<LSDBinding>>();
			// For each partial binding pb
			for (List<LSDBinding> prevBindings : bindingsList) {
				// (Computing freevars-of-that-type union [new-var] for
				// below)
				Set<LSDVariable> freeVariables = new HashSet<LSDVariable>(
						existingFreeVars);
				for (LSDBinding b : prevBindings)
					freeVariables.add(b.getVariable());
				List<LSDVariable> variableChoices = new ArrayList<LSDVariable>();
				for (LSDVariable v : freeVariables) {
					if (v.getType() == type)
						variableChoices.add(v);
				}
				if (i == predicateToAdd.getKeyArgument()) {
					variableChoices.add(LSDVariable.newFreeVariable(
							freeVariables, type));

				} else {
					variableChoices.add(LSDVariable.newConstantHolder(
							freeVariables, type));
				}
				// For each variable in freevars-of-that-type + new-var
				for (LSDVariable nextVariable : variableChoices) {
					// add pb + chosen-var to the 2nd queue of bindings
					ArrayList<LSDBinding> newBindings = new ArrayList<LSDBinding>(
							prevBindings);
					newBindings.add(new LSDBinding(nextVariable));
					newBindingsList.add(newBindings);
				}
			}
			// Swap the lists of partial bindings.
			bindingsList = newBindingsList;
		}

		// remove all bindings list where the variable is not linked. 
		for (Iterator<List<LSDBinding>> i = bindingsList.iterator(); i
				.hasNext();) {
			List<LSDBinding> bindings = i.next();
			boolean linked = false;
			for (LSDBinding b : bindings) {
				if (existingFreeVars.contains(b.getVariable())) {
					linked = true;
					break;
				}
			}
			if (!linked)
				i.remove();
			// MK 020611. I was first thinking about removing all bindings list
			// where key varaibles do not match, but I realized that it is
			// easier to do when removing from a set of ungrounded rules list.

		}
		return bindingsList;
	}

	int countRemainingMatches(LSDRule rule) {
		// (Check if the rule has matches)
		return remainingRuleChecker.countTrueConclusions(rule);
	}

	int countRemainingMatches(LSDRule rule, int i) {
		// (Check if the rule has matches)
		return remainingRuleChecker.countTrueConclusions(rule, i);
	}

	public int countMatches(LSDRule rule) {
		// (Check if the rule has matches)
		return ruleChecker.countTrueConclusions(rule);
	}

	public int countExceptions(LSDRule rule) {
		// (Check if the rule has exceptions)
		return ruleChecker.countCounterExamples(rule);
	}

	int countExceptions(LSDRule rule, int max) {
		// (Check if the rule has exceptions)
		return ruleChecker.countCounterExamples(rule, max);
	}

	double measureAccuracy(LSDRule rule, double min, int maxExceptions,
			double matches) {
		int accuracyMaxExceptions = ((int) Math
				.floor((matches / min) - matches)) + 1;
		double exceptions = countExceptions(rule, Math.min(maxExceptions,
				accuracyMaxExceptions));
		if (exceptions >= maxExceptions)
			return 0;
		return matches / (matches + exceptions);
	}

	public void shutdown() {
		ruleChecker.shutdown();
		// FIXME XXX The following code shouldn't cause a null pointer
		// exception, should it? XXX
		// remainingRuleChecker.shutdown();
	}

	protected class Grounding {
		public int numMatches;

		public boolean scanned = false;

		private Set<LSDVariable> remainingVariables;

		public Set<String> usedConstants = new HashSet<String>();

		public LSDRule rule;

		public Grounding(LSDRule rule) {
			remainingVariables = new LinkedHashSet<LSDVariable>(rule
					.getFreeVariables());
			this.rule = rule;

		}
		// MK 020511 : Is there a way to check a variable is in a particular position within a rule? 
		private Set<LSDVariable> getVariablesToSubtitute() {
			LinkedHashSet<LSDVariable> varsToSubstitue = new LinkedHashSet<LSDVariable>();
			for (LSDVariable v: remainingVariables) {
				if (v.isConstantHolder()) {
				varsToSubstitue.add(v);
				}
			}
			return varsToSubstitue;
		}
		public boolean isGrounded() {
			ArrayList<LSDLiteral> literalsList = rule.getLiterals();
			for (LSDLiteral literal : literalsList) {
				List<LSDBinding> bindingsList = literal.getBindings();
				for (LSDBinding binding : bindingsList) {
					if (binding.getGroundConst() != null)
						return true;
				}
			}
			return false;
		}

		public Grounding(Grounding oldGrounding) {
			this.remainingVariables = new HashSet<LSDVariable>(
					oldGrounding.remainingVariables);
			this.usedConstants = new HashSet<String>(oldGrounding.usedConstants);
			this.rule = oldGrounding.rule;
		}

		public Grounding addGrounding(LSDVariable variable, String constant) {
			Grounding newGrounding = new Grounding(this);

			assert remainingVariables.contains(variable) : ("Error: "
					+ remainingVariables + " doesn't contain " + variable);
			newGrounding.remainingVariables.remove(variable);
			if (constant != null) {
				assert !usedConstants.contains(constant);
				newGrounding.remainingVariables.remove(variable);
				newGrounding.usedConstants.add(constant);
				try {
					newGrounding.rule = rule.substitute(variable,
							new LSDBinding(constant));
				} catch (LSDInvalidTypeException e) {
					System.err
							.println("We're dealing with consts, so why type mismatch?");
					System.exit(-15);
				}
			}
			return newGrounding;
		}
	}


	// counts number of contextual references
	private int counttextual(Collection<LSDRule> selectedSubset) {

		int count = 0;
		for (LSDRule rule : selectedSubset) {
			go: for (LSDLiteral literal : rule.getLiterals()) {
				for (LSDBinding bind : literal.getBindings()) {
					int i = 0;
					if (bind.isBound())
						for (LSDFact delta : readDeltaFacts) {
							if (delta.toString()
									.contains(bind.getGroundConst()))
								break;
							else
								i++;
						}
					if (i == readDeltaFacts.size()) {
						count++;
						break go;
					}
				}

			}
		}
		return count;
	}

	// Sorts rules based on score
	private ArrayList<LSDRule> sortRules(List<LSDRule> rules) {
		LSDRule[] temp = new LSDRule[rules.size()];
		for (int i = 0; i < temp.length; i++) {
			temp[i] = rules.get(i);
		}
		Arrays.sort(temp, new LSDRule().new LSDRuleComparator());
		ArrayList<LSDRule> sortedList = new ArrayList<LSDRule>();
		for (LSDRule rule : temp) {
			sortedList.add(rule);
		}
		return sortedList;

	}

	// MK: copied from optimized diff
	
	// This code is called by bruteforce learning
	public List<LSDRule> enumerateRules(int maxLiterals) {
		List<LSDRule> rules = new ArrayList<LSDRule>();
		List<LSDRule> partialRules = new ArrayList<LSDRule>(
				enumerateConclusions());
		statsGeneratedPartials += partialRules.size();
		statsEnqueuedPartials += partialRules.size();
		for (int currentLength = 1; currentLength <= maxLiterals; currentLength++) {

			System.out.println("Finding rules of length " + currentLength);
			List<LSDRule> newPartialRules = new ArrayList<LSDRule>();
			nextEnumerationTiming();
			List<LSDRule> ungroundedRules = extendUngroundedRules(partialRules,
					newPartialRules);
//			System.out.println("MK 020811: before removing duplicate rules:" +ungroundedRules.size());
//			
//			TreeSet<LSDRule> afterRemovingDuplicates = new TreeSet<LSDRule>(); 
//			afterRemovingDuplicates.addAll(ungroundedRules);
//			ungroundedRules = new ArrayList<LSDRule> (); 
//			ungroundedRules.addAll(afterRemovingDuplicates);
//			System.out.println("MK 020811: after removing duplicate rules:" +ungroundedRules.size());
//			
////			Object[] uRules = ungroundedRules.toArray();
//			
//			ArrayList<LSDRule> toBeRemoved = new ArrayList<LSDRule> (); 
//			for (int i=0; i< uRules.length; i++) { 
//				LSDRule ri = (LSDRule) uRules[i];
//				for (int j=i+1; j<uRules.length; j++) { 
//					LSDRule rj =(LSDRule) uRules[j]; 
//					if (ri.sameIgnoreConstantHolders(rj)) { 
//						System.out.println("MK 020811: getting rid of duplicates" +ri.toString()+ "\t"+ rj.toString());
//						toBeRemoved.add(rj); 
//					}
//				}
//			}
//			ungroundedRules.removeAll(toBeRemoved); 
//			
			// remove duplicate rules by comparing hashCode based on 
			double iterationTimeUngroundRuleGeneration = nextEnumerationTiming();
			timeUngroundRuleGeneration += iterationTimeUngroundRuleGeneration;
			
			System.out.println("Ungrounded rules, length " + currentLength
					+ ": " + iterationTimeUngroundRuleGeneration + " s");
			System.out.println("Total ungrounded rules generated: "+ungroundedRules.size());
			partialRules = newPartialRules;
			rules.addAll(groundRules(ungroundedRules));
			
			double iterationTimePartiallyGroundRuleGeneration = nextEnumerationTiming();
			timePartiallyGroundRuleGeneration += iterationTimePartiallyGroundRuleGeneration;
			System.out.println("Rule grounding, length " + currentLength + ": "
					+ iterationTimePartiallyGroundRuleGeneration + " s");
			System.out.println("Total grounded rules generated: "
					+ rules.size() + " rules");
			if (currentLength == maxLiterals)
				break;
			try {
				remainingRuleChecker.shutdown();
				remainingRuleChecker = createReducedRuleChecker(rules);
			} catch (Exception e) {
				System.out.println(e);
				e.printStackTrace();
			}
			System.out.println("Creating new rule checker: "
					+ nextEnumerationTiming() + " s");
			System.out.println("Enqueued partial rules: " + partialRules.size()
					+ " rules");

			partialRules = narrowSearch(newPartialRules, currentLength);
			System.out.println("Reduced enqueued partial rules: "
					+ partialRules.size() + " rules");
			System.out.println("Reducing partial rule set: "
					+ nextEnumerationTiming() + " s");
			if (partialRules.size() == 0)
				break;
		}
			
		// MK 020611: This part is left out because it is too restrictive 
		// go over rules that have universal quantifiers in an undesirable positions and remove them. 
//		List<LSDRule> tobeRemoved = new ArrayList<LSDRule>();
//		for (LSDRule rule: rules) { 
//			if (rule.toBeRemovedDueToUndesirableUniversalQuantifiers()) { 
//				tobeRemoved.add(rule);
//			}
//		}
//		rules.removeAll(tobeRemoved);
	
		return rules;
	}
	// MK: copied from optimized diff
	private List<LSDRule> enumerateConclusions() {
		List<LSDRule> conclusions = new ArrayList<LSDRule>();
		System.out.println("[enumerateConclusion: getUniquePredicates]:\t" +getUniquePredicates(workingSetDeltaKB, false));
		
		 ArrayList<LSDPredicate> conclusionPreds= getUniquePredicates(workingSetDeltaKB, false); 
		 
		for (LSDPredicate predicate : conclusionPreds ) {
	
			List<LSDBinding> bindings = predicate.getFreshBindingsForConclusion();
			// Add it to the queue of partial rules.
			LSDRule rule = new LSDRule();
			try {
				rule.addLiteral(new LSDLiteral(predicate, bindings,
						isConclusion));
			} catch (LSDInvalidTypeException e) {
				System.err
						.println("We're taking types directly from the predicates, so we should never have this type error.");
				System.exit(-7);
			}
			startTimer();
			int numMatches = countRemainingMatches(rule, minMatches);
			statsPartialValidQueryCount++;
			if (numMatches >= minMatches) {
				System.out.println("MK 020611: conclusion rule" +rule);
				conclusions.add(rule);
			}
		}
		return conclusions;
	}
		
	public String getConcreteRule(LSDRule r ) { 
		String concreteRuleDesc = r.toString();
		List<LSDVariable> vars = r.getFreeVariables(); 
		
		HashMap<LSDVariable, String> constVars = new HashMap<LSDVariable,String> (); 
		for (LSDVariable v : vars) { 
			if (v.isConstantHolder()) { 
				constVars.put(v, null);
			}
		}
		if (constVars.size()==0) { 
			return concreteRuleDesc;
		}
		for (LSDVariable constH : constVars.keySet()) { 
			Set<String> consts =ruleChecker.getReplacementConstants(r, constH); 
			if (consts.size()==1) { 
				String CONST = consts.iterator().next().toString(); 
				System.err.println("MK 020811: before rule:"+ concreteRuleDesc);
				concreteRuleDesc= concreteRuleDesc.replace(constH.getName(), CONST);
				System.err.println("MK 020811: after rule:"+ concreteRuleDesc);
						
			}
		}
		return concreteRuleDesc;
	}
}