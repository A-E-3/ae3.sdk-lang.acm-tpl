package ru.myx.renderer.tpl.fn;

import ru.myx.ae3.base.BaseFunctionAbstract;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.exec.ExecCallableBoth;
import ru.myx.ae3.exec.ExecProcess;
import ru.myx.ae3.help.Convert;
import ru.myx.ae3.report.Report;

/** @author myx */
public final class FunctionDoAudit extends BaseFunctionAbstract implements ExecCallableBoth.NativeJ2 {

	@Override
	public BaseObject callNJ2(final BaseObject instance, final BaseObject argument1, final BaseObject argument2) {

		final String subject = Convert.Any.toString(argument1, "no subject");
		final String message = Convert.Any.toString(argument2, "no message");
		Report.audit("ACM.TPL", subject, message);
		return BaseObject.UNDEFINED;
	}
	
	@Deprecated
	@Override
	public final int execArgumentsMinimal() {

		return 1;
	}
	
	@Override
	public Class<? extends Object> execResultClassJava() {

		return Void.class;
	}
	
	@Override
	public BaseObject execScope() {

		/** executes in real current scope */
		return ExecProcess.GLOBAL;
	}
	
	@Override
	public String toString() {

		return "[TPL: FunctionDoAudit]";
	}
	
}
