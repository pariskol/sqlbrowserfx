package gr.sqlbrowserfx.dock.nodes;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.controlsfx.control.PopOver;
import org.dockfx.DockNode;
import org.dockfx.DockPos;
import org.slf4j.LoggerFactory;

import gr.sqlbrowserfx.conn.SqlConnector;
import gr.sqlbrowserfx.dock.Dockable;
import gr.sqlbrowserfx.factories.DialogFactory;
import gr.sqlbrowserfx.listeners.SimpleChangeListener;
import gr.sqlbrowserfx.listeners.SimpleObservable;
import gr.sqlbrowserfx.nodes.LineChartBox;
import gr.sqlbrowserfx.sqlPane.SqlPane;
import gr.sqlbrowserfx.sqlPane.SqlTableTab;
import gr.sqlbrowserfx.sqlTableView.EditBox;
import gr.sqlbrowserfx.sqlTableView.SqlTableRow;
import gr.sqlbrowserfx.utils.AppManager;
import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class DSqlPane extends SqlPane implements Dockable, SimpleChangeListener<String>, SimpleObservable<String> {

	private Button chartButton;
	private Button logButton;
	private ComboBox<String> columnsBox;
	private ComboBox<String> nameColumnsBox;
	private List<SimpleChangeListener<String>> listeners;
	private List<PieChart> charts;
	private List<String> chartColumns;
	private List<DockNode> chartsDNs;
	private List<LineChartBox> lineChartBoxes;

	private DockNode thisDockNode = null;
	private Button lineChartButton;
	private DSqlConsoleBox sqlConsoleBox;
	private Button showChartButton;
	private List<String> columnNames;
	private DockNode dRecordsTabPane = null;
	private DockNode dLogListView;;

	
	public DSqlPane() {
		this(null);
	}

	public DSqlPane(SqlConnector sqlConnector) {
		super(sqlConnector);

		listeners = new ArrayList<>();
		charts = new ArrayList<>();
		chartsDNs = new ArrayList<>();
		chartColumns = new ArrayList<>();
		lineChartBoxes = new ArrayList<>();

		thisDockNode = new DockNode(this, "Data explorer", JavaFXUtils.icon("/res/table.png"));
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
					this.searchButtonAction();
					sqlTableViewRef.requestFocus();
					break;
				case C:
					this.copyAction();
					sqlTableViewRef.requestFocus();
					break;
				case D:
					this.deleteButtonAction();
					sqlTableViewRef.requestFocus();
					break;
				case E:
					this.editButtonAction(simulateClickEvent(editButton));
					sqlTableViewRef.requestFocus();
					break;
				case Q:
					this.addButtonAction();
					sqlTableViewRef.requestFocus();
					break;
				case I:
					this.importCsvAction();
					sqlTableViewRef.requestFocus();
					break;
				case R:
					this.refreshButtonAction();
					sqlTableViewRef.requestFocus();
					break;
				case T:
					this.sqlConsoleButtonAction();
					sqlConsoleBox.getCodeAreaRef().requestFocus();
				default:
					break;
				}
			}
			sqlTableViewRef.requestFocus();
		});

		thisDockNode.setOnClose(() ->{
			AppManager.removeSqlPane(this);
//			it does not work as expected
//			if (sqlConsoleBox != null)
//				sqlConsoleBox.asDockNode().close();
//			if (dRecordsTabPane != null)
//				dRecordsTabPane.close();
//			if (dLogListView != null)
//				dLogListView.close();
		});
	}

	@Override
	protected FlowPane createToolbar() {
		FlowPane flowPane = super.createToolbar();
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
			this.fillChartColumnBoxes();
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
				sqlConnector.executeAsync(() -> {
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
			this.fillChartColumnBoxes();
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

//		sqlConsoleButton = new Button("", JavaFXUtils.icon("/res/console.png"));
//		sqlConsoleButton.setOnMouseClicked(mouseEvent -> this.sqlConsoleButtonAction());
//		sqlConsoleButton.setOnAction(mouseEvent -> this.sqlConsoleButtonAction());

		logButton = new Button("", JavaFXUtils.icon("/res/monitor.png"));
		logButton.setOnAction(actionEvent -> {
			logListView = new ListView<>();
			uiLogging = true;
			dLogListView = new DockNode(logListView, "Log", JavaFXUtils.icon("/res/monitor.png"));
			dLogListView.setOnClose(() -> {
				logListView = null;
				dLogListView = null;
				uiLogging = false;
			});
			if (sqlConsoleBox != null)
				dLogListView.dock(thisDockNode.getDockPane(), DockPos.RIGHT, sqlConsoleBox.asDockNode(), new double[] {0.7f,0.3f});
			else
				dLogListView.dock(thisDockNode.getDockPane(), DockPos.BOTTOM, new double[] {0.7f,0.3f});

		});
		flowPane.getChildren().addAll(chartButton, lineChartButton, logButton);
		return flowPane;
	}
	
	private ObservableList<XYChart.Data<String, Number>> getLineChartData(LineChartBox box, String tableName) {
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

	@SuppressWarnings("unused")
	private int countSUM(String columnName, String groupColumnName) throws SQLException {
		String tableName = this.getSqlTableView().getTableName();

		AtomicInteger total = new AtomicInteger(0);
		sqlConnector.executeQuery("SELECT SUM(" + columnName + ") FROM " + tableName, resultSet -> {
			total.set(resultSet.getInt(1));
		});

		return total.get();
	}
	

	private ObservableList<PieChart.Data> getPieChartData(String columnName, String groupColumnName) {

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

	@Override
	protected void sqlConsoleButtonAction() {
		if (sqlConsoleBox == null) {
			sqlConsoleBox = new DSqlConsoleBox(this.sqlConnector, this);
				sqlConsoleBox.asDockNode().setOnClose(() -> sqlConsoleBox = null);
				sqlConsoleBox.asDockNode().setMaxHeight(1080);
				sqlConsoleBox.asDockNode().dock(this.asDockNode().getDockPane(), DockPos.BOTTOM, thisDockNode, new double[] {0.7f,0.3f});
		}
	}
	
	@Override
	protected void tableComboBoxAction(ComboBox<String> comboBox) {
		super.tableComboBoxAction(comboBox);
//		sqlConsoleBox = null;
		while (chartsDNs.size() > 0) {
			DockNode dockNode = chartsDNs.get(0);
			dockNode.close();
		}
		charts.clear();
		chartColumns.clear();
		chartsDNs.clear();
//		thisDockNode.setTitle(tablesBox.getSelectionModel().getSelectedItem());
	}

	@Override
	protected void getData(String table) {
		if (table != null && !table.equals("empty")) {
			super.getData(table);
			this.fillChartColumnBoxes();
		}
	}

	private void fillChartColumnBoxes() {
		columnNames = getSqlTableView().getColumnsNames();
		columnsBox.setItems(FXCollections.observableList(columnNames));
		nameColumnsBox.setItems(FXCollections.observableList(columnNames));
	}
	@Override
	public void enableFullMode() {
		Platform.runLater(() -> {
//			((Label)tablesTabPane.getSelectionModel().getSelectedItem().getGraphic()).setText(sqlTableViewRef.titleProperty().get());
			tablesTabPane.getSelectionModel().getSelectedItem().setContent(sqlTableViewRef);
			if (isFullMode()) {
				this.createRecordsTabPane();
				this.createRecordsAddTab();
				if (dRecordsTabPane == null) {
					dRecordsTabPane = new DockNode(recordsTabPaneRef);
					dRecordsTabPane.dock(thisDockNode.getDockPane(), DockPos.RIGHT, thisDockNode, new double[] {0.7f, 0.3f});
					dRecordsTabPane.setOnClose(() -> {
						dRecordsTabPane = null;
						this.setFullMode(false);
					});
				} else {
					dRecordsTabPane.setContents(recordsTabPaneRef);
				}
//				tablesTabPane.getSelectionModel().getSelectedItem().setContent(sqlTableViewRef);
				((SqlTableTab) tablesTabPane.getSelectionModel().getSelectedItem()).setRecordsTabPane(recordsTabPaneRef);
			}

			sqlQueryRunning.set(false);
		});
	}
	
	@Override
	public void disableFullMode() {
		if (dRecordsTabPane != null)
			dRecordsTabPane.close();
		
		super.disableFullMode();
	}
	
	@Override
	protected void tablesTabPaneClickAction() {
		super.tablesTabPaneClickAction();
		if (dRecordsTabPane != null && recordsTabPaneRef != null)
			dRecordsTabPane.setContents(recordsTabPaneRef);
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
	public void changed(String data) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addListener(SimpleChangeListener<String> listener) {
		listeners.add(listener);
	}

	@Override
	public void removeListener(SimpleChangeListener<String> listener) {
		listeners.remove(listener);
	}
	
	public void setFullMode(boolean mode) {
		fullModeCheckBox.setSelected(mode);
	}
	
	public void showConsole() {
		this.sqlConsoleButtonAction();
	}

	public DSqlConsoleBox getSqlConsoleBox() {
		return sqlConsoleBox;
	}

}
