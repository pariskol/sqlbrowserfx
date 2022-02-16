package gr.sqlbrowserfx;

import gr.sqlbrowserfx.utils.PropertiesLoader;

public class GUIStarter {

	public static void main(String[] args) {
		if (PropertiesLoader.getProperty("sqlbrowserfx.mode", String.class, "advanced").equals("advanced"))
			SqlBrowserFXApp.main(args);
		else
			SqlBrowserFXAppWithoutDocking.main(args);
	}
}
