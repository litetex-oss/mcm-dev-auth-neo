package net.litetex.devauthneo.shared.config;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;


class CombinedConfiguration implements Configuration
{
	private final List<Configuration> configurations;
	
	CombinedConfiguration(final Configuration... configurations)
	{
		this.configurations = ImmutableList.copyOf(configurations).reverse();
	}
	
	@Override
	public void load()
	{
		for(final Configuration configuration : this.configurations)
		{
			configuration.load();
		}
	}
	
	@Override
	public void save()
	{
		for(final Configuration configuration : this.configurations)
		{
			configuration.save();
		}
	}
	
	@Override
	public String getString(final String path, final String def)
	{
		String result = def;
		for(final Configuration configuration : this.configurations)
		{
			result = configuration.getString(path, result);
		}
		return result;
	}
	
	@Override
	public boolean getBoolean(final String path, final boolean def)
	{
		boolean result = def;
		for(final Configuration configuration : this.configurations)
		{
			result = configuration.getBoolean(path, result);
		}
		return result;
	}
	
	@Override
	public int getInteger(final String path, final int def)
	{
		int result = def;
		for(final Configuration configuration : this.configurations)
		{
			result = configuration.getInteger(path, result);
		}
		return result;
	}
	
	@Override
	public List<String> getStringList(final String path)
	{
		for(final Configuration configuration : this.configurations)
		{
			final List<String> result = configuration.getStringList(path);
			if(!result.isEmpty())
			{
				return result;
			}
		}
		return Collections.emptyList();
	}
	
	@Override
	public void setString(final String path, final String value)
	{
		for(final Configuration configuration : this.configurations)
		{
			configuration.setString(path, value);
		}
	}
	
	@Override
	public void setBoolean(final String path, final boolean value)
	{
		for(final Configuration configuration : this.configurations)
		{
			configuration.setBoolean(path, value);
		}
	}
	
	@Override
	public void setInteger(final String path, final int value)
	{
		for(final Configuration configuration : this.configurations)
		{
			configuration.setInteger(path, value);
		}
	}
	
	@Override
	public void setStringList(final String path, final List<String> value)
	{
		for(final Configuration configuration : this.configurations)
		{
			configuration.setStringList(path, value);
		}
	}
	
	@Override
	public boolean contains(final String path)
	{
		for(final Configuration configuration : this.configurations)
		{
			if(configuration.contains(path))
			{
				return true;
			}
		}
		return false;
	}
	
	@Override
	public void remove(final String path)
	{
		for(final Configuration configuration : this.configurations)
		{
			configuration.remove(path);
		}
	}
}
