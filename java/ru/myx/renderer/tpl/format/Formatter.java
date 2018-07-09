/*
 * Created on 10.12.2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ru.myx.renderer.tpl.format;

import ru.myx.renderer.tpl.parse.Token;

/**
 * @author myx
 * 
 */
public interface Formatter {
	
	/**
     * 
     */
	Formatter	DEFAULT		= new FormatterDefault();
	
	/**
     * 
     */
	Formatter	NO_IDENT	= new FormatterNoIdent();
	
	/**
     * 
     */
	Formatter	JS			= new FormatterJs();
	
	/**
     * 
     */
	Formatter	WIPE_TAGS	= new FormatterWipeTags();
	
	/**
     * 
     */
	Formatter	XML			= new FormatterXml();
	
	/**
	 * @param previous
	 *            - previous token
	 * @param line
	 *            - line number
	 * @param found
	 *            - constant body itself
	 * @param whitespace
	 *            - previous whitespace
	 * @return result
	 */
	Token format(final Token previous, final int line, final String found, final String whitespace);
}
