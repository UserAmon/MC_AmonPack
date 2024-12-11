package OLD;
/*
import commands.Commands;
import General.AmonPackPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static OLD.Dungeon.*;

public class DungEvent implements Cloneable{
    Location LocOfEvent;
    List<String> Effects;
    List<String> Conditions;
    String type;
    EntityType entity;
    String entityname;
    int radius;
    int charging;
    int reqkills;
    int actkills;
    String particle;
    String num;
    int offset;
    DungEvent de = this;

    public DungEvent(String num,List<String> effects, List<String> conditions, String type,Location loc) {
        Effects = effects;
        Conditions = conditions;
        this.type = type;
        this.LocOfEvent = loc;
        this.num = num;
    }
    public DungEvent(String num,List<String> effects, List<String> conditions, String type,Location loc, int r, int ch,String particle) {
        Effects = effects;
        Conditions = conditions;
        this.type = type;
        this.LocOfEvent = loc;
        this.radius = r;
        this.charging = ch;
        this.particle = particle;
        this.num = num;
    }
    public DungEvent(String num,List<String> effects, String type,Location loc, int radius, String ent, String entname, int reqkills) {
        Effects = effects;
        this.type = type;
        this.entity = EntityType.valueOf(ent);
        this.entityname = entname;
        this.radius = radius;
        this.LocOfEvent = loc;
        this.num = num;
        this.reqkills = reqkills;
        this.actkills = 0;
    }

    public void KillCounter(Dungeon dung) {
        actkills++;
        if (actkills>=reqkills){
            dung.UsedDungEvents.add(this);
            exeeffects(dung);
            actkills=0;
        }
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public void exeeffects(Dungeon dung) {
        for (String st: this.getEffects()) {
            Location loc = this.getLocOfEvent();
            if (st.startsWith("Command:")){
                int seconds = 0;
                st = st.substring("Command:".length());
                if (loc != null){
                    st = st.replaceAll("%Location%", Objects.requireNonNull(loc.getWorld()).getName() + "," + loc.getX() + "," + (loc.getY() + 1) + "," + loc.getZ());
                }
                List<String> options = new ArrayList<>();
                Pattern pattern = Pattern.compile("\\[(.*?)\\]");
                Matcher matcher = pattern.matcher(st);
                while (matcher.find()) {
                    String textInsideBrackets = matcher.group(1);
                    options.add(textInsideBrackets);
                }
                if (!options.isEmpty()) {
                    seconds = Integer.parseInt(options.get(0));
                    st = st.replaceAll("[" + options.get(0) + "]", "");
                    st = st.replaceAll("\\[", "");
                    st = st.replaceAll("\\]", "");
                }
                ExeCommendAfter(seconds,st);
            }else if (st.startsWith("Objective:")){
                int seconds = 0;
                st = st.substring("Objective:".length());
                List<String> options = new ArrayList<>();
                Pattern pattern = Pattern.compile("\\[(.*?)\\]");
                Matcher matcher = pattern.matcher(st);
                while (matcher.find()) {
                    String textInsideBrackets = matcher.group(1);
                    options.add(textInsideBrackets);
                }
                if (!options.isEmpty()) {
                    seconds = Integer.parseInt(options.get(0));
                    st = st.replaceAll("[" + options.get(0) + "]", "");
                    st = st.replaceAll("\\[", "");
                    st = st.replaceAll("\\]", "");
                }
                String finalSt = st;
                Bukkit.getScheduler().runTaskLater(AmonPackPlugin.plugin, () -> {
                    dung.setCurrentObjective(finalSt);
                    for (Player p:dung.playerBossBars.keySet()) {
                            dung.CurrentObjectiveUpdater(p);
                    }
                }, 1+(seconds* 20L));

            }else if (st.startsWith("CommandForAllDungeon:")){
                int seconds = 0;
                st = st.substring("CommandForAllDungeon:".length());
                if (loc != null){
                    st = st.replaceAll("%Location%", Objects.requireNonNull(loc.getWorld()).getName() + "," + loc.getX() + "," + (loc.getY() + 1) + "," + loc.getZ());
                }
                List<String> options = new ArrayList<>();
                Pattern pattern = Pattern.compile("\\[(.*?)\\]");
                Matcher matcher = pattern.matcher(st);
                while (matcher.find()) {
                    String textInsideBrackets = matcher.group(1);
                    options.add(textInsideBrackets);
                }
                if (!options.isEmpty()) {
                    String tempoptionsmen = options.get(0).replaceAll("\\[", "");
                    tempoptionsmen = tempoptionsmen.replaceAll("\\]", "");
                    seconds = Integer.parseInt(tempoptionsmen);
                    String TempString = "["+options.get(0)+"]";
                    st = st.substring(0,(st.length()-TempString.length()));
                }
                for (Player p:Dungeons.PInDung(dung,dung.getXRange(), dung.getZRange())) {
                    st = st.replaceAll("%Player%", p.getName());
                    ExeCommendAfter(seconds,st);
                }
            }else if (st.startsWith("PotionEffect:")){
                int seconds;
                String Type;
                st = st.substring("PotionEffect:".length());
                List<String> options = new ArrayList<>();
                Pattern pattern = Pattern.compile("\\[(.*?)\\]");
                Matcher matcher = pattern.matcher(st);
                while (matcher.find()) {
                    String textInsideBrackets = matcher.group(1);
                    options.add(textInsideBrackets);
                }
                Type = options.get(0);
                seconds = Integer.parseInt(options.get(1));
                for (Player p:Dungeons.PInDung(dung,dung.getXRange(), dung.getZRange())) {
                    p.addPotionEffect(new PotionEffect(Objects.requireNonNull(PotionEffectType.getByName(Type)),seconds*20,10,false,false));
                }
            }else if (st.startsWith("ClearDung:")){
                st = st.substring("ClearDung:".length());
                int seconds;
                List<String> options = new ArrayList<>();
                Pattern pattern = Pattern.compile("\\[(.*?)\\]");
                Matcher matcher = pattern.matcher(st);
                while (matcher.find()) {
                    String textInsideBrackets = matcher.group(1);
                    options.add(textInsideBrackets);
                }
                if (!options.isEmpty()) {
                    String tempoptionsmen = options.get(0).replaceAll("\\[", "");
                    tempoptionsmen = tempoptionsmen.replaceAll("\\]", "");
                    seconds = Integer.parseInt(tempoptionsmen);
                    String TempString = "["+options.get(0)+"]";
                    st = st.substring(0,(st.length()-TempString.length()));
                    String finalSt1 = st;
                    Bukkit.getScheduler().runTaskLater(AmonPackPlugin.plugin, ()->{
                        Dungeons.KillMobsInDUng(dung, dung.getXRange(), dung.getZRange(), finalSt1);
                    }, seconds* 20L);
                }else{
                        Dungeons.KillMobsInDUng(dung, dung.getXRange(), dung.getZRange());
                }
            }else if (st.startsWith("Doors:")){
                List<String> DoorsLoc = new ArrayList<>();
                Pattern pattern = Pattern.compile("\\[(.*?)\\]");
                Matcher matcher = pattern.matcher(st);
                while (matcher.find()) {
                    String textInsideBrackets = matcher.group(1);
                    DoorsLoc.add(textInsideBrackets);
                }
                int choice = Integer.parseInt(DoorsLoc.get(7));
                Location loc2 = new Location(loc.getWorld(),Double.parseDouble(DoorsLoc.get(0))+dung.offset,Double.parseDouble(DoorsLoc.get(1)),Double.parseDouble(DoorsLoc.get(2)));
                Location loc3 = new Location(loc.getWorld(),Double.parseDouble(DoorsLoc.get(3))+dung.offset,Double.parseDouble(DoorsLoc.get(4)),Double.parseDouble(DoorsLoc.get(5)));
                Dungeon.Doors d1 = new Dungeon.Doors(loc2,loc3,Material.getMaterial(DoorsLoc.get(6)));
                dung.DoorsList.put(loc,d1);
                dung.DoorsMechanics(d1.l1,d1.l2,d1.m,choice);
            }else if (st.startsWith("SpawnMobsFor:")){
                List<String> MobSpawningEffect = new ArrayList<>();
                List<String> MobSpawningMobType = new ArrayList<>();
                Pattern pattern = Pattern.compile("\\[(.*?)\\]");
                Matcher matcher = pattern.matcher(st);
                while (matcher.find()) {
                    String textInsideBrackets = matcher.group(1);
                    MobSpawningEffect.add(textInsideBrackets);
                }
                int seconds = Integer.parseInt(MobSpawningEffect.get(0));
                int amount = Integer.parseInt(MobSpawningEffect.get(1));
                int Xoffset = Integer.parseInt(MobSpawningEffect.get(2));
                int Zoffset = Integer.parseInt(MobSpawningEffect.get(3));
                int peiod = Integer.parseInt(MobSpawningEffect.get(4));
                int delay = Integer.parseInt(MobSpawningEffect.get(5));
                for (int i = 0; i < MobSpawningEffect.size(); i++) {
                    if (i >=6){
                        MobSpawningMobType.add(MobSpawningEffect.get(i));
                    }}
                Bukkit.getScheduler().runTaskLater(AmonPackPlugin.plugin, () -> {
                    dung.SpawnMobsForX(this,seconds,amount,Xoffset,Zoffset,peiod,MobSpawningMobType);
                }, 1+(delay* 20L));
            }else if (st.startsWith("SpawnMobAndTeleport:")){
                List<String> MobSpawningEffect = new ArrayList<>();
                List<String> MobSpawningMobType = new ArrayList<>();
                Pattern pattern = Pattern.compile("\\[(.*?)\\]");
                Matcher matcher = pattern.matcher(st);
                while (matcher.find()) {
                    String textInsideBrackets = matcher.group(1);
                    MobSpawningEffect.add(textInsideBrackets);
                }
                int TPX = Integer.parseInt(MobSpawningEffect.get(0));
                int TPY = Integer.parseInt(MobSpawningEffect.get(1));
                int TPZ = Integer.parseInt(MobSpawningEffect.get(2));
                int amount = 0;
                if (MobSpawningEffect.get(3).startsWith("Amount:")){
                    amount=Integer.parseInt(MobSpawningEffect.get(3).substring(7));
                }
                for (int i = 0; i < MobSpawningEffect.size(); i++) {
                    if (i >=3){
                        if (!MobSpawningEffect.get(i).startsWith("Amount:")){
                            MobSpawningMobType.add(MobSpawningEffect.get(i));
                        }}}
                    dung.SpawnMobAndTP(this,TPX+offset,TPY,TPZ,MobSpawningMobType,amount);
            }}
    }

    public void ExeCommendAfter(int i, String st){
        Bukkit.getScheduler().runTaskLater(AmonPackPlugin.plugin, () -> {
            Commands.ExecuteCommandExample example = new Commands.ExecuteCommandExample();
            example.executeCommand(st);
                }, 1+(i* 20L));

    }

    public boolean checkconditions(DungEvent de, Location loc,Dungeon dung){
        int NoMobsCondition =0;
        List<String> allowedmobs = new ArrayList<>();
        int RangeCondition =20;
        String EType = "";
        String EName = "";
        String reqmobs = "";
        for (String con:de.getConditions()) {
            if (con != null && con.startsWith("NoMobs:")){
                con = con.substring("NoMobs:".length());
                String counter;
                if (con.length() > 2){
                    counter = con.substring(0,3).replaceAll(" ", "");
                }else{
                    counter = con;
                }
                List<String> options = new ArrayList<>();
                Pattern pattern = Pattern.compile("\\[(.*?)\\]");
                Matcher matcher = pattern.matcher(con);
                while (matcher.find()) {
                    String textInsideBrackets = matcher.group(1);
                    options.add(textInsideBrackets);
                }
                if (!options.isEmpty()) {
                    allowedmobs.addAll(options);
                }
                NoMobsCondition = Integer.parseInt(counter);
            }
            if (con != null && con.startsWith("ReqMobs:")){
                con = con.substring("ReqMobs:".length());
                List<String> options = new ArrayList<>();
                Pattern pattern = Pattern.compile("\\[(.*?)\\]");
                Matcher matcher = pattern.matcher(con);
                while (matcher.find()) {
                    String textInsideBrackets = matcher.group(1);
                    options.add(textInsideBrackets);
                }
                String counter;
                if (con.length() > 2){
                    counter = con.substring(0,3).replaceAll(" ", "");
                }else{
                    counter = con;
                }
                if (!options.isEmpty()) {
                    reqmobs = options.get(0);
                    allowedmobs.addAll(options);
                }
                NoMobsCondition = Integer.parseInt(counter);
            }
            if (con != null && con.startsWith("RangeLoc:")){
                con = con.substring("RangeLoc:".length());
                RangeCondition = Integer.parseInt(con);
            }
        if (con != null && con.startsWith("UsedEvent:")){
            List<String> TypeNameEvent = new ArrayList<>();
            Pattern pattern = Pattern.compile("\\[(.*?)\\]");
            Matcher matcher = pattern.matcher(con);
            while (matcher.find()) {
                String textInsideBrackets = matcher.group(1);
                TypeNameEvent.add(textInsideBrackets);
            }
            EType = TypeNameEvent.get(0);
            EName = TypeNameEvent.get(1);
        }}
        if (loc.getWorld().equals(de.getLocOfEvent().getWorld())&& loc.distance(de.getLocOfEvent()) <= RangeCondition){
            if ((!reqmobs.equalsIgnoreCase("") && ReqMob(de.getLocOfEvent(),NoMobsCondition,5,reqmobs)) || (reqmobs.equalsIgnoreCase("") && Dungeon.EnemiesAround(de.getLocOfEvent(), NoMobsCondition, 5,allowedmobs))){
                    if (EName.equalsIgnoreCase("") && EType.equalsIgnoreCase("")){
                        return true;
                    } else{
                        for (DungEvent dunge:dung.UsedDungEvents) {
                            if (dunge.getType().equalsIgnoreCase(EType) && dunge.getNum().equalsIgnoreCase(EName)){
                                return true;
                            }}}}}
        return false;
    }
    public void changeloc(int i, World w){
        Location loc = new Location(w,LocOfEvent.getX(),LocOfEvent.getY(),LocOfEvent.getZ());
        LocOfEvent = loc.clone();
        offset = 0;
    }

    public void ChangeWorldE(World w) {
        LocOfEvent.clone().setWorld(w);
    }

    public Location getLocOfEvent() {
        return LocOfEvent;
    }
    public List<String> getEffects() {
        return Effects;
    }
    public List<String> getConditions() {
        return Conditions;
    }
    public String getType() {
        return type;
    }
    public EntityType getEntity() {
        return entity;
    }
    public String getEntityname() {
        return entityname;
    }
    public int getRadius() {
        return radius;
    }
    public int getTime() {
        return charging;
    }
    public String getParticle() {
        return particle;
    }
    public String getNum() {
        return num;
    }
    public void setActkills(int actkills) {
        this.actkills = actkills;
    }
}
*/