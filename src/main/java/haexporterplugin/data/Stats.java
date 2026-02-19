package haexporterplugin.data;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Setter
@Getter
public class Stats {
    private Map<String, SkillInfo> skills;

    public Stats() {
    }

    public Stats(Map<String, SkillInfo> skills) {
        this.skills = skills;
    }

}
