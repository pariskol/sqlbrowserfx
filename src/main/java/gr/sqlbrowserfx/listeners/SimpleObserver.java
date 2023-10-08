package gr.sqlbrowserfx.listeners;

public interface SimpleObserver<T> {

	void onObservableChange(T newValue);
}
