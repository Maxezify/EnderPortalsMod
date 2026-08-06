package com.maxezify.enderportals.client;

import com.maxezify.enderportals.EnderPortalsMod;
import com.maxezify.enderportals.network.ConsoleActionPayload;
import com.maxezify.enderportals.network.ConsoleStatePayload;
import com.maxezify.enderportals.tardis.AllyLinks;
import com.maxezify.enderportals.tardis.ConsoleLog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Le Contrôle de l'amitié à l'écran : le carnet d'alliés à gauche, le pavé
 * numérique à droite, et le terminal en bas.
 *
 * <p>L'écran n'a aucune mémoire propre au-delà des chiffres en cours de frappe.
 * Tout ce qu'il affiche vient du dernier {@link ConsoleStatePayload} reçu, et
 * chaque clic n'envoie qu'une intention au serveur — qui répond par un état
 * complet. C'est ce qui fait que le panneau se met à jour tout seul quand c'est
 * l'allié, à l'autre bout, qui vient d'agir.</p>
 *
 * <p>Le terminal porte ce que le panneau a à dire. Ces phrases partaient
 * autrefois dans le chat, en bleu : elles y arrivaient pendant qu'on manipulait
 * l'appareil, donc hors du champ de vision, et s'y mêlaient au reste. Elles
 * s'affichent maintenant sur l'appareil qui les produit, et le journal étant
 * tenu côté serveur, celles reçues pendant une absence sont là à la prochaine
 * ouverture.</p>
 */
public class FriendshipConsoleScreen extends Screen {

    private static final ResourceLocation TEXTURE =
            EnderPortalsMod.id("textures/gui/friendship_console.png");

    /**
     * Le panneau, et la planche dont il est découpé.
     *
     * <p>Il faisait 220 de large jusqu'à la 0.29.0, et sa planche tenait dans
     * 256×256. La colonne du carnet ayant grandi de 56 px pour loger la
     * pastille de confiance, le panneau dépasse les 256 : la planche passe donc
     * à 512 de large, et chaque découpe doit dire cette taille — les raccourcis
     * de {@code GuiGraphics} supposent 256×256 en dur, et auraient étiré la
     * moitié de l'interface.</p>
     */
    private static final int WIDTH = 276;
    private static final int HEIGHT = 234;
    private static final int SHEET_W = 512;
    private static final int SHEET_H = 256;

    /** Longueur d'un code d'ami. Voir {@code TardisStateManager}. */
    private static final int CODE_LENGTH = 8;

    // Carnet, à gauche.
    private static final int LIST_X = 8;
    private static final int LIST_Y = 8;
    private static final int LIST_W = 152;
    private static final int ROW_H = 20;
    private static final int FIRST_ROW_Y = 26;
    private static final int MAX_ROWS = 6;

    // Pavé, à droite.
    private static final int PAD_X = LIST_X + LIST_W + 8;
    private static final int DISPLAY_Y = 10;
    private static final int DISPLAY_H = 20;
    private static final int KEY_W = 30;
    private static final int KEY_H = 24;
    private static final int KEY_GAP = 3;
    private static final int KEYS_Y = 36;

    /**
     * Le code du joueur se loge sous les deux boutons, dans la colonne du pavé.
     * La bande libre va du filet d'or jusqu'au terminal ; le texte s'y centre au
     * lieu de se poser sur son bord haut, où il paraissait tomber du filet.
     */
    private static final int CODE_ZONE_Y = 141;
    private static final int CODE_ZONE_H = 19;
    private static final int COLOR_ON_PANEL = 0xFF3F3B46;

    // Terminal, en bas, sur toute la largeur.
    private static final int TERM_X = 8;
    private static final int TERM_W = WIDTH - 16;
    private static final int TERM_Y = 162;
    private static final int TERM_H = 64;
    /** Marge de texte à l'intérieur de l'encart. */
    private static final int TERM_TEXT_X = TERM_X + 5;
    /** Retrait du texte après le chevron — les suites de ligne s'y alignent. */
    private static final int TERM_INDENT = 10;
    private static final int TERM_LOG_Y = TERM_Y + 16;
    private static final int TERM_LINE_H = 9;
    private static final int TERM_LINES = 4;
    /**
     * La ligne d'état, détachée du journal par un filet. Elle porte la cause
     * quand le passage ne fonctionne pas — en permanence, sans rien survoler :
     * une cause qu'il faut aller chercher n'est pas lue.
     */
    private static final int TERM_STATUS_Y = TERM_Y + 54;
    /** Largeur utile de la ligne d'état : de la marge de texte au bord opposé. */
    private static final int TERM_STATUS_W = TERM_W - 11;
    /** Largeur de repli, ascenseur déduit. Doit rester d'accord avec TERM_BAR_X. */
    private static final int TERM_TEXT_W = TERM_W - 24;
    private static final int TERM_BAR_X = TERM_X + TERM_W - 7;
    private static final String TERM_PROMPT = ">";

