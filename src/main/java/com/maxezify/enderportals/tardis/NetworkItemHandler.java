package com.maxezify.enderportals.tardis;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.List;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Vue {@link IItemHandler} agrégée sur un réseau de rangements — le
 * « Centraliseur-port ». Un slot global est projeté sur (sous-handler, slot
 * local), et chaque opération est déléguée au rangement réel concerné.
 *
 * <p>La liste des sous-handlers est résolue <strong>paresseusement, une fois
 * par tick</strong> : {@link #ensureFresh()} ne relance le parcours du réseau
 * que si l'horloge du monde a avancé. Cela garde la vue vivante (au plus un
 * tick de retard quand on ajoute ou casse un coffre) sans relancer un parcours
 * complet à chaque lecture de slot d'un terminal.</p>
 */
public class NetworkItemHandler implements IItemHandler {

    private final Supplier<List<IItemHandler>> partsSupplier;
    private final LongSupplier clock;

    private List<IItemHandler> parts = List.of();
    /** Décalage de slot cumulé : base[i] = premier slot global du sous-handler i. */
    private int[] base = new int[]{0};
    private int totalSlots;
    private long resolvedTick = Long.MIN_VALUE;

    public NetworkItemHandler(Supplier<List<IItemHandler>> partsSupplier, LongSupplier clock) {
        this.partsSupplier = partsSupplier;
        this.clock = clock;
    }

    private void ensureFresh() {
        long now = clock.getAsLong();
        if (now == resolvedTick) {
            return;
        }
        resolvedTick = now;
        parts = partsSupplier.get();
        base = new int[parts.size() + 1];
        int running = 0;
        for (int i = 0; i < parts.size(); i++) {
            base[i] = running;
            running += parts.get(i).getSlots();
        }
        base[parts.size()] = running;
        totalSlots = running;
    }

    /** Index du sous-handler contenant le slot global donné, ou -1 si hors bornes. */
    private int partOf(int slot) {
        if (slot < 0 || slot >= totalSlots) {
            return -1;
        }
        // base est trié croissant : recherche linéaire (peu de sous-handlers).
        for (int i = 0; i < parts.size(); i++) {
            if (slot < base[i + 1]) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getSlots() {
        ensureFresh();
        return totalSlots;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        ensureFresh();
        int i = partOf(slot);
        return i < 0 ? ItemStack.EMPTY : parts.get(i).getStackInSlot(slot - base[i]);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        ensureFresh();
        int i = partOf(slot);
        return i < 0 ? stack : parts.get(i).insertItem(slot - base[i], stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        ensureFresh();
        int i = partOf(slot);
        return i < 0 ? ItemStack.EMPTY : parts.get(i).extractItem(slot - base[i], amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        ensureFresh();
        int i = partOf(slot);
        return i < 0 ? 0 : parts.get(i).getSlotLimit(slot - base[i]);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        ensureFresh();
        int i = partOf(slot);
        return i >= 0 && parts.get(i).isItemValid(slot - base[i], stack);
    }
}
