package haexporterplugin.data;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

public class Root {
    @Setter
    @Getter
    private Player player;
    @Getter
    @Setter
    private ArrayList<Object> events;

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
