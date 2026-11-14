package net.litetex.devauthneo.shared.config;

import java.util.List;


public abstract class RuntimeConfiguration implements Configuration
{
	public static RuntimeConfiguration environmentVariables(final String prefix)
	{
		return new RuntimeConfiguration()
		{
			@Override
			public String getString(final String path, final String def)
			{
				final String name = prefix + "_" + path.replace(".", "_").replace("-", "_").toUpperCase();
				final String value = System.getenv(name);
				return value != null ? value : def;
			}
		};
	}
	
	public static RuntimeConfiguration systemProperties(final String prefix)
	{
		return new RuntimeConfiguration()
		{
			@Override
			public String getString(final String path, final String def)
			{
				return System.getProperty(prefix + "." + path, def);
			}
		};
	}
	
	@Override
	public boolean getBoolean(final String path, final boolean def)
	{
		final String val = this.getString(path, Boolean.toString(def));
		return "1".equals(val) || Boolean.parseBoolean(val);
	}
	
	@Override
	public int getInteger(final String path, final int def)
	{
		try
		{
			return Integer.parseInt(this.getString(path, Integer.toString(def)));
		}
		catch(final NumberFormatException e)
		{
			return def;
		}
	}
	
	@Override
	public List<String> getStringList(final String path)
	{
		final String value = this.getString(path, "");
		return value.isEmpty() ? List.of() : List.of(value.split(","));
	}
	
	@Override
	public boolean contains(final String path)
	{
		return this.getString(path, null) != null;
	}
	
	@Override
	public void load()
	{
	}
	
	@Override
	public void save()
	{
	}
	
	@Override
	public void setString(final String path, final String value)
	{
	}
	
	@Override
	public void setBoolean(final String path, final boolean value)
	{
	}
	
	@Override
	public void setInteger(final String path, final int value)
	{
	}
	
	@Override
	public void setStringList(final String path, final List<String> value)
	{
	}
	
	@Override
	public void remove(final String path)
	{
	}
}
