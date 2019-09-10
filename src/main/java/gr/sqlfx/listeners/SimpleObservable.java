package gr.sqlfx.listeners;

public interface SimpleObservable<T> {

	public void changed();
	public void addListener(SimpleChangeListener<T> listener);
	public void removeListener(SimpleChangeListener<T> listener);
}
