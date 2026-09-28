package haexporterplugin.data;

import lombok.Getter;

import javax.annotation.Nullable;
import java.io.IOException;

// Non-2xx response to a pairing request. Carries the endpoint's optional (sanitized) "error" text, if it sent one.
public class PairingException extends IOException {
    @Getter
    @Nullable
    private final String serverMessage;

    public PairingException(String message, @Nullable String serverMessage) {
        super(message);
        this.serverMessage = serverMessage;
    }
}
