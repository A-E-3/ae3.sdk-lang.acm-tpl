package ru.myx.renderer.tpl;

import java.util.function.Function;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.eval.Evaluate;
import ru.myx.ae3.exec.ExecProcess;
import ru.myx.ae3.exec.ExecStateCode;
import ru.myx.ae3.exec.Instruction;
import ru.myx.ae3.exec.ProgramPart;
import ru.myx.ae3.exec.ResultHandler;

/**
 * TPL TAG impl
 *
 * @author Alexander I. Kharitchev
 */
final class TagPROCESS implements Instruction {
	
	
	private final String expression;

	private final ProgramPart calc;

	private final Function<String, String> folder;

	TagPROCESS(final Function<String, String> folder, final String expression) {
		this.folder = folder;
		this.expression = expression;
		this.calc = Evaluate.prepareFunctionObjectForExpression(expression, null);
	}

	@Override
	public final ExecStateCode execCall(final ExecProcess ctx) throws Exception {
		
		
		ctx.vmFrameEntryExCode();
		final BaseObject result = this.calc.execCallPreparedInilne(ctx);
		if (result == BaseObject.UNDEFINED) {
			return null;
		}
		/**
		 * TODO: clean up
		 */
		final ProgramPart part = (ProgramPart) TplParser.createInstruction(this.folder, this, null, result.toString());
		return part.execCallPrepare(ctx, BaseObject.UNDEFINED, ResultHandler.FU_BNN_NXT, false);
		
	}

	@Override
	public final int getOperandCount() {
		
		
		return 0;
	}

	@Override
	public final int getResultCount() {
		
		
		return 0;
	}

	@Override
	public final String toCode() {
		
		
		return "PROCESS: " + this.expression;
	}
}
