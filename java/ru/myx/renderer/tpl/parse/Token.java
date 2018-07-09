/**
 * 
 */
package ru.myx.renderer.tpl.parse;

/**
 * @author myx
 * 
 */
public interface Token {
	/**
	 * @param text
	 */
	void concatenate(final String text);
	
	/**
	 * @return string
	 */
	String getSource();
	
	/**
	 * @return string
	 */
	String getSourceOriginal();
	
	/**
	 * @return int
	 */
	int getType();
	
	/**
	 * @return boolean
	 */
	boolean isOutput();
	
	/**
	 * @return boolean
	 */
	boolean isTag();
}
