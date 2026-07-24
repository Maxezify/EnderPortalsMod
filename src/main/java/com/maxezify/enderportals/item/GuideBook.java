package com.maxezify.enderportals.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;

import java.util.ArrayList;
import java.util.List;

/**
 * Construit le livre écrit « Guide de la Porte de l'Ender ». Le composant est
 * bâti avec l'API typée (pas de JSON) : les pages sont des textes
 * traduisibles, résolus côté client → le livre s'affiche dans la langue du
 * joueur.
 */
public final class GuideBook {

    private static final String TITLE = "Guide de la Porte de l'Ender";
    private static final String AUTHOR = "Le Vortex";
    private static final int PAGE_COUNT = 9;

    public static ItemStack create() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);

        List<Filterable<Component>> pages = new ArrayList<>(PAGE_COUNT);
        for (int i = 1; i <= PAGE_COUNT; i++) {
            pages.add(Filterable.passThrough(Component.translatable("enderportals.book.page" + i)));
        }

        book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
                Filterable.passThrough(TITLE), AUTHOR, 0, pages, false));
        book.set(DataComponents.CUSTOM_NAME,
                Component.translatable("item.enderportals.guide_book")
                        .withStyle(style -> style.withColor(ChatFormatting.AQUA).withItalic(false)));
        return book;
    }

    private GuideBook() {
    }
}
