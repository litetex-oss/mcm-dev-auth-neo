package net.litetex.devauthneo.shared.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import net.litetex.devauthneo.shared.json.JSONSerializer;


public class FileConfiguration implements Configuration
{
	private static final Gson GSON = JSONSerializer.GSON;
	
	private final Path file;
	private JsonObject root;
	
	public FileConfiguration(final Path file)
	{
		this.file = file;
		this.load();
	}
	
	@Override
	public void load()
	{
		JsonObject root = null;
		if(Files.exists(this.file))
		{
			try(final BufferedReader reader = Files.newBufferedReader(this.file, StandardCharsets.UTF_8))
			{
				root = GSON.fromJson(reader, JsonObject.class);
			}
			catch(final IOException e)
			{
				LoggerFactory.getLogger(this.getClass()).error("Failed to read file[{}]", this.file, e);
			}
		}
		if(root == null)
		{
			root = new JsonObject();
		}
		this.root = root;
	}
	
	@Override
	public void save()
	{
		if(!Files.exists(this.file.getParent()))
		{
			try
			{
				Files.createDirectories(this.file.getParent());
			}
			catch(final IOException e)
			{
				LoggerFactory.getLogger(this.getClass()).warn("Failed to create parent directory", e);
			}
		}
		
		try(final BufferedWriter writer = Files.newBufferedWriter(this.file, StandardCharsets.UTF_8))
		{
			GSON.toJson(this.root, writer);
		}
		catch(final IOException e)
		{
			LoggerFactory.getLogger(this.getClass()).error("Failed to save file[{}]", this.file, e);
		}
	}
	
	@Override
	public String getString(final String path, final String def)
	{
		final JsonElement el = this.root.get(path);
		if(el == null || !el.isJsonPrimitive())
		{
			return def;
		}
		
		return el.getAsJsonPrimitive().getAsString();
	}
	
	@Override
	public boolean getBoolean(final String path, final boolean def)
	{
		final JsonElement el = this.root.get(path);
		if(el == null || !el.isJsonPrimitive())
		{
			return def;
		}
		
		final JsonPrimitive val = el.getAsJsonPrimitive();
		return val.isBoolean() ? val.getAsBoolean() : def;
	}
	
	@Override
	public int getInteger(final String path, final int def)
	{
		final JsonElement el = this.root.get(path);
		if(el == null || !el.isJsonPrimitive())
		{
			return def;
		}
		
		final JsonPrimitive val = el.getAsJsonPrimitive();
		return val.isNumber() ? val.getAsInt() : def;
	}
	
	@Override
	public List<String> getStringList(final String path)
	{
		final JsonElement el = this.root.get(path);
		if(el == null || !el.isJsonArray())
		{
			return Collections.emptyList();
		}
		
		final List<String> list = new ArrayList<>();
		for(final JsonElement child : el.getAsJsonArray())
		{
			if(child.isJsonPrimitive())
			{
				list.add(child.getAsJsonPrimitive().getAsString());
			}
		}
		return list;
	}
	
	@Override
	public void setString(final String path, final String value)
	{
		this.root.add(path, new JsonPrimitive(value));
	}
	
	@Override
	public void setBoolean(final String path, final boolean value)
	{
		this.root.add(path, new JsonPrimitive(value));
	}
	
	@Override
	public void setInteger(final String path, final int value)
	{
		this.root.add(path, new JsonPrimitive(value));
	}
	
	@Override
	public void setStringList(final String path, final List<String> value)
	{
		final JsonArray array = new JsonArray(value.size());
		value.forEach(array::add);
		this.root.add(path, array);
	}
	
	@Override
	public boolean contains(final String path)
	{
		return this.root.has(path);
	}
	
	@Override
	public void remove(final String path)
	{
		this.root.remove(path);
	}
}
