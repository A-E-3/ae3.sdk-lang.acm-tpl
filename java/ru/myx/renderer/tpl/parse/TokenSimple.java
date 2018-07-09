/**
 * 
 */
package ru.myx.renderer.tpl.parse;

/**
 * @author myx
 * 
 */
public final class TokenSimple implements Token {
	final int			type;
	
	private String		source;
	
	private String		original;
	
	private final int	line;
	
	/**
	 * @param type
	 * @param source
	 * @param line
	 * @param original
	 */
	public TokenSimple(final int type, final String source, final int line, final String original) {
		this.type = type;
		this.source = source;
		this.line = line;
		this.original = original;
	}
	
	@Override
	public void concatenate(final String text) {
		this.source += text;
		this.original = this.source;
	}
	
	@Override
	public String getSource() {
		return this.source;
	}
	
	@Override
	public String getSourceOriginal() {
		return this.original;
	}
	
	@Override
	public int getType() {
		return this.type;
	}
	
	@Override
	public boolean isOutput() {
		return this.type == Tokens.TP_OUTPUT;
	}
	
	@Override
	public boolean isTag() {
		return this.type == Tokens.TP_TAG;
	}
	
	@Override
	public String toString() {
		return "line: " + this.line;
	}
}
