package net.litetex.devauthneo.config.microsoft.oauth2.authcode;

import java.util.List;


public record EmbeddedAuthCodeGrantFlowConfig(
	String clientId,
	List<String> scopes,
	String redirectUri,
	boolean cefUseTemporaryCacheDir
) implements AuthCodeGrantFlowConfig
{
}
