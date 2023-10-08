package gr.sqlbrowserfx.listeners;

import javafx.event.Event;
import javafx.event.EventType;

import java.io.Serial;

public class TableColumnFilteringEvent extends Event{

    /**
	 * 
	 */
	@Serial
    private static final long serialVersionUID = 1L;
	
	public static final EventType<?> EVENT_TYPE = new EventType<>(TableColumnFilteringEvent.class.getSimpleName());

    public TableColumnFilteringEvent() {
        super(EVENT_TYPE);
    }
}
