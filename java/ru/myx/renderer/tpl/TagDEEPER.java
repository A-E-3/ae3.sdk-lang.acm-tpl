package ru.myx.renderer.tpl;

import static ru.myx.ae3.exec.ExecStateCode.REPEAT;

import ru.myx.ae3.base.BaseJoined;
import ru.myx.ae3.base.BaseMap;
import ru.myx.ae3.base.BaseObject;
import ru.myx.ae3.exec.ExecProcess;
import ru.myx.ae3.exec.ExecStateCode;
import ru.myx.ae3.exec.Instruction;

/**
 * TPL TAG impl
 *
 * @author Alexander I. Kharitchev
 */
final class TagDEEPER implements Instruction {
	
	
	final TagRECURSION recursion;
	
	TagDEEPER(final TagRECURSION recursion) {
		this.recursion = recursion;
	}
	@Override
	public BaseObject basePrototype() {
		
		
		return null;
	}
	
	/**
	 * Executed as an Instruction (RECURSION tag): (setup function and) call it
	 */
	@Override
	public final ExecStateCode execCall(final ExecProcess ctx) throws Exception {
		
		
		/**
		 * before fiddling with IP
		 */
		final BaseMap arguments = (BaseMap) ctx.ra0RB;
		
		final int start = this.recursion.start;
		final int length = this.recursion.length;
		
		ctx.vmFrameEntryExBlock();
		
		ctx.rb7FV = new BaseJoined(arguments, ctx.rb7FV);
		
		ctx.ri0FI0 = ctx.ri08IP = start;
		ctx.ri09IL = start + length;
		
		return REPEAT;
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
		
		
		return "DEEPER (" + this.recursion + ")";
	}
}
