package haexporterplugin.events;

public class LevelEvent {
    private final String skill;
    private final int level;

    public LevelEvent(String skill, int level){
        this.skill = skill;
        this.level = level;
    }
}
