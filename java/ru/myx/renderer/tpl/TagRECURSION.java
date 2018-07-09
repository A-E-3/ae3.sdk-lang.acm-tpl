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
final class TagRECURSION implements Instruction {
	
	
	int length = -1;
	
	int start = -1;
	
	TagRECURSION() {
		//
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
		
		// cheat, used in execCallPrepare, should be the same, no race
		final int start = this.start = ctx.ri08IP + 1;
		final int length = this.length;
		
		/**
		 * skip function body
		 */
		ctx.ri08IP = start + length - 1;
		
		ctx.vmFrameEntryExBlock();
		// ctx.vmFrameEntryExCall(false, ResultHandler.FA_BNN_NXT);
		// ctx.vmFrameEntryOpFull(start + length);
		
		ctx.rb7FV = new BaseJoined(arguments, ctx.rb7FV);
		
		ctx.ri0FI0 = ctx.ri08IP = start;
		ctx.ri09IL = start + length;
		
		return REPEAT;
	}
	
	@Override
	public int getConstant() {
		
		
		return this.length;
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
	public boolean isRelativeAddressInConstant() {
		
		
		return true;
	}
	
	@Override
	public final String toString() {
		
		
		return "[" + this.getClass().getSimpleName() + " (index=" + (this.start - 1) + ", length=" + this.length + ")]";
	}
	
	@Override
	public final String toCode() {
		
		
		return "RECURSION " + this.length;
	}
}
