package ru.myx.renderer.tpl;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import java.util.function.Function;
import ru.myx.ae3.act.Context;
import ru.myx.ae3.base.Base;
import ru.myx.ae3.base.BaseArray;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.eval.Evaluate;
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
final class TagSQLUSE implements Instruction {
	
	private final ProgramPart source;

	private final ProgramPart caller;

	TagSQLUSE(final Function<String, String> folder, final Stack<TagRECURSION> recursions, final String parameters, final Token[] source) {
		this.source = TplParser.createProgramPart(folder, this, recursions, source);
		this.caller = Evaluate.prepareFunctionObjectForExpression("TPL.GET_ARGUMENTS(" + parameters + ')', null);
	}

	@Override
	public final ExecStateCode execCall(final ExecProcess ctx) throws Exception {
		
		ctx.vmFrameEntryExCode();
		final BaseArray list = (BaseArray) this.caller.execCallPreparedInilne(ctx);
		if (list == null || list.length() == 0) {
			return ctx.vmRaise("TagSQLUSE: no connection list or connection list is empty!");
		}
		final List<String> lockedConnections = new ArrayList<>();
		try {
			for (int i = 0; i < list.length(); ++i) {
				final String connection = list.baseGet(i, BaseObject.UNDEFINED).baseToJavaString();
				final String name = "$conn-" + connection;
				final BaseObject parent = ctx.baseGet(name, BaseObject.UNDEFINED);
				if (parent.baseValue() == null) {
					final Connection conn = Context.getServer(ctx).getServerConnection(connection);
					if (conn == null) {
						throw new IllegalArgumentException("TagSQLUSE: DataSource ('" + connection + "') is undefined!");
					}
					final BaseObject connObject = Base.forUnknown(conn);
					assert connObject != null : "NULL java value";
					assert connObject.baseValue() == conn : "Should hold a jdbc connection!";
					ctx.contextCreateMutableBinding(name, connObject, false);
					lockedConnections.add(name);
				}
			}
			return this.source.execCallPrepare(ctx, null, ResultHandler.FU_BNN_NXT, true);
		} finally {
			for (final String name : lockedConnections) {
				final BaseObject parent = ctx.baseGet(name, BaseObject.UNDEFINED);
				if (parent.baseValue() != null) {
					ctx.baseDelete(name);
					try {
						((Connection) parent).close();
					} catch (final Throwable t) {
						// ignore
					}
				}
			}
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
	public final String toCode() {
		
		return "SQLUSE";
	}
}
