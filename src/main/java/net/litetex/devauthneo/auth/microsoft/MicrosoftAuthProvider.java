package net.litetex.devauthneo.auth.microsoft;

import static java.util.Objects.requireNonNullElseGet;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.litetex.devauthneo.auth.AuthProvider;
import net.litetex.devauthneo.auth.microsoft.oauth.OAuthGrantFlow;
import net.litetex.devauthneo.auth.microsoft.oauth.authcode.EmbeddedAuthCodeGrantFlow;
import net.litetex.devauthneo.auth.microsoft.oauth.authcode.ExternalAuthCodeGrantFlow;
import net.litetex.devauthneo.auth.microsoft.oauth.devicecode.DeviceCodeGrantFlow;
import net.litetex.devauthneo.auth.shared.HttpClientUtil;
import net.litetex.devauthneo.config.DevAuthNeoConfig;
import net.litetex.devauthneo.config.microsoft.oauth2.authcode.EmbeddedAuthCodeGrantFlowConfig;
import net.litetex.devauthneo.config.microsoft.oauth2.authcode.ExternalAuthCodeGrantFlowConfig;
import net.litetex.devauthneo.config.microsoft.oauth2.devicecode.DeviceCodeGrantFlowConfig;
import net.litetex.devauthneo.shared.json.JSONSerializer;


public class MicrosoftAuthProvider implements AuthProvider
{
	private static final Logger LOG = LoggerFactory.getLogger(MicrosoftAuthProvider.class);
	
	private static final URI MINECRAFT_PROFILE_URI = URI.create("https://api.minecraftservices.com/minecraft/profile");
	
	public static final String IDENTIFIER = "microsoft";
	
	private static final String ACCESS_TOKEN = "accessToken";
	private static final String UUID = "uuid";
	private static final String USERNAME = "username";
	
	private final DevAuthNeoConfig config;
	private final Path file;
	private final OAuthGrantFlow oAuthGrantFlow;
	
	private Map<String, Tokens> accountTokens = new HashMap<>();
	private Map<String, ProfileInfo> accountProfileInfos = new HashMap<>();
	
	public MicrosoftAuthProvider(final DevAuthNeoConfig config)
	{
		this.config = config;
		this.file = config.stateDir().resolve("microsoft-accounts.json");
		this.oAuthGrantFlow = switch(config.oAuth2())
		{
			case final EmbeddedAuthCodeGrantFlowConfig c -> new EmbeddedAuthCodeGrantFlow(c, config.stateDir());
			case final ExternalAuthCodeGrantFlowConfig c -> ExternalAuthCodeGrantFlow.create(c);
			case final DeviceCodeGrantFlowConfig c -> DeviceCodeGrantFlow.create(c);
			default -> throw new IllegalArgumentException("Unknown oauth2 config type");
		};
	}
	
	@Override
	public Set<String> possibleArgs()
	{
		return Set.of(ACCESS_TOKEN, UUID, USERNAME);
	}
	
	@Override
	public Map<String, String> getLoginParams(final String account)
	{
		this.readFile();
		
		final MicrosoftTokenManager loginExecutor = new MicrosoftTokenManager(
			this.oAuthGrantFlow,
			this.config.forceHandleAllTokensAsExpired(),
			this.accountTokens.get(account));
		LOG.debug("Getting session token");
		final String sessionToken = loginExecutor.getSessionToken();
		
		final AtomicBoolean markSaveRequired = new AtomicBoolean(false);
		loginExecutor.requiresTokenUpdate().ifPresent(t -> {
			LOG.debug("Updated tokens");
			this.accountTokens.put(account, t);
			markSaveRequired.set(true);
			this.saveAsync();
		});
		
		LOG.debug("Getting account profile info");
		final ProfileInfo accountProfileInfo = this.getAccountProfileInfo(account, sessionToken, markSaveRequired);
		if(markSaveRequired.get())
		{
			this.saveAsync();
		}
		
		LOG.debug("Returning login params");
		
		return Map.of(
			ACCESS_TOKEN, sessionToken,
			UUID, accountProfileInfo.uuid(),
			USERNAME, accountProfileInfo.name());
	}
	
	@SuppressWarnings("checkstyle:MagicNumber")
	private ProfileInfo getAccountProfileInfo(
		final String account,
		final String sessionToken,
		final AtomicBoolean markSaveRequired)
	{
		ProfileInfo profileInfo = this.accountProfileInfos.get(account);
		
		if(profileInfo == null
			|| profileInfo.fetchedAt().plus(this.config.cacheProfileInfoDuration()).isBefore(Instant.now()))
		{
			try(final HttpClient httpClient = HttpClientUtil.newHttpClientBuilder().build())
			{
				final HttpResponse<String> response = httpClient.send(
					HttpClientUtil.newHttpClientRequest(MINECRAFT_PROFILE_URI)
						.setHeader("Authorization", "Bearer " + sessionToken)
						.GET()
						.build(),
					HttpResponse.BodyHandlers.ofString());
				
				if(response.statusCode() == 404)
				{
					throw new RuntimeException("404 received for minecraft profile, does the user own the game?");
				}
				
				final JsonObject profileObject = JsonParser.parseString(
						HttpClientUtil.checkStatus(response).body())
					.getAsJsonObject();
				
				profileInfo = new ProfileInfo(
					profileObject.get("id").getAsString(),
					profileObject.get("name").getAsString(),
					Instant.now());
				this.accountProfileInfos.put(account, profileInfo);
				markSaveRequired.set(true);
				
				LOG.debug("Fetched ProfileInfo: {}", profileInfo);
			}
			catch(final Exception e)
			{
				LOG.warn("Failed to fetch minecraft profile - Trying to use cache", e);
			}
		}
		return Objects.requireNonNull(profileInfo, "No profile info present");
	}
	
	private void readFile()
	{
		if(!Files.exists(this.file))
		{
			return;
		}
		
		final long startMs = System.currentTimeMillis();
		try
		{
			final PersistentState persistentState =
				JSONSerializer.GSON.fromJson(Files.readString(this.file), PersistentState.class);
			
			this.accountTokens = requireNonNullElseGet(persistentState.accountTokens(), HashMap::new);
			this.accountProfileInfos = requireNonNullElseGet(persistentState.accountProfileInfos(), HashMap::new);
			
			LOG.debug(
				"Took {}ms to read {}x accountTokens",
				System.currentTimeMillis() - startMs,
				this.accountTokens.size());
		}
		catch(final Exception ex)
		{
			LOG.warn("Failed to read file['{}']", this.file, ex);
		}
	}
	
	private void saveAsync()
	{
		CompletableFuture.runAsync(this::saveToFile);
	}
	
	private synchronized void saveToFile()
	{
		final long startMs = System.currentTimeMillis();
		try
		{
			Files.writeString(
				this.file, JSONSerializer.GSON.toJson(new PersistentState(
					this.accountTokens,
					this.accountProfileInfos)));
			LOG.debug(
				"Took {}ms to write {}x accountTokens",
				System.currentTimeMillis() - startMs,
				this.accountTokens.size());
		}
		catch(final Exception ex)
		{
			LOG.warn("Failed to write file['{}']", this.file, ex);
		}
	}
	
	record PersistentState(
		Map<String, Tokens> accountTokens,
		Map<String, ProfileInfo> accountProfileInfos
	)
	{
	}
	
	
	record ProfileInfo(
		String uuid,
		String name,
		Instant fetchedAt)
	{
	}
}
