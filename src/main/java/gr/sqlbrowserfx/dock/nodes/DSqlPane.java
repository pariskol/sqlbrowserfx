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
import org.fxmisc.richtext.CodeArea;
import org.slf4j.LoggerFactory;

import gr.sqlbrowserfx.SqlBrowserFXAppManager;
import gr.sqlbrowserfx.conn.SqlConnector;
import gr.sqlbrowserfx.dock.DockWeights;
import gr.sqlbrowserfx.dock.Dockable;
import gr.sqlbrowserfx.factories.DialogFactory;
import gr.sqlbrowserfx.listeners.SimpleChangeListener;
import gr.sqlbrowserfx.listeners.SimpleObservable;
import gr.sqlbrowserfx.nodes.LineChartBox;
import gr.sqlbrowserfx.nodes.sqlPane.SqlPane;
import gr.sqlbrowserfx.nodes.sqlPane.SqlTableTab;
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
	private DSqlConsolePane sqlConsoleBox;
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

		this.getSelectedSqlTableView().setColumnWidth(1, 100, 300);
		this.getSelectedSqlTableView().setOnKeyPressed(keyEvent -> {
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

	}

	@Override
	public FlowPane createToolbar() {
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
			chartDockNode.dock(this.asDockNode().getDockPane(), DockPos.RIGHT, this.asDockNode());
//			chartDockNode.setOnClose(() -> {
//				charts.remove(chart);
//				chartColumns.remove(nameColumnsBox.getSelectionModel().getSelectedItem());
//				chartsDNs.remove(chartDockNode);
//			});
			chartsDNs.add(chartDockNode);
		});

		chartButton = new Button("", JavaFXUtils.icon("/res/chart-pie.png"));
		chartButton.setOnMouseClicked(mouseEvent -> {

			if (chartButton.isFocused() && this.getPopOver().isShowing() || ((SqlTableTab)tablesTabPane.getSelectionModel().getSelectedItem()).getCustomText().isEmpty())
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
				dLogListView.dock(this.asDockNode().getDockPane(), DockPos.RIGHT, sqlConsoleBox.asDockNode(),
						DockWeights.asDoubleArrray(0.7f, 0.3f));
			else
				dLogListView.dock(this.asDockNode().getDockPane(), DockPos.BOTTOM, DockWeights.asDoubleArrray(0.7f, 0.3f));

		});
		flowPane.getChildren().addAll(chartButton, lineChartButton, logButton);
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
			LoggerFactory.getLogger(getClass()).debug("Exception ignored", ne);
		}

		return records;
	}

// Uncomment this section for piecharts to refresh on changes
//-------------------------------------------------------------------------------------------------------------
//	@Override
//	public int deleteRecord(SqlTableRow sqlTableRow) {
//		int result = super.deleteRecord(sqlTableRow);
//		this.changed();
//		Platform.runLater(() -> {
//			for (int i = 0; i < charts.size(); i++) {
//				PieChart chart = charts.get(i);
//				chart.setData(this.getPieChartData(chart.getTitle(), chartColumns.get(i)));
//			}
//		});
//		return result;
//	}
//
//	@Override
//	public void updateRecord(SqlTableRowEditBox editBox, SqlTableRow sqlTableRow) {
//		super.updateRecord(editBox, sqlTableRow);
//		this.changed();
//		Platform.runLater(() -> {
//			for (int i = 0; i < charts.size(); i++) {
//				PieChart chart = charts.get(i);
//				chart.setData(this.getPieChartData(chart.getTitle(), chartColumns.get(i)));
//			}
//		});
//	}
//
//	@Override
//	public void insertRecord(SqlTableRowEditBox editBox) {
//		super.insertRecord(editBox);
//		this.changed();
//		Platform.runLater(() -> {
//			for (int i = 0; i < charts.size(); i++) {
//				PieChart chart = charts.get(i);
//				chart.setData(this.getPieChartData(chart.getTitle(), chartColumns.get(i)));
//			}
//		});
//	}
//-------------------------------------------------------------------------------------------------------------

	@Override
	protected void sqlConsoleButtonAction() {
		if (sqlConsoleBox == null) {
			sqlConsoleBox = new DSqlConsolePane(this.sqlConnector, this);
			sqlConsoleBox.asDockNode().setOnClose(() -> sqlConsoleBox = null);
			sqlConsoleBox.asDockNode().setMaxHeight(1080);
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
//		while (chartsDNs.size() > 0) {
//			DockNode dockNode = chartsDNs.get(0);
//			dockNode.close();
//		}
		charts.clear();
		chartColumns.clear();
		chartsDNs.clear();
	}

	@Override
	protected void getData(String table) {
		if (table != null && !table.equals("empty")) {
			super.getData(table);
			this.fillChartColumnBoxes();
		}
	}

	private void fillChartColumnBoxes() {
		columnNames = getSelectedSqlTableView().getColumnsNames();
		columnsBox.setItems(FXCollections.observableList(columnNames));
		nameColumnsBox.setItems(FXCollections.observableList(columnNames));
	}

	@Override
	public void enableFullMode() {
		Platform.runLater(() -> {
			tablesTabPane.getSelectionModel().getSelectedItem().setContent(sqlTableViewRef);
			if (isFullMode()) {
				this.createRecordsTabPane();
				this.createRecordsAddTab();
				if (dRecordsTabPane == null) {
					dRecordsTabPane = new DockNode(recordsTabPaneRef, this.asDockNode().getTitle() + " : Full mode",
							JavaFXUtils.icon("/res/details.png"));
					dRecordsTabPane.dock(this.asDockNode().getDockPane(), DockPos.RIGHT, this.asDockNode(),
							DockWeights.asDoubleArrray(0.7f, 0.3f));
					dRecordsTabPane.setOnClose(() -> {
						dRecordsTabPane = null;
						this.setFullMode(false);
					});
				} else {
					dRecordsTabPane.setContents(recordsTabPaneRef);
				}
				((SqlTableTab) tablesTabPane.getSelectionModel().getSelectedItem())
						.setRecordsTabPane(recordsTabPaneRef);
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
		if (thisDockNode == null) {
			thisDockNode = new DockNode(this, "Data explorer", JavaFXUtils.icon("/res/table.png"));
			thisDockNode.setOnClose(() -> {
				chartsDNs.forEach(chart -> chart.close());
				chartsDNs.clear();
				charts.clear();
			});

			thisDockNode.setOnClose(() -> {
				SqlBrowserFXAppManager.removeSqlPane(this);
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

	public DSqlConsolePane getSqlConsoleBox() {
		return sqlConsoleBox;
	}

}
