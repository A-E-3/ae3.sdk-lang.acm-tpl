package ru.myx.renderer.tpl.fn;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;

import ru.myx.ae3.act.Context;
import ru.myx.ae3.base.BaseArray;
import ru.myx.ae3.base.BaseFunctionAbstract;
import ru.myx.ae3.base.BaseMessage;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.binary.TransferBuffer;
import ru.myx.ae3.binary.TransferCopier;
import ru.myx.ae3.exec.ExecCallableFull;
import ru.myx.ae3.exec.ExecProcess;
import ru.myx.ae3.exec.ExecStateCode;
import ru.myx.ae3.help.Convert;

/**
 * @author myx
 *
 */
public final class FunctionSqlExec extends BaseFunctionAbstract implements ExecCallableFull {
	
	
	@Override
	public String toString() {
		
		
		return "[TPL: FunctionSqlExec]";
	}
	
	@Override
	public final int execArgumentsAcceptable() {
		
		
		return Integer.MAX_VALUE;
	}
	
	@Override
	public int execArgumentsDeclared() {
		
		
		return 2;
	}
	
	@Override
	public final int execArgumentsMinimal() {
		
		
		return 2;
	}
	
	@Override
	public ExecStateCode execCallImpl(final ExecProcess context) throws Exception {
		
		
		final BaseArray list = context;
		final String connectionName = Convert.ListEntry.toString(list, 0, "default");
		final String query = Convert.ListEntry.toString(list, 1, "undefined");
		final Object parentConnection = context.baseGet("$conn-" + connectionName, BaseObject.UNDEFINED).baseValue();
		final Connection conn;
		if (parentConnection == null) {
			conn = Context.getServer(context).getServerConnection(connectionName);
			if (conn == null) {
				throw new IllegalArgumentException("TagSQL: DataSource ('" + connectionName + "') is undefined!");
			}
		} else {
			assert parentConnection instanceof Connection : "Should be instance of Connection, but: class=" + parentConnection.getClass().getName() + ", string="
					+ parentConnection;
			conn = (Connection) parentConnection;
		}
		try {
			if (list.length() < 3) {
				try (final Statement st = conn.createStatement()) {
					st.execute(query);
				}
			} else {
				try (final PreparedStatement ps = conn.prepareStatement(query)) {
					int index = 1;
					for (int i = 2; i < list.length(); ++i) {
						final Object parameter = list.baseGet(i, BaseObject.UNDEFINED).baseValue();
						if (parameter == null) {
							ps.setNull(index++, Types.NULL);
							continue;
						}
						if (parameter instanceof Long) {
							ps.setLong(index++, ((Long) parameter).longValue());
							continue;
						}
						if (parameter instanceof Integer) {
							ps.setInt(index++, ((Integer) parameter).intValue());
							continue;
						}
						if (parameter instanceof Byte) {
							ps.setByte(index++, ((Byte) parameter).byteValue());
							continue;
						}
						if (parameter instanceof Float) {
							ps.setFloat(index++, ((Float) parameter).floatValue());
							continue;
						}
						if (parameter instanceof Double) {
							ps.setDouble(index++, ((Double) parameter).doubleValue());
							continue;
						}
						if (parameter instanceof Date) {
							ps.setTimestamp(index++, new Timestamp(((Date) parameter).getTime()));
							continue;
						}
						if (parameter instanceof String) {
							ps.setString(index++, (String) parameter);
							continue;
						}
						if (parameter instanceof BaseMessage) {
							final BaseMessage message = (BaseMessage) parameter;
							if (message.isCharacter()) {
								ps.setString(index++, message.toCharacter().getText().toString());
								continue;
							}
							if (message.isBinary()) {
								final TransferCopier binary = message.toBinary().getBinary();
								final long length = binary.length();
								if (length > Integer.MAX_VALUE) {
									throw new RuntimeException("Bigger than maximum byte array size, size=" + length + "!");
								}
								ps.setBinaryStream(index++, binary.nextInputStream(), (int) length);
								continue;
							}
							try {
								final TransferCopier binary = message.toBinary().getBinary();
								final long length = binary.length();
								if (length > Integer.MAX_VALUE) {
									throw new RuntimeException("Bigger than maximum byte array size, size=" + length + "!");
								}
								ps.setBinaryStream(index++, binary.nextInputStream(), (int) length);
								continue;
							} catch (final IOException e) {
								throw new RuntimeException(e);
							}
						}
						if (parameter instanceof TransferCopier) {
							final TransferCopier copier = (TransferCopier) parameter;
							final long length = copier.length();
							if (length > Integer.MAX_VALUE) {
								throw new RuntimeException("Bigger than maximum byte array size, size=" + length + "!");
							}
							ps.setBinaryStream(index++, copier.nextInputStream(), (int) length);
							continue;
						}
						if (parameter instanceof TransferBuffer) {
							final TransferBuffer binary = (TransferBuffer) parameter;
							final long length = binary.remaining();
							if (length > Integer.MAX_VALUE) {
								throw new RuntimeException("Bigger than maximum byte array size, size=" + length + "!");
							}
							ps.setBinaryStream(index++, binary.toInputStream(), (int) length);
							continue;
						}
						ps.setObject(index++, parameter);
					}
					ps.executeUpdate();
				}
			}
		} finally {
			if (parentConnection == null) {
				try {
					conn.close();
				} catch (final Throwable t) {
					// ignore
				}
			}
		}
		context.vmSetCallResultUndefined();
		return null;
	}
	
	@Override
	public final boolean execHasNamedArguments() {
		
		
		return true;
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
