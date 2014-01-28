package com.eviware.loadui.util.projects;

import com.eviware.loadui.api.model.ComponentBlueprint;

import java.util.ArrayList;
import java.util.List;


public class ComponentBuilder
{

	private String componentType;
	private List<ComponentBlueprint> child;
	private List<ComponentBlueprint.PropertyDescriptor> properties;
	private boolean concurrentUsers;

	private ComponentBuilder( String componentType )
	{
		this.child = new ArrayList<>();
		this.componentType = componentType;
		this.properties = new ArrayList<>();
		this.concurrentUsers = false;
	}

	public ComponentBuilder child( ComponentBlueprint... componentBlueprints )
	{
		for( ComponentBlueprint component : componentBlueprints ){
			this.child.add( component );
		}
		return this;
	}

	public <T> ComponentBuilder property( String key, Class<T> propertyType, Object value )
	{
		this.properties.add( new PropertyDescriptor( key, propertyType, value ) );
		return this;
	}

	public ComponentBuilder concurrent()
	{
		this.concurrentUsers = true;
		return this;
	}

	public ComponentBuilder arrival()
	{
		this.concurrentUsers = false;
		return this;
	}

	public ComponentBlueprintImpl build()
	{
		return new ComponentBlueprintImpl( componentType, child, properties, concurrentUsers );
	}

	public static WithType create()
	{
		return new WithType();
	}

	public static class WithType
	{
		public ComponentBuilder type( String componentType )
		{
			return new ComponentBuilder( componentType );
		}
	}

	public class ComponentBlueprintImpl implements ComponentBlueprint
	{
		private String componentType;
		private List<ComponentBlueprint> children;
		private List<PropertyDescriptor> properties;
		private boolean concurrentUsers;

		public ComponentBlueprintImpl( String componentType, List<ComponentBlueprint> children, List<ComponentBlueprint.PropertyDescriptor> properties, boolean concurrentUsers ){

			this.componentType = componentType;
			this.children = children;
			this.properties = properties;
			this.concurrentUsers = concurrentUsers;
		}

		public String getComponentType()
		{
			return componentType;
		}

		public List<ComponentBlueprint> getChildren()
		{
			return children;
		}

		public List<PropertyDescriptor> getProperties()
		{
			return properties;
		}

		public boolean isConcurrentUsers()
		{
			return concurrentUsers;
		}
	}

	public class PropertyDescriptor<Type extends Class> implements ComponentBlueprint.PropertyDescriptor<Type>
	{
		private Type type;
		private String key;
		private Object value;

		private PropertyDescriptor( String propertyName, Type type, Object value )
		{
			this.type = type;
			this.key = propertyName;
			this.value = value;
		}

		public Type getType()
		{
			return type;
		}

		public String getKey()
		{
			return key;
		}

		public Object getValue()
		{
			return value;
		}
	}
}
