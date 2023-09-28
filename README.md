# SqlBrowserFX

SqlBrowserFX is a feature rich cross platform sql client for SQLite , MySQL, MariaDB and PostgreSQL (partially supported). 

![](images/sqlbrowserfx.png)

### Features

* Manage data (insert, update, delete) via gui.
* Execute raw sql queries.
* Editor for sql with syntax highlighting, autocomplete, refactoring features.
* Support for sql, java files editing via drag and drop.
* Adjustable responsive ui supporting multiple code areas and tables opened at the same time.
* Graphical representation of database as tree.
* Graphical representation of database as diagram.
* Exposure of database to the web as RESTful service with one click.
* Import, export csv files.
* Queries History.
* Savable queries.
* Support for SQLite.
* Support for MySQL, MariaDB.
* Partial Support for PostgreSQL.
* Cross Platform.
* Css themable (Dark, Light etc).
* Generation of simple database tables diagram.

### Prerequisites

* Java 21 +
* Installation of desired database.

### Installing

Copy sqlbrowser-for-build.db to sqlbrowser.db.
Import the project to your favorite ide as maven project and run GUIStarter class.
You can also run install.sh script , if you are using linux in order to install sqlbrowserfx as cli command
'sqlfx'.

### Compatibility with older versions of java
* Tag 2.5.0 -> Java 17
* Tag 1.5.0 -> Java 8


### Build standalone app

Run build.sh script, this will generate all files needed in 'dist' folder.
Run SqlBrowserFX.exe for Windows, or run sqlbrowserfx.sh for Linux.


## Awesome projects used

* [DockFX](https://github.com/RobertBColton/DockFX) - The docking framework used (a moded version actually).
* [RichTextFΧ](https://github.com/FXMisc/RichTextFX) - Library which provides editor with syntax highlighting feature.
* [ControlsFX](https://github.com/controlsfx/controlsfx) - Library which provides many useful custom gui components.
* [Spark Java](https://github.com/perwendel/spark)  - The web framework used. (Until version 1.5.0)
* [Javalin](https://github.com/tipsy/javalin) - The NEW web framework used.
* [Icons8](https://icons8.com/) - The icons used.





