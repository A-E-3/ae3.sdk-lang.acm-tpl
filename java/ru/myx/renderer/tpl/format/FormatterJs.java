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

final class FormatterJs implements Formatter {

	@Override
	public Token format(final Token previous, final int line, final String text, final String whitespace) {

		if (text.length() == 0) {
			return null;
		}
		
		class State {

			public static final int CODE = 0;
			
			public static final int COMMENT_SUSPECT = 1;
			
			public static final int LINE_COMMENT = 2;
			
			public static final int BLOCK_COMMENT = 3;
			
			public static final int BLOCK_COMMENT_SUSPECT = 4;
			
			public static final int STR1 = 5;
			
			public static final int STR1_ESC = 6;
			
			public static final int STR2 = 7;
			
			public static final int STR2_ESC = 8;
		}
		
		final StringBuilder body = new StringBuilder();
		int state = State.CODE;
		for (final StringTokenizer tokenizer = new StringTokenizer(text, "\\\n/'\"*", true); tokenizer.hasMoreTokens();) {
			final String current = tokenizer.nextToken();
			final int count = current.length();
			if (count <= 0) {
				assert count == 0;
				continue;
			}
			if (count == 1) {
				switch (current.charAt(0)) {
					case '\\' :
						switch (state) {
							case State.LINE_COMMENT :
								continue;
							case State.BLOCK_COMMENT :
								continue;
							case State.STR1_ESC :
								state = State.STR1;
								break;
							case State.STR2_ESC :
								state = State.STR2;
								break;
							case State.COMMENT_SUSPECT :
								state = State.CODE;
								body.append('/');
								break;
							case State.BLOCK_COMMENT_SUSPECT :
								state = State.BLOCK_COMMENT;
								continue;
							case State.STR1 :
								state = State.STR1_ESC;
								break;
							case State.STR2 :
								state = State.STR2_ESC;
								break;
							default :
								break;
						}
						body.append('\\');
						continue;
					case '\n' :
						switch (state) {
							case State.LINE_COMMENT :
								state = State.CODE;
								break;
							case State.BLOCK_COMMENT :
								break;
							case State.STR1_ESC :
								state = State.STR1;
								break;
							case State.STR2_ESC :
								state = State.STR2;
								break;
							case State.COMMENT_SUSPECT :
								state = State.CODE;
								body.append('/');
								break;
							case State.BLOCK_COMMENT_SUSPECT :
								state = State.BLOCK_COMMENT;
								break;
							default :
								break;
						}
						body.append('\n');
						continue;
					case '\'' :
						switch (state) {
							case State.LINE_COMMENT :
								continue;
							case State.BLOCK_COMMENT :
								continue;
							case State.STR1_ESC :
								state = State.STR1;
								break;
							case State.STR2_ESC :
								state = State.STR2;
								break;
							case State.COMMENT_SUSPECT :
								state = State.STR1;
								body.append('/');
								break;
							case State.BLOCK_COMMENT_SUSPECT :
								state = State.BLOCK_COMMENT;
								continue;
							case State.CODE :
								state = State.STR1;
								break;
							case State.STR1 :
								state = State.CODE;
								break;
							default :
								break;
						}
						body.append('\'');
						continue;
					case '"' :
						switch (state) {
							case State.LINE_COMMENT :
								continue;
							case State.BLOCK_COMMENT :
								continue;
							case State.STR1_ESC :
								state = State.STR1;
								break;
							case State.STR2_ESC :
								state = State.STR2;
								break;
							case State.COMMENT_SUSPECT :
								state = State.STR2;
								body.append('/');
								break;
							case State.BLOCK_COMMENT_SUSPECT :
								state = State.BLOCK_COMMENT;
								continue;
							case State.CODE :
								state = State.STR2;
								break;
							case State.STR2 :
								state = State.CODE;
								break;
							default :
								break;
						}
						body.append('"');
						continue;
					case '/' :
						switch (state) {
							case State.LINE_COMMENT :
								continue;
							case State.BLOCK_COMMENT :
								continue;
							case State.STR1_ESC :
								state = State.STR1;
								break;
							case State.STR2_ESC :
								state = State.STR2;
								break;
							case State.COMMENT_SUSPECT :
								state = State.LINE_COMMENT;
								continue;
							case State.BLOCK_COMMENT_SUSPECT :
								state = State.CODE;
								continue;
							case State.CODE :
								state = State.COMMENT_SUSPECT;
								/** Not appending at this stage */
								continue;
							default :
								break;
						}
						body.append('/');
						continue;
					case '*' :
						switch (state) {
							case State.LINE_COMMENT :
								continue;
							case State.BLOCK_COMMENT :
								state = State.BLOCK_COMMENT_SUSPECT;
								/** no output */
								continue;
							case State.STR1_ESC :
								state = State.STR1;
								break;
							case State.STR2_ESC :
								state = State.STR2;
								break;
							case State.COMMENT_SUSPECT :
								state = State.BLOCK_COMMENT;
								/** no output */
								continue;
							default :
								break;
						}
						body.append('*');
						continue;
					default :
				}
			}
			switch (state) {
				case State.LINE_COMMENT :
				case State.BLOCK_COMMENT :
					/** won't add anything */
					continue;
				case State.STR1_ESC :
					state = State.STR1;
					body.append(current);
					continue;
				case State.STR2_ESC :
					state = State.STR2;
					body.append(current);
					continue;
				case State.COMMENT_SUSPECT :
					state = State.CODE;
					body.append('/');
					body.append(current.trim());
					continue;
				case State.BLOCK_COMMENT_SUSPECT :
					state = State.BLOCK_COMMENT;
					/** won't add anything */
					continue;
				case State.CODE :
					body.append(current.trim());
					continue;
				case State.STR1 :
				case State.STR2 :
					body.append(current);
					continue;
				default :
			}
		}
		
		return new TokenSimple(Tokens.TP_OUTPUT, body.toString(), line, text);
	}
}
