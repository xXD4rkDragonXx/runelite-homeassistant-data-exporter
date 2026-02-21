package haexporterplugin.data;

import haexporterplugin.HAExporterConfig;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.GameState;

import javax.inject.Inject;
import java.util.ArrayList;

public class Root {
    private @Inject HAExporterConfig config;

    @Setter
    @Getter
    private Player player;
    @Getter
    @Setter
    private ArrayList<Object> events;
    @Getter
    @Setter
    private GameState state;
    @Setter
    private int tickDelay;

    public Root() {
        this.events = new ArrayList<>();
    }

    public void addEvent(Object event){
        this.events.add(event);
    }

    public void resetEvents(){
        this.events.clear();
    }

}
