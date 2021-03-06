/*
 * Copyright (C) 2016, 2017, 2018 Adrian Siekierka
 *
 * This file is part of FoamFix.
 *
 * FoamFix is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FoamFix is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FoamFix.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7
 *
 * If you modify this Program, or any covered work, by linking or
 * combining it with the Minecraft game engine, the Mojang Launchwrapper,
 * the Mojang AuthLib and the Minecraft Realms library (and/or modified
 * versions of said software), containing parts covered by the terms of
 * their respective licenses, the licensors of this Program grant you
 * additional permission to convey the resulting work.
 */

package pl.asie.foamfix.common;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import pl.asie.foamfix.FoamFix;

import java.lang.invoke.MethodHandle;
import java.util.*;

/**
 * Created by asie on 12/31/16.
 */
public class FoamyExtendedBlockState extends FoamyBlockState implements IExtendedBlockState {
	private final ImmutableMap<IUnlistedProperty<?>, Optional<?>> unlistedProperties;

	public FoamyExtendedBlockState(PropertyValueMapper owner, Block block, ImmutableMap<IProperty<?>, Comparable<?>> properties, ImmutableMap<IUnlistedProperty<?>, Optional<?>> unlistedProperties) {
		super(owner, block, properties);
		this.unlistedProperties = unlistedProperties;
	}

	public FoamyExtendedBlockState(PropertyValueMapper owner, Block block, ImmutableMap<IProperty<?>, Comparable<?>> properties, ImmutableMap<IUnlistedProperty<?>, Optional<?>> unlistedProperties, int value) {
		super(owner, block, properties);
		this.unlistedProperties = unlistedProperties;
		this.value = value;
	}

	@Override
	public <T extends Comparable<T>, V extends T> IBlockState withProperty(IProperty<T> property, V propertyValue) {
		if (!this.getProperties().containsKey(property)) {
			throw new IllegalArgumentException("Cannot set property " + property + " as it does not exist in " + getBlock().getBlockState());
		} else {
			if (!property.getAllowedValues().contains(propertyValue)) {
				throw new IllegalArgumentException("Cannot set property " + property + " to " + value + " on block " + Block.REGISTRY.getNameForObject(getBlock()) + ", it is not an allowed value");
			} else {
				if (this.getProperties().get(property) == propertyValue) {
					return this;
				}

				int newValue = owner.withPropertyValue(value, property, propertyValue);
				if (newValue < 0) {
					throw new IllegalArgumentException("Cannot set property " + property + " because FoamFix could not find a mapping for it! Please reproduce without FoamFix first!");
				}

				IBlockState state = owner.getPropertyByValue(newValue);
				if (state == null) {
					throw new IllegalArgumentException("Incomplete? list of values when trying to set property " + property + "! Please reproduce without FoamFix first! (Info: " + getBlock().getRegistryName() + " " + value + " -> " + newValue + ")");
				}

				for (Optional optional : unlistedProperties.values()) {
					if (optional.isPresent()) {
						return new FoamyExtendedBlockState(owner, getBlock(), state.getProperties(), unlistedProperties, newValue);
					}
				}

				return state;
			}
		}
	}

	@Override
	public <V> IExtendedBlockState withProperty(IUnlistedProperty<V> property, V value) {
		if (!property.isValid(value)) {
			throw new IllegalArgumentException("Cannot set unlisted property " + property + " to " + value + " on block " + Block.REGISTRY.getNameForObject(getBlock()) + ", it is not an allowed value");
		}

		boolean hasOpt = false;
		boolean setValue = false;

		// TODO: Call with known initial capacity
		ImmutableMap.Builder<IUnlistedProperty<?>, Optional<?>> newMap = new ImmutableMap.Builder<>();
		for (Map.Entry<IUnlistedProperty<?>, Optional<?>> entry : unlistedProperties.entrySet()) {
			if (entry.getKey().equals(property)) {
				newMap.put(property, Optional.ofNullable(value));
				setValue = true;
			} else {
				newMap.put(entry.getKey(), entry.getValue());
				hasOpt |= entry.getValue().isPresent();
			}
		}

		if (!setValue) {
			throw new IllegalArgumentException("Cannot set unlisted property " + property + " as it does not exist in " + getBlock().getBlockState());
		}

		if (value != null || hasOpt) {
			return new FoamyExtendedBlockState(owner, getBlock(), getProperties(), newMap.build(), this.value);
		} else {
			return (IExtendedBlockState) owner.getPropertyByValue(this.value);
		}
	}

	@Override
	public Collection<IUnlistedProperty<?>> getUnlistedNames() {
		return unlistedProperties.keySet();
	}

	@Override
	public <V> V getValue(IUnlistedProperty<V> property) {
		Optional optional = this.unlistedProperties.get(property);

		if (optional == null) {
			throw new IllegalArgumentException("Cannot get unlisted property " + property + " as it does not exist in " + getBlock().getBlockState());
		}

		return property.getType().cast(optional.orElse(null));
	}

	@Override
	public ImmutableMap<IUnlistedProperty<?>, Optional<?>> getUnlistedProperties() {
		return unlistedProperties;
	}

	@Override
	public IBlockState getClean() {
		return owner.getPropertyByValue(this.value);
	}
}
