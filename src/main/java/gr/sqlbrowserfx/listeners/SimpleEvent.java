package gr.sqlbrowserfx.listeners;

public class SimpleEvent<T, D> {

	private T type;
	private D data;

	SimpleEvent(T type, D data) {
		this.type = type;
		this.data = data;
	}
	
	public T getType() {
		return type;
	}

	public D getData() {
		return data;
	}

}
