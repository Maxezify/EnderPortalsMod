package com.maxezify.enderportals.item;

import com.maxezify.enderportals.ModBlocks;
import com.maxezify.enderportals.ModComponents;
import com.maxezify.enderportals.ModDimensions;
import com.maxezify.enderportals.block.TardisDoorBlock;
import com.maxezify.enderportals.block.entity.TardisDoorBlockEntity;
import com.maxezify.enderportals.tardis.FriendCode;
import com.maxezify.enderportals.tardis.TardisData;
import com.maxezify.enderportals.tardis.TardisHelper;
import com.maxezify.enderportals.tardis.TardisStateManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * La Clé du TARDIS.
 * <ul>
 *   <li>Clic droit sur une porte active non liée : lie la clé.</li>
 *   <li>Accroupi, clic droit par terre : matérialise la porte à l'endroit visé
 *       (fondu).</li>
 *   <li>Clic droit sur la porte extérieure : l'ouvre ; re-clic : la referme
 *       et la fait disparaître en fondu.</li>
 *   <li>Clic droit sur la porte intérieure : rappelle ou dématérialise la
 *       porte extérieure.</li>
 * </ul>
 */
public class TardisKeyItem extends Item {

    public TardisKeyItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        ServerLevel serverLevel = (ServerLevel) level;
        MinecraftServer server = serverLevel.getServer();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        ItemStack stack = context.getItemInHand();

