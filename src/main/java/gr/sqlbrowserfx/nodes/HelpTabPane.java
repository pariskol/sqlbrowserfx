package gr.sqlbrowserfx.nodes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import gr.sqlbrowserfx.LoggerConf;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;

public class HelpTabPane extends TabPane{

	public HelpTabPane() {
		super();
		String desc = ""
				+ "SqlBrowserFX is a feature rich cross platform sql client for SQLite , MySQL for both windows and linux \n"
				+ "\n"
				+ "\n"
				+ "### Features\n"
				+ "\n"
				+ "* Manage data (insert, update, delete) via gui.\n"
				+ "* Execute raw sql queries.\n"
				+ "* Editor for sql with syntax highlighting, autocomplete features.\n"
				+ "* Adjustable responsive ui.\n"
				+ "* Graphical representation of database as tree.\n"
				+ "* Exposure of database to the web as RESTful service with one click.\n"
				+ "* Import, export csv files.\n"
				+ "* Queries History.\n"
				+ "* Savable queries.\n"
				+ "* Support for SQLite.\n"
				+ "* Support for MySQL.\n"
				+ "* Cross Platform.\n"
				+ "\n"
				+ "\n"
				+ "### Prerequisites\n"
				+ "\n"
				+ "* JDK 8 with JavaFX like oracle jdk 8 or zulufx 8.\n"
				+ "* MySQL server for usage with mysql.\n"
				+ "\n"
				+ "\n"
				+ "\n"
				+ "## Awesome projects used\n"
				+ "\n"
				+ "* [DockFX](https://github.com/RobertBColton/DockFX) - The docking framework used (a moded version actually).\n"
				+ "* [RichTextFΧ](https://github.com/FXMisc/RichTextFX) - Library which provides editor with syntax highlighting feature.\n"
				+ "* [ControlsFX](https://github.com/controlsfx/controlsfx) - Library which provides many useful custom gui components.\n"
				+ "* [Spark Java](https://github.com/perwendel/spark)  - The web framework used.\n"
				+ "\n"
				+ "\n"
				+ "\n";
		String license = "MIT License\n"
				+ "\n"
				+ "Copyright (c) 2019 Paris Kolovos\n"
				+ "\n"
				+ "Permission is hereby granted, free of charge, to any person obtaining a copy\n"
				+ "of this software and associated documentation files (the \"Software\"), to deal\n"
				+ "in the Software without restriction, including without limitation the rights\n"
				+ "to use, copy, modify, merge, publish, distribute, sublicense, and/or sell\n"
				+ "copies of the Software, and to permit persons to whom the Software is\n"
				+ "furnished to do so, subject to the following conditions:\n"
				+ "\n"
				+ "The above copyright notice and this permission notice shall be included in all\n"
				+ "copies or substantial portions of the Software.\n"
				+ "\n"
				+ "THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR\n"
				+ "IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,\n"
				+ "FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE\n"
				+ "AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER\n"
				+ "LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,\n"
				+ "OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE\n"
				+ "SOFTWARE.";
		
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
		TextArea restTextArea = new TextArea("SqlBrowserFX provides a simple rest api for fast prototyping\n\n"
										   + "Available Endpoints:\n\n"
										   + "/tables (get db tables)\n"
										   + "/get/:table?column1=...&column2=... (with parameters matching table columns)\n"
										   + "/save/:table (with json body with keys matching table columns)\n"
										   + "/delete/:table (with json body with keys matching table columns)\n");
		restTextArea.setEditable(false);
		tab = new Tab("Rest Api", restTextArea);
		tab.setClosable(false);
		this.getTabs().add(tab);
		tab = new Tab("License", licenseTextArea);
		tab.setClosable(false);
		this.getTabs().add(tab);
		
		TextArea contactTextArea = new TextArea(
				"GitHub Page: https://github.com/pariskol/sqlbrowserfx\n\n" +
			    "Email: pariskolovos@live.com");
		contactTextArea.setEditable(false);
		tab = new Tab("Contact", contactTextArea);
		tab.setClosable(false);
		this.getTabs().add(tab);
		
		TextArea propsArea;
		try {
			String propsStr = StringUtils.join(Files.lines(Paths.get("./sqlbrowserfx.properties")).collect(Collectors.toList()), "\n");
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
