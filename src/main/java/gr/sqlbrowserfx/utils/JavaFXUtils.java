package gr.sqlbrowserfx.utils;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import jfxtras.styles.jmetro.JMetro;
import jfxtras.styles.jmetro.Style;

public class JavaFXUtils {

	private static final boolean ENABLE_JMETRO =  PropertiesLoader.getProperty("sqlbrowsefx.jmetro.theme", String.class) != null;
	private static final String JMETRO = (String) PropertiesLoader.getProperty("sqlbrowsefx.jmetro.theme", String.class);

	
	public static ImageView icon(String url) {
		return new ImageView(new Image(url));
	}

	public static ImageView createImageView(String url, Double width, Double height) {
		return new ImageView(new Image(url, width, height, true, false));
	}
	
	public static void addMouseScrolling(Node node) {
        node.setOnScroll((ScrollEvent event) -> {
            // Adjust the zoom factor as per your requirement
        	if (event.isControlDown()) {
	            double zoomFactor = 1.05;
	            double deltaY = event.getDeltaY();
	            if (deltaY < 0){
	                zoomFactor = 2.0 - zoomFactor;
	            }
	            node.setScaleX(node.getScaleX() * zoomFactor);
	            node.setScaleY(node.getScaleY() * zoomFactor);
	            event.consume();
        	}
        });
	}
	
	public static void applyJMetro(Node node) {
		Parent parent = (Parent) node;
		if (ENABLE_JMETRO && JMETRO.equals("dark"))
			new JMetro(Style.DARK).setParent(parent);	
		else if (ENABLE_JMETRO && JMETRO.equals("light"))
			new JMetro(Style.LIGHT).setParent(parent);
	}
}
