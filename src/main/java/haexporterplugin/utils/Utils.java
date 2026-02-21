package haexporterplugin.utils;

import haexporterplugin.enums.AccountType;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarbitID;


@Slf4j
@UtilityClass
public class Utils {
    public AccountType getAccountType(Client client) {
        return AccountType.get(client.getVarbitValue(VarbitID.IRONMAN));
    }
}
