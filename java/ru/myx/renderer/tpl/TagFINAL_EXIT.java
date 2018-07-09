package ru.myx.renderer.tpl;

import ru.myx.ae3.act.Context;
import ru.myx.ae3.answer.Reply;
import ru.myx.ae3.answer.ReplyAnswer;
import ru.myx.ae3.exec.ExecProcess;
import ru.myx.ae3.exec.ExecStateCode;
import ru.myx.ae3.exec.Instruction;
import ru.myx.ae3.exec.ResultHandler;

/**
 * TPL TAG impl
 *
 * @author Alexander I. Kharitchev
 */
final class TagFINAL_EXIT implements Instruction {
	
	
	TagFINAL_EXIT() {
		//
	}
	
	@Override
	public final ExecStateCode execCall(final ExecProcess ctx) throws Exception {
		
		
		final String content = ctx.ra3RS.toString();
		
		final Object contentTypeObject = ctx.stackPop().baseValue();
		final String contentType = contentTypeObject == null
			? "text/plain"
			: contentTypeObject.toString();
		
		final ReplyAnswer reply = Reply.string("TPL_FINAL", Context.getRequest(ctx), content) //
				.setContentType(contentType) //
				.setFinal()//
		;
		
		return ResultHandler.FC_PNN_RET.execReturn(ctx, reply);
	}
	
	@Override
	public final int getOperandCount() {
		
		
		return 1;
	}
	
	@Override
	public final int getResultCount() {
		
		
		return 0;
	}
	
	@Override
	public final String toCode() {
		
		
		return "FINAL_EXIT    2	0         \tPOP, ra3RS               \tEFC_PNN_RET";
	}
}
