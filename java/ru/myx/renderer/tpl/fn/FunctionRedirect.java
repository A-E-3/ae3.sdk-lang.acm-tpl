package ru.myx.renderer.tpl.fn;

import ru.myx.ae3.act.Context;
import ru.myx.ae3.answer.Reply;
import ru.myx.ae3.base.BaseArray;
import ru.myx.ae3.base.BaseFunctionAbstract;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.exec.ExecCallableFull;
import ru.myx.ae3.exec.ExecFunctionUncheckedResultCode;
import ru.myx.ae3.exec.ExecProcess;
import ru.myx.ae3.exec.ExecStateCode;
import ru.myx.ae3.help.Convert;
import ru.myx.ae3.serve.ServeRequest;

/**
 * @author myx
 *
 */
public final class FunctionRedirect extends BaseFunctionAbstract implements ExecCallableFull, ExecFunctionUncheckedResultCode {
	
	
	@Override
	public String toString() {
		
		
		return "[TPL: FunctionRedirect]";
	}
	
	@Override
	public final int execArgumentsAcceptable() {
		
		
		return 2;
	}
	
	@Override
	public int execArgumentsDeclared() {
		
		
		return 2;
	}
	
	@Override
	public final int execArgumentsMinimal() {
		
		
		return 1;
	}
	
	@Override
	public ExecStateCode execCallImpl(final ExecProcess context) {
		
		
		final BaseArray list = context;
		final String target = Convert.ListEntry.toString(list, 0, "/");
		final boolean moved = Convert.ListEntry.toBoolean(list, 1, false);
		final ServeRequest query = Context.getRequest(context);
		final String url = Context.getServer(context).fixUrl(target);
		assert !url.equals(query.getUrl()) || !"GET".equals(query.getVerbOriginal()) //
		: "Recursive redirect detected: " + url;
		context.vmSetCallResult(Reply.redirect("TPL/TAG/REDIR", query, moved, url));
		return ExecStateCode.RETURN;
	}
	
	@Override
	public Class<? extends Object> execResultClassJava() {
		
		
		return Void.class;
	}
	
	@Override
	public BaseObject execScope() {
		
		
		/**
		 * executes in real current scope
		 */
		return ExecProcess.GLOBAL;
	}
	
}
