package ridvan.snorelistener.helpers;

public interface Action<T> {
    void call(T obj);
}
