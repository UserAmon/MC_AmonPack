package AvatarSystems.Bounties.Objects;

import java.util.List;

public class Bounty {
    private String id;
    private String displayName;
    private List<String> lore;
    private BountyType type;
    private String target;
    private int amount;
    private List<String> rewards;

    public enum BountyType {
        KILL_MOB,
        MINING,
        LUMBERING,
        FARMING
    }

    public Bounty(String id, String displayName, List<String> lore, BountyType type, String target, int amount,
            List<String> rewards) {
        this.id = id;
        this.displayName = displayName;
        this.lore = lore;
        this.type = type;
        this.target = target;
        this.amount = amount;
        this.rewards = rewards;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getLore() {
        return lore;
    }

    public BountyType getType() {
        return type;
    }

    public String getTarget() {
        return target;
    }

    public int getAmount() {
        return amount;
    }

    public List<String> getRewards() {
        return rewards;
    }
}
