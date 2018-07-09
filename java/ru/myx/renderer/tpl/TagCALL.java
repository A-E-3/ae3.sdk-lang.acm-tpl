package ru.myx.renderer.tpl;

import java.util.Stack;

import java.util.function.Function;
import ru.myx.ae3.act.Context;
import ru.myx.ae3.base.BaseFunction;
import ru.myx.ae3.eval.Evaluate;
import ru.myx.ae3.exec.ExecArgumentsEmpty;
import ru.myx.ae3.exec.ExecNonMaskedException;
import ru.myx.ae3.exec.ExecProcess;
import ru.myx.ae3.exec.ExecStateCode;
import ru.myx.ae3.exec.Instruction;
import ru.myx.ae3.exec.ProgramPart;
import ru.myx.ae3.exec.ResultHandler;
import ru.myx.renderer.tpl.parse.Token;

/**
 * TPL TAG impl
 *
 * @author Alexander I. Kharitchev
 */
final class TagCALL implements Instruction {
	
	private final String expression;
	
	private final ProgramPart call;
	
	private final ProgramPart source;
	
	TagCALL(final Function<String, String> folder, final Stack<TagRECURSION> recursions, final String expression, final Token[] source) throws Evaluate.CompilationException {
		this.expression = expression;
		this.call = Evaluate.prepareFunctionObjectForExpression(expression, null);
		this.source = TplParser.createProgramPart(folder, this, recursions, source);
	}
	
	@Override
	public final ExecStateCode execCall(final ExecProcess ctx) {
		
		final BaseFunction prev = Context.sourceReplace(ctx, this.source);
		try {
			ctx.vmFrameEntryExCall(true, null, this.call, ExecArgumentsEmpty.INSTANCE, ResultHandler.FA_BNN_NXT);
			ctx.vmScopeDeriveLocals();
			// ctx.vmFrameEntryExCode();
			final Object result = this.call.execCallPreparedInilne(ctx);
			if (result == null || !(result instanceof ExecStateCode)) {
				return null;
			}
			return (ExecStateCode) result;
		} catch (final Error e) {
			throw e;
		} catch (final ExecNonMaskedException e) {
			throw e;
		} catch (final Throwable t) {
			throw new RuntimeException("CALL: " + this.expression + ", class=" + t.getClass().getName(), t);
		} finally {
			Context.sourceReplace(ctx, prev);
		}
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
	public String toCode() {
		
		return "CALL: " + this.expression;
	}
}
