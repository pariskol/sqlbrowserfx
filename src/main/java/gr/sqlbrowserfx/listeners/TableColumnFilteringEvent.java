package gr.sqlbrowserfx.listeners;

import javafx.event.Event;
import javafx.event.EventType;

public class TableColumnFilteringEvent extends Event{

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public static final EventType<?> EVENT_TYPE = new EventType<>(TableColumnFilteringEvent.class.getSimpleName());

    public TableColumnFilteringEvent() {
        super(EVENT_TYPE);
    }
}
