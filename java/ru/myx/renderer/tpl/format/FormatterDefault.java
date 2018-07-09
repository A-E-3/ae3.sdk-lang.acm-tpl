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

final class FormatterDefault implements Formatter {
	@Override
	public Token format(final Token previous, final int line, final String text, final String whitespace) {
		if (text.length() == 0) {
			return null;
		}
		if (previous != null && previous.isOutput()) {
			if (whitespace != null) {
				final String prev = previous.getSourceOriginal();
				if (prev.length() > 0
						&& !Character.isJavaIdentifierPart( prev.charAt( prev.length() - 1 ) )
						|| text.length() > 0
						&& !Character.isJavaIdentifierPart( text.charAt( 0 ) )) {
					previous.concatenate( whitespace );
				}
			}
			previous.concatenate( text );
			return null;
		}
		return new TokenSimple( Tokens.TP_OUTPUT, whitespace == null
				|| text.length() > 0
				&& Character.isJavaIdentifierPart( text.charAt( 0 ) )
				? text
				: whitespace + text, line, text );
	}
}
