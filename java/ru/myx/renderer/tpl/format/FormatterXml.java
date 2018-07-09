/*
 * Created on 10.12.2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ru.myx.renderer.tpl.format;

import ru.myx.renderer.tpl.parse.Token;
import ru.myx.renderer.tpl.parse.TokenSimple;
import ru.myx.renderer.tpl.parse.Tokens;

final class FormatterXml implements Formatter {
	private static final int	ST_TEXT			= 0;
	
	private static final int	ST_TAG			= 1;
	
	private static final int	ST_ATTRIBUTE1	= 2;
	
	private static final int	ST_ATTRIBUTE2	= 3;
	
	private static final int	ST_WSPACE		= 4;
	
	private static final int	ST_TWSPACE		= 5;
	
	@Override
	public Token format(final Token previous, final int line, final String text, final String whitespace) {
		final int length = text.length();
		if (length == 0) {
			return null;
		}
		final StringBuilder result = new StringBuilder( length );
		int state = FormatterXml.ST_TEXT;
		for (int i = 0; i < length; ++i) {
			final char c = text.charAt( i );
			switch (state) {
			case FormatterXml.ST_TEXT: {
				if (c == '<') {
					result.append( c );
					state = FormatterXml.ST_TAG;
				} else //
				if (Character.isWhitespace( c )) {
					state = FormatterXml.ST_WSPACE;
				} else {
					result.append( c );
				}
			}
				break;
			case FormatterXml.ST_TAG: {
				if (c == '\'') {
					result.append( c );
					state = FormatterXml.ST_ATTRIBUTE1;
				} else //
				if (c == '"') {
					result.append( c );
					state = FormatterXml.ST_ATTRIBUTE2;
				} else //
				if (c == '>') {
					result.append( c );
					state = FormatterXml.ST_TEXT;
				} else //
				if (Character.isWhitespace( c )) {
					state = FormatterXml.ST_TWSPACE;
				} else {
					result.append( c );
				}
			}
				break;
			case FormatterXml.ST_ATTRIBUTE1: {
				result.append( c );
				if (c == '\'') {
					state = FormatterXml.ST_TAG;
				}
			}
				break;
			case FormatterXml.ST_ATTRIBUTE2: {
				result.append( c );
				if (c == '"') {
					state = FormatterXml.ST_TAG;
				}
			}
				break;
			case FormatterXml.ST_WSPACE: {
				if (!Character.isWhitespace( c )) {
					result.append( ' ' ).append( c );
					state = FormatterXml.ST_TEXT;
				}
			}
				break;
			case FormatterXml.ST_TWSPACE: {
				if (!Character.isWhitespace( c )) {
					result.append( ' ' ).append( c );
					state = FormatterXml.ST_TAG;
				}
			}
				break;
			default:
				throw new IllegalArgumentException( "Unknown tokenizer state: " + state );
			}
		}
		if (state == FormatterXml.ST_WSPACE || state == FormatterXml.ST_TWSPACE) {
			result.append( ' ' );
		}
		
		if (text.length() == 0) {
			return null;
		}
		if (previous != null && previous.isOutput()) {
			previous.concatenate( result.toString() );
			return null;
		}
		return new TokenSimple( Tokens.TP_OUTPUT, result.toString(), line, text );
	}
}