        if (state.is(ModBlocks.TARDIS_DOOR.get())) {
            handleDoorClick(server, serverLevel, pos, state, player, stack);
            return InteractionResult.SUCCESS;
        }
        if (state.is(ModBlocks.INACTIVE_TARDIS_DOOR.get())) {
            player.displayClientMessage(Component.translatable("enderportals.message.door_hint"), true);
            return InteractionResult.SUCCESS;
        }
        handleGroundClick(server, serverLevel, context, player, stack);
        return InteractionResult.SUCCESS;
    }

    private static void handleDoorClick(MinecraftServer server, ServerLevel level, BlockPos pos, BlockState state,
                                        Player player, ItemStack stack) {
        BlockPos base = state.getValue(TardisDoorBlock.HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        // Une porte en cours d'effacement n'est pas écartée ici : c'est
        // TardisDoorBlock.operate qui tranche, et il le dit au joueur.
        if (!(level.getBlockEntity(base) instanceof TardisDoorBlockEntity door)
                || door.getTardisId() == null) {
            return;
        }
        TardisData data = TardisStateManager.get(server).getTardis(door.getTardisId());
        if (data == null) {
            return;
        }
        // Le contrôle de propriété passe avant tout le reste, liaison comprise :
        // sans lui, une clé vierge posée sur la porte matérialisée d'autrui
        // suffirait à s'en emparer, et une clé volée ouvrirait la base de son
        // propriétaire. Le partage consenti, c'est le Passage des Alliés.
        if (!isOwner(player, data)) {
            player.displayClientMessage(Component.translatable("enderportals.message.not_your_door"), true);
            level.playSound(null, base, SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, 0.8f, 0.6f);
            return;
        }
        String bound = stack.get(ModComponents.TARDIS_ID.get());
        if (bound == null) {
            stack.set(ModComponents.TARDIS_ID.get(), door.getTardisId().toString());
            stampCode(stack, data);
            level.playSound(null, base, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.2f);
            player.displayClientMessage(Component.translatable("enderportals.message.key_bound"), false);
            return;
        }
        if (!bound.equals(door.getTardisId().toString())) {
            player.displayClientMessage(Component.translatable("enderportals.message.wrong_key"), true);
            level.playSound(null, base, SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, 0.8f, 0.6f);
            return;
        }
        // Les clés liées avant l'arrivée du Passage des Alliés n'ont pas encore
        // leur code : on le pose au premier usage plutôt que d'obliger à
        // reforger la clé.
        stampCode(stack, data);
        // Le geste lui-même appartient à la porte, clé ou pas : elle s'ouvre à
        // la main comme toutes les portes du jeu. Ce que la clé ajoute ici, et
        // qu'aucune main nue ne peut faire, c'est l'accroupissement — la porte
        // se dématérialise et repart dans la poche.
        TardisDoorBlock.operate(state, level, pos, player, player.isShiftKeyDown());
    }

    private static void handleGroundClick(MinecraftServer server, ServerLevel level, UseOnContext context,
                                          Player player, ItemStack stack) {
        String bound = stack.get(ModComponents.TARDIS_ID.get());
        if (bound == null) {
            player.displayClientMessage(Component.translatable("enderportals.message.key_unbound"), true);
            return;
        }
        if (level.dimension().equals(ModDimensions.ENDER_WORLD)) {
            player.displayClientMessage(Component.translatable("enderportals.message.already_inside"), true);
            return;
        }
        TardisData data;
        try {
            data = TardisStateManager.get(server).getTardis(UUID.fromString(bound));
        } catch (IllegalArgumentException e) {
            data = null;
        }
        if (data == null) {
            player.displayClientMessage(Component.translatable("enderportals.message.key_unbound"), true);
            return;
        }
        if (!isOwner(player, data)) {
            player.displayClientMessage(Component.translatable("enderportals.message.not_your_door"), true);
            return;
        }
        // Déplacer sa base est un geste voulu, pas un clic qui traîne. Sans
        // l'accroupissement, la porte suivait chaque clic droit au sol — on
        // pose une torche, on ouvre une carte, et la base a changé de place.
        // Le même geste dématérialise la porte : les deux moitiés de la même
        // action se demandent maintenant de la même façon.
        //
        // Le contrôle vient après l'identité et avant l'état : un joueur dont
        // la clé n'est liée à rien mérite qu'on le lui dise plutôt qu'on lui
        // apprenne un geste qui ne servirait à rien.
        if (!player.isShiftKeyDown()) {
            player.displayClientMessage(Component.translatable("enderportals.message.key_needs_sneak"), true);
            return;
        }
        // Même délai que sur la porte elle-même : reposer la porte ailleurs
        // pendant qu'elle achève son fondu la dupliquerait le temps du
        // recouvrement, et le clic au sol se répète tout aussi vite.
        if (data.isBusy(server)) {
            player.displayClientMessage(Component.translatable("enderportals.message.door_busy"), true);
            return;
        }
        BlockPos clicked = context.getClickedPos();
        BlockPos base = level.getBlockState(clicked).canBeReplaced() ? clicked : clicked.relative(context.getClickedFace());
        // Respecte la spawn protection, le mode aventure et les mods de claim.
        if (!level.mayInteract(player, base) || !level.mayInteract(player, base.above())) {
            player.displayClientMessage(Component.translatable("enderportals.message.protected"), true);
            return;
        }
        Direction facing = player.getDirection().getOpposite();
        if (TardisHelper.deployExterior(server, data, level, base, facing, false, player)) {
            // Seulement si la porte est bien partie : un refus faute de place
            // n'a rien lancé, et n'a donc rien à faire attendre.
            data.markBusy(server, TardisDoorBlockEntity.FADE_IN_TICKS);
            TardisStateManager.get(server).setDirty();
        }
    }

    /**
     * Clic droit dans le vide : la clé se lie à la porte dont on est
     * propriétaire.
     *
     * <p>C'est la seule issue à une clé perdue. La liaison classique exige de
     * cliquer une porte <b>matérialisée</b> ; si la clé disparaît alors que la
     * porte est rangée, il n'y a plus rien à cliquer, la porte intérieure est
     * hors d'atteinte, et le rituel refuse d'éveiller une seconde porte. La base
     * était perdue pour de bon. Reforger une clé et la relier ici répare cela,
     * et reste sûr par construction : on ne peut se lier qu'à sa propre porte.</p>
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return InteractionResultHolder.pass(stack);
        }
        TardisData mine = TardisStateManager.get(server).findByOwner(player.getUUID());
        if (mine == null) {
            player.displayClientMessage(Component.translatable("enderportals.message.key_no_door"), true);
            return InteractionResultHolder.pass(stack);
        }
        String wanted = mine.id.toString();
        if (wanted.equals(stack.get(ModComponents.TARDIS_ID.get()))) {
            // Déjà la bonne : ne rien réécrire, pour ne pas faire clignoter
            // l'objet dans l'inventaire à chaque clic.
            return InteractionResultHolder.pass(stack);
        }
        stack.set(ModComponents.TARDIS_ID.get(), wanted);
        stampCode(stack, mine);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 1.0f, 1.4f);
        player.displayClientMessage(Component.translatable("enderportals.message.key_rebound"), false);
        return InteractionResultHolder.success(stack);
    }

    /** Ce joueur est-il le propriétaire de cette porte ? */
    private static boolean isOwner(Player player, TardisData data) {
        return data.ownerUuid != null && data.ownerUuid.equals(player.getUUID());
    }

    /** Recopie le code d'ami de la porte sur la clé, s'il n'y est pas déjà. */
    private static void stampCode(ItemStack stack, @Nullable TardisData data) {
        if (data != null && data.friendCode != 0
                && !Integer.valueOf(data.friendCode).equals(stack.get(ModComponents.FRIEND_CODE.get()))) {
            stack.set(ModComponents.FRIEND_CODE.get(), data.friendCode);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        String bound = stack.get(ModComponents.TARDIS_ID.get());
        if (bound != null) {
            // Le composant est une chaîne libre : une clé forgée à la commande
            // peut en porter une plus courte que 8 caractères, et un
            // substring(0, 8) sec ferait planter le client au survol.
            tooltip.add(Component.translatable("enderportals.tooltip.key_bound",
                    bound.substring(0, Math.min(8, bound.length()))).withStyle(ChatFormatting.AQUA));
            Integer code = stack.get(ModComponents.FRIEND_CODE.get());
            if (code != null && code != 0) {
                tooltip.add(Component.translatable("enderportals.tooltip.friend_code",
                        FriendCode.format(code)).withStyle(ChatFormatting.GOLD));
            }
        } else {
            tooltip.add(Component.translatable("enderportals.tooltip.key_unbound").withStyle(ChatFormatting.GRAY));
        }
        // Ce que la clé fait et que la main ne fait pas. Ouvrir n'y est pas :
        // c'est le geste de la main, et l'écrire ici laisserait croire qu'il
        // faut la clé pour ça. La liaison et le code, eux, restent affichés :
        // ils décrivent cette clé-ci, pas le fonctionnement des clés.
        EnderTooltip.details(tooltip, flag,
                EnderTooltip.line("enderportals.tooltip.key_place"),
                EnderTooltip.line("enderportals.tooltip.key_dismiss"));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
