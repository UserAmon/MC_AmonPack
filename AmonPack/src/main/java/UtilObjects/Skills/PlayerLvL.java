package UtilObjects.Skills;

public class PlayerLvL {
    String Player;
    int Job1LvL;
    int Job2LvL;
    int Job3LvL;
    int Job4LvL;
    int LvLFactor = 5;

    public PlayerLvL(String player, int job1LvL, int job2LvL, int job3LvL, int job4LvL) {
        Player = player;
        Job1LvL = job1LvL;
        Job2LvL = job2LvL;
        Job3LvL = job3LvL;
        Job4LvL = job4LvL;
    }
    public int RealLvL(int i) {
        int level = 0;
        int threshold = LvLFactor;
        while (i >= threshold) {
            level++;
            threshold *= 2;
        }
        return level;
    }

    public void setJob1LvL(int job1LvL) {
        Job1LvL = job1LvL;
    }
    public void setJob2LvL(int job2LvL) {
        Job2LvL = job2LvL;
    }
    public void setJob3LvL(int job3LvL) {
        Job3LvL = job3LvL;
    }
    public void setJob4LvL(int job4LvL) {
        Job4LvL = job4LvL;
    }
    public String getPlayer() {
        return Player;
    }
    public int getJob1LvL() {
        return Job1LvL;
    }
    public int getJob2LvL() {
        return Job2LvL;
    }
    public int getJob3LvL() {
        return Job3LvL;
    }
    public int getJob4LvL() {
        return Job4LvL;
    }
    public int getLvLFactor() {
        return LvLFactor;
    }
}
