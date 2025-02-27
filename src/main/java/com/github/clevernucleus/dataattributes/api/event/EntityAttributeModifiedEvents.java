package com.github.clevernucleus.dataattributes.api.event;

import org.apache.commons.lang3.mutable.MutableDouble;
import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;

/**
 * Holds some attribute events. Both occur on both server and client.
 * 
 * @author CleverNucleus
 *
 */
public final class EntityAttributeModifiedEvents {
	
	/**
	 * Fires when the value of an attribute instance has been modified, either by adding/removing a modifier or changing the value of the modifier.
	 */
	public static final Event<Modified> MODIFIED = EventFactory.createArrayBacked(Modified.class, callbacks -> (attribute, livingEntity, modifier, prevValue, isWasAdded) -> {
		for(Modified callback : callbacks) {
			callback.onModified(attribute, livingEntity, modifier, prevValue, isWasAdded);
		}
	});
	
	/**
	 * Fires when after the attribute instance value is calculated, but before it is output. This offers one last chance to alter the 
	 * value in some way (for example round a decimal to an integer).
	 */
	public static final Event<Clamp> CLAMPED = EventFactory.createArrayBacked(Clamp.class, callbacks -> (attribute, value) -> {
		for(Clamp callback : callbacks) {
			callback.onClamped(attribute, value);
		}
	});
	
	@FunctionalInterface
	public interface Modified {
		void onModified(final EntityAttribute attribute, final @Nullable LivingEntity livingEntity, final EntityAttributeModifier modifier, final double prevValue, final boolean isWasAdded);
	}
	
	@FunctionalInterface
	public interface Clamp {
		void onClamped(final EntityAttribute attribute, final MutableDouble value);
	}
}
