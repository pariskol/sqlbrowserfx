package gr.sqlbrowserfx.nodes;

import java.util.ArrayList;
import java.util.List;

import gr.sqlbrowserfx.conn.SqlTable;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Line;

public class DbDiagramPane extends ScrollPane implements ContextMenuOwner {

	private int counter = 0;
	private int rows = 0;
	private int[][] grid;
	private static final int COLS_NUM = 7;

	private final Pane diagramPane = new Pane();
	private final List<SqlTableNode> diagramNodes = new ArrayList<>();
	private SqlTableNode selectedDiagramNode = null;
	private ContextMenu contextMenu = createContextMenu();

	public DbDiagramPane() {
		setOnMouseClicked(mouseEvent -> {
			// mouse event handler is used instead of setContextMenu() because setContextMenu() seems to stop event propagation to cells
			contextMenu.hide();
			if (mouseEvent.getButton() == MouseButton.SECONDARY) {
		        contextMenu.show(this, mouseEvent.getScreenX(), mouseEvent.getScreenY());
		        return;
		    }
		});
	}
	
	public DbDiagramPane(List<SqlTable> tables) {
		this();
		init(tables);
	}
	
	public void setLoading(boolean loading) {
		if (loading) {
			ProgressIndicator progressIndicator = new ProgressIndicator();
			progressIndicator.setMaxHeight(40);
			progressIndicator.setMaxWidth(40);
			this.setContent(progressIndicator);
		}
		else {
			Platform.runLater(() -> this.setContent(this.diagramPane));
		}
	}
	
	public void init(List<SqlTable> tables) {
	    this.grid = new int[(int) Math.ceil(tables.size() / COLS_NUM) + 1][COLS_NUM];
	    this.setContent(diagramPane);

	    tables.forEach(table -> {
	        final var tableDiagramNode = new SqlTableNode(table);
	        var node = new Group(tableDiagramNode);

	        if (counter == COLS_NUM) {
	            rows++;
	            counter = 0;
	        }

	        var sizeOfCellAbove = getSizeOfCellsAbove();
	        var extraSpace = sizeOfCellAbove * 15;

	        // Initial position
	        double x = 20 + counter * 240;
	        double y = 20 + rows * 300 + extraSpace;

	        // Adjust position to avoid overlap
	        var adjustedPosition = adjustPositionToAvoidOverlap(x, y, tableDiagramNode);
	        x = adjustedPosition[0];
	        y = adjustedPosition[1];

	        node.relocate(x, y);
	        this.grid[rows][counter] = table.getColumns().size();

	        tableDiagramNode.setOnMouseClicked(event -> {
	        	highlightRelatedTableNodes(tableDiagramNode);
	        	selectedDiagramNode = tableDiagramNode;
	        });

	        diagramNodes.add(tableDiagramNode);
	        diagramPane.getChildren().add(node);

	        counter++;
	    });

	    JavaFXUtils.timer(500, this::connectAllTableNodes);
	}

	// Method to adjust node positions to avoid overlap
	private double[] adjustPositionToAvoidOverlap(double x, double y, SqlTableNode newNode) {
	    double newX = x;
	    double newY = y;
	    boolean overlaps;

	    do {
	        overlaps = false;

	        for (SqlTableNode existingNode : diagramNodes) {
	            if (nodesOverlap(newX, newY, newNode, existingNode)) {
	                // If overlap detected, shift the new node's position
	                overlaps = true;
	                newX += 50; // Shift by 50px horizontally
	                if (newX > diagramPane.getWidth() - 200) {
	                    // Wrap to a new row if reaching diagram pane boundary
	                    newX = 20;
	                    newY += 300;
	                }
	                break;
	            }
	        }
	    } while (overlaps);

	    return new double[]{newX, newY};
	}

	// Method to check if two nodes overlap
	private boolean nodesOverlap(double x, double y, SqlTableNode newNode, SqlTableNode existingNode) {
	    double newWidth = newNode.getWidth();
	    double newHeight = newNode.getHeight();

	    double existingX = existingNode.getLayoutX();
	    double existingY = existingNode.getLayoutY();
	    double existingWidth = existingNode.getWidth();
	    double existingHeight = existingNode.getHeight();

	    return x < existingX + existingWidth &&
	           x + newWidth > existingX &&
	           y < existingY + existingHeight &&
	           y + newHeight > existingY;
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
		clearAllHighligts();
		
		tableDiagramNode.highlight();
		diagramNodes.stream()
			.filter(node -> tableDiagramNode.getSqlTable().getRelatedTables().contains(node.getSqlTable().getName()))
			.forEach(SqlTableNode::highlight);
		
		tableDiagramNode.getLines().forEach(line -> {
			line.setStyle(
				"""
					-fx-stroke: -fx-accent;
					-fx-stroke-width: 3;
	        	"""
			);
		});
	}
	
	private void clearAllHighligts() {
		diagramNodes.forEach(diagramNode -> clearHighlight(diagramNode));
	}
	
	private void clearHighlight(SqlTableNode tableDiagramNode) {
		tableDiagramNode.unhighlight();
		tableDiagramNode.getLines().forEach(line -> line.setStyle("-fx-stroke: " + tableDiagramNode.getColor() + ";" + "-fx-stroke-width: 1;"));
		diagramNodes.stream()
			.filter(node -> tableDiagramNode.getSqlTable().getRelatedTables().contains(node.getSqlTable().getName()))
			.forEach(SqlTableNode::unhighlight);
	}
	
	private void connectAllTableNodes() {
		diagramNodes
			.forEach(targetNode -> {
				diagramPane.getChildren().removeAll(targetNode.getLines());
				targetNode.getLines().clear();
				diagramNodes.stream()
				.filter(node -> targetNode.getSqlTable().getRelatedTables().contains(node.getSqlTable().getName()))
				.forEach(node -> targetNode.getLines().addAll(connectNodes(targetNode, node, targetNode.getColor())));
			});
	}
	
	public List<Line> connectNodes(SqlTableNode node1, SqlTableNode node2, String lineColor) {
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
    		line.setStyle("-fx-stroke: " + lineColor + ";");
        	line.toBack();
        });
        
        return lines;
	}

	@Override
	public ContextMenu createContextMenu() {
		var showSchema = new MenuItem("Show schema");
		showSchema.setOnAction(event -> {
			if (selectedDiagramNode != null) {
				selectedDiagramNode.showSchemaPopup();
			}
		});
		return new ContextMenu(showSchema);
	}

}
