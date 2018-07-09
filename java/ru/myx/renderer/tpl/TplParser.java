package ru.myx.renderer.tpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.function.Function;

import ru.myx.ae3.act.Context;
import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseList;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.eval.BalanceType;
import ru.myx.ae3.eval.Evaluate;
import ru.myx.ae3.eval.LanguageImpl;
import ru.myx.ae3.exec.Exec;
import ru.myx.ae3.exec.ExecProcess;
import ru.myx.ae3.exec.Instruction;
import ru.myx.ae3.exec.InstructionEditable;
import ru.myx.ae3.exec.Instructions;
import ru.myx.ae3.exec.ModifierArgument;
import ru.myx.ae3.exec.ModifierArgumentA30IMM;
import ru.myx.ae3.exec.ModifierArgumentA32FVIMM;
import ru.myx.ae3.exec.ModifierArguments;
import ru.myx.ae3.exec.OperationsA01;
import ru.myx.ae3.exec.OperationsA10;
import ru.myx.ae3.exec.OperationsA11;
import ru.myx.ae3.exec.OperationsA2X;
import ru.myx.ae3.exec.ProgramAssembly;
import ru.myx.ae3.exec.ProgramPart;
import ru.myx.ae3.exec.ResultHandler;
import ru.myx.ae3.exec.parse.expression.TokenInstruction;
import ru.myx.ae3.report.Report;
import ru.myx.renderer.tpl.parse.Token;
import ru.myx.renderer.tpl.parse.Tokens;

final class TplParser {
	
	private static final void applyElseToParent(final Instruction self, final Instruction elsePart) {
		
		if (self == null) {
			throw new IllegalArgumentException("ELSE without an ELSE-enabled tag encountered!");
		}
		if (!(self instanceof TagIF)) {
			throw new IllegalArgumentException("Attempt to apply ELSE for a non ELSE-enabled tag encountered!");
		}
		final TagIF parent = (TagIF) self;
		if (parent.getInstructionElse() != null) {
			throw new IllegalArgumentException("Attempt to apply ELSE for an ELSE-enabled tag with ELSE part already defined encountered!");
		}
		parent.setElse(elsePart);
	}

	static final Instruction createInstruction(final Function<String, String> folder, final Instruction self, final Stack<TagRECURSION> recursions, final String source)
			throws Evaluate.CompilationException {
		
		final ProgramAssembly assembly = new ProgramAssembly();
		TplParser.parse(assembly, Tokens.parse(source), self, recursions == null
			? new Stack<TagRECURSION>()
			: recursions, folder);
		return assembly.toProgram(0);
	}

	static final ProgramPart createProgramPart(final Function<String, String> folder, final Instruction self, final Stack<TagRECURSION> recursions, final Token[] source)
			throws Evaluate.CompilationException {
		
		final ProgramAssembly assembly = new ProgramAssembly();
		assembly.addDebug("TPL BLOCK, tokens=" + source);
		TplParser.parse(assembly, source, self, recursions == null
			? new Stack<TagRECURSION>()
			: recursions, folder);
		return assembly.toProgram(0);
	}

	private static final TagRECURSION getNearestRecursion(final Stack<TagRECURSION> recursions) {
		
		return recursions.isEmpty()
			? null
			: recursions.peek();
	}

