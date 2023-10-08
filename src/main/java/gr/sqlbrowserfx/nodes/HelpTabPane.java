package gr.sqlbrowserfx.nodes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import gr.sqlbrowserfx.LoggerConf;
import gr.sqlbrowserfx.utils.HttpClient;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;

public class HelpTabPane extends TabPane{

	public HelpTabPane() throws IOException {
		super();
		String desc = HttpClient.GET("https://raw.githubusercontent.com/pariskol/sqlbrowserfx/master/README.md");
		String license = HttpClient.GET("https://raw.githubusercontent.com/pariskol/sqlbrowserfx/master/LICENSE");
		
		TextArea descTextArea = new TextArea(desc);
		descTextArea.setEditable(false);
		descTextArea.setWrapText(true);
		
		TextArea licenseTextArea = new TextArea(license);
		licenseTextArea.setEditable(false);
		Tab tab = new Tab("Description", descTextArea);
		tab.setClosable(false);
		this.getTabs().add(tab);
		tab = new Tab("Shortcuts", new HelpShortcutsTabPane());
		tab.setClosable(false);
		this.getTabs().add(tab);
		TextArea restTextArea = new TextArea("""
                SqlBrowserFX provides a simple rest api for fast prototyping

                Available Endpoints:

                /api/tables (get db tables)
                /api/get/:table?column1=...&column2=... (with parameters matching table columns)
                /api/save/:table (with json body with keys matching table columns)
                /api/delete/:table (with json body with keys matching table columns)
                """);
		restTextArea.setEditable(false);
		tab = new Tab("Rest Api", restTextArea);
		tab.setClosable(false);
		this.getTabs().add(tab);
		tab = new Tab("License", licenseTextArea);
		tab.setClosable(false);
		this.getTabs().add(tab);
		
		TextArea contactTextArea = new TextArea(
                """
                        GitHub Page: https://github.com/pariskol/sqlbrowserfx

                        Email: pariskolovos@live.com""");
		contactTextArea.setEditable(false);
		tab = new Tab("Contact", contactTextArea);
		tab.setClosable(false);
		this.getTabs().add(tab);
		
		TextArea propsArea;
		try(var lines = Files.lines(Paths.get("./sqlbrowserfx.properties"))) {
			String propsStr = StringUtils.join(lines.collect(Collectors.toList()), "\n");
			propsArea = new TextArea(propsStr);
			propsArea.setEditable(false);
			tab = new Tab("Properties", propsArea);
			tab.setClosable(false);
			this.getTabs().add(tab);
		} catch (IOException e) {
			LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).error(e.getMessage());
		}
		
	}
	
}
