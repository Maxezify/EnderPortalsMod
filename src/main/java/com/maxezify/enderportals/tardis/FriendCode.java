package com.maxezify.enderportals.tardis;

/**
 * La forme d'un code d'ami : sa longueur, et la façon dont il s'écrit.
 *
 * <p>Cette classe existe parce que la règle était écrite <b>trois fois</b> — au
 * terminal du Contrôle de l'amitié, sur l'infobulle de la clé, et dans l'écran
 * client — avec deux constantes de longueur indépendantes qui valaient huit
 * chacune de leur côté. Rien n'était faux, et rien n'aurait signalé qu'une des
 * trois cesse de suivre les autres : un code passé à six chiffres se serait
 * affiché en deux groupes ici et d'un bloc ailleurs, sans qu'aucun contrôle ne
 * s'en aperçoive.</p>
 */
public final class FriendCode {

    /**
     * Longueur d'un code d'ami. Fixe, donc la frappe au pavé du Contrôle de
     * l'amitié fait toujours exactement huit touches et il n'y a aucune
     * longueur variable à gérer.
     */
    public static final int DIGITS = 8;

    /** Position de la coupure à l'affichage. */
    private static final int GROUP = 4;

    /**
     * Le code tel qu'on le lit et tel qu'on le dicte : deux groupes de quatre.
     *
     * <p>Un code de longueur inattendue est rendu tel quel plutôt que découpé
     * au hasard — une clé forgée à la commande peut en porter un, et une
     * infobulle n'a pas à faire planter le client pour si peu.</p>
     */
    public static String format(int code) {
        String digits = Integer.toString(code);
        return digits.length() == DIGITS
                ? digits.substring(0, GROUP) + " " + digits.substring(GROUP)
                : digits;
    }

    private FriendCode() {
    }
}
