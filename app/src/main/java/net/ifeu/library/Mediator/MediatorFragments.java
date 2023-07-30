package net.ifeu.library.Mediator;

import java.util.ArrayList;
import java.util.List;

public class MediatorFragments {

    final private List<IMediator> receivers = new ArrayList<>();

    public void addReceiver(IMediator receiver) {
        this.receivers.add(receiver);
    }

    public void notify(String event, Object payload) {
        for (IMediator mediator : receivers) {
            mediator.notify(event, payload);
        }
    }
}
