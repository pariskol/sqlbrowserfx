package gr.sqlbrowserfx.listeners;

import javafx.event.Event;
import javafx.event.EventType;

public class SimpleEvent extends Event {
	
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public static final EventType<?> EVENT_TYPE = new EventType<>("SimpleEvent");

    public SimpleEvent() {
        super(EVENT_TYPE);
    }
    
}