    // Planche de sprites, dans les marges que le panneau laisse libres : la
    // colonne à sa droite pour le sablier et les touches, la bande sous lui pour
    // les boutons, trop larges pour la colonne. Doit rester d'accord avec
    // tex_console_gui() de tools/gen_assets.py, qui refuse de générer une
    // planche recouverte par le panneau.
    private static final int KEY_U = WIDTH + 2, KEY_V = 20;
    private static final int KEY_HOVER_U = WIDTH + 2, KEY_HOVER_V = 48;
    private static final int VALIDATE_U = 0, VALIDATE_V = HEIGHT;
    private static final int VALIDATE_HOVER_U = 48, VALIDATE_HOVER_V = HEIGHT;
    private static final int CLEAR_U = 96, CLEAR_V = HEIGHT;
    private static final int CLEAR_HOVER_U = 144, CLEAR_HOVER_V = HEIGHT;
    private static final int HOURGLASS_U = WIDTH + 4, HOURGLASS_V = 0;
    private static final int HOURGLASS_SIZE = 16;
    private static final int ACTION_W = 46, ACTION_H = 20;

    private static final int COLOR_KEY_LABEL = 0xFF2A2733;
    private static final int COLOR_ACTION_LABEL = 0xFFF4F0E4;

    private static final int COLOR_TEXT = 0xFFE8E4F0;
    private static final int COLOR_DIM = 0xFF9A93AD;
    private static final int COLOR_LINKED = 0xFF48D65E;
    private static final int COLOR_AWAITING = 0xFFD9C33A;
    private static final int COLOR_ASKED = 0xFFFF9A28;

    /**
     * La pastille de confiance, à droite du pseudo. Elle n'apparaît que sur un
     * lien ouvert : c'est là que le réglage a un sens, un passage étant la seule
     * façon d'entrer chez quelqu'un.
     */
    private static final int BADGE_W = 54;
    private static final int BADGE_H = 12;
    /** Marge entre la pastille et le bord droit de la ligne. */
    private static final int BADGE_MARGIN = 3;

    /** Fond, liseré haut, liseré bas et texte, dans l'ordre des trois degrés. */
    private static final int[][] BADGE_COLORS = {
            {0xFF4A4658, 0xFF625E74, 0xFF322F3E, 0xFFD8D4E4},
            {0xFF276579, 0xFF3C8CA6, 0xFF17414F, 0xFFDCF3FF},
            {0xFF8E6220, 0xFFB98430, 0xFF5C3E12, 0xFFFFEFC8},
    };
    private static final int BADGE_BORDER = 0xFF17141F;
    /** Éclaircissement au survol, appliqué au fond seul. */
    private static final int BADGE_HOVER = 0x28FFFFFF;

    private static final int COLOR_TERM_TITLE = 0xFFD8B45E;
    private static final int COLOR_TERM_PROMPT = 0xFF6F63A0;
    private static final int COLOR_TERM_TRACK = 0xFF241E38;
    private static final int COLOR_WARN = 0xFFE05555;

    private final BlockPos console;
    private ConsoleStatePayload state;
    private final StringBuilder typed = new StringBuilder(CODE_LENGTH);

    private int leftPos;
    private int topPos;
    private int scroll;

    /** Journal mis en lignes pour l'affichage, et l'état dont il est issu. */
    private final List<TermLine> termLines = new ArrayList<>();
    private ConsoleStatePayload termSource;
    /**
     * Le journal dont {@link #termLines} est le repli. Le panneau se relit
     * chaque seconde ; comparer le journal plutôt que l'état évite de replier
     * pour rien, et surtout de renvoyer le lecteur en bas alors qu'il vient de
     * remonter — le témoin de passage change sans que le journal bouge.
     */
    private List<ConsoleLog.Entry> termLog = List.of();
    /** Lignes remontées depuis le bas. Zéro : on regarde la plus récente. */
    private int termScroll;

