package haexporterplugin.data;

public class SkillInfo {
    private Integer xp;
    private Integer level;

    public SkillInfo() {
    }

    public SkillInfo(Integer xp, Integer level) {
        this.xp = xp;
        this.level = level;
    }

    public Integer getXp() {
        return xp;
    }

    public void setXp(Integer xp) {
        this.xp = xp;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }
}
