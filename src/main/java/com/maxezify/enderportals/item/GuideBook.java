package com.maxezify.enderportals.item;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

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
    private static final int PAGE_COUNT = 7;

    public static ItemStack create() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);

        List<RawFilteredPair<Text>> pages = new java.util.ArrayList<>(PAGE_COUNT);
        for (int i = 1; i <= PAGE_COUNT; i++) {
            pages.add(RawFilteredPair.of(Text.translatable("enderportals.book.page" + i)));
        }

        book.set(DataComponentTypes.WRITTEN_BOOK_CONTENT, new WrittenBookContentComponent(
                RawFilteredPair.of(TITLE), AUTHOR, 0, pages, false));
        book.set(DataComponentTypes.CUSTOM_NAME,
                Text.translatable("item.enderportals.guide_book")
                        .styled(style -> style.withColor(Formatting.AQUA).withItalic(false)));
        return book;
    }

    private GuideBook() {
    }
}
