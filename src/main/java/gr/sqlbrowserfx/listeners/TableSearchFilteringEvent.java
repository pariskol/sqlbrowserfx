package gr.sqlbrowserfx.listeners;

import javafx.event.Event;
import javafx.event.EventType;

public class TableSearchFilteringEvent extends Event {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public static final EventType<?> EVENT_TYPE = new EventType<>(TableSearchFilteringEvent.class.getSimpleName());

    public TableSearchFilteringEvent() {
        super(EVENT_TYPE);
    }
}
