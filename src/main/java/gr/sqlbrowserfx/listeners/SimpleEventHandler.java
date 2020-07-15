package gr.sqlbrowserfx.listeners;

import javafx.event.EventHandler;

public abstract class SimpleEventHandler implements EventHandler<SimpleEventWithData> {

    public abstract void onEvent1(int param0);

    public abstract void onEvent2(String param0);

    @Override
    public void handle(SimpleEventWithData event) {
    }
}
