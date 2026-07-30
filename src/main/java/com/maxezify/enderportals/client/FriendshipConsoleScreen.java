package com.maxezify.enderportals.client;

import com.maxezify.enderportals.EnderPortalsMod;
import com.maxezify.enderportals.network.ConsoleActionPayload;
import com.maxezify.enderportals.network.ConsoleStatePayload;
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
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.UUID;

/**
 * Le Contrôle de l'amitié à l'écran : un pavé numérique à droite, le carnet
 * d'alliés à gauche.
 *
 * <p>L'écran n'a aucune mémoire propre au-delà des chiffres en cours de frappe.
 * Tout ce qu'il affiche vient du dernier {@link ConsoleStatePayload} reçu, et
 * chaque clic n'envoie qu'une intention au serveur — qui répond par un état
 * complet. C'est ce qui fait que le panneau se met à jour tout seul quand c'est
 * l'allié, à l'autre bout, qui vient d'agir.</p>
 */
public class FriendshipConsoleScreen extends Screen {

    private static final ResourceLocation TEXTURE =
            EnderPortalsMod.id("textures/gui/friendship_console.png");

    private static final int WIDTH = 220;
    private static final int HEIGHT = 192;

    /** Longueur d'un code d'ami. Voir {@code TardisStateManager}. */
    private static final int CODE_LENGTH = 8;

    // Carnet, à gauche.
    private static final int LIST_X = 8;
    private static final int LIST_Y = 8;
    private static final int LIST_W = 96;
    private static final int ROW_H = 20;
    private static final int FIRST_ROW_Y = 26;
    private static final int MAX_ROWS = 7;

    // Pavé, à droite.
    private static final int PAD_X = 112;
    private static final int DISPLAY_Y = 10;
    private static final int DISPLAY_H = 20;
    private static final int KEY_W = 30;
    private static final int KEY_H = 24;
    private static final int KEY_GAP = 3;
    private static final int KEYS_Y = 36;

    /**
     * Bande d'état en pied de panneau : elle est dessinée sur la face claire, pas
     * dans l'encart sombre du carnet, donc son texte doit être foncé.
     */
    private static final int STATUS_Y = 172;
    private static final int COLOR_ON_PANEL = 0xFF3F3B46;
    private static final int COLOR_WARN = 0xFFA02020;

    // Planche de sprites, sous le panneau dans la même texture. Doit rester
    // d'accord avec tex_console_gui() de tools/gen_assets.py.
    private static final int KEY_U = 0, KEY_V = 192;
    private static final int KEY_HOVER_U = 30, KEY_HOVER_V = 192;
    private static final int VALIDATE_U = 0, VALIDATE_V = 216;
    private static final int VALIDATE_HOVER_U = 46, VALIDATE_HOVER_V = 216;
    private static final int CLEAR_U = 92, CLEAR_V = 216;
    private static final int CLEAR_HOVER_U = 138, CLEAR_HOVER_V = 216;
    private static final int HOURGLASS_U = 184, HOURGLASS_V = 192;
    private static final int HOURGLASS_SIZE = 16;
    private static final int ACTION_W = 46, ACTION_H = 20;

    private static final int COLOR_KEY_LABEL = 0xFF2A2733;
    private static final int COLOR_ACTION_LABEL = 0xFFF4F0E4;

    private static final int COLOR_TEXT = 0xFFE8E4F0;
    private static final int COLOR_DIM = 0xFF9A93AD;
    private static final int COLOR_LINKED = 0xFF48D65E;
    private static final int COLOR_AWAITING = 0xFFD9C33A;
    private static final int COLOR_ASKED = 0xFFFF9A28;

    private final BlockPos console;
    private ConsoleStatePayload state;
    private final StringBuilder typed = new StringBuilder(CODE_LENGTH);

    private int leftPos;
    private int topPos;
    private int scroll;

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

        // Un code n'est fait que de chiffres : le pavé n'en porte pas d'autres.
        // Neuf touches en trois rangées, puis le zéro centré sous elles.
        for (int i = 0; i < 9; i++) {
            addKey(String.valueOf(i + 1), i % 3, i / 3);
        }
        addKey("0", 1, 3);

        int actionsY = topPos + KEYS_Y + 4 * (KEY_H + KEY_GAP) + 2;
        addRenderableWidget(new SpriteButton(leftPos + PAD_X, actionsY, ACTION_W, ACTION_H,
                Component.translatable("enderportals.console.validate"),
                VALIDATE_U, VALIDATE_V, VALIDATE_HOVER_U, VALIDATE_HOVER_V,
                COLOR_ACTION_LABEL, this::validate));
        addRenderableWidget(new SpriteButton(leftPos + PAD_X + 50, actionsY, ACTION_W, ACTION_H,
                Component.translatable("enderportals.console.clear"),
                CLEAR_U, CLEAR_V, CLEAR_HOVER_U, CLEAR_HOVER_V,
                COLOR_ACTION_LABEL, () -> typed.setLength(0)));
    }

    private void addKey(String label, int col, int row) {
        int x = leftPos + PAD_X + col * (KEY_W + KEY_GAP);
        int y = topPos + KEYS_Y + row * (KEY_H + KEY_GAP);
        addRenderableWidget(new SpriteButton(x, y, KEY_W, KEY_H, Component.literal(label),
                KEY_U, KEY_V, KEY_HOVER_U, KEY_HOVER_V, COLOR_KEY_LABEL, () -> type(label)));
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
        renderStatus(guiGraphics);
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

    /**
     * Le pied du panneau : mon code à gauche — celui qu'on dicte à l'autre — et
     * l'avertissement d'un passage non accolé à droite, quand il y a lieu.
     */
    private void renderStatus(GuiGraphics guiGraphics) {
        guiGraphics.drawString(font, Component.translatable("enderportals.console.my_code",
                        formatCode(state.myCode())),
                leftPos + LIST_X, topPos + STATUS_Y, COLOR_ON_PANEL, false);
        if (!state.passageReady()) {
            Component warning = Component.translatable("enderportals.console.no_passage");
            guiGraphics.drawString(font, warning,
                    leftPos + WIDTH - 8 - font.width(warning), topPos + STATUS_Y, COLOR_WARN, false);
        }
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

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (state.allies().size() > MAX_ROWS) {
            scroll -= (int) Math.signum(scrollY);
            clampScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
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

    @Override
    public void onClose() {
        PacketDistributor.sendToServer(ConsoleActionPayload.close(console));
        super.onClose();
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
        private final Runnable action;

        private SpriteButton(int x, int y, int width, int height, Component label,
                             int u, int v, int hoverU, int hoverV, int labelColor, Runnable action) {
            super(x, y, width, height, label);
            this.u = u;
            this.v = v;
            this.hoverU = hoverU;
            this.hoverV = hoverV;
            this.labelColor = labelColor;
            this.action = action;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            boolean lit = isHoveredOrFocused();
            guiGraphics.blit(TEXTURE, getX(), getY(), lit ? hoverU : u, lit ? hoverV : v,
                    this.width, this.height);
            Font font = Minecraft.getInstance().font;
            guiGraphics.drawCenteredString(font, getMessage(), getX() + this.width / 2,
                    getY() + (this.height - font.lineHeight) / 2 + 1, labelColor);
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
