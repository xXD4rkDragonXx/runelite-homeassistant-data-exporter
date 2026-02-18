package haexporterplugin.data;

public interface TokenCallback {
    void onSuccess(String token);
    void onFailure(Exception e);
}