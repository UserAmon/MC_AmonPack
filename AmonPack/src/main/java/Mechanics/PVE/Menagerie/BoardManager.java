package Mechanics.PVE.Menagerie;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

public class BoardManager {

    public BoardManager() {
    }

    public void addRowsWithoutremove(Player player,String texts, int i2) {
        Objective objective = null;
        for (Objective o:player.getScoreboard().getObjectives()) {
            if(o.getName().contains("Slots")){
                objective=o;
            }}
        if (objective == null) {

            for (Objective o:player.getScoreboard().getObjectives()) {
                throw new IllegalStateException("Objective 'Board Slots' does not exist.  try "+o.getName());
            }

        }

            if(texts.length()>6&& texts.substring(0,3).contains("&")){
                String text = texts.substring(2);
                Score score = objective.getScore(ChatColor.getByChar(texts.substring(0,2))+text);
                score.setScore(i2);
            }else{
                String text = texts;
                Score score = objective.getScore(ChatColor.DARK_PURPLE+ text);
                score.setScore(i2);
        }
    }


    public void addRows(Player player, String[] texts) {
        Scoreboard scoreboard = player.getScoreboard();
        Objective objective = null;
        for (Objective o:player.getScoreboard().getObjectives()) {
            if(o.getName().contains("Slots")){
                objective=o;
            }}
        if (objective == null) {
            for (Objective o:player.getScoreboard().getObjectives()) {
                throw new IllegalStateException("Objective 'Board Slots' does not exist.  try "+o.getName());
            }}
        for (String entry : scoreboard.getEntries()) {
            Score score = objective.getScore(entry);
            if (score.getScore() >= 0) {
                scoreboard.resetScores(entry);
            }
        }
        objective.getScore("").setScore(0);
        for (int i = 0; i < texts.length; i++) {
            if(texts[i].length()>6&& texts[i].substring(3,5).contains(":")){
                String text = texts[i].substring(4);
                Score score = objective.getScore(ChatColor.DARK_PURPLE+text);
                score.setScore(Integer.parseInt(texts[i].substring(2,3)));
            }else{
                String text = texts[i];
                int newScore = 1;
                Score score = objective.getScore(ChatColor.DARK_PURPLE+ text);
                score.setScore(newScore);
            }
        }
        objective.getScore("    ").setScore(6);
    }

    public void removeTopRows(Player player) {
        Scoreboard scoreboard = player.getScoreboard();
        Objective objective = null;
        for (Objective o:player.getScoreboard().getObjectives()) {
            if(o.getName().contains("Slots")){
                objective=o;
            }}

        if (objective == null) {
            throw new IllegalStateException("Objective 'Board Slots' does not exist.");
        }

        for (String entry : scoreboard.getEntries()) {
            Score score = objective.getScore(entry);
            if (score.getScore() >= 0) {
                scoreboard.resetScores(entry);
            }
        }/*

        Map<String, Integer> scores = new HashMap<>();
        for (String entry : scoreboard.getEntries()) {
            Score score = objective.getScore(entry);
            scores.put(entry, score.getScore());
        }

        PriorityQueue<Map.Entry<String, Integer>> pq = new PriorityQueue<>(
                (a, b) -> Integer.compare(b.getValue(), a.getValue())
        );
        pq.addAll(scores.entrySet());

        int count = 0;
        while (!pq.isEmpty() && count < n) {
            Map.Entry<String, Integer> highestEntry = pq.poll();
            scoreboard.resetScores(highestEntry.getKey());
            count++;
        }*/

    }
}
