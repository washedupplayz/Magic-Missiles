package net.washedupplayz.magicmissiles.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.washedupplayz.magicmissiles.entity.MissileEntity;

/**
 * Handheld launcher. On use it fires a missile in the direction the player is
 * looking — handy for testing before the silo/radar automation is in place.
 */
public class MissileLauncherItem extends Item {
    private static final float LAUNCH_SPEED = 1.6f;
    private static final int COOLDOWN_TICKS = 30;

    public MissileLauncherItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            Vec3 look = player.getLookAngle();
            MissileEntity missile = new MissileEntity(level, player);
            missile.setPos(
                    player.getX() + look.x * 1.5,
                    player.getEyeY() + look.y * 1.5 - 0.1,
                    player.getZ() + look.z * 1.5);
            missile.shoot(look.x, look.y, look.z, LAUNCH_SPEED, 0.0f);
            level.addFreshEntity(missile);
        }

        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
