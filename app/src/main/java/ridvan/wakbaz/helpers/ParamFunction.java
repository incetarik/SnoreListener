package ridvan.wakbaz.helpers;

public interface ParamFunction<T, R> {
    R call(T... objects);
}
