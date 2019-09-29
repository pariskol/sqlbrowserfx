package gr.sqlbrowserfx.listeners;

public interface SimpleObservable<T> {

	public void changed();
	public void changed(T data);
	public void addListener(SimpleChangeListener<T> listener);
	public void removeListener(SimpleChangeListener<T> listener);
}
