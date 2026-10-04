package net.litetex.devauthneo.config.microsoft.oauth2.devicecode;

import java.util.List;

import net.litetex.devauthneo.config.microsoft.oauth2.OAuth2GrantFlowConfig;


public record DeviceCodeGrantFlowConfig(
	String predefinedProvider,
	String clientId,
	List<String> scopes
) implements OAuth2GrantFlowConfig
{
}
