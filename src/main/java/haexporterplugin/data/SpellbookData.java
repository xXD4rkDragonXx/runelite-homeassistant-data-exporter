package haexporterplugin.data;

public class SpellbookData {
    private int id;
    private String name;

    public static String getSpellbookName(int id) {
        switch (id) {
            case 0:
                return "standard";
            case 1:
                return "ancient";
            case 2:
                return "lunar";
            case 3:
                return "arceuus";
            default:
                return "unknown";
        }
    }

    public SpellbookData(int id){
        this.id = id;
        this.name = getSpellbookName(id);
    }
}