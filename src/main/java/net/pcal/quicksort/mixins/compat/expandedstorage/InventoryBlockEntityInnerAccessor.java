package net.pcal.quicksort.mixins.compat.expandedstorage;

import compasses.expandedstorage.impl.block.entity.extendable.InventoryBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "compasses.expandedstorage.impl.block.entity.extendable.InventoryBlockEntity$1")
public interface InventoryBlockEntityInnerAccessor {
	/**
	 * The compiler-generated field pointing to the outer class.
	 * 
	 * @remarks `InventoryBlockEntity.this`
	 */
	@Accessor(value = "this$0", remap = false)
	InventoryBlockEntity getOuter();
}
