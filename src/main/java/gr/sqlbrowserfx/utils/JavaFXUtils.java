package gr.sqlbrowserfx.utils;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import jfxtras.styles.jmetro.JMetro;
import jfxtras.styles.jmetro.Style;

public class JavaFXUtils {

	private static  String CSS_THEME = "/themes/" + (String) PropertiesLoader.getProperty("sqlbrowserfx.css.theme", String.class, "flat-dark");
	private static final boolean ENABLE_JMETRO =  PropertiesLoader.getProperty("sqlbrowserfx.jmetro.theme", String.class) != null;
	private static final String JMETRO = (String) PropertiesLoader.getProperty("sqlbrowserfx.jmetro.theme", String.class);

	
	public static ImageView createIcon(String url) {
		url = CSS_THEME + url;
		return new ImageView(new Image(url));
	}

	public static ImageView createImageView(String url, Double width, Double height) {
		url = CSS_THEME + url;
		return new ImageView(new Image(url, width, height, true, false));
	}
	
	public static Image createImage(String url) {
		url = CSS_THEME + url;
		return new Image(url);
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
