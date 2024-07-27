package net.pcal.quicksort;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public interface QuicksortingContainer {

    ServerLevel getWorld();

    BlockPos getBlockPos();

    int getContainerSize();

    ItemStack getItemStack(int slot);

    static QuicksortingContainer of(final ServerLevel world, final BlockPos blockPos, final Container container) {
        return new QuicksortingContainer() {

            @Override
            public ServerLevel getWorld() {
                return world;
            }

            @Override
            public BlockPos getBlockPos() {
                return blockPos;
            }

            @Override
            public int getContainerSize() {
                return container.getContainerSize();
            }

            @Override
            public ItemStack getItemStack(int slot) {
                return container.getItem(slot);
            }
        };
    }

}
