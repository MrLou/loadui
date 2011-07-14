/*
 * Copyright 2011 eviware software ab
 * 
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * 
 * http://ec.europa.eu/idabc/eupl5
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the Licence for the specific language governing permissions and limitations
 * under the Licence.
 */
package com.eviware.loadui.impl.addon;

import java.util.Collection;

import org.springframework.core.convert.ConversionService;

import com.eviware.loadui.api.addon.AddonHolder;
import com.eviware.loadui.api.addon.AddonHolder.Support;
import com.eviware.loadui.api.addon.AddonItem;
import com.eviware.loadui.api.addressable.AddressableRegistry;
import com.eviware.loadui.api.model.PropertyHolder;
import com.eviware.loadui.api.property.PropertyMap;
import com.eviware.loadui.api.traits.Releasable;
import com.eviware.loadui.config.AddonItemConfig;
import com.eviware.loadui.config.AddonListConfig;
import com.eviware.loadui.impl.property.AttributeHolderSupport;
import com.eviware.loadui.impl.property.PropertyMapImpl;
import com.eviware.loadui.util.BeanInjector;
import com.eviware.loadui.util.events.EventSupport;
import com.google.common.base.Objects;

public class AddonItemSupportImpl implements AddonItem.Support, Releasable
{
	private final AddonHolderSupportImpl owner;
	private final AddonItemConfig config;
	private final AddonListConfig listConfig;

	private final EventSupport eventSupport = new EventSupport();
	private final AttributeHolderSupport attributeSupport;

	private PropertyMapImpl propertyMap;
	private AddonHolderSupportImpl addonHolderSupport;

	public AddonItemSupportImpl( AddonHolderSupportImpl owner, AddonItemConfig config, AddonListConfig listConfig )
	{
		this.owner = owner;
		this.config = config;
		this.listConfig = listConfig;

		if( !config.isSetId() )
			config.setId( BeanInjector.getBean( AddressableRegistry.class ).generateId() );

		attributeSupport = new AttributeHolderSupport( config.getAttributes() == null ? config.addNewAttributes()
				: config.getAttributes() );
	}

	@Override
	public String getId()
	{
		return config.getId();
	}

	@Override
	public PropertyMap getPropertyMap( PropertyHolder owner )
	{
		if( propertyMap == null )
		{
			propertyMap = new PropertyMapImpl( owner, BeanInjector.getBean( ConversionService.class ),
					config.getProperties() == null ? config.addNewProperties() : config.getProperties() );
		}

		return propertyMap;
	}

	@Override
	public Support getAddonHolderSupport( AddonHolder owner )
	{
		if( addonHolderSupport == null )
		{
			addonHolderSupport = new AddonHolderSupportImpl( owner, config.getAddons() == null ? config.addNewAddons()
					: config.getAddons() );
		}

		return addonHolderSupport;
	}

	@Override
	public String getAttribute( String key, String defaultValue )
	{
		return attributeSupport.getAttribute( key, defaultValue );
	}

	@Override
	public void setAttribute( String key, String value )
	{
		attributeSupport.setAttribute( key, value );
	}

	@Override
	public void removeAttribute( String key )
	{
		attributeSupport.removeAttribute( key );
	}

	@Override
	public Collection<String> getAttributes()
	{
		return attributeSupport.getAttributes();
	}

	@Override
	public String getType()
	{
		return config.getType();
	}

	@Override
	public void delete()
	{
		for( int i = listConfig.sizeOfAddonArray() - 1; i <= 0; i-- )
		{
			if( Objects.equal( config.getId(), listConfig.getAddonArray( i ).getId() ) )
			{
				listConfig.removeAddon( i );
				owner.removeAddonItem( this );
				return;
			}
		}
	}

	@Override
	public void release()
	{
		eventSupport.clearEventListeners();
	}
}