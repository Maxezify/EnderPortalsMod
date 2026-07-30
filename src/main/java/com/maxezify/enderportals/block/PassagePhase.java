package com.maxezify.enderportals.block;

import net.minecraft.util.StringRepresentable;

/**
 * Les trois états visibles d'un Passage des Alliés.
 *
 * <p>{@link #OPENING} n'est pas un détail d'esthétique : c'est l'état pendant
 * lequel le passage est déjà engagé mais pas encore franchissable. Le distinguer
 * de {@link #OPEN} évite qu'un joueur traverse au premier tick de l'animation,
 * avant que son pair ait vu quoi que ce soit.</p>
 */
public enum PassagePhase implements StringRepresentable {

    CLOSED("closed"),
    OPENING("opening"),
    OPEN("open"),
    /**
     * Ouvert <b>et</b> doublé d'un portail Immersive Portals.
     *
     * <p>Franchissable exactement comme {@link #OPEN}, à une différence près :
     * son modèle n'a pas de voile. Le voile est une face pleine au milieu du
     * bloc, là même où se pose le plan du portail — il l'occulterait, comme le
     * fond du caisson occultait celui de la Porte de l'Ender jusqu'à la 0.6.1.
     * Le retirer du modèle est plus simple que de l'escamoter au rendu, et
     * l'arche garde son cadre.</p>
     *
     * <p>La traversée par contact est aussi désactivée dans cette phase :
     * Immersive Portals s'en charge, et les deux mécanismes ensemble se
     * marcheraient dessus.</p>
     */
    THROUGH("through");

    private final String name;

    PassagePhase(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
