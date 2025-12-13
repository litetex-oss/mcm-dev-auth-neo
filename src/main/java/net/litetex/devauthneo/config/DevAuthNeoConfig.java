package net.litetex.devauthneo.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import net.litetex.devauthneo.auth.microsoft.MicrosoftAuthProvider;
import net.litetex.devauthneo.config.microsoft.oauth2.OAuth2GrantFlowConfig;
import net.litetex.devauthneo.config.microsoft.oauth2.authcode.EmbeddedAuthCodeGrantFlowConfig;
import net.litetex.devauthneo.config.microsoft.oauth2.authcode.ExternalAuthCodeGrantFlowConfig;
import net.litetex.devauthneo.config.microsoft.oauth2.devicecode.DeviceCodeGrantFlowConfig;
import net.litetex.devauthneo.shared.config.ConfigValueContainer;
import net.litetex.devauthneo.shared.config.Configuration;


public record DevAuthNeoConfig(
	boolean enabled,
	ConfigValueContainer<String> account,
	ConfigValueContainer<String> accountType,
	Path stateDir,
	OAuth2GrantFlowConfig oAuth2
)
{
	@SuppressWarnings("checkstyle:MagicNumber")
	public DevAuthNeoConfig(final Configuration configuration, final Path defaultStateDir)
	{
		this(
			configuration.getBoolean("enabled", false),
			ConfigValueContainer.string(configuration, "account", null),
			ConfigValueContainer.string(configuration, "account-type", MicrosoftAuthProvider.IDENTIFIER),
			Optional.ofNullable(configuration.getString("state-dir", null))
				.filter(s -> !s.isEmpty())
				.map(Paths::get)
				.orElse(defaultStateDir),
			buildOAuth2(configuration)
		);
	}
	
	private static OAuth2GrantFlowConfig buildOAuth2(final Configuration configuration)
	{
		final String prefix = "microsoft.oauth2.";
		
		final String authCode = "auth-code";
		final String authCodeEmbedded = authCode + "-embedded";
		final String grantFlow = configuration.getString(prefix + "grant-flow", authCodeEmbedded);
		
		// Common
		final String clientId = configuration.getString(prefix + "client-id", null);
		final List<String> scopes = configuration.getStringList(prefix + "scopes");
		
		if(grantFlow.startsWith(authCode))
		{
			final String redirectUri = configuration.getString(prefix + "redirect-uri", null);
			if(authCodeEmbedded.equals(grantFlow))
			{
				return new EmbeddedAuthCodeGrantFlowConfig(
					clientId,
					scopes,
					redirectUri,
					configuration.getBoolean(prefix + "use-temporary-cache-dir", true));
			}
			else if((authCode + "-external").equals(grantFlow))
			{
				return new ExternalAuthCodeGrantFlowConfig(
					configuration.getString(prefix + "predefined-provider", null),
					clientId,
					scopes,
					redirectUri,
					configuration.getBoolean(prefix + "open-system-browser", true),
					validatePortOrNull(configuration.getInteger(prefix + "redirect-port", -1)));
			}
		}
		else if("device-code".equals(grantFlow))
		{
			return new DeviceCodeGrantFlowConfig(
				configuration.getString(prefix + "predefined-provider", null),
				clientId,
				scopes);
		}
		
		throw new IllegalArgumentException("Unknown OAuth2 strategy: " + grantFlow);
	}
	
	@SuppressWarnings("checkstyle:MagicNumber")
	private static Integer validatePortOrNull(final int port)
	{
		return port > 0 && port <= 65536 ? port : null;
	}
}
