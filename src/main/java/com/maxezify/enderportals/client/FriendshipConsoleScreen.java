package com.maxezify.enderportals.client;

import com.maxezify.enderportals.EnderPortalsMod;
import com.maxezify.enderportals.network.ConsoleActionPayload;
import com.maxezify.enderportals.network.ConsoleStatePayload;
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

    private static final int WIDTH = 220;
    private static final int HEIGHT = 234;

    /** Longueur d'un code d'ami. Voir {@code TardisStateManager}. */
    private static final int CODE_LENGTH = 8;

    // Carnet, à gauche.
    private static final int LIST_X = 8;
    private static final int LIST_Y = 8;
    private static final int LIST_W = 96;
    private static final int ROW_H = 20;
    private static final int FIRST_ROW_Y = 26;
    private static final int MAX_ROWS = 6;

    // Pavé, à droite.
    private static final int PAD_X = 112;
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
    private static final int TERM_W = 204;
    private static final int TERM_Y = 162;
    private static final int TERM_H = 64;
    /** Marge de texte à l'intérieur de l'encart. */
    private static final int TERM_TEXT_X = TERM_X + 5;
    /** Retrait du texte après le chevron — les suites de ligne s'y alignent. */
    private static final int TERM_INDENT = 10;
    private static final int TERM_LOG_Y = TERM_Y + 17;
    private static final int TERM_LINE_H = 9;
    private static final int TERM_LINES = 5;
    /** Largeur de repli, ascenseur déduit. Doit rester d'accord avec TERM_BAR_X. */
    private static final int TERM_TEXT_W = 180;
    private static final int TERM_BAR_X = TERM_X + TERM_W - 7;
    private static final String TERM_PROMPT = ">";

    // Planche de sprites, dans les marges que le panneau laisse libres : la
    // colonne à sa droite pour le sablier et les touches, la bande sous lui pour
    // les boutons, trop larges pour la colonne. Doit rester d'accord avec
    // tex_console_gui() de tools/gen_assets.py, qui refuse de générer une
    // planche recouverte par le panneau.
    private static final int KEY_U = 222, KEY_V = 20;
    private static final int KEY_HOVER_U = 222, KEY_HOVER_V = 48;
    private static final int VALIDATE_U = 0, VALIDATE_V = 234;
    private static final int VALIDATE_HOVER_U = 48, VALIDATE_HOVER_V = 234;
    private static final int CLEAR_U = 96, CLEAR_V = 234;
    private static final int CLEAR_HOVER_U = 144, CLEAR_HOVER_V = 234;
    private static final int HOURGLASS_U = 224, HOURGLASS_V = 0;
    private static final int HOURGLASS_SIZE = 16;
    private static final int ACTION_W = 46, ACTION_H = 20;

    private static final int COLOR_KEY_LABEL = 0xFF2A2733;
    private static final int COLOR_ACTION_LABEL = 0xFFF4F0E4;

    private static final int COLOR_TEXT = 0xFFE8E4F0;
    private static final int COLOR_DIM = 0xFF9A93AD;
    private static final int COLOR_LINKED = 0xFF48D65E;
    private static final int COLOR_AWAITING = 0xFFD9C33A;
    private static final int COLOR_ASKED = 0xFFFF9A28;

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

        // La police est en place : le journal peut être replié. Un changement de
        // résolution rejoue init(), et la largeur de repli ne bouge pas — mais
        // repartir de l'état courant coûte moins qu'un cas particulier.
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

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, WIDTH, HEIGHT);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderDisplay(guiGraphics);
        renderList(guiGraphics, mouseX, mouseY);
        renderCode(guiGraphics);
        renderTerminal(guiGraphics);
        // En dernier : une infobulle se pose par-dessus tout le reste.
        renderPassageTooltip(guiGraphics, mouseX, mouseY);
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
            renderRow(guiGraphics, ally, x, y, isOver(mouseX, mouseY, x, y));
        }
    }

    private void renderRow(GuiGraphics guiGraphics, ConsoleStatePayload.Ally ally, int x, int y,
                           boolean hovered) {
        int border = borderColor(ally.state());
        if (hovered) {
            guiGraphics.fill(x, y, x + LIST_W, y + ROW_H - 2, 0x30FFFFFF);
        }
        if (border != 0) {
            guiGraphics.renderOutline(x, y, LIST_W, ROW_H - 2, border);
        }
        if (ally.state() == ConsoleStatePayload.PENDING) {
            // Sablier : un seul des deux codes a été tapé.
            guiGraphics.blit(TEXTURE, x + 2, y + 2, HOURGLASS_U, HOURGLASS_V,
                    HOURGLASS_SIZE, HOURGLASS_SIZE);
        } else {
            PlayerFaceRenderer.draw(guiGraphics, skinOf(ally.uuid()), x + 2, y + 2, 16);
        }
        int nameColor = ally.state() == ConsoleStatePayload.PENDING ? COLOR_DIM : COLOR_TEXT;
        guiGraphics.drawString(font, trim(ally.name()), x + 22, y + 6, nameColor, false);
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

    private String trim(String name) {
        return font.width(name) <= LIST_W - 26 ? name : font.plainSubstrByWidth(name, LIST_W - 32) + "…";
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
        termSource = state;
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
            rebuildTerminal();
        }
        int headerY = topPos + TERM_Y + 3;
        guiGraphics.drawString(font, Component.translatable("enderportals.console.terminal"),
                leftPos + TERM_TEXT_X, headerY, COLOR_TERM_TITLE, false);
        renderPassageLamp(guiGraphics, headerY);

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
        int color = state.passageReady() ? COLOR_LINKED : COLOR_WARN;
        int textX = leftPos + TERM_X + TERM_W - 5 - font.width(label);
        guiGraphics.drawString(font, label, textX, y, color, false);
        int lampX = textX - 9;
        guiGraphics.fill(lampX - 1, y, lampX + 6, y + 7, 0xFF15111F);
        guiGraphics.fill(lampX, y + 1, lampX + 5, y + 6, color);
    }

    private Component passageLabel() {
        return Component.translatable(state.passageReady()
                ? "enderportals.console.passage_ok"
                : "enderportals.console.no_passage");
    }

    /**
     * L'explication du témoin, au survol. « Ne fonctionne pas » désigne un
     * défaut sans le nommer ; le joueur a besoin de savoir qu'il lui manque un
     * Passage des Alliés accolé à ce panneau-ci. C'est exactement ce que dit le
     * refus reçu au clic, et la même phrase sert donc aux deux.
     *
     * <p>Le survol se teste sur l'emprise du témoin et de son libellé, calculée
     * comme au dessin plutôt que retenue d'une image à l'autre : rien à garder
     * en cohérence.</p>
     */
    private void renderPassageTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int right = leftPos + TERM_X + TERM_W - 5;
        int left = right - font.width(passageLabel()) - 10;
        int y = topPos + TERM_Y + 3;
        if (mouseX >= left && mouseX < right && mouseY >= y && mouseY < y + font.lineHeight) {
            guiGraphics.renderTooltip(font, Component.translatable(state.passageReady()
                    ? "enderportals.console.passage_ok_hint"
                    : "enderportals.message.passage_missing"), mouseX, mouseY);
        }
    }

    // ------------------------------------------------------------------
    // Interaction
    // ------------------------------------------------------------------

    private boolean isOver(int mouseX, int mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + LIST_W && mouseY >= y && mouseY < y + ROW_H - 2;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            List<ConsoleStatePayload.Ally> allies = state.allies();
            int shown = Math.min(MAX_ROWS, allies.size() - scroll);
            for (int i = 0; i < shown; i++) {
                int x = leftPos + LIST_X;
                int y = topPos + FIRST_ROW_Y + i * ROW_H;
                if (isOver((int) mouseX, (int) mouseY, x, y)) {
                    UUID target = allies.get(scroll + i).uuid();
                    PacketDistributor.sendToServer(hasShiftDown()
                            ? ConsoleActionPayload.forget(console, target)
                            : ConsoleActionPayload.toggle(console, target));
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
            guiGraphics.blit(TEXTURE, getX(), getY(), lit ? hoverU : u, lit ? hoverV : v,
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
