package gr.sqlbrowserfx.listeners;

public interface SimpleObservable<T> {

	public void changed();
	public void changed(T data);
	public void addObserver(SimpleObserver<T> listener);
	public void removeObserver(SimpleObserver<T> listener);
}
