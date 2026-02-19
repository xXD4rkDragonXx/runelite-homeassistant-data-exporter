package haexporterplugin.data;

public class SpellbookData {
    private int id;
    private String name;

    public static String getSpellbookName(int id){
        return switch (id)
        {
            case 0 -> "standard";
            case 1 -> "ancient";
            case 2 -> "lunar";
            case 3 -> "arceuus";
            default -> "unknown";
        };
    }

    public SpellbookData(int id){
        this.id = id;
        this.name = getSpellbookName(id);
    }
}