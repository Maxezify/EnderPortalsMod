package com.maxezify.enderportals.tardis;

import net.minecraft.world.entity.player.Player;

/**
 * Le prix des machines de l'Ender, en points d'expérience.
 *
 * <p>Le mod facture deux gestes : ranger à distance avec le Sac, et expédier une
 * créature avec le Téléporteur. Les deux déplacent de la matière à travers les
 * mondes, et tous deux se paient de la même monnaie — d'où ce comptage commun
 * plutôt qu'une copie de la formule dans chaque machine.</p>
 *
 * <p><b>Points, pas niveaux.</b> Minecraft affiche des niveaux mais compte des
 * points, et la conversion n'est pas linéaire : du niveau 30 au 31 il faut plus
 * de sept fois ce qu'il faut du niveau 1 au 2. Facturer « un niveau » ferait
 * donc payer au débutant une bouchée et au vétéran une fortune. La formule
 * ci-dessous est celle de vanilla, reprise telle quelle pour que le compte du
 * mod tombe exactement sur celui de la barre verte.</p>
 */
public final class EnderXp {

    /** Points d'expérience actuellement détenus par le joueur. */
    public static int points(Player player) {
        return forLevel(player.experienceLevel)
                + Math.round(player.experienceProgress * player.getXpNeededForNextLevel());
    }

    /** Le joueur peut-il payer ce prix ? */
    public static boolean has(Player player, int cost) {
        return points(player) >= cost;
    }

    /**
     * Prélève le prix. À n'appeler qu'une fois le service rendu : une machine
     * qui échoue ne doit rien coûter.
     */
    public static void charge(Player player, int cost) {
        if (cost > 0) {
            player.giveExperiencePoints(-cost);
        }
    }

    /** Points cumulés nécessaires pour atteindre un niveau (formule vanilla). */
    private static int forLevel(int level) {
        if (level <= 0) {
            return 0;
        }
        if (level <= 16) {
            return level * level + 6 * level;
        }
        if (level <= 31) {
            return (int) (2.5 * level * level - 40.5 * level + 360.0);
        }
        return (int) (4.5 * level * level - 162.5 * level + 2220.0);
    }

    private EnderXp() {
    }
}
