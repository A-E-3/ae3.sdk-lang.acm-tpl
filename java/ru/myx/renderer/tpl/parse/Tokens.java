/*
 * Created on 16.06.2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ru.myx.renderer.tpl.parse;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import ru.myx.ae3.Engine;
import ru.myx.ae3.report.Report;
import ru.myx.renderer.tpl.format.Formatter;

/**
 * @author myx
 * 
 *         To change the template for this generated type comment go to
 *         Window>Preferences>Java>Code Generation>Code and Comments
 */
public class Tokens {
	private static final boolean	KEEP_FORMATTING	= Report.MODE_DEBUG || Engine.GROUP_DEVEL;
	
	/**
	 * 
	 */
	public static final int			TP_OUTPUT		= 0;
	
	/**
	 * 
	 */
	public static final int			TP_TAG			= 1;
	
	/**
	 * @param tokens
	 * @param start
	 * @param tagOpener
	 * @param tagSource
	 * @return
	 */
	public static final int findClosing(
			final Token[] tokens,
			final int start,
			final String tagOpener,
			final String tagSource) {
		int level = 1;
		for (int i = start; i < tokens.length; ++i) {
			final Token current = tokens[i];
			if (current.isOutput()) {
				continue;
			}
			if (current.getSource().equals( tagSource ) && --level == 0) {
				return i;
			}
			if (current.getSource().startsWith( tagOpener )) {
				level++;
			}
		}
		return -1;
	}
	
	/**
	 * @param source
	 * @return
	 */
	public static final Token[] parse(final String source) {
		final List<Token> tokens = new ArrayList<>( 16 );
		final int length = source.length();
		Token previous = null;
		boolean searchOpen = true;
		int found = 0;
		int line = 1;
		String whitespace = null;
		Formatter formatter = Formatter.DEFAULT;
		Stack<Formatter> formatters = null;
		for (;;) {
			if (searchOpen) {
				final int pos;
				{
					int position = -1;
					int lines = 0;
					for (int i = found; i < length; ++i) {
						final char c = source.charAt( i );
						if (c == '\n') {
							lines++;
						}
						if (c == '<' && source.regionMatches( i, "<%", 0, 2 )) {
							position = i;
							line += lines;
							break;
						}
					}
					pos = position;
				}
				final String text = pos == -1
						? source.substring( found )
						: source.substring( found, pos );
				if (text.length() > 0) {
					final Token token = formatter.format( previous, line, text, whitespace );
					if (token != null) {
						tokens.add( token );
						previous = token;
						whitespace = null;
					}
				}
				if (pos == -1) {
					break;
				}
				found = pos + 2;
				searchOpen = false;
			} else {
				final int pos;
				{
					int position = -1;
					int lines = 0;
					for (int i = found; i < length; ++i) {
						final char c = source.charAt( i );
						if (c == '\n') {
							lines++;
						}
						if (c == '%' && source.regionMatches( i, "%>", 0, 2 )) {
							position = i;
							line += lines;
							break;
						}
					}
					pos = position;
				}
				if (pos == -1) {
					if (source.substring( found ).replace( '\r', ' ' ).replace( '\n', ' ' ).replace( '\t', ' ' ).trim()
							.length() > 0) {
						throw new IllegalStateException( "Tag is not closed!" );
					}
					Report.warning( "TPL-PARSER", "Script ends with an unclosed tpl empty tag!" );
					break;
				}
				final String original = source.substring( found, pos );
				final String text = original.trim();
				if (text.length() > 0) {
					whitespace = null;
					final char first = text.charAt( 0 );
					if (first == 'C' && text.startsWith( "CODE:" )) {
						final Token token = new TokenSimple( Tokens.TP_TAG, text, line, source.substring( found - 2,
								pos + 2 ) );
						tokens.add( token );
						previous = token;
						final int closing;
						{
							int position = -1;
							int lines = 0;
							for (int i = pos; i < length; ++i) {
								final char c = source.charAt( i );
								if (c == '\n') {
									lines++;
								}
								if (c == '<' && source.regionMatches( i, "<%/CODE", 0, 7 )) {
									position = i;
									line += lines;
									break;
								}
							}
							closing = position;
						}
						if (closing == -1) {
							throw new IllegalStateException( "Can't find closing tag for CODE!" );
						}
						final String code = source.substring( pos + 2, closing );
						final Token codeToken = Formatter.DEFAULT.format( token, line, code, whitespace );
						if (codeToken != null) {
							tokens.add( codeToken );
							previous = codeToken;
						}
						found = closing;
						searchOpen = true;
						continue;
					} else //
					if (first == 'F' && text.startsWith( "FORMAT:" )) {
						final String format = text.substring( 7 ).trim();
						final Formatter replacement;
						if (format.equals( "'default'" )) {
							replacement = Formatter.DEFAULT;
						} else //
						if (format.equals( "'no_tags'" )) {
							replacement = Formatter.WIPE_TAGS;
						} else //
						if (format.equals( "'no_ident'" )) {
							replacement = Formatter.NO_IDENT;
						} else //
						if (format.equals( "'js'" ) || format.equals( "'css'" )) {
							replacement = Formatter.JS;
						} else //
						if (format.equals( "'xml'" ) || format.equals( "'html'" )) {
							replacement = Formatter.XML;
						} else {
							throw new IllegalArgumentException( "Unknown format: " + format );
						}
						if (formatters == null) {
							formatters = new Stack<>();
						}
						formatters.push( formatter );
						formatter = replacement;
					} else //
					if (first == '/' && text.equals( "/FORMAT" )) {
						if (formatters == null || formatters.isEmpty()) {
							throw new IllegalStateException( "Format closing tag found but nothing to close!" );
						}
						formatter = formatters.pop();
					} else {
						final Token token = new TokenSimple( Tokens.TP_TAG, text, line, source.substring( found - 2,
								pos + 2 ) );
						tokens.add( token );
						previous = token;
					}
				} else {
					if (Tokens.KEEP_FORMATTING && found != pos) {
						final int cr = original.lastIndexOf( '\n' );
						whitespace = cr <= 0
								? ""
								: original.substring( cr ); // CR
					}
				}
				found = pos + 2;
				searchOpen = true;
			}
		}
		return tokens.toArray( new Token[tokens.size()] );
	}
}
