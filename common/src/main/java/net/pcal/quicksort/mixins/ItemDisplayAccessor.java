package net.pcal.quicksort.mixins;

import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.ItemDisplay.class)
public interface ItemDisplayAccessor {

    @Invoker("setItemTransform")
    void quicksort$setItemTransform(ItemDisplayContext transform);
}
