package gr.paris.dock.nodes;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.controlsfx.control.PopOver;
import org.dockfx.DockNode;
import org.dockfx.DockPos;
import org.slf4j.LoggerFactory;

import gr.paris.dock.Dockable;
import gr.paris.nodes.LineChartBox;
import gr.sqlfx.conn.SqlConnector;
import gr.sqlfx.factories.DialogFactory;
import gr.sqlfx.listeners.SimpleChangeListener;
import gr.sqlfx.listeners.SimpleObservable;
import gr.sqlfx.sqlPane.SqlPane;
import gr.sqlfx.sqlTableView.EditBox;
import gr.sqlfx.sqlTableView.SqlTableRow;
import gr.sqlfx.utils.JavaFXUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Orientation;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class DSqlPane extends SqlPane implements Dockable, SimpleChangeListener<String>, SimpleObservable<String> {

	private Button chartButton;
	private Button sqlConsoleButton;
	private Button logButton;
	private ComboBox<String> columnsBox;
	private ComboBox<String> nameColumnsBox;
	List<SimpleChangeListener<String>> listeners;
	List<PieChart> charts;
	List<String> chartColumns;
	List<DockNode> chartsDNs;
	List<LineChartBox> lineChartBoxes;

	private DockNode thisDockNode = null;
	private Button lineChartButton;
	private DSqlConsoleBox sqlConsoleBox;
	private Button showChartButton;
	private List<String> columnNames;

	public DSqlPane() {
		super();
	}

	public DSqlPane(SqlConnector connector) {
		super(connector);

		listeners = new ArrayList<>();
		charts = new ArrayList<>();
		chartsDNs = new ArrayList<>();
		chartColumns = new ArrayList<>();
		lineChartBoxes = new ArrayList<>();

		thisDockNode = new DockNode(this, "Empty", JavaFXUtils.icon("/res/database.png"));
		thisDockNode.setOnClose(() -> {
			chartsDNs.forEach(chart -> chart.close());
			chartsDNs.clear();
			charts.clear();
		});

		this.getSqlTableView().setColumnWidth(1, 100, 300);
		this.getSqlTableView().setOnKeyPressed(keyEvent -> {
			if (keyEvent.isControlDown()) {
				switch (keyEvent.getCode()) {
				case F:
					searchButton.getOnAction().handle(new ActionEvent());
					break;
				case C:
					this.copyAction();
					break;
				case D:
					this.deleteButtonAction();
					break;
				case E:
					this.editButtonAction(simulateClickEvent(contextMenu));;
					break;
				case Q:
					this.addButtonAction();
					break;
				case I:
					this.importCsvAction();
					break;
				case R:
					this.refreshButtonAction();
					break;
				case T:
					this.sqlConsoleButtonAction();
				default:
					break;
				}
			}
			sqlTableView.requestFocus();
		});

		nameColumnsBox = new ComboBox<>();
		columnsBox = new ComboBox<>();
		showChartButton = new Button("Show", JavaFXUtils.icon("/res/chart-pie.png"));
		showChartButton.setOnAction(event -> {
			PieChart chart = new PieChart();
			chart.setClockwise(true);
//		    chart.setLegendSide(Side.LEFT);
			chart.setTitle(columnsBox.getSelectionModel().getSelectedItem());
			chart.setData(this.getPieChartData(chart.getTitle(), nameColumnsBox.getSelectionModel().getSelectedItem()));

			charts.add(chart);
			chartColumns.add(nameColumnsBox.getSelectionModel().getSelectedItem());

			DockNode chartDockNode = new DockNode(chart, "Chart", JavaFXUtils.icon("/res/chart-pie.png"));
			chartDockNode.setPrefSize(100, 100);
			chartDockNode.dock(this.asDockNode().getDockPane(), DockPos.RIGHT, thisDockNode);
			chartDockNode.setOnClose(() -> {
				charts.remove(chart);
				chartColumns.remove(nameColumnsBox.getSelectionModel().getSelectedItem());
				chartsDNs.remove(chartDockNode);
			});
			chartsDNs.add(chartDockNode);
		});

		chartButton = new Button("", JavaFXUtils.icon("/res/chart-pie.png"));
		chartButton.setOnMouseClicked(mouseEvent -> {

			if (chartButton.isFocused() && this.getPopOver().isShowing() || this.getTablesBox() == null
					|| this.getTablesBox().getSelectionModel().isEmpty())
				return;

			chartButton.requestFocus();
			PopOver popOver = new PopOver(new VBox(new Label("Select display column"), nameColumnsBox,
					new Label("Select plot column"), columnsBox, showChartButton));
			popOver.setDetachable(false);

			this.setPopOver(popOver);
			this.getPopOver().show(chartButton);
		});

		Button showLineChartButton = new Button("Show", JavaFXUtils.icon("/res/chart.png"));
		showLineChartButton.setOnMouseClicked(actionEvent -> {
			ObservableList<XYChart.Series<String, Number>> data =
		            FXCollections.<XYChart.Series<String, Number>>observableArrayList();
		        
			for (LineChartBox box : lineChartBoxes) {
				XYChart.Series<String, Number> series = new XYChart.Series<String, Number>();
				series.setName(box.getValue());
				String tableName = this.getSqlTableView().getTableName();
				sqlConnector.getExecutorService().execute(() -> {
					series.setData(this.getLineChartData(box, tableName));
				});
				data.add(series);
			}
			

			LineChart<String, Number> lineChart = new LineChart<>(new CategoryAxis(), new NumberAxis());
			lineChart.setCreateSymbols(false); 
			lineChart.setData(data);
			
			Platform.runLater(() -> {
				DockNode lineDockNode = new DockNode(lineChart);
				lineDockNode.dock(thisDockNode.getDockPane(), DockPos.RIGHT,thisDockNode);
			});
		});

		lineChartButton = new Button("", JavaFXUtils.icon("/res/chart.png"));
		lineChartButton.setOnMouseClicked(mouseEvent -> {

			if (lineChartButton.isFocused() && this.getPopOver().isShowing() || this.getTablesBox() == null
					|| this.getTablesBox().getSelectionModel().isEmpty())
				return;
	
			lineChartBoxes.clear();
			lineChartButton.requestFocus();
			PopOver popOver = new PopOver();
			LineChartBox contentBox = new LineChartBox(columnNames);
			lineChartBoxes.add(contentBox);
			HBox hbox = new HBox(contentBox);
			Button addBoxButton = new Button("", JavaFXUtils.icon("/res/add.png"));
			addBoxButton.setOnAction(actionEvent -> {
				LineChartBox tContentBox = new LineChartBox(columnNames);
				hbox.getChildren().addAll(tContentBox);
				lineChartBoxes.add(tContentBox);
			});
			contentBox.getChildren().addAll(showLineChartButton, addBoxButton);
			popOver.setContentNode(hbox);
			popOver.setDetachable(false);

			this.setPopOver(popOver);
			this.getPopOver().show(lineChartButton);
		});

		sqlConsoleButton = new Button("", JavaFXUtils.icon("/res/console.png"));
		sqlConsoleButton.setOnMouseClicked(mouseEvent -> this.sqlConsoleButtonAction());

		logButton = new Button("", JavaFXUtils.icon("/res/monitor.png"));
		logButton.setOnAction(actionEvent -> {
			logListView = new ListView<>();
			uiLogging = true;
			DockNode dockNode = new DockNode(logListView, "Log", JavaFXUtils.icon("/res/monitor.png"));
			dockNode.setOnClose(() -> {
				logListView = null;
				uiLogging = false;
			});
			dockNode.dock(thisDockNode.getDockPane(), DockPos.RIGHT, thisDockNode);
		});
		this.getToolBar().getChildren().addAll(chartButton, lineChartButton, sqlConsoleButton, logButton);
	}

	public ObservableList<XYChart.Data<String, Number>> getLineChartData(LineChartBox box, String tableName) {
		ObservableList<XYChart.Data<String, Number>> data = FXCollections.observableArrayList();
		
		try {
			sqlConnector
					.executeQuery(
							"SELECT " + box.getPlotColumn1() + ", " + box.getPlotColumn2() + " FROM "
									+ tableName + " WHERE " + box.getKeyColumn() + "='" + box.getValue() + "' ORDER BY DATE(" + box.getPlotColumn1() + ") ASC",
							rset -> data.add(
									new XYChart.Data<String, Number>(rset.getString(1),rset.getDouble(2))
									));
		} catch (SQLException e) {
			DialogFactory.createErrorDialog(e);
		}
		
		return data;
	}

	public int countSUM(String columnName, String groupColumnName) throws SQLException {
		String tableName = this.getSqlTableView().getTableName();

		AtomicInteger total = new AtomicInteger(0);
		sqlConnector.executeQuery("SELECT SUM(" + columnName + ") FROM " + tableName, resultSet -> {
			total.set(resultSet.getInt(1));
		});

		return total.get();
	}
	

	public ObservableList<PieChart.Data> getPieChartData(String columnName, String groupColumnName) {

		ObservableList<PieChart.Data> records = FXCollections.observableArrayList();

		try {
//			int total = countSUM(columnName, groupColumnName);
			String tableName = this.getSqlTableView().getTableName();

			sqlConnector.executeQuery("SELECT * FROM " + tableName + " GROUP BY " + groupColumnName + " ORDER BY "
					+ columnName + " DESC LIMIT 10", row -> {
//						double percentage = ((double) row.getInt(columnName) / (double) total * 100);
						String pieText = row.getString(groupColumnName);
//						PieChart.Data pieData = new PieChart.Data(pieText, percentage);
						PieChart.Data pieData = new PieChart.Data(pieText, row.getInt(columnName));
						records.add(pieData);
					});

//			records.forEach(record -> {
//				record.nameProperty().bind(Bindings.concat(record.getName(), " ", record.pieValueProperty(), "%"));
//			});

		} catch (SQLException e) {
			DialogFactory.createErrorDialog(e);
		} catch (NullPointerException ne) {
			LoggerFactory.getLogger(getClass()).debug("Exception ignored", ne);
		}

		return records;
	}

	@Override
	public int deleteRecord(SqlTableRow sqlTableRow) {
		int result = super.deleteRecord(sqlTableRow);
		this.changed();
		Platform.runLater(() -> {
			for (int i = 0; i < charts.size(); i++) {
				PieChart chart = charts.get(i);
				chart.setData(this.getPieChartData(chart.getTitle(), chartColumns.get(i)));
			}
		});
		return result;
	}

	@Override
	public void updateRecord(EditBox editBox, SqlTableRow sqlTableRow) {
		super.updateRecord(editBox, sqlTableRow);
		this.changed();
		Platform.runLater(() -> {
			for (int i = 0; i < charts.size(); i++) {
				PieChart chart = charts.get(i);
				chart.setData(this.getPieChartData(chart.getTitle(), chartColumns.get(i)));
			}
		});
	}

	@Override
	public void insertRecord(EditBox editBox) {
		super.insertRecord(editBox);
		this.changed();
		Platform.runLater(() -> {
			for (int i = 0; i < charts.size(); i++) {
				PieChart chart = charts.get(i);
				chart.setData(this.getPieChartData(chart.getTitle(), chartColumns.get(i)));
			}
		});
	}

	public void sqlConsoleButtonAction() {
		if (sqlConsoleBox == null) {
			sqlConsoleBox = new DSqlConsoleBox(this.sqlConnector, this);
			if (fullModeCheckBox.isSelected() && fullModeSplitPane != null) {
				fullModeSplitPane.getItems().clear();
				SplitPane splitPane = new SplitPane(tabPane, sqlConsoleBox.asDockNode());
				sqlConsoleBox.asDockNode().setOnClose(() -> {
					splitPane.getItems().remove(sqlConsoleBox.asDockNode());
					sqlConsoleBox = null;
				});
				sqlConsoleBox.asDockNode().getDockTitleBar().setDragAllowed(false);
				// set opposite orientation on purpose
				if (fullModeCheckBox.getText().contains("horizontal"))
					splitPane.setOrientation(Orientation.VERTICAL);
				else
					splitPane.setOrientation(Orientation.HORIZONTAL);
				fullModeSplitPane.getItems().addAll(sqlTableView, splitPane);
				fullModeSplitPane.setDividerPositions(0.7, 0.3);
			} else {
				sqlConsoleBox.asDockNode().setOnClose(() -> sqlConsoleBox = null);
				sqlConsoleBox.asDockNode().setPrefSize(this.getWidth(), this.getHeight()/3);
				sqlConsoleBox.asDockNode().dock(this.asDockNode().getDockPane(), DockPos.BOTTOM, thisDockNode);
			}
		}
	}
	@Override
	public void tableCheckBoxAction() {
		super.tableCheckBoxAction();
		sqlConsoleBox = null;
		while (chartsDNs.size() > 0) {
			DockNode dockNode = chartsDNs.get(0);
			dockNode.close();
		}
		charts.clear();
		chartColumns.clear();
		chartsDNs.clear();
		thisDockNode.setTitle(tablesBox.getSelectionModel().getSelectedItem());
	}

	@Override
	protected void getData() {
		super.getData();
		columnNames = getSqlTableView().getColumnsNames();
		columnsBox.setItems(FXCollections.observableList(columnNames));
		nameColumnsBox.setItems(FXCollections.observableList(columnNames));
	}

	@Override
	public DockNode asDockNode() {
		return thisDockNode;
	}

	@Override
	public void onChange(String tableName) {
		if (tablesBox.getSelectionModel().getSelectedItem().equals(tableName))
			tablesBox.getOnAction().handle(new ActionEvent());
	}

	@Override
	public void changed() {
		listeners.forEach(listener -> listener.onChange(getTablesBox().getValue()));
	}

	@Override
	public void addListener(SimpleChangeListener<String> listener) {
		listeners.add(listener);
	}

	@Override
	public void removeListener(SimpleChangeListener<String> listener) {
		listeners.remove(listener);
	}

	public boolean isFullMode() {
		return fullModeCheckBox.isSelected();
	}
	
	public void setFullMode(boolean mode) {
		fullModeCheckBox.setSelected(mode);
	}
	
	public void showConsole() {
		this.sqlConsoleButtonAction();
	}
}
