package gr.sqlbrowserfx;

public class GUIStarter {

	public static void main(String[] args) {
		if (System.getProperty("sqlbrowserfx.mode", "advanced").equals("advanced"))
			SqlBrowserFXApp.main(args);
		else
			SqlBrowserFXAppWithoutDocking.main(args);
	}
}
