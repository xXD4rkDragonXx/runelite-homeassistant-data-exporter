package haexporterplugin.utils;

@FunctionalInterface
public interface Evaluable {
    String evaluate(boolean rich);
}