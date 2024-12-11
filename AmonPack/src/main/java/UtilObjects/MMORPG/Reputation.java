package UtilObjects.MMORPG;

public class Reputation {
    private String Name;
    private String SqlName;
    private String Material;
    private int RepLvL;
    private int PlaceInGui;
    private String Color;

    public Reputation(String name, String sqlname,String mat, int repLvL, int placeInGui, String color) {
        Name = name;
        SqlName = sqlname;
        Material = mat;
        RepLvL = repLvL;
        PlaceInGui = placeInGui;
        Color = color;
    }


    public String getName() {
        return Name;
    }
    public int getRepLvL() {
        return RepLvL;
    }
    public int getPlaceInGui() {
        return PlaceInGui;
    }
    public String getColor() {
        return Color;
    }
    public String getSqlName() {
        return SqlName;
    }
    public String getMaterial() {
        return Material;
    }
}
