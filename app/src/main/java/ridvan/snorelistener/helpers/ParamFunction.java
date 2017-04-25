package ridvan.snorelistener.helpers;

public interface ParamFunction<T, R> {
    R call(T... objects);
}
