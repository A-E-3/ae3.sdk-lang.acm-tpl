package ru.myx.renderer.tpl;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import ru.myx.ae3.act.Context;
import ru.myx.ae3.base.BaseArray;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.eval.Evaluate;
import ru.myx.ae3.exec.ExecProcess;
import ru.myx.ae3.exec.ExecStateCode;
import ru.myx.ae3.exec.Instruction;
import ru.myx.ae3.exec.ProgramPart;
import ru.myx.ae3.help.Convert;

/**
 * TPL TAG impl
 *
 * @author Alexander I. Kharitchev
 */
final class TagSQLBATCH implements Instruction {
	
	
	private final ProgramPart caller;
	
	TagSQLBATCH(final String params) {
		this.caller = Evaluate.prepareFunctionObjectForExpression("TPL.GET_ARGUMENTS(" + params + ')', null);
	}
	
	@Override
	public final ExecStateCode execCall(final ExecProcess ctx) throws Exception {
		
		
		ctx.vmFrameEntryExCode();
		final BaseArray list = (BaseArray) this.caller.execCallPreparedInilne(ctx);
		final String connectionName = Convert.ListEntry.toString(list, 0, "default");
		if (list.length() < 2) {
			return null;
		}
		final BaseObject parameters = list.baseGet(1, null);
		if (parameters == BaseObject.UNDEFINED || parameters == BaseObject.NULL) {
			return null;
		}
		final Collection<?> queries;
		if (parameters.baseIsPrimitive()) {
			queries = Collections.singleton(parameters);
		} else //
		if (parameters instanceof Collection<?>) {
			queries = (Collection<?>) parameters;
		} else {
			final BaseArray array = parameters.baseArray();
			if (array != null) {
				final int length = array.length();
				final List<String> temp = new ArrayList<>(length);
				for (int i = 0; i < length; ++i) {
					temp.add(array.baseGet(i, BaseObject.UNDEFINED).baseToJavaString());
				}
				queries = temp;
			} else {
				queries = Collections.singleton(parameters);
			}
		}
		if (queries.isEmpty()) {
			return null;
		}
		final BaseObject parentConnection = ctx.baseGet("$conn-" + connectionName, BaseObject.UNDEFINED);
		final Connection conn;
		if (parentConnection.baseValue() == null) {
			conn = Context.getServer(ctx).getServerConnection(connectionName);
			if (conn == null) {
				throw new IllegalArgumentException("TagSQL: DataSource ('" + connectionName + "') is undefined!");
			}
		} else {
			conn = (Connection) parentConnection;
		}
		try {
			try (final Statement st = conn.createStatement()) {
				for (final Object query : queries) {
					if (query != null) {
						st.execute(query.toString());
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
			}
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
	public String toCode() {
		
		
		return "SQLBATCH";
	}
}
