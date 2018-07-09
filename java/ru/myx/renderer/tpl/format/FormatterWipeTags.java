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

final class FormatterWipeTags implements Formatter {
	@Override
	public Token format(final Token previous, final int line, final String text, final String whitespace) {
		if (text.length() == 0) {
			return null;
		}
		final boolean wsStart = Character.isWhitespace( text.charAt( 0 ) );
		final boolean wsEnd = Character.isWhitespace( text.charAt( text.length() - 1 ) );
		final String body = wsStart
				? wsEnd
						? ' ' + text.trim() + ' '
						: ' ' + text.trim()
				: wsEnd
						? text.trim() + ' '
						: text;
		
		return new TokenSimple( Tokens.TP_OUTPUT, body, line, text );
	}
}
