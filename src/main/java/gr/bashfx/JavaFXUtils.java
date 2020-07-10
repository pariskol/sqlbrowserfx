package gr.bashfx;

import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;

public class JavaFXUtils {

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
}
