package net.litetex.devauthneo.auth.microsoft;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import net.litetex.devauthneo.auth.microsoft.oauth.OAuthGrantFlow;
import net.litetex.devauthneo.auth.microsoft.token.OAuthToken;
import net.litetex.devauthneo.auth.microsoft.token.Token;
import net.litetex.devauthneo.auth.microsoft.token.XBLToken;
import net.litetex.devauthneo.auth.shared.HttpClientUtil;


class MicrosoftLoginExecutor
{
	private static final Logger LOG = LoggerFactory.getLogger(MicrosoftLoginExecutor.class);
	
	private static final URI XBL_URI = URI.create("https://user.auth.xboxlive.com/user/authenticate");
	private static final URI XSTS_URI = URI.create("https://xsts.auth.xboxlive.com/xsts/authorize");
	private static final URI MINECRAFT_URI =
		URI.create("https://api.minecraftservices.com/authentication/login_with_xbox");
	private static final URI MINECRAFT_PROFILE_URI = URI.create("https://api.minecraftservices.com/minecraft/profile");
	
	private static final int NOT_FOUND = 404;
	
	private final OAuthGrantFlow oAuthGrantFlow;
	private final Tokens tokens;
	
	private boolean updatedTokens;
	
	MicrosoftLoginExecutor(
		final OAuthGrantFlow oAuthGrantFlow,
		final Tokens tokens)
	{
		this.oAuthGrantFlow = oAuthGrantFlow;
		this.tokens = Objects.requireNonNullElseGet(tokens, Tokens::new);
	}
	
	public LoginData login()
	{
		LOG.info("Executing login; Selected OAuth2 Provider is {}", this.oAuthGrantFlow);
		final Token mcSession = this.getToken("session", Tokens::getSession, Tokens::setSession, this::fetchMcSession);
		
		try(final HttpClient httpClient = HttpClientUtil.newHttpClientBuilder().build())
		{
			final HttpResponse<String> response = httpClient.send(
				HttpClientUtil.newHttpClientRequest(MINECRAFT_PROFILE_URI)
					.setHeader("Authorization", "Bearer " + mcSession.getToken())
					.GET()
					.build(),
				HttpResponse.BodyHandlers.ofString());
			
			if(response.statusCode() == NOT_FOUND)
			{
				throw new RuntimeException("404 received for minecraft profile, does the user own the game?");
			}
			
			final String body = HttpClientUtil.checkStatus(response).body();
			final JsonObject profileObject = JsonParser.parseString(body).getAsJsonObject();
			
			return new LoginData(
				mcSession.getToken(),
				profileObject.get("id").getAsString(),
				profileObject.get("name").getAsString());
		}
		catch(final Exception e)
		{
			throw new RuntimeException("Failed to fetch minecraft profile", e);
		}
	}
	
	private Token fetchMcSession()
	{
		final XBLToken xstsToken = this.getToken("xsts", Tokens::getXsts, Tokens::setXsts, this::fetchXSTSToken);
		
		final JsonObject object = new JsonObject();
		object.addProperty("identityToken", "XBL3.0 x=" + xstsToken.getUserHash() + ";" + xstsToken.getToken());
		
		final JsonObject res = HttpClientUtil.jsonPost(MINECRAFT_URI, object);
		
		return Token.fromJson(res);
	}
	
	private XBLToken fetchXSTSToken()
	{
		final XBLToken xblToken = this.getToken("xbl", Tokens::getXbl, Tokens::setXbl, this::fetchXBLToken);
		
		final JsonObject object = new JsonObject();
		
		final JsonObject properties = new JsonObject();
		properties.addProperty("SandboxId", "RETAIL");
		
		final JsonArray userTokens = new JsonArray();
		userTokens.add(new JsonPrimitive(xblToken.getToken()));
		properties.add("UserTokens", userTokens);
		
		object.add("Properties", properties);
		
		object.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
		object.addProperty("TokenType", "JWT");
		
		final JsonObject res = HttpClientUtil.jsonPost(XSTS_URI, object);
		
		return XBLToken.fromJson(res, true);
	}
	
	private XBLToken fetchXBLToken()
	{
		final OAuthToken oAuthToken = this.getToken(
			"oAuth",
			Tokens::getOauth,
			Tokens::setOauth,
			this.oAuthGrantFlow::getToken,
			this.oAuthGrantFlow::refreshToken);
		
		final JsonObject object = new JsonObject();
		
		final JsonObject properties = new JsonObject();
		object.add("Properties", properties);
		properties.addProperty("AuthMethod", "RPS");
		properties.addProperty("SiteName", "user.auth.xboxlive.com");
		properties.addProperty("RpsTicket", "d=" + oAuthToken.getToken());
		
		// noinspection HttpUrlsUsage
		object.addProperty("RelyingParty", "http://auth.xboxlive.com");
		object.addProperty("TokenType", "JWT");
		
		final JsonObject res = HttpClientUtil.jsonPost(XBL_URI, object);
		
		return XBLToken.fromJson(res, true);
	}
	
	private <T extends Token> T getToken(
		final String name,
		final Function<Tokens, T> getter,
		final BiConsumer<Tokens, T> setter,
		final Supplier<T> fetchFunc)
	{
		return this.getToken(name, getter, setter, fetchFunc, null);
	}
	
	private <T extends Token> T getToken(
		final String name,
		final Function<Tokens, T> getter,
		final BiConsumer<Tokens, T> setter,
		final Supplier<T> fetchFunc,
		final Function<T, T> refreshFunc)
	{
		final T existingToken = getter.apply(this.tokens);
		if(existingToken != null)
		{
			if(!existingToken.isExpired())
			{
				return existingToken;
			}
			
			if(refreshFunc != null)
			{
				try
				{
					refreshFunc.apply(existingToken);
				}
				catch(final Exception ex)
				{
					LOG.warn("Failed to refresh token - fetching a new one", ex);
				}
			}
		}
		
		return this.setTokenAfterFetch(name, setter, fetchFunc.get());
	}
	
	private <T extends Token> T setTokenAfterFetch(
		final String name,
		final BiConsumer<Tokens, T> setter,
		final T token)
	{
		if(LOG.isInfoEnabled())
		{
			LOG.info(
				"Updated token {} (Expiry: {} or in {})",
				name,
				token.getExpiry(),
				Duration.between(Instant.now(), token.getExpiry()));
		}
		
		this.updatedTokens = true;
		setter.accept(this.tokens, token);
		return token;
	}
	
	public Optional<Tokens> requiresTokenUpdate()
	{
		return this.updatedTokens ? Optional.of(this.tokens) : Optional.empty();
	}
}
