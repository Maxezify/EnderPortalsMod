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
 * Construit le livre écrit « World of Ender ».
 *
 * <p>Le contenu est bâti ici, avec l'API typée, et non déclaré dans le JSON de
 * la recette. Ce n'est pas un détail de style : les composants de texte d'un
 * objet ne s'écrivent pas comme le reste du JSON, et ce mod s'y est trompé
 * <b>quatre fois</b> entre juillet et la 0.16.3.</p>
 *
 * <p>La règle, telle que le jeu l'a finalement dite dans ses journaux : un
 * composant de texte se persiste en <b>chaîne contenant du JSON</b>, jamais en
 * objet. C'est la forme que l'on retrouve dans la syntaxe des commandes, où les
 * apostrophes délimitent bien une chaîne :</p>
 *
 * <pre>/give @s written_book[custom_name='{"text":"Guide"}']</pre>
 *
 * <p>Cela vaut aussi bien pour {@code custom_name} que pour les {@code pages}
 * de {@code written_book_content} — et c'est contre-intuitif, parce que tout le
 * JSON qui les entoure, lui, est bien structuré. La 0.16.3 avait corrigé les
 * pages et laissé {@code custom_name} en objet : la recette restait rejetée,
 * pour la moitié du défaut qui subsistait.</p>
 *
 * <p>Se tromper de forme ne produit aucun message en jeu : la recette entière
 * est rejetée au chargement du datapack, et le joueur voit simplement une case
 * de résultat vide devant une grille correcte. Il faut ouvrir les journaux pour
 * l'apprendre — c'est ce qui a permis au défaut de survivre douze versions.</p>
 *
 * <p>Passer par le code supprime la question. Le compilateur vérifie les types,
 * il n'y a plus de forme à deviner, et les pages restent des textes
 * traduisibles résolus côté client : chacun lit le guide dans sa langue.</p>
 */
public final class GuideBook {

    private static final String TITLE = "World of Ender";
    private static final String AUTHOR = "Le Vortex";

    /** Nombre de pages. Les clés {@code enderportals.book.pageN} suivent. */
    public static final int PAGE_COUNT = 15;

    public static ItemStack create() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);

        List<Filterable<Component>> pages = new ArrayList<>(PAGE_COUNT);
        for (int page = 1; page <= PAGE_COUNT; page++) {
            pages.add(Filterable.passThrough(Component.translatable("enderportals.book.page" + page)));
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