    /** Une ligne du terminal, déjà repliée à la largeur de l'encart. */
    private record TermLine(FormattedCharSequence text, int color, boolean first) {
    }

    private FriendshipConsoleScreen(ConsoleStatePayload state) {
        super(Component.translatable("block.enderportals.friendship_console"));
        this.console = state.console();
        this.state = state;
    }

    /**
     * Ouvre le panneau, ou met à jour celui déjà ouvert. Deux écrans successifs
     * pour le même panneau effaceraient les chiffres en cours de frappe : on
     * réutilise donc l'instance existante.
     */
    public static void show(ConsoleStatePayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof FriendshipConsoleScreen screen
                && screen.console.equals(payload.console())) {
            screen.state = payload;
            screen.clampScroll();
        } else {
            minecraft.setScreen(new FriendshipConsoleScreen(payload));
        }
    }

    // ------------------------------------------------------------------
    // Construction
    // ------------------------------------------------------------------

    @Override
    protected void init() {
        leftPos = (width - WIDTH) / 2;
        topPos = (height - HEIGHT) / 2;

        // Neuf touches, trois rangées pleines, pas de zéro : les codes sont
        // tirés dans 1 à 9 justement pour que le pavé n'ait pas de dixième
        // touche orpheline. Voir TardisStateManager.CODE_ALPHABET.
        for (int i = 0; i < 9; i++) {
            addKey(String.valueOf(i + 1), i % 3, i / 3);
        }

        int actionsY = topPos + KEYS_Y + 3 * (KEY_H + KEY_GAP) + 1;
        addRenderableWidget(new SpriteButton(leftPos + PAD_X, actionsY, ACTION_W, ACTION_H,
                Component.translatable("enderportals.console.validate"),
                VALIDATE_U, VALIDATE_V, VALIDATE_HOVER_U, VALIDATE_HOVER_V,
                COLOR_ACTION_LABEL, true, this::validate));
        addRenderableWidget(new SpriteButton(leftPos + PAD_X + 50, actionsY, ACTION_W, ACTION_H,
                Component.translatable("enderportals.console.clear"),
                CLEAR_U, CLEAR_V, CLEAR_HOVER_U, CLEAR_HOVER_V,
                COLOR_ACTION_LABEL, true, () -> typed.setLength(0)));

        // La police est en place : le journal peut être replié. Les lignes
        // survivent à un changement de résolution — la largeur de repli ne
        // dépend pas de la fenêtre — donc seule la source est à redemander.
        termSource = null;
    }

    private void addKey(String label, int col, int row) {
        int x = leftPos + PAD_X + col * (KEY_W + KEY_GAP);
        int y = topPos + KEYS_Y + row * (KEY_H + KEY_GAP);
        addRenderableWidget(new SpriteButton(x, y, KEY_W, KEY_H, Component.literal(label),
                KEY_U, KEY_V, KEY_HOVER_U, KEY_HOVER_V, COLOR_KEY_LABEL, false,
                () -> type(label)));
    }

    private void type(String digit) {
        if (typed.length() < CODE_LENGTH) {
            typed.append(digit);
        }
    }

    private void backspace() {
        if (typed.length() > 0) {
            typed.setLength(typed.length() - 1);
        }
    }

    private void validate() {
        if (typed.length() != CODE_LENGTH) {
            return;
        }
        // parseInt ne peut pas déborder : huit chiffres tiennent dans un int.
        int code = Integer.parseInt(typed.toString());
        typed.setLength(0);
        PacketDistributor.sendToServer(ConsoleActionPayload.code(console, code));
    }

    // ------------------------------------------------------------------
    // Rendu
    // ------------------------------------------------------------------

    /**
     * Une découpe de la planche, taille dite explicitement.
     *
     * <p>Le raccourci à six arguments de {@code GuiGraphics} suppose une texture
     * de 256×256 et divise les coordonnées par cette valeur. La planche en fait
     * 512 de large depuis que le panneau dépasse 256 : passer par le raccourci
     * aurait affiché deux fois trop grand, et donc la moitié gauche de
     * l'interface étirée sur tout l'écran.</p>
     */
    private static void sheet(GuiGraphics guiGraphics, int x, int y, int u, int v, int w, int h) {
        guiGraphics.blit(TEXTURE, x, y, 0, (float) u, (float) v, w, h, SHEET_W, SHEET_H);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        sheet(guiGraphics, leftPos, topPos, 0, 0, WIDTH, HEIGHT);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderDisplay(guiGraphics);
        renderList(guiGraphics, mouseX, mouseY);
        renderCode(guiGraphics);
        renderTerminal(guiGraphics);
    }

    private void renderDisplay(GuiGraphics guiGraphics) {
        // Les chiffres frappés, groupés par quatre : un code se relit à l'œil.
        String shown = typed.isEmpty() ? "--------" : typed.toString();
        StringBuilder spaced = new StringBuilder();
        for (int i = 0; i < shown.length(); i++) {
            if (i == 4) {
                spaced.append("  ");
            }
            spaced.append(shown.charAt(i));
        }
        int textY = topPos + DISPLAY_Y + (DISPLAY_H - font.lineHeight) / 2;
        guiGraphics.drawCenteredString(font, spaced.toString(),
                leftPos + PAD_X + 48, textY, 0xFFF2ECFF);
    }

    /** Sous les deux boutons : le code du joueur, celui qu'il dicte à l'autre. */
    private void renderCode(GuiGraphics guiGraphics) {
        int y = topPos + CODE_ZONE_Y + (CODE_ZONE_H - font.lineHeight) / 2;
        guiGraphics.drawString(font, Component.translatable("enderportals.console.my_code",
                formatCode(state.myCode())), leftPos + PAD_X, y, COLOR_ON_PANEL, false);
    }

    private static String formatCode(int code) {
        String digits = Integer.toString(code);
        return digits.length() == CODE_LENGTH
                ? digits.substring(0, 4) + " " + digits.substring(4)
                : digits;
    }

    private void renderList(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, Component.translatable("enderportals.console.allies"),
                leftPos + LIST_X + 4, topPos + LIST_Y + 4, COLOR_TEXT, false);

        List<ConsoleStatePayload.Ally> allies = state.allies();
        if (allies.isEmpty()) {
            guiGraphics.drawString(font, Component.translatable("enderportals.console.empty"),
                    leftPos + LIST_X + 4, topPos + FIRST_ROW_Y + 4, COLOR_DIM, false);
            return;
        }
        int shown = Math.min(MAX_ROWS, allies.size() - scroll);
        for (int i = 0; i < shown; i++) {
            ConsoleStatePayload.Ally ally = allies.get(scroll + i);
            int x = leftPos + LIST_X;
            int y = topPos + FIRST_ROW_Y + i * ROW_H;
            renderRow(guiGraphics, ally, x, y, mouseX, mouseY);
        }
    }

    private void renderRow(GuiGraphics guiGraphics, ConsoleStatePayload.Ally ally, int x, int y,
                           int mouseX, int mouseY) {
        int border = borderColor(ally.state());
        if (isOver(mouseX, mouseY, x, y)) {
            guiGraphics.fill(x, y, x + LIST_W, y + ROW_H - 2, 0x30FFFFFF);
        }
        if (border != 0) {
            guiGraphics.renderOutline(x, y, LIST_W, ROW_H - 2, border);
        }
        if (ally.state() == ConsoleStatePayload.PENDING) {
            // Sablier : un seul des deux codes a été tapé.
            sheet(guiGraphics, x + 2, y + 2, HOURGLASS_U, HOURGLASS_V,
                    HOURGLASS_SIZE, HOURGLASS_SIZE);
        } else {
            PlayerFaceRenderer.draw(guiGraphics, skinOf(ally.uuid()), x + 2, y + 2, 16);
        }
        int nameColor = ally.state() == ConsoleStatePayload.PENDING ? COLOR_DIM : COLOR_TEXT;
        guiGraphics.drawString(font, trim(ally.name(), hasBadge(ally)), x + 22, y + 6, nameColor, false);
        if (hasBadge(ally)) {
            renderBadge(guiGraphics, ally, badgeX(x), y + (ROW_H - 2 - BADGE_H) / 2,
                    isOverBadge(mouseX, mouseY, x, y));
        }
    }

    /** Seul un lien ouvert porte une pastille. */
    private static boolean hasBadge(ConsoleStatePayload.Ally ally) {
        return ally.state() == ConsoleStatePayload.LINKED;
    }

    private static int badgeX(int rowX) {
        return rowX + LIST_W - BADGE_W - BADGE_MARGIN;
    }

    /**
     * La pastille : un petit cabochon peint comme les touches du pavé — fond
     * plein, liseré clair en haut, sombre en bas — plutôt qu'un rectangle plat.
     * Le panneau n'a aucune surface plate ailleurs, et une seule aurait sauté
     * aux yeux.
     */
    private void renderBadge(GuiGraphics guiGraphics, ConsoleStatePayload.Ally ally,
                             int x, int y, boolean hovered) {
        int[] colors = BADGE_COLORS[Math.max(0, Math.min(BADGE_COLORS.length - 1, ally.trust()))];
        guiGraphics.fill(x, y, x + BADGE_W, y + BADGE_H, BADGE_BORDER);
        guiGraphics.fill(x + 1, y + 1, x + BADGE_W - 1, y + BADGE_H - 1, colors[0]);
        guiGraphics.fill(x + 1, y + 1, x + BADGE_W - 1, y + 2, colors[1]);
        guiGraphics.fill(x + 1, y + BADGE_H - 2, x + BADGE_W - 1, y + BADGE_H - 1, colors[2]);
        if (hovered) {
            guiGraphics.fill(x + 1, y + 1, x + BADGE_W - 1, y + BADGE_H - 1, BADGE_HOVER);
        }
        Component label = trustLabel(ally.trust());
        // Un libellé traduit peut déborder de la pastille : on le rogne comme
        // les pseudos plutôt que de le laisser sortir sous le cadre.
        int width = font.width(label);
        if (width > BADGE_W - 4) {
            label = Component.literal(
                    font.plainSubstrByWidth(label.getString(), BADGE_W - 4 - font.width("…")) + "…");
            width = font.width(label);
        }
        guiGraphics.drawString(font, label, x + (BADGE_W - width) / 2,
                y + (BADGE_H - font.lineHeight) / 2 + 1, colors[3], false);
    }

    private static Component trustLabel(int trust) {
        return Component.translatable(switch (trust) {
            case AllyLinks.GUEST -> "enderportals.console.trust_guest";
            case AllyLinks.PARTNER -> "enderportals.console.trust_partner";
            default -> "enderportals.console.trust_visitor";
        });
    }

    private int borderColor(int allyState) {
        return switch (allyState) {
            case ConsoleStatePayload.LINKED -> COLOR_LINKED;
            case ConsoleStatePayload.AWAITING_THEM -> COLOR_AWAITING;
            // Clignotant : c'est le seul état qui réclame un geste de ma part.
            case ConsoleStatePayload.THEY_ASK ->
                    (System.currentTimeMillis() / 400L) % 2L == 0L ? COLOR_ASKED : 0;
            default -> 0;
        };
    }

    /**
     * Le pseudo, rogné pour ne pas courir sous la pastille. Le budget est
     * calculé depuis la place réellement occupée à droite, et non depuis la
     * largeur de la ligne : la pastille n'est pas toujours là.
     */
    private String trim(String name, boolean badge) {
        int budget = LIST_W - 26 - (badge ? BADGE_W + BADGE_MARGIN : 0);
        return font.width(name) <= budget
                ? name
                : font.plainSubstrByWidth(name, Math.max(0, budget - font.width("…"))) + "…";
    }

    /**
     * La texture de peau de l'allié. Un allié connecté a son {@code PlayerInfo}
     * déjà côté client ; hors ligne, on retombe sur la peau par défaut déduite
     * de son UUID plutôt que d'aller la chercher sur le réseau.
     */
    private ResourceLocation skinOf(UUID uuid) {
        if (minecraft != null && minecraft.getConnection() != null) {
            PlayerInfo info = minecraft.getConnection().getPlayerInfo(uuid);
            if (info != null) {
                return info.getSkin().texture();
            }
        }
        return DefaultPlayerSkin.get(uuid).texture();
    }

    // ------------------------------------------------------------------
    // Terminal
    // ------------------------------------------------------------------

    /**
     * Replie le journal à la largeur de l'encart. Le calcul ne se refait qu'au
     * changement d'état — comparé par identité, l'état étant reconstruit à
     * chaque envoi du serveur — et non à chaque image.
     */
    private void rebuildTerminal() {
        termLines.clear();
        for (ConsoleLog.Entry entry : state.log()) {
            Component text = Component.translatable(entry.key(), entry.args().toArray());
            int color = toneColor(entry.tone());
            boolean first = true;
            for (FormattedCharSequence line : font.split(text, TERM_TEXT_W)) {
                termLines.add(new TermLine(line, color, first));
                first = false;
            }
        }
        // Une nouvelle ligne ramène la vue en bas : c'est elle qu'on veut lire.
        termScroll = 0;
    }

    private static int toneColor(int tone) {
        return switch (tone) {
            case ConsoleLog.GOOD -> 0xFF6FE07E;
            case ConsoleLog.WARN -> 0xFFE8B24A;
            case ConsoleLog.BAD -> 0xFFE87070;
            default -> 0xFF7FD3E8;
        };
    }

    private void renderTerminal(GuiGraphics guiGraphics) {
        if (termSource != state) {
            termSource = state;
            if (!state.log().equals(termLog)) {
                termLog = state.log();
                rebuildTerminal();
            }
        }
        int headerY = topPos + TERM_Y + 3;
        guiGraphics.drawString(font, Component.translatable("enderportals.console.terminal"),
                leftPos + TERM_TEXT_X, headerY, COLOR_TERM_TITLE, false);
        renderPassageLamp(guiGraphics, headerY);
        renderPassageStatus(guiGraphics);

        if (termLines.isEmpty()) {
            guiGraphics.drawString(font, Component.translatable("enderportals.console.log_empty"),
                    leftPos + TERM_TEXT_X, topPos + TERM_LOG_Y, COLOR_DIM, false);
            return;
        }
        int total = termLines.size();
        int firstShown = Math.max(0, total - TERM_LINES - termScroll);
        int lastShown = Math.min(total, firstShown + TERM_LINES);
        for (int i = firstShown; i < lastShown; i++) {
            TermLine line = termLines.get(i);
            int y = topPos + TERM_LOG_Y + (i - firstShown) * TERM_LINE_H;
            if (line.first()) {
                guiGraphics.drawString(font, TERM_PROMPT, leftPos + TERM_TEXT_X, y,
                        COLOR_TERM_PROMPT, false);
            }
            guiGraphics.drawString(font, line.text(), leftPos + TERM_TEXT_X + TERM_INDENT, y,
                    line.color(), false);
        }
        renderTerminalBar(guiGraphics, total);
    }

    /** L'ascenseur du terminal, quand le journal dépasse la hauteur visible. */
    private void renderTerminalBar(GuiGraphics guiGraphics, int total) {
        if (total <= TERM_LINES) {
            return;
        }
        int x = leftPos + TERM_BAR_X;
        int y = topPos + TERM_LOG_Y;
        int height = TERM_LINES * TERM_LINE_H;
        guiGraphics.fill(x, y, x + 3, y + height, COLOR_TERM_TRACK);
        int thumb = Math.max(6, height * TERM_LINES / total);
        int span = total - TERM_LINES;
        int offset = (height - thumb) * (span - termScroll) / span;
        guiGraphics.fill(x, y + offset, x + 3, y + offset + thumb, COLOR_TERM_PROMPT);
    }

    /**
     * Le témoin du panneau, dans l'en-tête du terminal : ce Contrôle commande-t-il
     * bien un passage à vous ?
     *
     * <p>Il était auparavant posé sous la dernière ligne du carnet, où sa phrase
     * dépassait des 96 px de l'encart. Un état permanent n'a de toute façon rien
     * à faire dans une liste : sa place est sur le bandeau, en face du titre.</p>
     *
     * <p>C'est la <b>couleur</b> qui porte l'état, comme sur n'importe quel
     * voyant ; le libellé se contente de dire ce qui marche ou non. Il ne dit
     * donc pas <i>pourquoi</i> — c'est le rôle de l'infobulle, qui nomme la
     * cause au survol.</p>
     */
    private void renderPassageLamp(GuiGraphics guiGraphics, int y) {
        Component label = passageLabel();
        int color = passageWorks() ? COLOR_LINKED : COLOR_WARN;
        int textX = leftPos + TERM_X + TERM_W - 5 - font.width(label);
        guiGraphics.drawString(font, label, textX, y, color, false);
        int lampX = textX - 9;
        guiGraphics.fill(lampX - 1, y, lampX + 6, y + 7, 0xFF15111F);
        guiGraphics.fill(lampX, y + 1, lampX + 5, y + 6, color);
    }

    /** Le passage est-il ouvert des deux côtés ? C'est la seule chose qui est verte. */
    private boolean passageWorks() {
        return state.passageState() == ConsoleStatePayload.PASSAGE_OPEN;
    }

    private Component passageLabel() {
        return Component.translatable(passageWorks()
                ? "enderportals.console.passage_ok"
                : "enderportals.console.no_passage");
    }

    /**
     * La ligne d'état, en bas du terminal.
     *
     * <p>« Ne fonctionne pas » prononce un verdict sans en donner la cause, et
     * les causes n'appellent pas le même geste : poser le panneau contre son
     * passage, cliquer le nom d'un allié, ou attendre que l'allié remette
     * l'arche qu'il vient de casser. Cette ligne nomme laquelle.</p>
     *
     * <p>Elle est <b>hors du journal</b> et non une ligne de plus dedans. Un
     * journal raconte ce qui est arrivé, dans l'ordre ; ceci est l'état courant.
     * L'y verser l'aurait soit répété à chaque relecture, soit laissé remonter
     * hors de vue à la ligne suivante — alors que la panne, elle, dure.</p>
     *
     * <p>Les quatre phrases sont taillées pour tenir sur une ligne : la bande
     * n'en a qu'une, et un repli mangerait le journal.</p>
     */
    private void renderPassageStatus(GuiGraphics guiGraphics) {
        guiGraphics.drawString(font, statusLine(), leftPos + TERM_TEXT_X, topPos + TERM_STATUS_Y,
                passageWorks() ? COLOR_LINKED : COLOR_WARN, false);
    }

    /**
     * La ligne d'état, garantie sur une seule ligne.
     *
     * <p>Deux des quatre phrases citent un pseudo, et un pseudo va jusqu'à seize
     * caractères : les tailler à la mesure ne suffit donc pas, il faut rogner le
     * nom. La bande n'a qu'une ligne et il n'y a pas de repli — un débordement
     * partirait sous le cadre, silencieusement.</p>
     */
    private Component statusLine() {
        String key = passageStatusKey();
        Component line = Component.translatable(key, state.passageAlly());
        if (font.width(line) <= TERM_STATUS_W) {
            return line;
        }
        int budget = TERM_STATUS_W - font.width(Component.translatable(key, "")) - font.width("…");
        return Component.translatable(key,
                font.plainSubstrByWidth(state.passageAlly(), Math.max(0, budget)) + "…");
    }

    /**
     * « Ouvert » nomme l'allié : depuis qu'un joueur pose autant d'arches qu'il
     * a d'amis, le mot seul ne dit plus rien — deux panneaux voisins commandent
     * deux couloirs différents.
     */
    private String passageStatusKey() {
        return switch (state.passageState()) {
            case ConsoleStatePayload.PASSAGE_NO_PANEL -> "enderportals.console.status_no_panel";
            case ConsoleStatePayload.PASSAGE_CLOSED -> "enderportals.console.status_closed";
            default -> "enderportals.console.status_open";
        };
    }

    // ------------------------------------------------------------------
    // Interaction
    // ------------------------------------------------------------------

    private boolean isOver(int mouseX, int mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + LIST_W && mouseY >= y && mouseY < y + ROW_H - 2;
    }

    /** Le curseur est-il sur la pastille de cette ligne ? */
    private boolean isOverBadge(int mouseX, int mouseY, int x, int y) {
        int bx = badgeX(x);
        int by = y + (ROW_H - 2 - BADGE_H) / 2;
        return mouseX >= bx && mouseX < bx + BADGE_W && mouseY >= by && mouseY < by + BADGE_H;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            List<ConsoleStatePayload.Ally> allies = state.allies();
            int shown = Math.min(MAX_ROWS, allies.size() - scroll);
            for (int i = 0; i < shown; i++) {
                int x = leftPos + LIST_X;
                int y = topPos + FIRST_ROW_Y + i * ROW_H;
                ConsoleStatePayload.Ally ally = allies.get(scroll + i);
                // La pastille passe avant la ligne : elle est dedans, et un clic
                // dessus ne doit pas refermer le passage par la même occasion.
                if (hasBadge(ally) && isOverBadge((int) mouseX, (int) mouseY, x, y)) {
                    PacketDistributor.sendToServer(
                            ConsoleActionPayload.trust(console, ally.uuid()));
                    return true;
                }
                if (isOver((int) mouseX, (int) mouseY, x, y)) {
                    PacketDistributor.sendToServer(hasShiftDown()
                            ? ConsoleActionPayload.forget(console, ally.uuid())
                            : ConsoleActionPayload.toggle(console, ally.uuid()));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** La molette agit sur ce qu'elle survole : le terminal, ou le carnet. */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isOverTerminal(mouseX, mouseY)) {
            termScroll += (int) Math.signum(scrollY);
            termScroll = Math.max(0, Math.min(termScroll, Math.max(0, termLines.size() - TERM_LINES)));
            return true;
        }
        if (state.allies().size() > MAX_ROWS) {
            scroll -= (int) Math.signum(scrollY);
            clampScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private boolean isOverTerminal(double mouseX, double mouseY) {
        return mouseX >= leftPos + TERM_X && mouseX < leftPos + TERM_X + TERM_W
                && mouseY >= topPos + TERM_Y && mouseY < topPos + TERM_Y + TERM_H;
    }

    private void clampScroll() {
        scroll = Math.max(0, Math.min(scroll, Math.max(0, state.allies().size() - MAX_ROWS)));
    }

    /** Les chiffres se frappent aussi au clavier — le pavé reste cliquable. */
    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (codePoint >= '0' && codePoint <= '9') {
            type(String.valueOf(codePoint));
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    /**
     * Retour arrière et entrée : ce que le pavé ne montre plus depuis qu'il n'a
     * que des chiffres, le clavier le fait. Rien n'est perdu, et le panneau reste
     * lisible.
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            backspace();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            validate();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * Le serveur doit savoir que ce panneau n'est plus à l'écran : c'est ce qui
     * décide si ses messages vont au terminal ou repartent dans le chat.
     *
     * <p>L'avis part de {@code removed()} et non de {@code onClose()}, parce que
     * {@code onClose()} ne couvre que l'échappe. Un joueur qui meurt devant son
     * panneau le voit remplacé par l'écran de mort sans qu'elle soit appelée : le
     * serveur le croirait encore devant, garderait ses messages pour un terminal
     * fermé, et lui rouvrirait l'interface de force au prochain rafraîchissement.
     * {@code removed()}, lui, passe sur tous les retraits d'écran.</p>
     */
    @Override
    public void removed() {
        // Le retrait peut aussi venir d'une déconnexion, où il n'y a plus de
        // connexion pour porter le message — et plus de panneau à refermer.
        if (minecraft != null && minecraft.getConnection() != null) {
            PacketDistributor.sendToServer(ConsoleActionPayload.close(console));
        }
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * Une touche ou un bouton du panneau, dessiné depuis la planche de sprites.
     *
     * <p>Les widgets de vanilla auraient fait l'affaire fonctionnellement, mais
     * leur gris passe-partout est justement ce qui faisait ressembler le panneau
     * à n'importe quelle interface. Le reste — le clic, son bruit, le focus, la
     * narration — est hérité tel quel : seul le dessin est reprisé.</p>
     */
    private static final class SpriteButton extends AbstractButton {

        private final int u;
        private final int v;
        private final int hoverU;
        private final int hoverV;
        private final int labelColor;
        private final boolean shadow;
        private final Runnable action;

        private SpriteButton(int x, int y, int width, int height, Component label,
                             int u, int v, int hoverU, int hoverV, int labelColor, boolean shadow,
                             Runnable action) {
            super(x, y, width, height, label);
            this.u = u;
            this.v = v;
            this.hoverU = hoverU;
            this.hoverV = hoverV;
            this.labelColor = labelColor;
            this.shadow = shadow;
            this.action = action;
        }

        /**
         * {@code drawCenteredString} porte toujours son ombre portée. Sur la face
         * claire d'une touche, un chiffre foncé doublé de son ombre se lit mal —
         * c'est flou, pas contrasté. On centre donc à la main pour pouvoir la
         * couper. Les boutons d'action, eux, portent un libellé clair sur une
         * couleur franche : là l'ombre aide, et elle est conservée.
         */
        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            boolean lit = isHoveredOrFocused();
            sheet(guiGraphics, getX(), getY(), lit ? hoverU : u, lit ? hoverV : v,
                    this.width, this.height);
            Font font = Minecraft.getInstance().font;
            int textX = getX() + (this.width - font.width(getMessage())) / 2;
            int textY = getY() + (this.height - font.lineHeight) / 2 + 1;
            guiGraphics.drawString(font, getMessage(), textX, textY, labelColor, shadow);
        }

        @Override
        public void onPress() {
            action.run();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
