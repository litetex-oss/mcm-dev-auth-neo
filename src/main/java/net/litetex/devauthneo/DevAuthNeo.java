package net.litetex.devauthneo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.litetex.devauthneo.auth.AuthProvider;
import net.litetex.devauthneo.auth.microsoft.MicrosoftAuthProvider;
import net.litetex.devauthneo.config.DevAuthNeoConfig;
import net.litetex.devauthneo.shared.config.ConfigValueContainer;
import net.litetex.devauthneo.shared.config.Configuration;
import net.litetex.devauthneo.shared.config.FileConfiguration;
import net.litetex.devauthneo.shared.config.RuntimeConfiguration;


public class DevAuthNeo
{
	private static final Logger LOG = LoggerFactory.getLogger(DevAuthNeo.class);
	
	private final Path userhomeConfigFilePath;
	private final DevAuthNeoConfig config;
	
	public DevAuthNeo()
	{
		final Path defaultDir = Paths.get(System.getProperty("user.home")).resolve(".dev-auth-neo");
		
		this.userhomeConfigFilePath = defaultDir.resolve("config.json");
		this.config = new DevAuthNeoConfig(
			Configuration.combining(
				RuntimeConfiguration.environmentVariables("DEVAUTH"),
				RuntimeConfiguration.systemProperties("devauth"),
				new FileConfiguration(this.userhomeConfigFilePath)),
			defaultDir);
		
		if(!Files.exists(this.config.stateDir()))
		{
			try
			{
				Files.createDirectories(this.config.stateDir());
			}
			catch(final IOException e)
			{
				throw new UncheckedIOException("Failed to create stateDir", e);
			}
		}
		
		LOG.debug("Initialized");
	}
	
	public String[] processArguments(final String[] args)
	{
		if(!this.config.enabled())
		{
			LOG.info("DevAuth disabled. Set e.g. -Ddevauth.enabled=1 to enable");
			return args;
		}
		
		final Map<String, AuthProvider> authProviders = Map.of(
			MicrosoftAuthProvider.IDENTIFIER, new MicrosoftAuthProvider(this.config.stateDir()));
		
		if(!this.readFromSysInIfEmpty(this.config.account(), () -> "account")
			|| !this.readFromSysInIfEmpty(
			this.config.accountType(),
			() -> "account type [" + String.join(", ", authProviders.keySet()) + "]"))
		{
			return args;
		}
		
		final String accountType = this.config.accountType().value();
		final AuthProvider ap = authProviders.get(accountType);
		if(ap == null)
		{
			LOG.warn("Unknown account type: {}", accountType);
			return args;
		}
		
		final Map<String, String> loginOverrideArgValues = ap.login(this.config.account().value());
		
		final Set<String> argsToIgnore = ap.possibleArgs()
			.stream()
			.map(s -> "--" + s)
			.collect(Collectors.toSet());
		
		final List<String> cleanedArgs = new ArrayList<>();
		for(int i = 0; i < args.length; i++)
		{
			final String arg = args[i];
			if(argsToIgnore.contains(arg))
			{
				i++; // Also skip next
			}
			else
			{
				cleanedArgs.add(arg);
			}
		}
		
		return Stream.concat(
				cleanedArgs.stream(),
				loginOverrideArgValues.entrySet()
					.stream()
					.map(e -> "--" + e.getKey() + "=" + e.getValue()))
			.toArray(String[]::new);
	}
	
	private boolean readFromSysInIfEmpty(
		final ConfigValueContainer<String> cvc,
		final Supplier<String> supplierAttributeForMsg)
	{
		if(cvc.value() != null)
		{
			return true;
		}
		
		final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		LOG.warn("No {} to use specified. Please enter manually:", supplierAttributeForMsg.get());
		try
		{
			cvc.set(br.readLine());
		}
		catch(final IOException e)
		{
			LOG.warn("Failed to read from sysin", e);
			cvc.set(null); // Reset and create file if not exists
		}
		
		if(cvc.value() == null)
		{
			LOG.error("Nothing provided!");
			LOG.warn(
				"Please try again or add it manually to config using environment "
					+ "variables, system properties or adding it to {}",
				this.userhomeConfigFilePath.toAbsolutePath());
			return false;
		}
		return true;
	}
}
