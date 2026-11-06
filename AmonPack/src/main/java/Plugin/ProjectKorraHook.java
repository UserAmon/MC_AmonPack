package Plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * Bezpieczny adapter mostkujący dla ProjectKorra.
 * Zapewnia, że w przypadku braku pluginu ProjectKorra na serwerze,
 * żadne klasy com.projectkorra.* nie są ładowane przez JVM, zapobiegając NoClassDefFoundError.
 */
public class ProjectKorraHook {

    public static boolean isEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled("ProjectKorra");
    }

    public static boolean hasElement(Player player, String elementName) {
        if (!isEnabled() || player == null || elementName == null) return false;
        try {
            return PKInternal.hasElement(player, elementName);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static int countHealingOrBloodAbilities(Player player) {
        if (!isEnabled() || player == null) return 0;
        try {
            return PKInternal.countHealingOrBloodAbilities(player);
        } catch (Throwable ignored) {
            return 0;
        }
    }

    public static void grantTemporaryElement(Player player, String elementName, RPG.Levels.BendingTree.PlayerBendingBranch branch) {
        if (!isEnabled() || player == null || elementName == null) return;
        try {
            PKInternal.grantTemporaryElement(player, elementName, branch);
        } catch (Throwable ignored) {}
    }

    public static void cleanupTemporaryElements(Player player, RPG.Levels.BendingTree.PlayerBendingBranch branch) {
        if (!isEnabled() || player == null) return;
        try {
            PKInternal.cleanupTemporaryElements(player, branch);
        } catch (Throwable ignored) {}
    }

    public static void blockChi(Player player) {
        if (!isEnabled() || player == null) return;
        try {
            PKInternal.blockChi(player);
        } catch (Throwable ignored) {}
    }

    public static void unblockChi(Player player) {
        if (!isEnabled() || player == null) return;
        try {
            PKInternal.unblockChi(player);
        } catch (Throwable ignored) {}
    }

    public static void resetAllElements(Player player, RPG.Levels.BendingTree.PlayerBendingBranch playersBranch) {
        if (!isEnabled() || player == null) return;
        try {
            PKInternal.resetAllElements(player, playersBranch);
        } catch (Throwable ignored) {}
    }

    public static void selectElement(Player player, String elementName, RPG.Levels.BendingTree.PlayerBendingBranch playersBranch) {
        if (!isEnabled() || player == null) return;
        try {
            PKInternal.selectElement(player, elementName, playersBranch);
        } catch (Throwable ignored) {}
    }

    public static void clearBendingBoardAndAbilities(Player player, RPG.Levels.BendingTree.ElementTree selectedElement, RPG.Levels.BendingTree.PlayerBendingBranch playersBranch) {
        if (!isEnabled() || player == null) return;
        try {
            PKInternal.clearBendingBoardAndAbilities(player, selectedElement, playersBranch);
        } catch (Throwable ignored) {}
    }

    public static void lockAndRemoveAbility(Player player, String lockedName, RPG.Levels.BendingTree.PlayerBendingBranch playersBranch) {
        if (!isEnabled() || player == null) return;
        try {
            PKInternal.lockAndRemoveAbility(player, lockedName, playersBranch);
        } catch (Throwable ignored) {}
    }

    public static void bindAbility(Player player, String abilityName, int slot) {
        if (!isEnabled() || player == null) return;
        try {
            PKInternal.bindAbility(player, abilityName, slot);
        } catch (Throwable ignored) {}
    }

    public static RPG.Levels.BendingTree.PlayerBendingBranch createDefaultPlayerBranch(String playerName) {
        return new RPG.Levels.BendingTree.PlayerBendingBranch(0, null, 0, new java.util.ArrayList<>(), 0, playerName, new java.util.ArrayList<>(), 0);
    }

    public static RPG.Levels.BendingTree.ElementTree getElementTree(RPG.Levels.BendingTree.PlayerBendingBranch branch) {
        if (!isEnabled() || branch == null || AmonPackPlugin.levelsBending == null) return null;
        try {
            return PKInternal.getElementTree(branch);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static void handleLevelMagicCommand(org.bukkit.command.CommandSender sender, String[] args) {
        if (!isEnabled()) {
            sender.sendMessage(org.bukkit.ChatColor.RED + "System magii Bending wymaga pluginu ProjectKorra na serwerze!");
            return;
        }
        try {
            PKInternal.handleLevelMagicCommand(sender, args);
        } catch (Throwable e) {
            sender.sendMessage(org.bukkit.ChatColor.RED + "Błąd komendy: " + e.getMessage());
        }
    }

    /**
     * Wewnętrzna klasa ładowana przez ClassLoader tylko wtedy, gdy ProjectKorra jest faktycznie obecny na serwerze.
     */
    private static class PKInternal {

        static boolean hasElement(Player player, String elementName) {
            com.projectkorra.projectkorra.BendingPlayer bp = com.projectkorra.projectkorra.BendingPlayer.getBendingPlayer(player);
            if (bp == null) return false;
            com.projectkorra.projectkorra.Element el = com.projectkorra.projectkorra.Element.getElement(elementName);
            return el != null && bp.hasElement(el);
        }

        static int countHealingOrBloodAbilities(Player player) {
            com.projectkorra.projectkorra.BendingPlayer bp = com.projectkorra.projectkorra.BendingPlayer.getBendingPlayer(player);
            if (bp == null) return 0;
            int count = 0;
            for (int i = 1; i <= 9; i++) {
                String abilityName = bp.getAbilities().get(i);
                if (abilityName != null && !abilityName.isEmpty()) {
                    com.projectkorra.projectkorra.ability.CoreAbility ability = com.projectkorra.projectkorra.ability.CoreAbility.getAbility(abilityName);
                    if (ability != null) {
                        if (ability instanceof com.projectkorra.projectkorra.ability.HealingAbility
                                || ability instanceof com.projectkorra.projectkorra.ability.BloodAbility
                                || (ability.getElement() != null && (ability.getElement().getName().equalsIgnoreCase("Healing") || ability.getElement().getName().equalsIgnoreCase("Blood")))) {
                            count++;
                        }
                    }
                }
            }
            return count;
        }

        static void grantTemporaryElement(Player player, String elementName, RPG.Levels.BendingTree.PlayerBendingBranch branch) {
            com.projectkorra.projectkorra.BendingPlayer bp = com.projectkorra.projectkorra.BendingPlayer.getBendingPlayer(player);
            if (bp == null) return;
            com.projectkorra.projectkorra.Element el = com.projectkorra.projectkorra.Element.getElement(elementName);
            if (el != null && !bp.hasElement(el)) {
                bp.getElements().add(el);
                if (branch != null) {
                    branch.getTemporaryElements().add(el);
                }
                player.sendMessage("§a[Nagroda] §dPrzyznano Ci tymczasowy żywioł: §f" + el.getName() + " §dna czas tego lochu!");
            }
        }

        static void cleanupTemporaryElements(Player player, RPG.Levels.BendingTree.PlayerBendingBranch branch) {
            if (branch == null) return;
            com.projectkorra.projectkorra.BendingPlayer bp = com.projectkorra.projectkorra.BendingPlayer.getBendingPlayer(player);
            if (bp != null) {
                for (com.projectkorra.projectkorra.Element el : branch.getTemporaryElements()) {
                    bp.getElements().remove(el);
                }
            }
            branch.getTemporaryElements().clear();
        }

        static void blockChi(Player player) {
            com.projectkorra.projectkorra.BendingPlayer bp = com.projectkorra.projectkorra.BendingPlayer.getBendingPlayer(player);
            if (bp != null && !bp.isChiBlocked()) {
                bp.blockChi();
            }
        }

        static void unblockChi(Player player) {
            com.projectkorra.projectkorra.BendingPlayer bp = com.projectkorra.projectkorra.BendingPlayer.getBendingPlayer(player);
            if (bp != null && bp.isChiBlocked()) {
                bp.unblockChi();
            }
        }

        static void resetAllElements(Player player, RPG.Levels.BendingTree.PlayerBendingBranch playersBranch) {
            com.projectkorra.projectkorra.BendingPlayer bPlayer = com.projectkorra.projectkorra.BendingPlayer.getBendingPlayer(player);
            if (bPlayer != null) {
                for (com.projectkorra.projectkorra.Element el : new com.projectkorra.projectkorra.Element[] {
                        com.projectkorra.projectkorra.Element.AIR,
                        com.projectkorra.projectkorra.Element.WATER,
                        com.projectkorra.projectkorra.Element.FIRE,
                        com.projectkorra.projectkorra.Element.EARTH }) {
                    RPG.Levels.BendingTree.ElementTree tree = (AmonPackPlugin.levelsBending != null) ? AmonPackPlugin.levelsBending.GetElement(el) : null;
                    if (tree != null && playersBranch != null) {
                        playersBranch.ClearAbilities(tree);
                    }
                }
                if (playersBranch != null) {
                    playersBranch.getElementsInPossesion().clear();
                    playersBranch.SetCurrentElement(null);
                    playersBranch.AddPoints(com.projectkorra.projectkorra.Element.AIR, -playersBranch.getAirPoints());
                    playersBranch.AddPoints(com.projectkorra.projectkorra.Element.WATER, -playersBranch.getWaterPoints());
                    playersBranch.AddPoints(com.projectkorra.projectkorra.Element.FIRE, -playersBranch.getFirePoints());
                    playersBranch.AddPoints(com.projectkorra.projectkorra.Element.EARTH, -playersBranch.getEarthPoints());
                }
                bPlayer.getElements().clear();
                bPlayer.getAbilities().clear();
                bPlayer.removeUnusableAbilities();
                bPlayer.saveElements();
                for (int i = 1; i <= 9; i++) {
                    bPlayer.saveAbility("", i);
                }
            }
        }

        static void selectElement(Player player, String elementName, RPG.Levels.BendingTree.PlayerBendingBranch playersBranch) {
            com.projectkorra.projectkorra.BendingPlayer bPlayer = com.projectkorra.projectkorra.BendingPlayer.getBendingPlayer(player);
            com.projectkorra.projectkorra.Element el = com.projectkorra.projectkorra.Element.getElement(elementName);
            if (bPlayer != null && el != null) {
                bPlayer.getElements().clear();
                bPlayer.addElement(el);
                bPlayer.saveElements();
                if (playersBranch != null) {
                    if (!playersBranch.getElementsInPossesion().contains(el)) {
                        playersBranch.getElementsInPossesion().add(el);
                    }
                    playersBranch.SetCurrentElement(el);
                    playersBranch.AddPoints(el, 5000);
                    playersBranch.unlockDefaultAbilities();
                }
            }
        }

        static void clearBendingBoardAndAbilities(Player player, RPG.Levels.BendingTree.ElementTree selectedElement, RPG.Levels.BendingTree.PlayerBendingBranch playersBranch) {
            com.projectkorra.projectkorra.BendingPlayer bPlayer = com.projectkorra.projectkorra.BendingPlayer.getBendingPlayer(player);
            if (bPlayer != null && selectedElement != null) {
                for (int i = 0; i <= 9; i++) {
                    String abilityName = bPlayer.getAbilities().get(i);
                    if (abilityName != null) {
                        com.projectkorra.projectkorra.ability.CoreAbility coreAbility = com.projectkorra.projectkorra.ability.CoreAbility.getAbility(abilityName);
                        if (coreAbility != null && (coreAbility.getElement().equals(selectedElement.getElement())
                                || (coreAbility.getElement() instanceof com.projectkorra.projectkorra.Element.SubElement && ((com.projectkorra.projectkorra.Element.SubElement) coreAbility.getElement()).getParentElement().equals(selectedElement.getElement())))) {
                            final int slot = i;
                            com.projectkorra.projectkorra.board.BendingBoardManager.getBoard(player).ifPresent(board -> board.clearSlot(slot));
                            bPlayer.getAbilities().remove(slot);
                        }
                    }
                }
                if (playersBranch != null) {
                    playersBranch.ClearAbilities(selectedElement);
                }
                bPlayer.removeUnusableAbilities();
            }
        }

        static void lockAndRemoveAbility(Player player, String lockedName, RPG.Levels.BendingTree.PlayerBendingBranch playersBranch) {
            if (playersBranch != null) {
                playersBranch.LockAndRemoveAbility(lockedName);
            }
            com.projectkorra.projectkorra.BendingPlayer bPlayer = com.projectkorra.projectkorra.BendingPlayer.getBendingPlayer(player);
            if (bPlayer != null) {
                for (int i = 0; i <= 9; i++) {
                    final int slot = i;
                    if (bPlayer.getAbilities().containsKey(slot) && lockedName.equalsIgnoreCase(bPlayer.getAbilities().get(slot))) {
                        com.projectkorra.projectkorra.board.BendingBoardManager.getBoard(player).ifPresent(board -> board.clearSlot(slot));
                        bPlayer.getAbilities().remove(slot);
                    }
                }
            }
        }

        static void handleLevelMagicCommand(org.bukkit.command.CommandSender sender, String[] args) {
            org.bukkit.entity.Player p = Bukkit.getPlayer(args[1]);
            if (p == null) return;
            RPG.Levels.BendingTree.PlayerBendingBranch branch = (AmonPackPlugin.levelsBending != null) ? AmonPackPlugin.levelsBending.GetBranchByPlayerName(p.getName()) : null;
            com.projectkorra.projectkorra.Element el = com.projectkorra.projectkorra.Element.getElement(args[3]);
            if (branch != null) {
                if (args[2].equalsIgnoreCase("Set")) {
                    branch.SetCurrentElement(el);
                } else {
                    int points = Integer.parseInt(args[2]);
                    if (args[3].equalsIgnoreCase("All")) {
                        branch.AddAllPoints(points);
                    } else if (el != null) {
                        branch.AddPoints(el, points);
                    }
                }
            } else if (el != null) {
                int points = Integer.parseInt(args[2]);
                java.util.List<com.projectkorra.projectkorra.Element> elements = new java.util.ArrayList<>();
                java.util.List<String> abilities = new java.util.ArrayList<>();
                elements.add(el);
                branch = new RPG.Levels.BendingTree.PlayerBendingBranch(0, el, 0, elements, 0, p.getName(), abilities, 0);
                AmonPackPlugin.levelsBending.AddNewBranch(branch);
                branch.AddPoints(el, points);
            }
        }

        static void bindAbility(Player player, String abilityName, int slot) {
            com.projectkorra.projectkorra.BendingPlayer bPlayer = com.projectkorra.projectkorra.BendingPlayer.getBendingPlayer(player);
            if (bPlayer != null) {
                bPlayer.bindAbility(abilityName, slot);
                bPlayer.saveAbility(abilityName, slot);
            }
        }

        static RPG.Levels.BendingTree.ElementTree getElementTree(RPG.Levels.BendingTree.PlayerBendingBranch branch) {
            if (branch == null || branch.getCurrentElement() == null) return null;
            return AmonPackPlugin.levelsBending.GetElement(branch.getCurrentElement());
        }
    }
}
