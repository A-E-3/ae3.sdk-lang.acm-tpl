package ru.myx.renderer.tpl;

import ru.myx.ae3.exec.ExecProcess;
import ru.myx.ae3.exec.ExecStateCode;
import ru.myx.ae3.exec.Instruction;

/**
 * TPL TAG impl
 * 
 * @author Alexander I. Kharitchev
 */
final class TagIF implements Instruction {
	
	private Instruction elsePart;
	
	/**
	 * Used only to file 'else' blocks upon compilation
	 */
	TagIF() {
		// empty
	}
	
	@Override
	public final ExecStateCode execCall(final ExecProcess process) {
		
		return process.vmRaise("NOT EXECUTABLE / DUMMY TAG!");
	}
	
	final Instruction getInstructionElse() {
		
		return this.elsePart;
	}
	
	@Override
	public final int getOperandCount() {
		
		return 0;
	}
	
	@Override
	public final int getResultCount() {
		
		return 0;
	}
	
	final void setElse(final Instruction elsePart) {
		
		this.elsePart = elsePart;
	}
	
	@Override
	public final String toCode() {
		
		return "IF-DUMMY-TAG";
	}
}
