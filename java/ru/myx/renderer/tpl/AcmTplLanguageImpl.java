package ru.myx.renderer.tpl;

import java.util.Stack;

import java.util.function.Function;
import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseFunction;
import ru.myx.ae3.base.BaseMap;
import ru.myx.ae3.base.BaseNativeObject;
import ru.myx.ae3.base.BaseProperty;
import ru.myx.ae3.eval.CompileTargetMode;
import ru.myx.ae3.eval.Evaluate;
import ru.myx.ae3.eval.LanguageImpl;
import ru.myx.ae3.exec.ExecProcess;
import ru.myx.ae3.exec.InstructionEditable;
import ru.myx.ae3.exec.Instructions;
import ru.myx.ae3.exec.OperationsA01;
import ru.myx.ae3.exec.ProgramAssembly;
import ru.myx.ae3.exec.ProgramPart;
import ru.myx.ae3.exec.ResultHandler;
import ru.myx.ae3.exec.fn.FunctionReturnArgumentsObject;
import ru.myx.ae3.reflect.ReflectionExplicit;
import ru.myx.renderer.tpl.fn.FunctionDoAudit;
import ru.myx.renderer.tpl.fn.FunctionDoLog;
import ru.myx.renderer.tpl.fn.FunctionRedirect;
import ru.myx.renderer.tpl.fn.FunctionSqlExec;
import ru.myx.renderer.tpl.parse.Token;
import ru.myx.renderer.tpl.parse.Tokens;

/**
 * @author myx
 *
 */
public class AcmTplLanguageImpl implements LanguageImpl {
	
	/**
	 *
	 */
	public static final AcmTplLanguageImpl INSTANCE = new AcmTplLanguageImpl();
	
	static {
		final BaseMap api = new BaseNativeObject(Base.forUnknown(AcmTplLanguageImpl.INSTANCE));
		ExecProcess.GLOBAL.baseDefine("TPL", api, BaseProperty.ATTRS_MASK_NNN);
		
		api.baseDefine("doAudit", new FunctionDoAudit());
		api.baseDefine("doLog", new FunctionDoLog());
		api.baseDefine("doRedirect", new FunctionRedirect());
		api.baseDefine("doSqlExec", new FunctionSqlExec());
	}

	/**
	 *
	 */
	@ReflectionExplicit
	public static final BaseFunction GET_ARGUMENTS = FunctionReturnArgumentsObject.INSTANCE;
	
	/**
	 *
	 */
	private AcmTplLanguageImpl() {
		// empty
	}
	
	@Override
	public final void compile(final String identity, final Function<String, String> folder, final String name, final ProgramAssembly assembly, final CompileTargetMode mode)
			throws Evaluate.CompilationException {
		
		final String source = folder.apply(name);
		if (source == null) {
			return;
		}
		final Token[] tokens = Tokens.parse(source);
		if (mode == CompileTargetMode.INLINE) {
			TplParser.parse(assembly, tokens, null, new Stack<TagRECURSION>(), folder);
			return;
		}
		final int start = assembly.size();
		assembly.addInstruction(Instructions.INSTR_FOTBLDR_0_SN_NEXT);
		final InstructionEditable frameEntry = OperationsA01.XEENTRFULL_P.instructionCreate(0, ResultHandler.FA_BNN_NXT);
		assembly.addInstruction(frameEntry);
		final int frameStart = assembly.size();
		/**
		 * Variables are not declared in TPL so GV (global) should be pulled to
		 * the FV (scope) of the beginning of TPL code.
		 */
		assembly.addInstruction(Instructions.INSTR_FPULLGV_0_0_NN_NEXT);
		TplParser.parse(assembly, tokens, null, new Stack<TagRECURSION>(), folder);
		assembly.addInstruction(Instructions.INSTR_ELEAVE_0_NN_NEXT);
		assembly.addInstruction(Instructions.INSTR_FOTDONE_1_S_NN_RETURN);
		assembly.addInstruction(Instructions.INSTR_ELEAVE_1_NN_NEXT);
		assembly.addInstruction(Instructions.INSTR_FOTDONE_B_1_S_NN_NEXT);
		final ProgramPart result = assembly.toProgram(start);
		frameEntry.setConstant(result.length() - frameStart - 1).setFinished();
		assembly.addInstruction(result);
	}
	
	@Override
	public String[] getAssociatedAliases() {
		
		return new String[]{
				//
				"ACM.TPL", //
				"Anything.TPL", //
				"Axiom.TPL", //
				"mwm.tpl", //
				"mwm.tpl.ncache", //
		};
	}
	
	@Override
	public String[] getAssociatedExtensions() {
		
		return new String[]{
				//
				".tpl", //
				".TPL", //
		};
	}
	
	@Override
	public String[] getAssociatedMimeTypes() {
		
		return new String[]{
				//
				"application/x-ae2-tpl", //
				"text/tpl", //
		};
	}
	
	@Override
	public String getKey() {
		
		return "TPL";
	}
}
