package net.litetex.devauthneo.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import net.litetex.devauthneo.auth.microsoft.MicrosoftAuthProvider;
import net.litetex.devauthneo.shared.config.ConfigValueContainer;
import net.litetex.devauthneo.shared.config.Configuration;


public record DevAuthNeoConfig(
	boolean enabled,
	ConfigValueContainer<String> account,
	ConfigValueContainer<String> accountType,
	Path stateDir
)
{
	public DevAuthNeoConfig(final Configuration configuration, final Path defaultStateDir)
	{
		this(
			configuration.getBoolean("enabled", false),
			ConfigValueContainer.string(configuration, "account", null),
			ConfigValueContainer.string(configuration, "accountType", MicrosoftAuthProvider.IDENTIFIER),
			Optional.ofNullable(configuration.getString("state-dir", null))
				.filter(s -> !s.isEmpty())
				.map(Paths::get)
				.orElse(defaultStateDir)
		);
	}
}
