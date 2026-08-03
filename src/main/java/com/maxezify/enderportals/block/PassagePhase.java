package com.maxezify.enderportals.block;

import net.minecraft.util.StringRepresentable;

/**
 * Les quatre états d'un Passage des Alliés.
 *
 * <p>{@link #OPENING} n'est pas un détail d'esthétique : c'est l'état pendant
 * lequel le passage est déjà engagé mais pas encore franchissable. Le distinguer
 * de {@link #OPEN} évite qu'un joueur traverse au premier tick de l'animation,
 * avant que son pair ait vu quoi que ce soit.</p>
 *
 * <p>Deux invariants tiennent le reste debout, et il faut les lire ensemble :
 * <b>une arche pleine ne porte jamais de portail</b> ({@link #CLOSED} et
 * {@link #OPENING} démontent la paire), et <b>une arche traversée par un
 * portail n'a rien dans son embrasure</b> ({@link #THROUGH} n'y dessine pas de
 * voile). Le défaut de la 0.15.0 était très exactement la violation du premier :
 * un portail visible devant un bloc resté plein, qu'on ne pouvait pas
 * franchir.</p>
 */
public enum PassagePhase implements StringRepresentable {

    CLOSED("closed"),
    OPENING("opening"),
    OPEN("open"),
    /**
     * Ouvert <b>et</b> doublé d'un portail Immersive Portals.
     *
     * <p>Franchissable exactement comme {@link #OPEN}, à une différence près :
     * le renderer n'y dessine aucun voile. Le voile occupe le plan même où se
     * pose le portail — il l'occulterait, comme le fond du caisson occultait
     * celui de la Porte de l'Ender jusqu'à la 0.6.1.</p>
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
