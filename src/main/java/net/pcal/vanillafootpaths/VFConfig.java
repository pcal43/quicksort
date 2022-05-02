package net.pcal.vanillafootpaths;

import net.minecraft.util.Identifier;

public abstract class VFConfig {

    public static record RuntimeBlockConfig(
            Identifier nextId,
            int stepCount,
            int timeoutTicks
    ) {}







}
