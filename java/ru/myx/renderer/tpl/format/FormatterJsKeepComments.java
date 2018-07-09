/*
 * Created on 10.12.2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ru.myx.renderer.tpl.format;

import java.util.StringTokenizer;

import ru.myx.renderer.tpl.parse.Token;
import ru.myx.renderer.tpl.parse.TokenSimple;
import ru.myx.renderer.tpl.parse.Tokens;

final class FormatterJsKeepComments implements Formatter {
	@Override
	public Token format(final Token previous, final int line, final String text, final String whitespace) {
		if (text.length() == 0) {
			return null;
		}
		
		final StringBuilder body = new StringBuilder();
		boolean first = true;
		for (final StringTokenizer tokenizer = new StringTokenizer( text, "\n", true ); tokenizer.hasMoreTokens();) {
			final String current = tokenizer.nextToken();
			final int count = current.length();
			if (count == 1 && current.charAt( 0 ) == '\n') {
				body.append( '\n' );
			} else {
				if (count > 0) {
					if (first) {
						if (tokenizer.hasMoreTokens()) {
							int len = count;
							final int st = 0;
							while (st < len && current.charAt( len - 1 ) <= ' ') {
								len--;
							}
							
							final String string = st > 0 || len < count
									? current.substring( st, len )
									: current;
							body.append( string );
						} else {
							body.append( current );
						}
					} else {
						if (tokenizer.hasMoreTokens()) {
							body.append( current.trim() );
						} else {
							final int len = count;
							int st = 0;
							
							while (st < len && current.charAt( st ) <= ' ') {
								st++;
							}
							// while ((st < len) && (val[off + len - 1] <= ' '))
							// {
							// len--;
							// }
							
							body.append( st > 0 || len < count
									? current.substring( st, len )
									: current );
						}
					}
				}
			}
			first = false;
		}
		
		return new TokenSimple( Tokens.TP_OUTPUT, body.toString(), line, text );
	}
}