	static final void parse(final ProgramAssembly assembly,
			final Token[] tokens,
			final Instruction self,
			final Stack<TagRECURSION> recursions,
			final Function<String, String> folder) throws Evaluate.CompilationException {
		
		ExecProcess ctx = null;
		for (int currentTokenIndex = 0; currentTokenIndex < tokens.length; currentTokenIndex++) {
			final Token currentToken = tokens[currentTokenIndex];
			final String source = currentToken.getSource();
			if (currentToken.isOutput()) {
				assembly.addDebug(currentToken + " // OUT");
				assembly.addInstruction(OperationsA10.XFLOAD_P.instruction(Base.forString(source), null, 0, ResultHandler.FB_BNO_NXT));
				continue;
			}
			if (source.length() == 0) {
				continue;
			}
			final char firstChar = source.charAt(0);
			if (firstChar == '/' && source.startsWith("//")) {
				continue;
			}
			assembly.addDebug(currentToken + " // " + source);
			if (firstChar == '=') {
				Evaluate.compileStatement(assembly, -1, source);
				continue;
				/** not working! really! <%= { a : 5 }.zz || 'b' %> works, but <%= { a : 5 }.zz ||
				 * 'b'; %> doesn't! <code>
				final String expression = source.substring( 1 ).trim();
				Evaluate.compileExpression( assembly, expression, ModifierStore.NO, ExecStateCode.NEXT );
				continue;
				</code> */
			}
			final String tagName;
			final String tagParam;
			{
				final int pos = source.indexOf(':');
				if (pos == -1) {
					tagName = source;
					tagParam = "";
				} else {
					tagName = source.substring(0, pos).trim();
					tagParam = source.substring(pos + 1).trim();
				}
			}
			if (firstChar == 'E' && tagName.equals("EXEC")) {
				Evaluate.compileStatement(assembly, -1, tagParam);
				continue;
			}
			if (firstChar == 'H' && tagName.equals("HINT")) {
				// ignore for now, sorry
				continue;
			}
			if (firstChar == 'I' && tagName.equals("IGNORE")) {
				currentTokenIndex = Tokens.findClosing(tokens, currentTokenIndex + 1, "IGNORE", "/IGNORE");
				if (currentTokenIndex == -1) {
					final String error = "TemplateProcessor: Can't find closing tag for 'IGNORE'";
					Report.error("ACM/TPL", error);
					throw new IllegalArgumentException(error);
				}
				continue;
			}
			if (firstChar == 'R' && tagName.equals("RETURN")) {
				Evaluate.compileExpression(assembly, tagParam, ResultHandler.FC_PNN_RET);
				continue;
			}
			if (firstChar == 'T' && tagName.equals("THROW")) {
				Evaluate.compileExpression(assembly, tagParam, ResultHandler.FB_BNN_ERR);
				continue;
			}
			if (firstChar == 'C' && tagName.equals("CONTINUE")) {
				if (tagParam.length() == 0) {
					assembly.addInstruction(Instructions.INSTR_NOP_0_NN_CONTINUE);
					continue;
				}
				if (tagParam.startsWith("IF:")) {
					Evaluate.compileExpression(assembly, tagParam.substring(3).trim(), ResultHandler.FA_BNN_NXT);
					assembly.addInstruction(Instructions.INSTR_ESKIPRB0_0_NN_CONTINUE);
					continue;
				}
				if (tagParam.startsWith("UNLESS:")) {
					Evaluate.compileExpression(assembly, tagParam.substring(7).trim(), ResultHandler.FA_BNN_NXT);
					assembly.addInstruction(Instructions.INSTR_ESKIP1_0_NN_CONTINUE);
					continue;
				}
				{
					final String error = "TemplateProcessor: Invalid CONTINUE parameter: " + tagParam;
					Report.error("ACM/TPL", error);
					throw new IllegalArgumentException(error);
				}
			}
			if (firstChar == 'B' && tagName.equals("BREAK")) {
				if (tagParam.length() == 0) {
					assembly.addInstruction(Instructions.INSTR_NOP_0_NN_BREAK);
					continue;
				}
				if (tagParam.startsWith("IF:")) {
					Evaluate.compileExpression(assembly, tagParam.substring(3).trim(), ResultHandler.FA_BNN_NXT);
					assembly.addInstruction(Instructions.INSTR_ESKIPRB0_0_NN_BREAK);
					continue;
				}
				if (tagParam.startsWith("UNLESS:")) {
					Evaluate.compileExpression(assembly, tagParam.substring(7).trim(), ResultHandler.FA_BNN_NXT);
					assembly.addInstruction(Instructions.INSTR_ESKIPRB1_0_NN_BREAK);
					continue;
				}
				{
					final String error = "TemplateProcessor: Invalid CONTINUE parameter: " + tagParam;
					Report.error("ACM/TPL", error);
					throw new IllegalArgumentException(error);
				}
			}
			if (firstChar == 'S' && tagName.equals("SQLEXEC")) {
				Evaluate.compileStatement(assembly, -1, "TPL.doSqlExec(" + tagParam + ')');
				continue;
			}
			if (firstChar == 'S' && tagName.equals("SQLBATCH")) {
				assembly.addInstruction(new TagSQLBATCH(tagParam));
				continue;
			}
			if (firstChar == 'L' && tagName.equals("LOG")) {
				Evaluate.compileStatement(assembly, -1, "TPL.doLog(" + tagParam + ')');
				continue;
			}
			if (firstChar == 'A' && tagName.equals("AUDIT")) {
				Evaluate.compileStatement(assembly, -1, "TPL.doAudit(" + tagParam + ')');
				continue;
			}
			if (firstChar == 'P' && tagName.equals("PROCESS")) {
				assembly.addInstruction(new TagPROCESS(folder, tagParam));
				continue;
			}
			if (firstChar == 'R' && tagName.equals("REDIRECT")) {
				Evaluate.compileStatement(assembly, -1, "TPL.doRedirect(" + tagParam + ')');
				continue;
			}
			if (firstChar == 'D' && tagName.equals("DEEPER")) {
				final TagRECURSION recursion = TplParser.getNearestRecursion(recursions);
				if (recursion == null) {
					throw new IllegalArgumentException("No recusrion found for DEEPER tag!");
				}

				Evaluate.compileExpression(assembly, "Create.mapFor(" + tagParam + ')', ResultHandler.FA_BNN_NXT);
				assembly.addInstruction(new TagDEEPER(recursion));

				continue;
			}
			if (firstChar == 'I' && tagName.equals("INCLUDE")) {
				if (tagParam.length() == 0) {
					throw new IllegalArgumentException("Tag 'INCLUDE' - no prameter specified!");
				}
				if (folder == null) {
					throw new IllegalArgumentException("Tag 'INCLUDE' - can be used in file-based templates ONLY!, including: " + tagParam);
				}
				final String name = tagParam.charAt(0) == '\''
					? tagParam.endsWith("'")
						? tagParam.substring(1, tagParam.length() - 1)
						: tagParam.substring(1)
					: tagParam.endsWith("'")
						? tagParam.substring(0, tagParam.length() - 1)
						: tagParam;
				final String include = folder.apply(name);
				final Instruction instruction;
				if (include == null) {
					final String text = " __ERROR: Invalid INCLUDE tag encoutered: template '" + name + "' is not known__ ";
					instruction = OperationsA10.XFLOAD_P.instruction(new ModifierArgumentA30IMM(text), 0, ResultHandler.FB_BNO_NXT);
				} else {
					instruction = TplParser.createInstruction(folder, null, null, include);
				}
				assembly.addInstruction(instruction);
				continue;
			}
			final int endPosition = Tokens.findClosing(tokens, currentTokenIndex + 1, tagName.equals("ELSE") || tagName.equals("TRY")
				? tagName
				: tagName + ':', '/' + tagName);
			if (endPosition == -1) {
				final String message = "TPL: Can't find closing tag for '" + tagName + "', opened by: " + currentToken.toString();
				Report.error("ACM/TPL", message);
				throw new IllegalArgumentException(message);
			}
			final Token[] innerTokens = TplParser.toSubTokens(tokens, currentTokenIndex + 1, endPosition);
			currentTokenIndex = endPosition;
			// //////////////////////////////////////////////////////////////////
			if (firstChar == 'I' && tagName.equals("IF")) {
				final int ifOffset = assembly.size();
				final TagIF tagIF = new TagIF();
				final ProgramPart instructionThen;
				{
					TplParser.parse(assembly, innerTokens, tagIF, recursions, folder);
					if (assembly.size() == ifOffset) {
						instructionThen = null;
					} else {
						instructionThen = assembly.toProgram(ifOffset);
					}
				}
				final ProgramPart instructionElse;
				{
					final Instruction instruction = tagIF.getInstructionElse();
					if (instruction == null) {
						instructionElse = null;
					} else {
						assembly.addInstruction(instruction);
						if (assembly.size() == ifOffset) {
							instructionElse = null;
						} else {
							instructionElse = assembly.toProgram(ifOffset);
						}
					}
				}
				if (instructionElse == null) {
					if (instructionThen == null) {
						// should execute in any case since can code
						Evaluate.compileStatement(assembly, -1, tagParam);
						assembly.addDebug(tokens[currentTokenIndex] + " // /IF    // opened by: " + source);
						continue;
					}
					/** ESKIP1 / ESKIP0 - doesn't require 'boolean' */
					Evaluate.compileExpression(assembly, tagParam, ResultHandler.FA_BNN_NXT);
					assembly.addInstruction(OperationsA01.XESKIPRB0_P.instruction(instructionThen.length() + 1, ResultHandler.FA_BNN_NXT));
					assembly.addInstruction(instructionThen);
					assembly.addDebug(tokens[currentTokenIndex] + " // /IF    // opened by: " + source);
					continue;
				}
				/** ESKIP1 / ESKIP0 - doesn't require 'boolean' */
				Evaluate.compileExpression(assembly, tagParam, ResultHandler.FA_BNN_NXT);
				if (instructionThen == null) {
					assembly.addInstruction(OperationsA01.XESKIPRB1_P.instruction(instructionElse.length() + 1, ResultHandler.FA_BNN_NXT));
					assembly.addInstruction(instructionElse);
					assembly.addDebug(tokens[currentTokenIndex] + " // /IF    // opened by: " + source);
					continue;
				}
				assembly.addInstruction(OperationsA01.XESKIPRB1_P.instruction(instructionElse.length() + 1, ResultHandler.FA_BNN_NXT));
				assembly.addInstruction(instructionElse);
				assembly.addInstruction(OperationsA01.XESKIP_P.instruction(instructionThen.length() + 1, ResultHandler.FA_BNN_NXT));
				assembly.addInstruction(instructionThen);
				assembly.addDebug(tokens[currentTokenIndex] + " // /IF    // opened by: " + source);
				continue;
			}
			if (firstChar == 'I' && tagName.equals("ITERATE")) {
				final int position = tagParam.indexOf(':');
				if (position == -1) {
					throw new IllegalArgumentException("ITERATE syntax is <iterator : iterable>, your syntax: " + tagParam + "!");
				}
				final String iterator = tagParam.substring(0, position).trim();
				assembly.addInstruction(OperationsA10.XFLOAD_P.instruction(new ModifierArgumentA32FVIMM(iterator), 0, ResultHandler.FB_BSN_NXT));
				final InstructionEditable frameEntry = OperationsA01.XEENTRITER_I.instructionCreate(0, ResultHandler.FA_BNN_NXT);
				assembly.addInstruction(frameEntry);
				final int frameStart = assembly.size();
				final InstructionEditable breakTarget = OperationsA01.XFBTGT_P.instructionCreate(0, ResultHandler.FA_BNN_NXT);
				assembly.addInstruction(breakTarget);
				Evaluate.compileExpression(assembly, tagParam.substring(position + 1).trim(), ResultHandler.FA_BNN_NXT);
				assembly.addInstruction(Instructions.INSTR_ITRPREP7_1_R_NN_NEXT);
				assembly.addInstruction(OperationsA01.XFCTGT_P.instruction(0, ResultHandler.FA_BNN_NXT));
				final int conditionStart = assembly.size();
				assembly.addInstruction(OperationsA10.XITRNEXT.instruction(Base.forString(iterator), null, 0, ResultHandler.FA_BNN_NXT));
				final InstructionEditable skipFalse = OperationsA01.XESKIPRB0_P.instructionCreate(0, ResultHandler.FA_BNN_NXT);
				assembly.addInstruction(skipFalse);
				final int skipPosition = assembly.size();

				TplParser.parse(assembly, innerTokens, self, recursions, folder);
				assembly.addInstruction(assembly.toProgram(skipPosition));

				assembly.addInstruction(OperationsA01.XESKIP_P.instruction(-assembly.getInstructionCount(conditionStart) - 1, ResultHandler.FA_BNN_NXT));
				skipFalse.setConstant(assembly.getInstructionCount(skipPosition)).setFinished();
				final int count = assembly.getInstructionCount(frameStart);
				frameEntry.setConstant(count).setFinished();
				breakTarget.setConstant(count - 1).setFinished();
				assembly.addInstruction(Instructions.INSTR_ELEAVE_1_NN_NEXT);
				assembly.addInstruction(OperationsA2X.XFSTORE_D.instruction(Base.forString(iterator), null, ModifierArguments.AE21POP, 0));
				assembly.addDebug(tokens[currentTokenIndex] + " // /ITERATE    // opened by: " + source);
				continue;
			}
			if (firstChar == 'S' && tagName.equals("SQL")) {
				if (ctx == null) {
					ctx = Exec.createProcess(Context.getServer(Exec.currentProcess()).getRootContext(), "TPL compiler context");
				}
				assembly.addInstruction(new TagSQL(folder, recursions, tagParam, innerTokens, ctx));
				continue;
			}
			if (firstChar == 'S' && tagName.equals("SQLUSE")) {
				assembly.addInstruction(new TagSQLUSE(folder, recursions, tagParam, innerTokens));
				continue;
			}
			if (firstChar == 'S' && tagName.equals("SET")) {
				final TokenInstruction reference = Evaluate.compileToken(assembly, tagParam, BalanceType.ARGUMENT_LIST);
				if (!reference.isAccessReference()) {
					assert reference.assertAccessReference();
					assembly.addError("Reference (lvalue) required (or 'null' if output should be ignored)!");
					return;
				}
				reference.toReferenceReadBeforeWrite(assembly, null, null, false, true);
				reference.toReferenceWriteAfterRead(assembly, null, null, new ModifierArgumentA30IMM(TplParser.toSourceOriginal(innerTokens)), ResultHandler.FA_BNN_NXT);
				continue;
			}
			if (firstChar == 'C' && tagName.equals("CODE")) {
				final String type = tagParam.replace('\'', ' ').replace('"', ' ').trim();
				final LanguageImpl language = Evaluate.getLanguageImpl(type);
				if (language == null) {
					throw new IllegalArgumentException("unknown renderer: " + type);
				}
				Evaluate.compileProgramInline(language, "tpl-code-tag", TplParser.toSourceOriginal(innerTokens), assembly);
				assembly.addDebug(tokens[currentTokenIndex] + " // /CODE    // opened by: " + source);
				continue;
			}
			if (firstChar == 'W' && tagName.equals("WHILE")) {
				final InstructionEditable frameEntry = OperationsA01.XEENTRCTRL_P.instructionCreate(0, ResultHandler.FA_BNN_NXT);
				assembly.addInstruction(frameEntry);
				final int frameStart = assembly.size();
				final InstructionEditable breakTarget = OperationsA01.XFBTGT_P.instructionCreate(0, ResultHandler.FA_BNN_NXT);
				assembly.addInstruction(breakTarget);
				assembly.addInstruction(OperationsA01.XFCTGT_P.instruction(0, ResultHandler.FA_BNN_NXT));
				final int conditionStart = assembly.size();
				/** ESKIP1 / ESKIP0 - doesn't require 'boolean' */
				Evaluate.compileExpression(assembly, tagParam, ResultHandler.FA_BNN_NXT);
				final InstructionEditable skipFalse = OperationsA01.XESKIPRB0_P.instructionCreate(0, ResultHandler.FA_BNN_NXT);
				assembly.addInstruction(skipFalse);
				final int skipPosition = assembly.size();

				TplParser.parse(assembly, innerTokens, self, recursions, folder);
				assembly.addInstruction(assembly.toProgram(skipPosition));

				assembly.addInstruction(OperationsA01.XESKIP_P.instruction(-assembly.getInstructionCount(conditionStart) - 1, ResultHandler.FA_BNN_NXT));
				skipFalse.setConstant(assembly.getInstructionCount(skipPosition)).setFinished();
				final int count = assembly.getInstructionCount(frameStart);
				frameEntry.setConstant(count).setFinished();
				breakTarget.setConstant(count - 1).setFinished();
				assembly.addInstruction(Instructions.INSTR_ELEAVE_0_NN_NEXT);
				assembly.addDebug(tokens[currentTokenIndex] + " // /WHILE    // opened by: " + source);
				continue;
			}
			if (firstChar == 'F' && tagName.equals("FOR")) {
				final String[] expressions = new String[3];
				{
					final StringBuilder buffer = new StringBuilder();
					int count = 0;
					int levelBrace = 0;
					boolean isQuote = false;
					boolean isApos = false;
					boolean nextSymbol = false;
					for (final char c : tagParam.toCharArray()) {
						switch (c) {
							case '[' :
							case '(' :
								if (!isQuote && !isApos) {
									levelBrace++;
								}
								nextSymbol = false;
								break;
							case ']' :
							case ')' :
								if (!isQuote && !isApos) {
									levelBrace--;
								}
								nextSymbol = false;
								break;
							case '"' :
								if (!nextSymbol) {
									if (!isApos) {
										isQuote = !isQuote;
									}
								}
								nextSymbol = false;
								break;
							case '\'' :
								if (!nextSymbol) {
									if (!isQuote) {
										isApos = !isApos;
									}
								}
								nextSymbol = false;
								break;
							case '\\' :
								if (isQuote || isApos) {
									nextSymbol = true;
								}
								break;
							case ';' :
								if (!isQuote && !isApos && levelBrace == 0) {
									if (count < 2) {
										expressions[count] = buffer.toString();
										buffer.setLength(0);
										count++;
										continue;
									}
									throw new IllegalArgumentException("Illegal statement count, valid syntax: FOR: <statementInit>; <condition>; <operation>");
								}
								break;
							default :
								nextSymbol = false;
						}
						buffer.append(c);
					}
					if (count != 2) {
						throw new IllegalArgumentException("Illegal statement count, valid syntax: FOR: <statementInit>; <condition>; <operation>");
					}
					expressions[2] = buffer.toString();
				}
				final InstructionEditable frameEntry = OperationsA01.XEENTRCTRL_P.instructionCreate(0, ResultHandler.FA_BNN_NXT);
				assembly.addInstruction(frameEntry);
				final int frameStart = assembly.size();
				final InstructionEditable breakTarget = OperationsA01.XFBTGT_P.instructionCreate(0, ResultHandler.FA_BNN_NXT);
				assembly.addInstruction(breakTarget);
				final InstructionEditable continueTarget = OperationsA01.XFCTGT_P.instructionCreate(0, ResultHandler.FA_BNN_NXT);
				assembly.addInstruction(continueTarget);
				/** statementInit */
				Evaluate.compileStatement(assembly, -1, expressions[0]);
				/** statementCondition */
				final int conditionStart = assembly.size();
				/** ESKIP1 / ESKIP0 - doesn't require 'boolean' */
				Evaluate.compileExpression(assembly, expressions[1], ResultHandler.FA_BNN_NXT);
				final InstructionEditable skipFalse = OperationsA01.XESKIPRB0_P.instructionCreate(0, ResultHandler.FA_BNN_NXT);
				assembly.addInstruction(skipFalse);
				final int skipPosition = assembly.size();
				/** loopBody */
				TplParser.parse(assembly, innerTokens, self, recursions, folder);
				assembly.addInstruction(assembly.toProgram(skipPosition));

				/** statementLoop */
				continueTarget.setConstant(assembly.getInstructionCount(frameStart) - 2).setFinished();
				Evaluate.compileStatement(assembly, -1, expressions[2]);
				assembly.addInstruction(OperationsA01.XESKIP_P.instruction(-assembly.getInstructionCount(conditionStart) - 1, ResultHandler.FA_BNN_NXT));
				skipFalse.setConstant(assembly.getInstructionCount(skipPosition)).setFinished();
				final int count = assembly.getInstructionCount(frameStart);
				frameEntry.setConstant(count).setFinished();
				breakTarget.setConstant(count - 1).setFinished();
				assembly.addInstruction(Instructions.INSTR_ELEAVE_0_NN_NEXT);
				/** debug message */
				assembly.addDebug(tokens[currentTokenIndex] + " // /FOR    // opened by: " + source);
				continue;
			}
			if (firstChar == 'E' && tagName.equals("ELSE")) {
				final int size = assembly.size();
				TplParser.parse(assembly, innerTokens, null, recursions, folder);
				TplParser.applyElseToParent(self, assembly.toProgram(size));
				continue;
			}
			if (firstChar == 'F' && tagName.equals("FINAL")) {
				final int initialOffset = assembly.size();

				Evaluate.compileExpression(assembly, tagParam, ResultHandler.FB_BSN_NXT);
				if (innerTokens.length == 0) {
					/** debug message */
					assembly.addDebug(tokens[currentTokenIndex] + " // /FINAL    // opened by: " + source);
					continue;
				}

				assembly.addInstruction(Instructions.INSTR_FOTBLDR_0_SN_NEXT);
				final InstructionEditable frameStart = OperationsA01.XEENTRNONE_P.instructionCreate(0, ResultHandler.FA_BNN_NXT);
				assembly.addInstruction(frameStart);
				final int frameStartPosition = assembly.size();
				TplParser.parse(assembly, innerTokens, self, recursions, folder);
				final int instructionCount = assembly.getInstructionCount(frameStartPosition);
				frameStart.setConstant(instructionCount).setFinished();
				/** empty block? */
				if (instructionCount == 0) {
					/** skip EENTRY and FOTNULL / FOTBLDR */
					assembly.truncate(initialOffset);
					Evaluate.compileExpression(assembly, tagParam, ResultHandler.FU_BNN_NXT);
					continue;
				}
				assembly.addInstruction(Instructions.INSTR_ELEAVE_1_NN_NEXT);
				assembly.addInstruction(Instructions.INSTR_FOTDONE_C_1_S_NN_NEXT);

				assembly.addInstruction(new TagFINAL_EXIT());

				assembly.addDebug(tokens[currentTokenIndex] + " // /FINAL    // opened by: " + source);
				continue;
			}
			if (firstChar == 'C' && tagName.equals("CHOOSE")) {
				if (ctx == null) {
					ctx = Exec.createProcess(Context.getServer(Exec.currentProcess()).getRootContext(), "TPL compiler context");
				}
				Evaluate.compileExpression(assembly, tagParam, ResultHandler.FA_BNN_NXT);
				if (innerTokens.length == 0) {
					/** debug message */
					assembly.addDebug(tokens[currentTokenIndex] + " // /CHOOSE    // opened by: " + source);
					continue;
				}
				boolean hasDefault = false;
				InstructionEditable jumpNext = null;
				int found = -1;
				final List<InstructionEditable> jumpOuts = new ArrayList<>();
				for (int idx = 0;;) {
					final Token token = innerTokens[idx];
					final String currentSource = token.getSource();
					if (currentSource.startsWith("MATCH:")) {
						if (hasDefault) {
							throw new IllegalStateException("CHOOSE already has default match block!");
						}
						if (found != -1) {
							TplParser.parse(assembly, TplParser.toSubTokens(innerTokens, found + 1, idx), self, recursions, folder);
							final InstructionEditable jumpOut = OperationsA01.XESKIP_P.instructionCreate(assembly.size(), ResultHandler.FA_BNN_NXT);
							assembly.addInstruction(jumpOut);
							jumpOuts.add(jumpOut);
						}
						if (jumpNext != null) {
							jumpNext.setConstant(assembly.getInstructionCount(jumpNext.getConstant())).setFinished();
							jumpNext = null;
							assembly.addInstruction(Instructions.INSTR_ESKIP_1_NN_NEXT);
						}
						final BaseList<?> vs = Evaluate.evaluateList(currentSource.substring(6).trim(), ctx, null);
						if (vs != null && vs.size() > 0) {
							final int length = vs.size();
							for (int jdx = length - 1; jdx >= 0; jdx--) {
								final BaseObject o = vs.baseGet(jdx, null);
								assembly.addInstruction(OperationsA11.XESKIPRB1XA_P.instruction(new ModifierArgumentA30IMM(o), jdx + 1, ResultHandler.FA_BNN_NXT));
							}
						}
						jumpNext = OperationsA01.XESKIP_P.instructionCreate(assembly.size(), ResultHandler.FA_BNN_NXT);
						assembly.addInstruction(jumpNext);
						found = idx;
					} else //
					if (currentSource.equals("MATCH")) {
						if (hasDefault) {
							throw new IllegalStateException("CHOOSE already has default match block!");
						}
						if (found != -1) {
							TplParser.parse(assembly, TplParser.toSubTokens(innerTokens, found + 1, idx), self, recursions, folder);
							final InstructionEditable jumpOut = OperationsA01.XESKIP_P.instructionCreate(assembly.size(), ResultHandler.FA_BNN_NXT);
							assembly.addInstruction(jumpOut);
							jumpOuts.add(jumpOut);
						}
						if (jumpNext != null) {
							jumpNext.setConstant(assembly.getInstructionCount(jumpNext.getConstant()) - 1).setFinished();
							jumpNext = null;
						}
						found = idx;
						hasDefault = true;
					} else //
					if (currentSource.startsWith("CHOOSE:")) {
						final int pos = Tokens.findClosing(innerTokens, idx + 1, "CHOOSE:", "/CHOOSE");
						if (pos == -1) {
							throw new IllegalArgumentException("Unable to find closing tag for inner CHOOSE!");
						}
						idx = pos;
					}
					if (++idx >= innerTokens.length) {
						if (found != -1) {
							TplParser.parse(assembly, TplParser.toSubTokens(innerTokens, found + 1, innerTokens.length), self, recursions, folder);
						}
						if (jumpNext != null) {
							jumpNext.setConstant(assembly.getInstructionCount(jumpNext.getConstant())).setFinished();
							jumpNext = null;
						}
						break;
					}
				}
				for (final InstructionEditable jumpOut : jumpOuts) {
					jumpOut.setConstant(assembly.getInstructionCount(jumpOut.getConstant())).setFinished();
				}
				/** debug message */
				assembly.addDebug(tokens[currentTokenIndex] + " // /CHOOSE    // opened by: " + source);
				continue;
			}
			if (firstChar == 'T' && tagName.equals("TRY")) {
				ProgramPart body = null;
				ProgramPart bodyCatch = null;
				for (int idx = 0; idx < innerTokens.length; idx++) {
					final Token token = innerTokens[idx];
					if (token.getSource().equals("CATCH")) {
						final int initialOffset = assembly.size();
						TplParser.parse(assembly, TplParser.toSubTokens(innerTokens, 0, idx), self, recursions, folder);
						body = assembly.toProgram(initialOffset);
						TplParser.parse(assembly, TplParser.toSubTokens(innerTokens, idx + 1, innerTokens.length), self, recursions, folder);
						final ProgramPart catchCode = assembly.toProgram(initialOffset);
						if (catchCode.length() == 0) {
							assembly.addDebug(token + " // CATCH");
							/** zero length - nothing to do, but still need 'handler' to mask
							 * exception */
							bodyCatch = assembly.toProgram(initialOffset);
						} else {
							assembly.addDebug(token + " // CATCH");
							assembly.addInstruction(OperationsA01.XEENTRCTCH_P.instruction(catchCode.length() + 2, ResultHandler.FA_BNN_NXT));
							assembly.addInstruction(OperationsA2X.XFDECLARE_D.instruction(new ModifierArgumentA30IMM("Exception"), ModifierArguments.AA0RB, 0, ResultHandler.FA_BNN_NXT));
							assembly.addInstruction(OperationsA2X.XFDECLARE_D.instruction(new ModifierArgumentA30IMM("Error"), ModifierArguments.AA0RB, 0, ResultHandler.FA_BNN_NXT));
							assembly.addInstruction(catchCode);
							assembly.addInstruction(Instructions.INSTR_ELEAVE_0_NN_NEXT);
							bodyCatch = assembly.toProgram(initialOffset);
						}
						break;
					} else //
					if (token.getSource().equals("TRY")) {
						final int pos = Tokens.findClosing(innerTokens, idx + 1, "TRY", "/TRY");
						if (pos == -1) {
							throw new IllegalArgumentException("Unable to find closing tag for inner TRY!");
						}
						idx = pos;
					}
				}
				if (body == null || bodyCatch == null) {
					throw new IllegalArgumentException("TRY with no CATCH!");
				}

				assembly.addInstruction(OperationsA01.XEENTRCTRL_P.instruction(body.length() + 2 + bodyCatch.length(), ResultHandler.FA_BNN_NXT));
				assembly.addInstruction(OperationsA01.XFETGT_P.instruction(1 + body.length(), ResultHandler.FA_BNN_NXT));
				assembly.addInstruction(body);
				assembly.addInstruction(OperationsA01.XESKIP_P.instruction(bodyCatch.length(), ResultHandler.FA_BNN_NXT));
				assembly.addInstruction(bodyCatch);
				assembly.addInstruction(Instructions.INSTR_ELEAVE_0_NN_NEXT);
				assembly.addDebug(tokens[currentTokenIndex] + " // /TRY");
				continue;
			}
			if (firstChar == 'O' && tagName.equals("OUTPUT")) {
				final String expression = tagParam.trim();
				final TokenInstruction reference = expression.length() == 4 && "null".equals(expression)
					? null
					: Evaluate.compileToken(assembly, expression, BalanceType.ARGUMENT_LIST);
				if (reference == null) {
					assembly.addInstruction(Instructions.INSTR_FOTNULL_0_SN_NEXT);
				} else {
					if (!reference.isAccessReference()) {
						// assert reference.assertAccessReference();
						assembly.addError("Reference (lvalue) required (or 'null' if output should be ignored)!");
						return;
					}
					reference.toReferenceReadBeforeWrite(assembly, null, null, false, true);
					assembly.addInstruction(Instructions.INSTR_FOTBLDR_0_SN_NEXT);
				}

				final InstructionEditable frameStart = OperationsA01.XEENTRNONE_P.instructionCreate(0, ResultHandler.FA_BNN_NXT);
				assembly.addInstruction(frameStart);
				final int frameStartPosition = assembly.size();
				TplParser.parse(assembly, innerTokens, self, recursions, folder);
				final int instructionCount = assembly.getInstructionCount(frameStartPosition);

				/** empty block? */
				if (instructionCount == 0) {
					/** skip EENTRY and FOTNULL / FOTBLDR */
					assembly.truncate(frameStartPosition - 2);
					if (reference != null) {
						reference.toReferenceWriteAfterRead(assembly, null, null, ModifierArgumentA30IMM.EMPTY_STRING, ResultHandler.FA_BNN_NXT);
					}
					continue;
				}

				frameStart.setConstant(instructionCount).setFinished();
				assembly.addInstruction(Instructions.INSTR_ELEAVE_1_NN_NEXT);
				assembly.addInstruction(Instructions.INSTR_FOTDONE_B_1_S_NN_NEXT);
				if (reference != null) {
					reference.toReferenceWriteAfterRead(assembly, null, null, ModifierArguments.AA0RB, ResultHandler.FA_BNN_NXT);
				}
				assembly.addDebug(tokens[currentTokenIndex] + " // /OUTPUT    // opened by: " + source);
				continue;
			}
			if (firstChar == 'O' && tagName.equals("OUTAPPEND")) {
				final String expression = tagParam.trim();
				if (expression.length() == 4 && "null".equals(expression)) {
					assembly.addError("OUTAPPEND does not allow 'null' argument, use OUTPUT with 'null' argument if you want to ignore all output!");
					return;
				}
				final TokenInstruction reference = Evaluate.compileToken(assembly, expression, BalanceType.ARGUMENT_LIST);
				if (!reference.isAccessReference()) {
					// assert reference.assertAccessReference();
					assembly.addError("Reference (lvalue) required (or 'null' if output should be ignored)!");
					return;
				}
				{
					final ModifierArgument value = reference.toReferenceReadBeforeWrite(assembly, null, null, true, false);
					assert value != null : "Must not be NULL, class=" + reference.getClass().getName() + ", value=" + reference;
					assembly.addInstruction(value == ModifierArguments.AE21POP
						? Instructions.INSTR_BOR_EMPTYSTRING_1_S_S
						: value == ModifierArguments.AA0RB
							? Instructions.INSTR_BOR_EMPTYSTRING_1_R_S
							/** looks like both arguments are detachable - BOR_T */
							: OperationsA2X.XEBOR_T.instruction(value, ModifierArgumentA30IMM.EMPTY_STRING, 0, ResultHandler.FB_BSN_NXT));
				}
				assembly.addInstruction(Instructions.INSTR_FOTBLDR_0_SN_NEXT);
				final InstructionEditable frameStart = OperationsA01.XEENTRNONE_P.instructionCreate(0, ResultHandler.FA_BNN_NXT);
				assembly.addInstruction(frameStart);
				final int frameStartPosition = assembly.size();
				TplParser.parse(assembly, innerTokens, self, recursions, folder);
				final int instructionCount = assembly.getInstructionCount(frameStartPosition);

				/** empty block? */
				if (instructionCount == 0) {
					/** skip EENTRY and FOTNULL / FOTBLDR */
					assembly.truncate(frameStartPosition - 2);
					reference.toReferenceWriteAfterRead(assembly, null, null, ModifierArguments.AE21POP, ResultHandler.FA_BNN_NXT);
					continue;
				}

				frameStart.setConstant(instructionCount).setFinished();
				assembly.addInstruction(Instructions.INSTR_ELEAVE_1_NN_NEXT);
				assembly.addInstruction(Instructions.INSTR_FOTDONE_B_1_S_NN_NEXT);
				assembly.addInstruction(Instructions.INSTR_MADDS_D_BA_RS_V);
				reference.toReferenceWriteAfterRead(assembly, null, null, ModifierArguments.AA0RB, ResultHandler.FA_BNN_NXT);
				assembly.addDebug(tokens[currentTokenIndex] + " // /OUTAPPEND    // opened by: " + source);
				continue;
			}
			if (firstChar == 'R' && tagName.equals("RECURSION")) {

				Evaluate.compileExpression(assembly, "Create.mapFor(" + tagParam + ')', ResultHandler.FA_BNN_NXT);

				final TagRECURSION recursion = new TagRECURSION();

				/** Tag DEEPER will search for corresponding RECURSION */
				recursions.push(recursion);

				final ProgramPart body = TplParser.createProgramPart(folder, recursion, recursions, innerTokens);
				recursion.length = body.length();

				/** clean-up */
				recursions.pop();

				assembly.addInstruction(recursion);
				assembly.addInstruction(body);

				continue;
			}
			if (firstChar == 'C' && tagName.equals("CALL")) {
				assembly.addDebug(currentToken + ".." + tokens[endPosition] + " // TPL BLOCK(" + tagName + "), arguments=" + tagParam + ", tokenCount=" + innerTokens.length);
				assembly.addInstruction(new TagCALL(folder, recursions, tagParam, innerTokens));
				continue;
			}
			Report.error("ACM/TPL", "TemplateProcessor: Unknown tag encountered, tag_name=" + tagName);
			throw new IllegalArgumentException("TemplateProcessor: Unknown tag encountered, tag_name=" + tagName);
		}
	}

	static final String toSource(final Token[] tokens) {
		
		final StringBuilder result = new StringBuilder(512);
		for (final Token element : tokens) {
			result.append(element.getSource());
		}
		return result.toString();
	}

	static final String toSourceOriginal(final Token[] tokens) {
		
		final StringBuilder result = new StringBuilder(512);
		for (final Token element : tokens) {
			result.append(element.getSourceOriginal());
		}
		return result.toString();
	}

	static final Token[] toSubTokens(final Token[] tokens, final int start, final int end) {
		
		final Token[] innerTokens = new Token[end - start];
		System.arraycopy(tokens, start, innerTokens, 0, innerTokens.length);
		return innerTokens;
	}

	private TplParser() {
		//
	}
}
