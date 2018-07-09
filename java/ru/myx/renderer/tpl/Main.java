package ru.myx.renderer.tpl;

import ru.myx.ae3.eval.Evaluate;
import ru.myx.ae3.produce.Produce;
import ru.myx.tpl.rt3.AcmTplPluginFactory;

/*
 * Created on 07.10.2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
/**
 * @author myx
 *
 *         To change the template for this generated type comment go to
 *         Window>Preferences>Java>Code Generation>Code and Comments
 */
public final class Main {

	/**
	 * @param args
	 */
	public static final void main(final String[] args) {
		
		System.out.println("BOOT: plugin: ACM [ACM.TPL] is being initialized...");
		Produce.registerFactory(new AcmTplPluginFactory());
		Evaluate.registerLanguage(AcmTplLanguageImpl.INSTANCE);
		System.out.println("BOOT: plugin: ACM [ACM.TPL] done.");
	}
}
