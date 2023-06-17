package gr.sqlbrowserfx;

import java.util.ArrayList;
import java.util.List;

import gr.sqlbrowserfx.conn.SqlTable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Line;

public final class DbDiagramPane extends BorderPane {

	private int counter = 0;
	private int rows = 0;
	private int[][] grid;
	private static int COLS_NUM = 7;

	private final Pane diagramPane = new Pane();
	private List<SqlTableNode> diagramNodes = new ArrayList<>();
	private List<Line> lines = new ArrayList<>();

	public DbDiagramPane(List<SqlTable> tables) {
		this.grid = new int[(int) Math.ceil(tables.size() / COLS_NUM) + 1][COLS_NUM];
		
		BorderPane.setAlignment(diagramPane, Pos.TOP_LEFT);
		this.setCenter(new ScrollPane(diagramPane));

		final CheckBox dragModeCheckbox = new CheckBox("Drag mode");
		BorderPane.setMargin(dragModeCheckbox, new Insets(6));
		this.setBottom(dragModeCheckbox);

		dragModeActiveProperty.bind(dragModeCheckbox.selectedProperty());

		tables.forEach(table -> {
			final var tableDiagramNode = new SqlTableNode(table);
			final Node node = makeDraggable(tableDiagramNode);

			if (counter == COLS_NUM) {
				rows++;
				counter = 0;
			}
			
			var sizeOfCellAbove = getSizeOfCellsAbove();
			var extraSpace = sizeOfCellAbove * 10;

			node.relocate(20 + counter * 240, 20 + rows * 300 + extraSpace);
			this.grid[rows][counter] = table.getColumns().size();
			tableDiagramNode.setOnMouseEntered(event -> highlightRelatedTableNodes(tableDiagramNode));
			tableDiagramNode.setOnMouseExited(event -> clearHighlight(tableDiagramNode));

			diagramNodes.add(tableDiagramNode);
			diagramPane.getChildren().add(node);
			
			counter++;
		});

	}
	
	// check length of cells above current cell
	private int getSizeOfCellsAbove() {
		var sizeOfCellAbove = 0; 

		var times = rows - 1;
		var i = 0;
		do {
			sizeOfCellAbove += this.grid[i][counter];
			i++;
			times--;
		} while(times > 0);
		
		return sizeOfCellAbove;
	}

	private void highlightRelatedTableNodes(SqlTableNode tableDiagramNode) {
		tableDiagramNode.highlight();
		diagramNodes.stream()
			.filter(node -> tableDiagramNode.getSqlTable().getRelatedTables().contains(node.getSqlTable().getName()))
			.forEach(node -> {
				node.highlight();
				connectNodes(tableDiagramNode, node);
			});
	}
	
	private void clearHighlight(SqlTableNode tableDiagramNode) {
		diagramPane.getChildren().removeAll(lines);
		lines.clear();
		
		tableDiagramNode.unhighlight();
		diagramNodes.stream()
			.filter(node -> tableDiagramNode.getSqlTable().getRelatedTables().contains(node.getSqlTable().getName()))
			.forEach(node -> node.unhighlight());
	}
	
	public void connectNodes(SqlTableNode node1, SqlTableNode node2) {
		var startX = node1.localToScene(node1.getBoundsInLocal()).getMinX() + node1.getWidth() / 2;
		var startY = node1.localToScene(node1.getBoundsInLocal()).getMinY() + node1.getHeight() / 2;
		var endX = node2.localToScene(node2.getBoundsInLocal()).getMinX() + node2.getWidth() / 2;
		var endY = node2.localToScene(node2.getBoundsInLocal()).getMinY() + node2.getHeight() / 2;
		var midX = (startX + endX) / 2;
		var midY = (startY + endY) / 2;

        // Create the line segments
		var lines = new ArrayList<Line>();
        lines.add(new Line(startX, startY, midX, startY));
        lines.add(new Line(midX, startY, midX, midY));
        lines.add(new Line(midX, midY, endX, midY));
        lines.add(new Line(endX, midY, endX, endY));
       
        diagramPane.getChildren().addAll(lines);
        
        lines.forEach(line -> {
        	line.setStyle("""
	    		-fx-stroke: -fx-accent;
				-fx-stroke-width: 3;
        	""");
        	line.toBack();
        });
        
        this.lines.addAll(lines);
	}

	private final BooleanProperty dragModeActiveProperty = new SimpleBooleanProperty(this, "dragModeActive", true);

	private Node makeDraggable(final SqlTableNode node) {
		final DragContext dragContext = new DragContext();
		final Group wrapGroup = new Group(node);

		wrapGroup.addEventFilter(MouseEvent.ANY, mouseEvent -> {
			if (dragModeActiveProperty.get()) {
				// disable mouse events for all children
				mouseEvent.consume();
			}
		});

		wrapGroup.addEventFilter(MouseEvent.MOUSE_PRESSED, mouseEvent -> {
			if (dragModeActiveProperty.get()) {
				// remember initial mouse cursor coordinates
				// and node position
				dragContext.mouseAnchorX = mouseEvent.getX();
				dragContext.mouseAnchorY = mouseEvent.getY();
				dragContext.initialTranslateX = node.getTranslateX();
				dragContext.initialTranslateY = node.getTranslateY();
			}
		});

		wrapGroup.addEventFilter(MouseEvent.MOUSE_DRAGGED, mouseEvent -> {
			if (dragModeActiveProperty.get()) {
				// shift node from its initial position by delta
				// calculated from mouse cursor movement
				node.setTranslateX(dragContext.initialTranslateX + mouseEvent.getX() - dragContext.mouseAnchorX);
				node.setTranslateY(dragContext.initialTranslateY + mouseEvent.getY() - dragContext.mouseAnchorY);
			}
		});

		return wrapGroup;
	}

	private static final class DragContext {
		public double mouseAnchorX;
		public double mouseAnchorY;
		public double initialTranslateX;
		public double initialTranslateY;
	}
}
