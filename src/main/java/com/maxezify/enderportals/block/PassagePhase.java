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
    OPEN("open");

    private final String name;

    PassagePhase(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
