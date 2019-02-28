package chuckcoughlin.bert.service;

import android.content.Intent;

public interface ObservableReceiver {
    public void register(BroadcastObserver observer);
    public void unregister(BroadcastObserver observer);
    public void notifyObservers(Intent intent);
}
