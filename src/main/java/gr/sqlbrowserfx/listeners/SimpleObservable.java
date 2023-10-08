package gr.sqlbrowserfx.listeners;

public interface SimpleObservable<T> {

	void changed();
	void changed(T data);
	void addObserver(SimpleObserver<T> listener);
	void removeObserver(SimpleObserver<T> listener);
}
