package gr.sqlbrowserfx.listeners;

import javafx.event.Event;
import javafx.event.EventType;

public class SimpleEventWithData<D> extends Event {
	
	private D data;

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public static final EventType<?> EVENT_TYPE = new EventType<>("SimpleEventWithData");

    public SimpleEventWithData(D data) {
        super(EVENT_TYPE);
        this.data = data;
    }
    
    public SimpleEventWithData() {
        super(EVENT_TYPE);
    }

	public D getData() {
		return data;
	}
    
}
