package net.litetex.devauthneo.auth.microsoft.oauth.devicecode;

import static java.util.Objects.requireNonNullElse;

import java.net.URI;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.litetex.devauthneo.auth.microsoft.oauth.BaseGrantFlow;
import net.litetex.devauthneo.auth.microsoft.token.OAuthToken;
import net.litetex.devauthneo.auth.shared.HttpClientUtil;
import net.litetex.devauthneo.config.microsoft.oauth2.devicecode.DeviceCodeGrantFlowConfig;


public class DeviceCodeGrantFlow extends BaseGrantFlow
{
	private static final String TENANT = "consumers";
	
	private static final URI DEVICE_CODE_URI = URI.create(
		"https://login.microsoftonline.com/" + TENANT + "/oauth2/v2.0/devicecode");
	private static final URI TOKEN_URI = URI.create(
		"https://login.microsoftonline.com/" + TENANT + "/oauth2/v2.0/token");
	
	private static final String MULTIMC_ID = "multimc";
	private static final Map<String, PredefinedProvider> PRE_DEFINED_PROVIDERS = Map.of(
		MULTIMC_ID, new PredefinedProvider("499546d9-bbfe-4b9b-a086-eb3d75afb78f"),
		"devlogin", new PredefinedProvider("170105bd-9573-4222-b09c-6f24c3b77cd8"),
		"prism", new PredefinedProvider("c36a9fb6-4f2a-41ff-90bd-ae7cc92031eb")
	);
	
	DeviceCodeGrantFlow(
		final DeviceCodeGrantFlowConfig config,
		final PredefinedProvider predefinedProvider)
	{
		super(
			requireNonNullElse(config.clientId(), predefinedProvider.clientId()),
			createScopes(config.scopes(), DEFAULT_SCOPES)
		);
	}
	
	public static DeviceCodeGrantFlow create(final DeviceCodeGrantFlowConfig config)
	{
		final PredefinedProvider predefinedProvider = Optional.ofNullable(config.predefinedProvider())
			.map(String::toLowerCase)
			.map(PRE_DEFINED_PROVIDERS::get)
			.orElseGet(() -> PRE_DEFINED_PROVIDERS.get(MULTIMC_ID));
		return new DeviceCodeGrantFlow(config, predefinedProvider);
	}
	
	@Override
	public OAuthToken getToken()
	{
		final JsonObject startAuthResponse = this.authorizationRequest(DEVICE_CODE_URI, Map.of());
		
		final int expiresInSec = startAuthResponse.get("expires_in").getAsInt();
		final long expiresMs = System.currentTimeMillis() + expiresInSec * 1000L;
		
		final int intervalMs = startAuthResponse.get("interval").getAsInt();
		final String deviceCode = startAuthResponse.get("device_code").getAsString();
		
		this.logger.info(
			"Starting Device auth (expires in {}):\n{}",
			Duration.ofSeconds(expiresInSec),
			startAuthResponse.get("message"));
		
		while(System.currentTimeMillis() < expiresMs)
		{
			try
			{
				Thread.sleep(intervalMs);
			}
			catch(final InterruptedException ignored)
			{
				// Ignored
			}
			
			final HttpResponse<String> response = HttpClientUtil.postForm(
				TOKEN_URI,
				Map.of(
					"grant_type", "urn:ietf:params:oauth:grant-type:device_code",
					"device_code", deviceCode,
					"client_id", this.clientId));
			if(response.statusCode() != 200)
			{
				final String error =
					JsonParser.parseString(response.body()).getAsJsonObject().get("error").getAsString();
				if("authorization_pending".equals(error))
				{
					continue;
				}
				throw new IllegalStateException("Device Flow failure:" + response.body());
			}
			
			return OAuthToken.fromJson(JsonParser.parseString(response.body()).getAsJsonObject());
		}
		throw new IllegalStateException("Timed out while doing device flow");
	}
	
	@Override
	protected URI refreshTokenUri()
	{
		return TOKEN_URI;
	}
	
	record PredefinedProvider(
		String clientId
	)
	{
	}
}
