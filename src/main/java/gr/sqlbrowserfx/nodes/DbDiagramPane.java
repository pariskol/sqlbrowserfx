package gr.sqlbrowserfx.nodes;

import java.util.ArrayList;
import java.util.List;

import gr.sqlbrowserfx.conn.SqlTable;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Line;

public class DbDiagramPane extends ScrollPane {

	private int counter = 0;
	private int rows = 0;
	private int[][] grid;
	private static final int COLS_NUM = 7;

	private final Pane diagramPane = new Pane();
	private final List<SqlTableNode> diagramNodes = new ArrayList<>();

	public DbDiagramPane() {
	}
	
	public DbDiagramPane(List<SqlTable> tables) {
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

			node.relocate(20 + counter * 240, 20 + rows * 300 + extraSpace);
			this.grid[rows][counter] = table.getColumns().size();
			tableDiagramNode.setOnMouseEntered(event -> highlightRelatedTableNodes(tableDiagramNode));
			tableDiagramNode.setOnMouseExited(event -> clearHighlight(tableDiagramNode));

			diagramNodes.add(tableDiagramNode);
			diagramPane.getChildren().add(node);
			
			counter++;
		});
		
		JavaFXUtils.timer(500, this::connectAllTableNodes);
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

}
