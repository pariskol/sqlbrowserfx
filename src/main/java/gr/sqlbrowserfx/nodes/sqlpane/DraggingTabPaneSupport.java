package gr.sqlbrowserfx.nodes.sqlpane;

import java.util.concurrent.atomic.AtomicLong;

import gr.sqlbrowserfx.utils.JavaFXUtils;
import javafx.collections.ListChangeListener.Change;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

public class DraggingTabPaneSupport {

    private Tab currentDraggingTab ;

    private static final AtomicLong idGenerator = new AtomicLong();

    private final String draggingID = "DraggingTabPaneSupport-"+idGenerator.incrementAndGet() ;

	private String dragIconUrl;

	public DraggingTabPaneSupport() {
	}
	
	public DraggingTabPaneSupport(String dragIconUrl) {
		this();
		this.dragIconUrl = dragIconUrl;
	}
	
	public void addSupport(TabPane tabPane) {
        tabPane.getTabs().forEach(this::addDragHandlers);
        tabPane.getTabs().addListener((Change<? extends Tab> c) -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    c.getAddedSubList().forEach(this::addDragHandlers);
                }
                if (c.wasRemoved()) {
                    c.getRemoved().forEach(this::removeDragHandlers);
                }
            }
        });

        // if we drag onto a tab pane (but not onto the tab graphic), add the tab to the end of the list of tabs:
        tabPane.setOnDragOver(e -> {
            if (draggingID.equals(e.getDragboard().getString()) && 
                    currentDraggingTab != null &&
                    currentDraggingTab.getTabPane() != tabPane) {
                e.acceptTransferModes(TransferMode.MOVE);
            }
        });
        tabPane.setOnDragDropped(e -> {
            if (draggingID.equals(e.getDragboard().getString()) && 
                    currentDraggingTab != null &&
                    currentDraggingTab.getTabPane() != tabPane) {

                currentDraggingTab.getTabPane().getTabs().remove(currentDraggingTab);
                tabPane.getTabs().add(currentDraggingTab);
                currentDraggingTab.getTabPane().getSelectionModel().select(currentDraggingTab);
            }
        });
    }
	
    public void addSupport(SqlPane sqlPane) {
    	TabPane tabPane = sqlPane.getTablesTabPane();
        tabPane.getTabs().forEach(tab -> addDragHandlers(tab, sqlPane));
        tabPane.getTabs().addListener((Change<? extends Tab> c) -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    c.getAddedSubList().forEach(tab -> addDragHandlers(tab, sqlPane));
                }
                if (c.wasRemoved()) {
                    c.getRemoved().forEach(this::removeDragHandlers);
                }
            }
        });

        // if we drag onto a tab pane (but not onto the tab graphic), add the tab to the end of the list of tabs:
        tabPane.setOnDragOver(e -> {
            if (draggingID.equals(e.getDragboard().getString()) && 
                    currentDraggingTab != null &&
                    currentDraggingTab.getTabPane() != tabPane) {
                e.acceptTransferModes(TransferMode.MOVE);
            }
        });
        tabPane.setOnDragDropped(e -> {
            if (draggingID.equals(e.getDragboard().getString()) && 
                    currentDraggingTab != null &&
                    currentDraggingTab.getTabPane() != tabPane) {

                currentDraggingTab.getTabPane().getTabs().remove(currentDraggingTab);
                tabPane.getTabs().add(currentDraggingTab);
                currentDraggingTab.getTabPane().getSelectionModel().select(currentDraggingTab);
            }
        });
    }

    private void addDragHandlers(Tab tab) {

        // move text to label graphic:
        if (tab.getText() != null && ! tab.getText().isEmpty()) {
        	Node graphic = tab.getGraphic() != null ? tab.getGraphic() : dragIconUrl != null ? JavaFXUtils.createIcon(dragIconUrl) : null;
            Label label = new Label(tab.getText(), graphic);
            
            tab.setText(null);
            tab.setGraphic(label);
        }

        Node graphic = tab.getGraphic();
        graphic.setOnDragDetected(e -> {
            Dragboard dragboard = graphic.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            // dragboard must have some content, but we need it to be a Tab, which isn't supported
            // So we store it in a local variable and just put arbitrary content in the dragbaord:
            content.putString(draggingID);
            dragboard.setContent(content);
            dragboard.setDragView(graphic.snapshot(null, null));
            currentDraggingTab = tab ;
        });
        graphic.setOnDragOver(e -> {
            if (draggingID.equals(e.getDragboard().getString()) && 
                    currentDraggingTab != null &&
                    currentDraggingTab.getGraphic() != graphic) {
                e.acceptTransferModes(TransferMode.MOVE);
            }
        });
        graphic.setOnDragDropped(e -> {
            if (draggingID.equals(e.getDragboard().getString()) && 
                    currentDraggingTab != null &&
                    currentDraggingTab.getGraphic() != graphic) {

                int index = tab.getTabPane().getTabs().indexOf(tab) ;
                currentDraggingTab.getTabPane().getTabs().remove(currentDraggingTab);
                tab.getTabPane().getTabs().add(index, currentDraggingTab);
                currentDraggingTab.getTabPane().getSelectionModel().select(currentDraggingTab);
            }
        });
        graphic.setOnDragDone(e -> currentDraggingTab = null);
    }
    
    private void addDragHandlers(Tab tab, SqlPane sqlPane) {

        // move text to label graphic:
        if (tab.getText() != null && ! tab.getText().isEmpty() && !tab.getText().equals("Add")) {
        	Node graphic = tab.getGraphic() != null ? tab.getGraphic() : JavaFXUtils.createIcon(dragIconUrl);
//            Label label = new Label(tab.getText(), graphic);
//            label.textProperty().bind(sqlPane.getSqlTableView().titleProperty());
//            tab.textProperty().unbind();
//            tab.setText(null);
//            tab.setGraphic(label);
        	tab.setGraphic(graphic);
        }

        Node graphic = tab.getGraphic();
        graphic.setOnDragDetected(e -> {
            Dragboard dragboard = graphic.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            // dragboard must have some content, but we need it to be a Tab, which isn't supported
            // So we store it in a local variable and just put arbitrary content in the dragbaord:
            content.putString(draggingID);
            dragboard.setContent(content);
            dragboard.setDragView(graphic.snapshot(null, null));
            currentDraggingTab = tab ;
        });
        graphic.setOnDragOver(e -> {
            if (draggingID.equals(e.getDragboard().getString()) && 
                    currentDraggingTab != null &&
                    currentDraggingTab.getGraphic() != graphic) {
                e.acceptTransferModes(TransferMode.MOVE);
            }
        });
        graphic.setOnDragDropped(e -> {
            if (draggingID.equals(e.getDragboard().getString()) && 
                    currentDraggingTab != null &&
                    currentDraggingTab.getGraphic() != graphic) {

                int index = tab.getTabPane().getTabs().indexOf(tab) ;
                currentDraggingTab.getTabPane().getTabs().remove(currentDraggingTab);
                tab.getTabPane().getTabs().add(index, currentDraggingTab);
                currentDraggingTab.getTabPane().getSelectionModel().select(currentDraggingTab);
            }
        });
        graphic.setOnDragDone(e -> currentDraggingTab = null);
    }

    private void removeDragHandlers(Tab tab) {
        tab.getGraphic().setOnDragDetected(null);
        tab.getGraphic().setOnDragOver(null);
        tab.getGraphic().setOnDragDropped(null);
        tab.getGraphic().setOnDragDone(null);
    }
}
