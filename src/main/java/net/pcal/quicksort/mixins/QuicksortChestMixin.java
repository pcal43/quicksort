package net.pcal.quicksort.mixins;

import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.pcal.quicksort.QuicksortService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.pcal.quicksort.QuicksortService.LOGGER_NAME;
import static net.pcal.quicksort.QuicksortService.LOG_PREFIX;

@Mixin(ChestBlockEntity.class)
public class QuicksortChestMixin {

    private static final Logger LOG = LogManager.getLogger(LOGGER_NAME);

    @Inject(method = "stopOpen", at = @At("TAIL"))
    public void stopOpen(ContainerUser user, CallbackInfo ci) {
        ChestBlockEntity e = (ChestBlockEntity) (Object) this;
        LOG.debug(LOG_PREFIX + "Mixin: Chest stopOpen called at " + e.getBlockPos());
        QuicksortService.getInstance().onChestClosed(e);
    }

    @Inject(method = "startOpen", at = @At("TAIL"))
    public void startOpen(ContainerUser user, CallbackInfo ci) {
        ChestBlockEntity e = (ChestBlockEntity) (Object) this;
        LOG.debug(LOG_PREFIX + "Mixin: Chest startOpen called at " + e.getBlockPos());
        QuicksortService.getInstance().onChestOpened(e);
    }
}