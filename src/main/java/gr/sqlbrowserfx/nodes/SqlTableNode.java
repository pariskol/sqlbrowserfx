package gr.sqlbrowserfx.nodes;

import java.util.ArrayList;
import java.util.List;

import org.controlsfx.control.PopOver;
import org.fxmisc.flowless.VirtualizedScrollPane;

import gr.sqlbrowserfx.conn.DbCash;
import gr.sqlbrowserfx.conn.SqlTable;
import gr.sqlbrowserfx.nodes.codeareas.sql.SqlCodeArea;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Line;

public class SqlTableNode extends VBox {

	private final SqlTable table;
	private final String color;
	private final List<Line> lines = new ArrayList<>();

	public SqlTableNode(SqlTable table) {
		// restore this line to have colorful lines
		this.color = "-fx-diagram-color";

		this.table = table;
		this.setMinWidth(150);
		var titleLabel = new Label(table.getName(), JavaFXUtils.createIcon("/icons/table.png"));
		titleLabel.setPadding(new Insets(0, 0, 6, 0));
		titleLabel.setAlignment(Pos.CENTER);
		configureTitleBorder(titleLabel);
		var columnsVbox = new VBox();
		table.getColumns().forEach(col -> {
			if (table.isPrimaryKey(col)) {
				columnsVbox.getChildren()
						.add(new Label(table.getPrimaryKey(), JavaFXUtils.createIcon("/icons/primary-key.png")));
			} else if (table.isForeignKey(col)) {
				columnsVbox.getChildren().add(new Label(col, JavaFXUtils.createIcon("/icons/foreign-key.png")));
			} else {
				columnsVbox.getChildren().add(new Label(col));
			}
		});

		columnsVbox.setPadding(new Insets(6));

		this.getChildren().addAll(titleLabel, columnsVbox);

		titleLabel.prefWidthProperty().bind(columnsVbox.widthProperty());
		configureBorder(this);

		this.setOnMouseClicked(event -> {
			var schema = DbCash.getSchemaFor(table.getName());
			SqlCodeArea codeArea = new SqlCodeArea(schema, false, false, true);
			VirtualizedScrollPane<SqlCodeArea> scrollPane = new VirtualizedScrollPane<>(codeArea);
			scrollPane.setPrefSize(600, 400);

			PopOver popOver = new PopOver(scrollPane);
			popOver.setArrowSize(0);
			popOver.setDetachable(false);
			popOver.show(this);
		});

	}

	public SqlTable getSqlTable() {
		return this.table;
	}

	public String getColor() {
		return this.color;
	}

	public List<Line> getLines() {
		return lines;
	}

	public void highlight() {
		configureSelectedBorder(this);
	}

	public void unhighlight() {
		configureBorder(this);
	}

	private void configureBorder(final Region region) {
		region.setStyle(
//		"-fx-border-color: " + color + ";" + 
		"""
			-fx-background-color: -fx-diagram-color;
			-fx-background-radius: 6;
			-fx-border-width: 1;
			-fx-border-radius: 6;
			-fx-padding: 6 0 6 0;
		""");
	}

	private void configureSelectedBorder(final Region region) {
		region.setStyle("""
					-fx-border-color: -fx-accent;
					-fx-background-color: -fx-diagram-color;
					-fx-background-radius: 6;
					-fx-border-width: 3;
					-fx-border-radius: 6;
					-fx-padding: 6 0 6 0;
				""");
	}

	private void configureTitleBorder(final Region region) {
		region.setStyle("""
					-fx-border-color: -fx-base;
					-fx-border-width: 0 0 1 0;
				""");
	}


}
