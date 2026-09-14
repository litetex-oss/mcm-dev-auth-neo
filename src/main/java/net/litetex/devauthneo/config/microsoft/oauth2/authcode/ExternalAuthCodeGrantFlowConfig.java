package net.litetex.devauthneo.config.microsoft.oauth2.authcode;

import java.util.List;


public record ExternalAuthCodeGrantFlowConfig(
	String predefinedProvider,
	String clientId,
	List<String> scopes,
	String redirectUri,
	boolean openSystemBrowser,
	Integer portForRedirect
) implements AuthCodeGrantFlowConfig
{
}
