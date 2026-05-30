package net.pcal.quicksort.mixins;

import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.class)
public interface DisplayAccessor {

    @Invoker("setPosRotInterpolationDuration")
    void quicksort$setPosRotInterpolationDuration(int ticks);

    @Invoker("setShadowRadius")
    void quicksort$setShadowRadius(float radius);

    @Invoker("setShadowStrength")
    void quicksort$setShadowStrength(float strength);

    @Invoker("setWidth")
    void quicksort$setWidth(float width);

    @Invoker("setHeight")
    void quicksort$setHeight(float height);
}
