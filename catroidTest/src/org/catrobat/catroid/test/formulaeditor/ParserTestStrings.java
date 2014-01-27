/**
 *  Catroid: An on-device visual programming system for Android devices
 *  Copyright (C) 2010-2013 The Catrobat Team
 *  (<http://developer.catrobat.org/credits>)
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *  
 *  An additional term exception under section 7 of the GNU Affero
 *  General Public License, version 3, is available at
 *  http://developer.catrobat.org/license_additional_term
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU Affero General Public License for more details.
 *  
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.test.formulaeditor;

import android.test.AndroidTestCase;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.formulaeditor.FormulaElement;
import org.catrobat.catroid.formulaeditor.Functions;
import org.catrobat.catroid.formulaeditor.InternFormulaParser;
import org.catrobat.catroid.formulaeditor.InternToken;
import org.catrobat.catroid.formulaeditor.InternTokenType;
import org.catrobat.catroid.formulaeditor.Operators;
import org.catrobat.catroid.formulaeditor.UserVariablesContainer;
import org.catrobat.catroid.uitest.util.UiTestUtils;

import java.util.LinkedList;
import java.util.List;

public class ParserTestStrings extends AndroidTestCase {

	private Sprite testSprite;
	private Project project;
	private static final double USER_VARIABLE_1_VALUE_TYPE_DOUBLE = 888.88;
	private static final String USER_VARIABLE_2_VALUE_TYPE_STRING = "another String";
	private static final String PROJECT_USER_VARIABLE_NAME = "projectUserVariable";
	private static final String PROJECT_USER_VARIABLE_NAME2 = "projectUserVariable2";

	@Override
	protected void setUp() {
		testSprite = new Sprite("testsprite");
		project = new Project(null, UiTestUtils.PROJECTNAME1);
		project.addSprite(testSprite);
		ProjectManager.getInstance().setProject(project);
		ProjectManager.getInstance().setCurrentSprite(testSprite);
		UserVariablesContainer userVariableContainer = ProjectManager.getInstance().getCurrentProject()
				.getUserVariables();
		userVariableContainer.addProjectUserVariable(PROJECT_USER_VARIABLE_NAME).setValue(
				USER_VARIABLE_1_VALUE_TYPE_DOUBLE);
		userVariableContainer.addProjectUserVariable(PROJECT_USER_VARIABLE_NAME2).setValue(
				USER_VARIABLE_2_VALUE_TYPE_STRING);
	}

	public void testStringInterpretation() {
		String testString = "testString";
		List<InternToken> internTokenList = new LinkedList<InternToken>();
		internTokenList.add(new InternToken(InternTokenType.STRING, testString));
		FormulaElement parseTree = new InternFormulaParser(internTokenList).parseFormula();
		assertNotNull("Formula is not parsed correctly:" + testString, parseTree);
		assertEquals("Formula interpretation is not as expected:" + testString, testString,
				parseTree.interpretRecursive(testSprite));
	}

	public void testLength() {
		String firstParameter = "testString";
		FormulaEditorUtil.testSingleParameterFunction(Functions.LENGTH, InternTokenType.STRING, firstParameter,
				(double) firstParameter.length(), testSprite);

		String number = "1.1";
		FormulaEditorUtil.testSingleParameterFunction(Functions.LENGTH, InternTokenType.NUMBER, number,
				(double) number.length(), testSprite);

		FormulaEditorUtil.testSingleParameterFunction(Functions.LENGTH, InternTokenType.USER_VARIABLE,
				PROJECT_USER_VARIABLE_NAME, (double) Double.toString(USER_VARIABLE_1_VALUE_TYPE_DOUBLE).length(),
				testSprite);

		FormulaEditorUtil.testSingleParameterFunction(Functions.LENGTH, InternTokenType.USER_VARIABLE,
				PROJECT_USER_VARIABLE_NAME2, (double) USER_VARIABLE_2_VALUE_TYPE_STRING.length(), testSprite);
	}

	public void testLetter() {
		String letterString = "letterString";
		String index = "7";
		FormulaEditorUtil.testDoubleParameterFunction(Functions.LETTER, InternTokenType.NUMBER, index,
				InternTokenType.STRING, letterString, String.valueOf(letterString.charAt(Integer.valueOf(index) - 1)),
				testSprite);

		index = "0";
		String emptyString = "";
		FormulaEditorUtil.testDoubleParameterFunction(Functions.LETTER, InternTokenType.NUMBER, index,
				InternTokenType.STRING, letterString, emptyString, testSprite);

		index = "-5";
		emptyString = "";
		List<InternToken> internTokenList = new LinkedList<InternToken>();
		internTokenList.add(new InternToken(InternTokenType.FUNCTION_NAME, Functions.LETTER.name()));
		internTokenList.add(new InternToken(InternTokenType.FUNCTION_PARAMETERS_BRACKET_OPEN));
		internTokenList.add(new InternToken(InternTokenType.OPERATOR, Operators.MINUS.name()));
		internTokenList.add(new InternToken(InternTokenType.NUMBER, "5"));
		internTokenList.add(new InternToken(InternTokenType.FUNCTION_PARAMETER_DELIMITER));
		internTokenList.add(new InternToken(InternTokenType.STRING, letterString));
		internTokenList.add(new InternToken(InternTokenType.FUNCTION_PARAMETERS_BRACKET_CLOSE));
		FormulaElement parseTree = new InternFormulaParser(internTokenList).parseFormula();
		assertNotNull("Formula is not parsed correctly: " + Functions.LETTER.name() + "(" + index + "," + letterString
				+ ")", parseTree);
		assertEquals("Formula interpretation is not as expected: " + Functions.LETTER.name() + "(" + index + ","
				+ letterString + ")", emptyString, parseTree.interpretRecursive(testSprite));

		index = "0";
		emptyString = "";
		letterString = emptyString;
		FormulaEditorUtil.testDoubleParameterFunction(Functions.LETTER, InternTokenType.NUMBER, String.valueOf(index),
				InternTokenType.STRING, letterString, emptyString, testSprite);

		internTokenList = FormulaEditorUtil.buildDoubleParameterFunction(Functions.LETTER, InternTokenType.NUMBER,
				index, InternTokenType.STRING, letterString);
		parseTree = new InternFormulaParser(internTokenList).parseFormula();
		assertNotNull("Formula is not parsed correctly: " + Functions.LETTER.name() + "(" + index + "," + letterString
				+ ")", parseTree);
		assertEquals("Formula interpretation is not as expected: " + Functions.LETTER.name() + "(" + index + ","
				+ letterString + ")", emptyString, parseTree.interpretRecursive(testSprite));

		letterString = "letterString";
		index = "2";
		FormulaEditorUtil.testDoubleParameterFunction(Functions.LETTER, InternTokenType.STRING,
				String.valueOf(letterString.charAt(Integer.valueOf(index) - 1)), InternTokenType.STRING, letterString,
				emptyString, testSprite);

		String firstParameter = "hello";
		String secondParameter = " world";
		internTokenList = FormulaEditorUtil.buildDoubleParameterFunction(Functions.JOIN, InternTokenType.STRING,
				firstParameter, InternTokenType.STRING, secondParameter);
		internTokenList = FormulaEditorUtil.buildSingleParameterFunction(Functions.LENGTH, internTokenList);
		internTokenList = FormulaEditorUtil.buildDoubleParameterFunction(Functions.LETTER, internTokenList,
				InternTokenType.STRING, firstParameter + secondParameter);
		parseTree = new InternFormulaParser(internTokenList).parseFormula();
		assertNotNull("Formula is not parsed correctly: " + Functions.LETTER.name() + "(" + Functions.LENGTH.name()
				+ "(" + Functions.JOIN.name() + "(" + firstParameter + "," + secondParameter + ")" + ")" + ","
				+ firstParameter + secondParameter + ")", parseTree);
		assertEquals(
				"Formula interpretation is not as expected: " + Functions.LETTER.name() + "(" + Functions.LENGTH.name()
						+ "(" + Functions.JOIN.name() + "(" + firstParameter + "," + secondParameter + ")" + ")" + ","
						+ firstParameter + secondParameter + ")", String.valueOf((firstParameter + secondParameter)
						.charAt((firstParameter + secondParameter).length() - 1)),
				parseTree.interpretRecursive(testSprite));

		index = "4";
		FormulaEditorUtil.testDoubleParameterFunction(Functions.LETTER, InternTokenType.NUMBER, index,
				InternTokenType.USER_VARIABLE, PROJECT_USER_VARIABLE_NAME,
				String.valueOf(Double.toString(USER_VARIABLE_1_VALUE_TYPE_DOUBLE).charAt(Integer.valueOf(index) - 1)),
				testSprite);

		index = "3";
		FormulaEditorUtil.testDoubleParameterFunction(Functions.LETTER, InternTokenType.NUMBER, index,
				InternTokenType.USER_VARIABLE, PROJECT_USER_VARIABLE_NAME2,
				String.valueOf(USER_VARIABLE_2_VALUE_TYPE_STRING.charAt(Integer.valueOf(index) - 1)), testSprite);

	}

	public void testJoin() {
		String firstParameter = "first";
		String secondParameter = "second";
		FormulaEditorUtil.testDoubleParameterFunction(Functions.JOIN, InternTokenType.STRING, firstParameter,
				InternTokenType.STRING, secondParameter, firstParameter + secondParameter, testSprite);

		firstParameter = "";
		secondParameter = "second";
		FormulaEditorUtil.testDoubleParameterFunction(Functions.JOIN, InternTokenType.STRING, firstParameter,
				InternTokenType.STRING, secondParameter, firstParameter + secondParameter, testSprite);

		firstParameter = "first";
		secondParameter = "";
		FormulaEditorUtil.testDoubleParameterFunction(Functions.JOIN, InternTokenType.STRING, firstParameter,
				InternTokenType.STRING, secondParameter, firstParameter + secondParameter, testSprite);

		firstParameter = "55";
		secondParameter = "66";
		FormulaEditorUtil.testDoubleParameterFunction(Functions.JOIN, InternTokenType.NUMBER, firstParameter,
				InternTokenType.NUMBER, secondParameter, firstParameter + secondParameter, testSprite);
		FormulaEditorUtil.testDoubleParameterFunction(Functions.JOIN, InternTokenType.NUMBER, firstParameter,
				InternTokenType.STRING, secondParameter, firstParameter + secondParameter, testSprite);
		FormulaEditorUtil.testDoubleParameterFunction(Functions.JOIN, InternTokenType.STRING, firstParameter,
				InternTokenType.NUMBER, secondParameter, firstParameter + secondParameter, testSprite);

		firstParameter = "5*3-6+(8*random(1,2))";
		secondParameter = "string'**##!§\"$\'§%%/&%(())??";
		FormulaEditorUtil.testDoubleParameterFunction(Functions.JOIN, InternTokenType.STRING, firstParameter,
				InternTokenType.STRING, secondParameter, firstParameter + secondParameter, testSprite);
		FormulaEditorUtil.testDoubleParameterFunction(Functions.JOIN, InternTokenType.STRING, firstParameter,
				InternTokenType.USER_VARIABLE, PROJECT_USER_VARIABLE_NAME2, firstParameter
						+ USER_VARIABLE_2_VALUE_TYPE_STRING, testSprite);
		FormulaEditorUtil.testDoubleParameterFunction(Functions.JOIN, InternTokenType.USER_VARIABLE,
				PROJECT_USER_VARIABLE_NAME, InternTokenType.USER_VARIABLE, PROJECT_USER_VARIABLE_NAME2,
				USER_VARIABLE_1_VALUE_TYPE_DOUBLE + USER_VARIABLE_2_VALUE_TYPE_STRING, testSprite);
		FormulaEditorUtil.testDoubleParameterFunction(Functions.JOIN, InternTokenType.USER_VARIABLE,
				PROJECT_USER_VARIABLE_NAME, InternTokenType.STRING, secondParameter, USER_VARIABLE_1_VALUE_TYPE_DOUBLE
						+ secondParameter, testSprite);
	}

	public void testEqual() {
		String firstOperand = "equalString";
		String secondOperand = "equalString";
		FormulaEditorUtil.testBinaryOperator(InternTokenType.STRING, firstOperand, Operators.EQUAL, InternTokenType.STRING,
				secondOperand, firstOperand.compareTo(secondOperand) == 0, testSprite);

		firstOperand = "1";
		secondOperand = "1.0";
		FormulaEditorUtil.testBinaryOperator(InternTokenType.NUMBER, firstOperand, Operators.EQUAL, InternTokenType.STRING,
				secondOperand, Double.valueOf(firstOperand).compareTo(Double.valueOf(secondOperand)) == 0, testSprite);
		FormulaEditorUtil.testBinaryOperator(InternTokenType.STRING, firstOperand, Operators.EQUAL, InternTokenType.NUMBER,
				secondOperand, Double.valueOf(firstOperand).compareTo(Double.valueOf(secondOperand)) == 0, testSprite);
		// TODO
		//		FormulaEditorUtil.testOperator(Operators.EQUAL, InternTokenType.STRING, firstOperand, InternTokenType.STRING,
		//				secondOperand, Double.valueOf(firstOperand).compareTo(Double.valueOf(secondOperand)) == 0, testSprite);

		firstOperand = "1.0";
		secondOperand = "1.9";
		FormulaEditorUtil.testBinaryOperator(InternTokenType.STRING, firstOperand, Operators.EQUAL, InternTokenType.NUMBER,
				secondOperand, Double.valueOf(firstOperand).compareTo(Double.valueOf(secondOperand)) == 1, testSprite);

		firstOperand = "!`\"§$%&/()=?";
		secondOperand = "!`\"§$%&/()=????";
		FormulaEditorUtil.testBinaryOperator(InternTokenType.STRING, firstOperand, Operators.EQUAL, InternTokenType.STRING,
				secondOperand, false, testSprite);

		firstOperand = "555.555";
		secondOperand = "055.77.77";
		FormulaEditorUtil.testBinaryOperator(InternTokenType.STRING, firstOperand, Operators.EQUAL, InternTokenType.STRING,
				secondOperand, false, testSprite);
	}

	public void testPlus() {
		String firstOperand = "1.3";
		String secondOperand = "3";
		FormulaEditorUtil.testBinaryOperator(InternTokenType.STRING, firstOperand, Operators.PLUS, InternTokenType.STRING,
				secondOperand, Double.valueOf(firstOperand) + Double.valueOf(secondOperand), testSprite);
		FormulaEditorUtil.testBinaryOperator(InternTokenType.NUMBER, firstOperand, Operators.PLUS, InternTokenType.STRING,
				secondOperand, Double.valueOf(firstOperand) + Double.valueOf(secondOperand), testSprite);
		FormulaEditorUtil.testBinaryOperator(InternTokenType.STRING, firstOperand, Operators.PLUS, InternTokenType.NUMBER,
				secondOperand, Double.valueOf(firstOperand) + Double.valueOf(secondOperand), testSprite);

		firstOperand = "NotANumber";
		secondOperand = "3.14";
		List<InternToken> internTokenList = new LinkedList<InternToken>();
		internTokenList.add(new InternToken(InternTokenType.STRING, firstOperand));
		internTokenList.add(new InternToken(InternTokenType.OPERATOR, Operators.PLUS.name()));
		internTokenList.add(new InternToken(InternTokenType.STRING, secondOperand));
		FormulaElement parseTree = new InternFormulaParser(internTokenList).parseFormula();
		assertNotNull("Formula is not parsed correctly: " + firstOperand + Operators.PLUS + secondOperand, parseTree);
		try {
			parseTree.interpretRecursive(testSprite);
			fail("Formula interpretation is not as expected: " + firstOperand + Operators.PLUS.name() + secondOperand);
		} catch (Exception exception) {
			assertEquals("Wrong exception message", exception.getMessage(), String.valueOf(Double.NaN));
		}
	}

	public void testFunctions() {
		String firstParaneter = "1.3";
		List<InternToken> internTokenList = FormulaEditorUtil.buildSingleParameterFunction(Functions.SQRT,
				InternTokenType.STRING, firstParaneter);
		FormulaElement parseTree = new InternFormulaParser(internTokenList).parseFormula();
		assertNotNull("Formula is not parsed correctly: " + Functions.SQRT + "(" + firstParaneter + ")", parseTree);
		assertEquals("Formula interpretation is not as expected: " + Functions.SQRT + "(" + firstParaneter + ")",
				Math.sqrt(Double.valueOf(firstParaneter)), parseTree.interpretRecursive(testSprite));

		firstParaneter = "NotANumber";
		internTokenList = FormulaEditorUtil.buildSingleParameterFunction(Functions.SQRT, InternTokenType.STRING,
				firstParaneter);
		parseTree = new InternFormulaParser(internTokenList).parseFormula();
		assertNotNull("Formula is not parsed correctly: " + Functions.SQRT + "(" + firstParaneter + ")", parseTree);
		assertEquals("Formula interpretation is not as expected: " + Functions.SQRT + "(" + firstParaneter + ")", 0d,
				parseTree.interpretRecursive(testSprite));
	}

	public void testLogic() {
		String firstOperand = "1.3";
		String secondOperand = "3";
		List<InternToken> internTokenList = new LinkedList<InternToken>();
		internTokenList.add(new InternToken(InternTokenType.STRING, firstOperand));
		internTokenList.add(new InternToken(InternTokenType.OPERATOR, Operators.SMALLER_THAN.name()));
		internTokenList.add(new InternToken(InternTokenType.STRING, secondOperand));
		FormulaElement parseTree = new InternFormulaParser(internTokenList).parseFormula();
		assertNotNull("Formula is not parsed correctly: " + firstOperand + Operators.SMALLER_THAN + secondOperand,
				parseTree);
		assertEquals("Formula interpretation is not as expected: " + firstOperand + Operators.SMALLER_THAN.name()
				+ secondOperand, Double.valueOf(firstOperand) < Double.valueOf(secondOperand),
				((Double) parseTree.interpretRecursive(testSprite)) == 1d);
	}
}
