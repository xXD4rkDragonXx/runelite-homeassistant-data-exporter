package haexporterplugin.data;

import javax.annotation.Nullable;

public interface TokenCallback {
    // name: optional (already sanitized) default friendly name supplied by the endpoint
    void onSuccess(String token, @Nullable String name);
    void onFailure(Exception e);
}