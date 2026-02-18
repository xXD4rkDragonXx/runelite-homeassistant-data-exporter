package haexporterplugin.data;

import lombok.Getter;

public class HAConnection {
    @Getter
    public String baseUrl;
    @Getter
    public String token;

    public HAConnection(String baseUrl, String token)
    {
        this.baseUrl = baseUrl;
        this.token = token;
    }


}
