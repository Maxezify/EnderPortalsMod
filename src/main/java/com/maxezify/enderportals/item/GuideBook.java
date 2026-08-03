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
 * la recette. Ce n'est pas un détail de style : le composant
 * {@code written_book_content} a un format que l'on se trompe à écrire à la
 * main, et ce mod s'y est trompé <b>quatre fois</b> entre juillet et la
 * 0.16.3. Le piège tient à ceci — deux composants de texte voisins veulent des
 * formes contraires :</p>
 * <ul>
 *   <li>{@code pages} passe par {@code ComponentSerialization.flatCodec} : le
 *       codec lit d'abord une <b>chaîne</b>, puis analyse le contenu de cette
 *       chaîne comme un composant de texte ;</li>
 *   <li>{@code custom_name} passe par {@code ComponentSerialization.CODEC} et
 *       veut un <b>objet</b>.</li>
 * </ul>
 *
 * <p>Se tromper de forme ne produit aucun message en jeu : la recette entière
 * est rejetée au chargement du datapack, et le joueur voit simplement une case
 * de résultat vide devant une grille correcte. Impossible à diagnostiquer sans
 * lire les journaux, et facile à réintroduire — c'est exactement ce qui s'est
 * produit à chaque réécriture.</p>
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
