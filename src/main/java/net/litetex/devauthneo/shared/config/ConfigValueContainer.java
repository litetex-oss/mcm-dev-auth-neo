package net.litetex.devauthneo.shared.config;

import java.util.Objects;

import org.apache.commons.lang3.function.TriConsumer;
import org.apache.commons.lang3.function.TriFunction;


public class ConfigValueContainer<T>
{
	private final Configuration config;
	
	private final String path;
	private final T defaultValue;
	private final TriConsumer<Configuration, String, T> setIntoConfig;
	
	private T value;
	
	public ConfigValueContainer(
		final Configuration config,
		final String path,
		final T defaultValue,
		final TriFunction<Configuration, String, T, T> getFromConfig,
		final TriConsumer<Configuration, String, T> setIntoConfig)
	{
		this.config = config;
		this.path = path;
		this.defaultValue = defaultValue;
		this.setIntoConfig = setIntoConfig;
		
		this.value = getFromConfig.apply(config, path, defaultValue);
	}
	
	public static ConfigValueContainer<String> string(
		final Configuration config,
		final String path,
		final String defaultValue)
	{
		return new ConfigValueContainer<>(
			config,
			path,
			defaultValue,
			Configuration::getString,
			Configuration::setString);
	}
	
	public T value()
	{
		return this.value;
	}
	
	public void set(final T value)
	{
		this.value = value;
		
		if(Objects.equals(this.defaultValue, value))
		{
			this.config.remove(this.path);
		}
		else
		{
			this.setIntoConfig.accept(this.config, this.path, value);
		}
		this.config.save();
	}
}
