package gr.sqlbrowserfx.dock.nodes;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.controlsfx.control.PopOver;
import org.dockfx.DockNode;
import org.dockfx.DockPos;
import org.dockfx.DockWeights;
import org.dockfx.Dockable;
import org.fxmisc.richtext.CodeArea;
import org.slf4j.LoggerFactory;

import gr.sqlbrowserfx.LoggerConf;
import gr.sqlbrowserfx.SqlBrowserFXAppManager;
import gr.sqlbrowserfx.conn.SqlConnector;
import gr.sqlbrowserfx.factories.DialogFactory;
import gr.sqlbrowserfx.listeners.SimpleObservable;
import gr.sqlbrowserfx.listeners.SimpleObserver;
import gr.sqlbrowserfx.nodes.LineChartBox;
import gr.sqlbrowserfx.nodes.sqlpane.SqlPane;
import gr.sqlbrowserfx.nodes.sqlpane.SqlTableTab;
import gr.sqlbrowserfx.nodes.tableviews.SqlTableView;
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
import javafx.scene.control.TabPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class DSqlPane extends SqlPane implements Dockable, SimpleObserver<String>, SimpleObservable<String> {

	private Button chartButton;
	private ComboBox<String> columnsBox;
	private ComboBox<String> nameColumnsBox;
	private List<SimpleObserver<String>> listeners;
	private List<PieChart> charts;
	private List<String> chartColumns;
	private List<DockNode> chartsDNs;
	private List<LineChartBox> lineChartBoxes;

	private DockNode thisDockNode = null;
	private Button lineChartButton;
	private DSqlConsolePane sqlConsoleBox;
	private Button showChartButton;
	private List<String> columnNames;
	private DockNode dRecordsTabPane = null;

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
	}

	@Override
	public FlowPane createToolbar() {
		FlowPane flowPane = super.createToolbar();
		nameColumnsBox = new ComboBox<>();
		columnsBox = new ComboBox<>();
		showChartButton = new Button("Show", JavaFXUtils.createIcon("/icons/chart-pie.png"));
		showChartButton.setOnAction(event -> {
			PieChart chart = new PieChart();
			chart.setClockwise(true);
			chart.setTitle(columnsBox.getSelectionModel().getSelectedItem());
			chart.setData(this.getPieChartData(chart.getTitle(), nameColumnsBox.getSelectionModel().getSelectedItem()));

			charts.add(chart);
			chartColumns.add(nameColumnsBox.getSelectionModel().getSelectedItem());

			DockNode chartDockNode = new DockNode(chart, "Chart", JavaFXUtils.createIcon("/icons/chart-pie.png"));
			chartDockNode.setPrefSize(100, 100);
			chartDockNode.dock(this.asDockNode().getDockPane(), DockPos.RIGHT, this.asDockNode());
			chartsDNs.add(chartDockNode);
		});

		chartButton = new Button("", JavaFXUtils.createIcon("/icons/chart-pie.png"));
		chartButton.setOnMouseClicked(mouseEvent -> {

			if (chartButton.isFocused() && this.getPopOver().isShowing() || ((SqlTableTab)tablesTabPane.getSelectionModel().getSelectedItem()).getCustomText().isEmpty())
				return;

			chartButton.requestFocus();
			this.fillChartColumnBoxes(getSelectedSqlTableView());
			PopOver popOver = new PopOver(new VBox(new Label("Select display column"), nameColumnsBox,
					new Label("Select plot column"), columnsBox, showChartButton));
			popOver.setDetachable(false);

			this.setPopOver(popOver);
			this.getPopOver().show(chartButton);
		});

		Button showLineChartButton = new Button("Show", JavaFXUtils.createIcon("/icons/chart.png"));
		showLineChartButton.setOnMouseClicked(actionEvent -> {
			ObservableList<XYChart.Series<String, Number>> data = FXCollections.<XYChart.Series<String, Number>>observableArrayList();

			for (LineChartBox box : lineChartBoxes) {
				XYChart.Series<String, Number> series = new XYChart.Series<String, Number>();
				series.setName(box.getValue());
				String tableName = this.getSelectedSqlTableView().getTableName();
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
				lineDockNode.dock(this.asDockNode().getDockPane(), DockPos.RIGHT, this.asDockNode());
			});
		});

		lineChartButton = new Button("", JavaFXUtils.createIcon("/icons/chart.png"));
		lineChartButton.setOnMouseClicked(mouseEvent -> {

			if (lineChartButton.isFocused() && this.getPopOver().isShowing() || this.getTablesBox() == null
					|| this.getTablesBox().getSelectionModel().isEmpty())
				return;

			lineChartBoxes.clear();
			lineChartButton.requestFocus();
			this.fillChartColumnBoxes(getSelectedSqlTableView());
			PopOver popOver = new PopOver();
			LineChartBox contentBox = new LineChartBox(columnNames);
			lineChartBoxes.add(contentBox);
			HBox hbox = new HBox(contentBox);
			Button addBoxButton = new Button("", JavaFXUtils.createIcon("/icons/add.png"));
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
//		TODO uncomment this to enable chart buttons
//		flowPane.getChildren().addAll(chartButton, lineChartButton);
		return flowPane;
	}

	private ObservableList<XYChart.Data<String, Number>> getLineChartData(LineChartBox box, String tableName) {
		ObservableList<XYChart.Data<String, Number>> data = FXCollections.observableArrayList();

		try {
			sqlConnector.executeQuery(
					"SELECT " + box.getPlotColumn1() + ", " + box.getPlotColumn2() + " FROM " + tableName + " WHERE "
							+ box.getKeyColumn() + "='" + box.getValue() + "' ORDER BY DATE(" + box.getPlotColumn1()
							+ ") ASC",
					rset -> data.add(new XYChart.Data<String, Number>(rset.getString(1), rset.getDouble(2))));
		} catch (SQLException e) {
			DialogFactory.createErrorDialog(e);
		}

		data.sort((o1, o2) -> {
	        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
			if (LocalDate.parse(((XYChart.Data<String, Number>) o1).getXValue().toString(), formatter)
					.isAfter(LocalDate.parse(((XYChart.Data<String, Number>) o2).getXValue().toString(),formatter))) {
				return 1;
			}
			return 0;
		});
		return data;
	}

	@SuppressWarnings("unused")
	private int countSUM(String columnName, String groupColumnName) throws SQLException {
		String tableName = this.getSelectedSqlTableView().getTableName();

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
			String tableName = this.getSelectedSqlTableView().getTableName();

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
			LoggerFactory.getLogger(LoggerConf.LOGGER_NAME).debug("Exception ignored", ne);
		}

		return records;
	}


	@Override
	protected void sqlConsoleButtonAction() {
		if (sqlConsoleBox == null) {
			sqlConsoleBox = new DSqlConsolePane(this.sqlConnector, this);
			sqlConsoleBox.asDockNode().setOnClose(() -> sqlConsoleBox = null);
			sqlConsoleBox.asDockNode().dock(this.asDockNode().getDockPane(), DockPos.TOP, this.asDockNode(),
					DockWeights.asDoubleArrray(0.3f, 0.7f));
		}
	}

	public CodeArea getSqlCodeAreaRef() {
		return sqlConsoleBox != null ? sqlConsoleBox.getCodeAreaRef() : null;
	}

	@Override
	protected void tableComboBoxAction(ComboBox<String> comboBox) {
		super.tableComboBoxAction(comboBox);
		charts.clear();
		chartColumns.clear();
		chartsDNs.clear();
	}

	@Override
	protected void getDataFromDB(String table, final SqlTableTab sqlTableTab) {
		if (table != null && !table.equals("empty")) {
			super.getDataFromDB(table, sqlTableTab);
			this.fillChartColumnBoxes(sqlTableTab.getSqlTableView());
		}
	}

	private void fillChartColumnBoxes(SqlTableView sqlTableView) {
		columnNames = sqlTableView.getColumnsNames();
		columnsBox.setItems(FXCollections.observableList(columnNames));
		nameColumnsBox.setItems(FXCollections.observableList(columnNames));
	}

	@Override
	public void enableFullMode(final SqlTableTab tab) {
//		super.enableFullMode(tab);
		Platform.runLater(() -> {
//			tab.setContent(guiState.getSqlTableView());
			if (isInFullMode()) {
//				final TabPane recordsTabPane = tab.getRecordsTabPane() != null ?
//						tab.getRecordsTabPane() :
//						this.createRecordsTabPane();
				TabPane recordsTabPane = this.createRecordsTabPane();
				if (dRecordsTabPane == null) {
					dRecordsTabPane = new DockNode(recordsTabPane, this.asDockNode().getTitle() + " : Full mode",
							JavaFXUtils.createIcon("/icons/details.png"));
					dRecordsTabPane.dock(this.asDockNode().getDockPane(), DockPos.RIGHT, this.asDockNode(),
							DockWeights.asDoubleArrray(0.7f, 0.3f));
					dRecordsTabPane.setOnClose(() -> {
						dRecordsTabPane = null;
						this.setFullMode(false);
						this.disableFullMode();
						System.gc();
					});
				} else {
					dRecordsTabPane.setContents(recordsTabPane);
				}
				tab.setRecordsTabPane(recordsTabPane);
			}

			sqlQueryRunning = false;
		});
	}

	@Override
	public void disableFullMode() {
		if (dRecordsTabPane != null) {
			dRecordsTabPane.close();
			
			tablesTabPane.getTabs().forEach(tab -> {
				if (tab instanceof SqlTableTab)
					((SqlTableTab)tab).setRecordsTabPane(null);
			});
		}
//		super.disableFullMode();
	}

	@Override
	protected void tablesTabPaneClickAction() {
		super.tablesTabPaneClickAction();
		if (dRecordsTabPane != null && getSelectedRecordsTabPane() != null)
			dRecordsTabPane.setContents(getSelectedRecordsTabPane());
	}

	@Override
	public DockNode asDockNode() {
		if (thisDockNode == null) {
			thisDockNode = new DockNode(this, "Data explorer", JavaFXUtils.createIcon("/icons/table.png"));
			thisDockNode.setOnClose(() -> {
				chartsDNs.forEach(chart -> chart.close());
				chartsDNs.clear();
				charts.clear();
			});

			thisDockNode.setOnClose(() -> {
				SqlBrowserFXAppManager.unregisterSqlPane(this);
//				it does not work as expected
//				if (sqlConsoleBox != null)
//					sqlConsoleBox.asDockNode().close();
//				if (dRecordsTabPane != null)
//					dRecordsTabPane.close();
//				if (dLogListView != null)
//					dLogListView.close();
			});
		}
		return thisDockNode;
	}
	
	@Override
	public void onObservaleChange(String tableName) {
		if (tablesBox.getSelectionModel().getSelectedItem().equals(tableName))
			tablesBox.getOnAction().handle(new ActionEvent());
	}

	@Override
	public void changed() {
		listeners.forEach(listener -> listener.onObservaleChange(getTablesBox().getValue()));
	}

	@Override
	public void changed(String data) {
	}

	@Override
	public void addObserver(SimpleObserver<String> listener) {
		listeners.add(listener);
	}

	@Override
	public void removeObserver(SimpleObserver<String> listener) {
		listeners.remove(listener);
	}

	public void setFullMode(boolean mode) {
		fullModeCheckBox.setSelected(mode);
	}

	public void showConsole() {
		this.sqlConsoleButtonAction();
	}

	public DSqlConsolePane getSqlConsoleBox() {
		return sqlConsoleBox;
	}

}
