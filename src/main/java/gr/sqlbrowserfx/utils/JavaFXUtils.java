package gr.sqlbrowserfx.utils;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class JavaFXUtils {

	public static ImageView icon(String url) {
		return new ImageView(new Image(url));
	}

	public static ImageView createImageView(String url, Double width, Double height) {
		return new ImageView(new Image(url, width, height, true, false));
	}
}
