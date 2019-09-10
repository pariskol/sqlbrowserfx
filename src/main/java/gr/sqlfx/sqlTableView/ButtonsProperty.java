package gr.sqlfx.sqlTableView;

import javafx.beans.InvalidationListener;
import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.HBox;


public class ButtonsProperty implements Property<HBox>{

	private HBox buttonsBox;

	public ButtonsProperty(HBox buttonsBox) {
		this.buttonsBox = buttonsBox;
	}
	
	@Override
	public Object getBean() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addListener(ChangeListener<? super HBox> arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public HBox getValue() {
		// TODO Auto-generated method stub
		return buttonsBox;
	}

	@Override
	public void removeListener(ChangeListener<? super HBox> arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addListener(InvalidationListener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeListener(InvalidationListener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setValue(HBox value) {
		buttonsBox = value;
	}

	@Override
	public void bind(ObservableValue<? extends HBox> observable) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void bindBidirectional(Property<HBox> other) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isBound() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void unbind() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void unbindBidirectional(Property<HBox> other) {
		// TODO Auto-generated method stub
		
	}

}
