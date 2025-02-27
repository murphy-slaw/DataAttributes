package com.github.clevernucleus.dataattributes.mixin;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableDouble;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.github.clevernucleus.dataattributes.api.attribute.IEntityAttribute;
import com.github.clevernucleus.dataattributes.api.attribute.StackingBehaviour;
import com.github.clevernucleus.dataattributes.api.event.EntityAttributeModifiedEvents;
import com.github.clevernucleus.dataattributes.mutable.MutableEntityAttribute;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.util.math.MathHelper;

@Mixin(EntityAttribute.class)
abstract class EntityAttributeMixin implements MutableEntityAttribute {
	@Unique private Map<IEntityAttribute, Double> data_parents, data_children;
	@Unique private Map<String, String> data_properties;
	@Unique private StackingBehaviour data_stackingBehaviour;
	@Unique private String data_translationKey;
	@Unique protected double data_fallbackValue, data_minValue, data_maxValue;
	
	@Final
	@Shadow
	private double fallback;
	
	@Final
	@Shadow
	private String translationKey;
	
	@Inject(method = "<init>", at = @At("TAIL"))
	private void init(String translationKey, double fallback, CallbackInfo info) {
		this.data_translationKey = translationKey;
		this.data_fallbackValue = fallback;
		this.data_minValue = Integer.MIN_VALUE;
		this.data_maxValue = Integer.MAX_VALUE;
		this.data_stackingBehaviour = StackingBehaviour.FLAT;
		this.data_parents = new Object2DoubleArrayMap<IEntityAttribute>();
		this.data_children = new Object2DoubleArrayMap<IEntityAttribute>();
		this.data_properties = new HashMap<String, String>();
	}
	
	@Inject(method = "getDefaultValue", at = @At("HEAD"), cancellable = true)
	private void onGetDefaultValue(CallbackInfoReturnable<Double> info) {
		info.setReturnValue(this.data_fallbackValue);
	}
	
	@Inject(method = "isTracked", at = @At("HEAD"), cancellable = true)
	private void onIsTracked(CallbackInfoReturnable<Boolean> info) {
		info.setReturnValue(true);
	}
	
	@Inject(method = "clamp", at = @At("HEAD"), cancellable = true)
	private void onClamp(double value, CallbackInfoReturnable<Double> info) {
		info.setReturnValue(this.data_clamp(value));
	}
	
	@Inject(method = "getTranslationKey", at = @At("HEAD"), cancellable = true)
	private void onGetTranslationKey(CallbackInfoReturnable<String> info) {
		info.setReturnValue(this.data_translationKey);
	}
	
	@Override
	public boolean contains(MutableEntityAttribute a, MutableEntityAttribute b) {
		if(b == null || a == b) return true;
		
		for(IEntityAttribute n : a.parentsMutable().keySet()) {
			MutableEntityAttribute m = (MutableEntityAttribute)n;
			
			if(m.contains(m, b)) return true;
		}
		
		return false;
	}
	
	@Override
	public void addParent(MutableEntityAttribute attributeIn, double multiplier) {
		this.data_parents.put(attributeIn, multiplier);
	}
	
	@Override
	public void addChild(MutableEntityAttribute attributeIn, final double multiplier) {
		if(this.contains(this, attributeIn)) return;
		
		attributeIn.addParent(this, multiplier);
		this.data_children.put(attributeIn, multiplier);
	}
	
	@Override
	public void transferAttribute(String translationKey, double minValue, double maxValue, double defaultValue, StackingBehaviour stackingBehaviour) {
		this.data_translationKey = translationKey;
		this.data_minValue = minValue;
		this.data_maxValue = maxValue;
		this.data_fallbackValue = defaultValue;
		this.data_stackingBehaviour = stackingBehaviour;
	}
	
	@Override
	public void transferProperties(Map<String, String> properties) {
		if(properties == null) return;
		this.data_properties = properties;
	}
	
	@Override
	public void clear() {
		this.transferAttribute(this.translationKey, this.fallback, this.fallback, this.fallback, StackingBehaviour.FLAT);
		this.transferProperties(new HashMap<String, String>());
		this.data_parents.clear();
		this.data_children.clear();
	}
	
	@Override
	public double minValue() {
		return this.data_minValue;
	}
	
	@Override
	public double maxValue() {
		return this.data_maxValue;
	}
	
	@Override
	public double stack(final double current, final double input) {
		return this.data_stackingBehaviour.result(current, input);
	}
	
	@Override
	public double sumStack(double positives, double negatives) {
		if(this.data_stackingBehaviour == StackingBehaviour.DIMINISHING) {
			return negatives - positives;
		} else {
			return positives - negatives;
		}
	}
	
	@Override
	public StackingBehaviour stackingBehaviour() {
		return this.data_stackingBehaviour;
	}
	
	@Override
	public Map<IEntityAttribute, Double> parentsMutable() {
		return this.data_parents;
	}
	
	@Override
	public Map<IEntityAttribute, Double> childrenMutable() {
		return this.data_children;
	}
	
	@Override
	public Map<IEntityAttribute, Double> parents() {
		return ImmutableMap.copyOf(this.data_parents);
	}
	
	@Override
	public Map<IEntityAttribute, Double> children() {
		return ImmutableMap.copyOf(this.data_children);
	}
	
	@Override
	public Collection<String> properties() {
		return ImmutableSet.copyOf(this.data_properties.keySet());
	}
	
	@Override
	public boolean hasProperty(final String property) {
		return this.data_properties.containsKey(property);
	}
	
	@Override
	public String getProperty(final String property) {
		return this.data_properties.getOrDefault(property, "");
	}
	
	protected double data_clamp(double value) {
		final MutableDouble mutable = new MutableDouble(value);
		
		EntityAttributeModifiedEvents.CLAMPED.invoker().onClamped((EntityAttribute)(Object)this, mutable);
		
		return MathHelper.clamp(mutable.getValue(), this.minValue(), this.maxValue());
	}
}
