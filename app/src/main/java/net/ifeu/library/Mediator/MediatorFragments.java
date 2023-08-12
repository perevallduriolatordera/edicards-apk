package net.ifeu.library.Mediator;

import java.util.ArrayList;
import java.util.List;

public class MediatorFragments {

    final private List<IMediator> receivers = new ArrayList<>();

    public void addReceiver(IMediator receiver, boolean force) {

        for (IMediator addedReceiver : receivers) {
            if (receiver.equals(addedReceiver)) {
                if (!force)
                    return;
                else
                    this.receivers.remove(addedReceiver);
            }
        }

        this.receivers.add(receiver);
    }

    public void notify(String event, Object payload) {
        for (IMediator mediator : receivers) {
            mediator.notify(event, payload);
        }
    }
}
