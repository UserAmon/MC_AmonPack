package CustomContent.Bosses;

import org.bukkit.Location;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class BossAnimator {

    public enum AnimationState {
        IDLE,
        WALK,
        ATTACK,
        CAST,
        HURT,
        DEATH
    }

    private AnimationState currentState = AnimationState.IDLE;
    private int stateTicks = 0;
    private int attackDuration = 15;
    private int castDuration = 30;
    private int hurtDuration = 8;
    private int deathDuration = 40;

    private float baseScale = 2.0f;
    private Location lastLocation = null;

    public BossAnimator(float baseScale) {
        this.baseScale = baseScale;
    }

    public void setBaseScale(float baseScale) {
        this.baseScale = baseScale;
    }

    public void triggerAttack() {
        if (currentState != AnimationState.DEATH) {
            this.currentState = AnimationState.ATTACK;
            this.stateTicks = 0;
        }
    }

    public void triggerCast() {
        if (currentState != AnimationState.DEATH) {
            this.currentState = AnimationState.CAST;
            this.stateTicks = 0;
        }
    }

    public void triggerHurt() {
        if (currentState != AnimationState.DEATH && currentState != AnimationState.ATTACK) {
            this.currentState = AnimationState.HURT;
            this.stateTicks = 0;
        }
    }

    public void triggerDeath() {
        this.currentState = AnimationState.DEATH;
        this.stateTicks = 0;
    }

    public boolean isDead() {
        return currentState == AnimationState.DEATH && stateTicks >= deathDuration;
    }

    public void tick(Location currentLocation) {
        stateTicks++;

        if (currentState == AnimationState.ATTACK && stateTicks >= attackDuration) {
            currentState = AnimationState.IDLE;
            stateTicks = 0;
        } else if (currentState == AnimationState.CAST && stateTicks >= castDuration) {
            currentState = AnimationState.IDLE;
            stateTicks = 0;
        } else if (currentState == AnimationState.HURT && stateTicks >= hurtDuration) {
            currentState = AnimationState.IDLE;
            stateTicks = 0;
        } else if (currentState != AnimationState.DEATH && currentState != AnimationState.ATTACK && currentState != AnimationState.CAST && currentState != AnimationState.HURT) {
            // Check if walking
            if (lastLocation != null && lastLocation.getWorld().equals(currentLocation.getWorld())) {
                double distSq = lastLocation.distanceSquared(currentLocation);
                if (distSq > 0.003) {
                    currentState = AnimationState.WALK;
                } else {
                    currentState = AnimationState.IDLE;
                }
            } else {
                currentState = AnimationState.IDLE;
            }
        }

        this.lastLocation = currentLocation.clone();
    }

    public Vector3f computeTranslation() {
        float transX = 0f;
        float transY = 0f;
        float transZ = 0f;

        switch (currentState) {
            case IDLE -> {
                // Delikatne oddychanie góra/dół
                transY = (float) Math.sin(stateTicks * 0.15) * 0.05f;
            }
            case WALK -> {
                // Krok / bobbing
                transY = (float) Math.abs(Math.sin(stateTicks * 0.45)) * 0.12f;
                transZ = (float) Math.sin(stateTicks * 0.45) * 0.04f;
            }
            case ATTACK -> {
                float prog = (float) stateTicks / attackDuration;
                if (prog < 0.4f) {
                    // Zamach w tył i do góry
                    transY = 0.25f * (prog / 0.4f);
                    transZ = -0.3f * (prog / 0.4f);
                } else if (prog < 0.7f) {
                    // Potężne uderzenie w dół i przód
                    float hitProg = (prog - 0.4f) / 0.3f;
                    transY = 0.25f - (0.45f * hitProg);
                    transZ = -0.3f + (0.6f * hitProg);
                } else {
                    // Powrót
                    float recProg = (prog - 0.7f) / 0.3f;
                    transY = -0.2f * (1.0f - recProg);
                    transZ = 0.3f * (1.0f - recProg);
                }
            }
            case CAST -> {
                // Lewitacja i pulsowanie energii
                transY = 0.35f + (float) Math.sin(stateTicks * 0.3) * 0.1f;
            }
            case HURT -> {
                // Odrzut do tyłu
                float prog = (float) stateTicks / hurtDuration;
                transZ = -0.2f * (1.0f - prog);
                transY = (float) Math.sin(prog * Math.PI) * 0.08f;
            }
            case DEATH -> {
                // Zapadanie się w ziemię
                float prog = Math.min(1.0f, (float) stateTicks / deathDuration);
                transY = -prog * 1.5f;
            }
        }

        return new Vector3f(transX, transY, transZ);
    }

    public Vector3f computeScale() {
        float sx = baseScale;
        float sy = baseScale;
        float sz = baseScale;

        switch (currentState) {
            case IDLE -> {
                float pulse = (float) Math.sin(stateTicks * 0.15) * 0.02f;
                sy += pulse * baseScale;
                sx -= pulse * 0.5f * baseScale;
                sz -= pulse * 0.5f * baseScale;
            }
            case WALK -> {
                float squash = (float) Math.sin(stateTicks * 0.45) * 0.04f;
                sy -= squash * baseScale;
                sx += squash * 0.5f * baseScale;
            }
            case ATTACK -> {
                float prog = (float) stateTicks / attackDuration;
                if (prog >= 0.4f && prog < 0.7f) {
                    // Rozciągnięcie przy uderzeniu
                    sy *= 1.1f;
                    sz *= 1.15f;
                }
            }
            case CAST -> {
                float charge = 1.0f + (float) Math.sin(stateTicks * 0.4) * 0.08f;
                sx *= charge;
                sy *= charge;
                sz *= charge;
            }
            case DEATH -> {
                float prog = Math.min(1.0f, (float) stateTicks / deathDuration);
                float fadeScale = 1.0f - (prog * 0.7f);
                sx *= fadeScale;
                sy *= fadeScale;
                sz *= fadeScale;
            }
        }

        return new Vector3f(sx, sy, sz);
    }

    public AxisAngle4f computeRotation() {
        float angle = 0f;
        float rx = 1f;
        float ry = 0f;
        float rz = 0f;

        switch (currentState) {
            case WALK -> {
                // Przechył w przód podczas marszu
                angle = (float) Math.toRadians(6.0);
            }
            case ATTACK -> {
                float prog = (float) stateTicks / attackDuration;
                if (prog < 0.4f) {
                    angle = (float) Math.toRadians(-18.0 * (prog / 0.4f));
                } else if (prog < 0.7f) {
                    float hitProg = (prog - 0.4f) / 0.3f;
                    angle = (float) Math.toRadians(-18.0 + (42.0 * hitProg));
                } else {
                    float recProg = (prog - 0.7f) / 0.3f;
                    angle = (float) Math.toRadians(24.0 * (1.0f - recProg));
                }
            }
            case HURT -> {
                float prog = (float) stateTicks / hurtDuration;
                angle = (float) Math.toRadians(-14.0 * (1.0f - prog));
            }
            case DEATH -> {
                float prog = Math.min(1.0f, (float) stateTicks / deathDuration);
                angle = (float) Math.toRadians(prog * 35.0);
            }
        }

        return new AxisAngle4f(angle, rx, ry, rz);
    }

    public AnimationState getCurrentState() {
        return currentState;
    }
}
