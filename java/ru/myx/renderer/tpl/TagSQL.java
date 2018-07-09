package ru.myx.renderer.tpl;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
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
import ru.myx.ae3.help.Convert;
import ru.myx.renderer.tpl.parse.Token;
import ru.myx.util.BaseMapSqlResultSet;

/**
 * TPL TAG impl
 *
 * @author Alexander I. Kharitchev
 */
final class TagSQL implements Instruction {
	
	private final String scopeName;
	
	private final ProgramPart source;
	
	private final ProgramPart caller;
	
	TagSQL(final Function<String, String> folder, final Stack<TagRECURSION> recursions, final String parameters, final Token[] source, final ExecProcess ctx) {
		// same context
		final BaseObject params = Evaluate.evaluateObject("Create.mapFor(" + parameters + ')', ctx, null);
		assert params != null : "NULL java value";
		this.scopeName = Base.getString(params, "ResultScope", "Record");
		this.source = TplParser.createProgramPart(folder, this, recursions, source);
		this.caller = Evaluate.prepareFunctionObjectForExpression("TPL.GET_ARGUMENTS(" + parameters + ')', null);
	}
	
	@Override
	public final ExecStateCode execCall(final ExecProcess ctx) throws Exception {
		
		ctx.vmFrameEntryExCode();
		final BaseArray list = (BaseArray) this.caller.execCallPreparedInilne(ctx);
		final String connectionName = Convert.ListEntry.toString(list, 0, "default");
		final String query = Convert.ListEntry.toString(list, 1, "undefined");
		final BaseObject prevRS = ctx.baseGet(this.scopeName, BaseObject.UNDEFINED);
		final BaseObject prevIndex = ctx.baseGet("CurrentIndex", BaseObject.UNDEFINED);
		final BaseObject parentConnection = ctx.baseGet("$conn-" + connectionName, BaseObject.UNDEFINED);
		final Connection conn;
		if (parentConnection.baseValue() == null) {
			conn = Context.getServer(ctx).getServerConnection(connectionName);
			if (conn == null) {
				return ctx.vmRaise("TagSQL: DataSource ('" + connectionName + "') is undefined!");
			}
			final BaseObject connObject = Base.forUnknown(conn);
			assert connObject != null : "NULL java value";
			assert connObject.baseValue() == conn : "Should hold a jdbc connection!";
			ctx.contextCreateMutableBinding("$conn-" + connectionName, connObject, false);
		} else {
			conn = (Connection) parentConnection;
		}
		try {
			try (final Statement st = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
				try (final ResultSet rs = st.executeQuery(query)) {
					ctx.contextCreateMutableBinding(this.scopeName, new BaseMapSqlResultSet(rs), false);
					for (int currentIndex = 0; rs.next(); currentIndex++) {
						ctx.contextSetMutableBinding("CurrentIndex", currentIndex, false);
						final ExecStateCode result = this.source.execCallPrepare(ctx, null, ResultHandler.FU_BNN_NXT, true);
						if (result != null) {
							if (result == ExecStateCode.BREAK) {
								return null;
							}
							if (result == ExecStateCode.CONTINUE) {
								continue;
							}
							return result;
						}
					}
				}
			}
		} finally {
			if (parentConnection.baseValue() == null) {
				try {
					conn.close();
				} catch (final Throwable t) {
					// ignore
				}
				ctx.baseDelete("$conn-" + connectionName);
			}
			ctx.contextSetMutableBinding(this.scopeName, prevRS, false);
			ctx.contextSetMutableBinding("CurrentIndex", prevIndex, false);
		}
		return null;
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
		
		return "SQL";
	}
}
