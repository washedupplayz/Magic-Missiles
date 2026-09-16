package net.washedupplayz.magicmissiles.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.washedupplayz.magicmissiles.missile.MissileManager;
import net.washedupplayz.magicmissiles.missile.MissileSpecs;

/**
 * Handheld launcher. On use it fires a missile in the direction the player is
 * looking — handy for testing before the silo/radar automation is in place. The
 * missile is a server-managed, ghost-rendered projectile (see {@link MissileManager}),
 * not a world entity.
 */
public class MissileLauncherItem extends Item {
    private static final int COOLDOWN_TICKS = 30;

    public MissileLauncherItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level instanceof ServerLevel serverLevel) {
            Vec3 look = player.getLookAngle();
            Vec3 pos = new Vec3(
                    player.getX() + look.x * 1.5,
                    player.getEyeY() + look.y * 1.5 - 0.1,
                    player.getZ() + look.z * 1.5);
            MissileManager.get(serverLevel).launch(pos, look, MissileSpecs.STANDARD, player, null);
        }

        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
