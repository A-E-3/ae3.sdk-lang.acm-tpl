package ru.myx.renderer.tpl.fn;

import ru.myx.ae3.base.BaseArray;
import ru.myx.ae3.base.BaseFunctionAbstract;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.exec.ExecCallableFull;
import ru.myx.ae3.exec.ExecProcess;
import ru.myx.ae3.exec.ExecStateCode;
import ru.myx.ae3.help.Convert;
import ru.myx.ae3.report.Report;

/**
 * @author myx
 *
 */
public final class FunctionDoLog extends BaseFunctionAbstract implements ExecCallableFull {
	
	
	@Override
	public String toString() {
		
		
		return "[TPL: FunctionDoLog]";
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
		final String subject = Convert.ListEntry.toString(list, 0, "no subject");
		final String message = Convert.ListEntry.toString(list, 1, "no message");
		Report.event("ACM.TPL", subject, message);
		context.vmSetCallResultUndefined();
		return null;
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
