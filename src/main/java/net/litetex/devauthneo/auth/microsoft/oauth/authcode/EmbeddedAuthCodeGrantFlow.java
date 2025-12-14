package net.litetex.devauthneo.auth.microsoft.oauth.authcode;

import static java.util.Objects.requireNonNullElse;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.swing.JFrame;

import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.slf4j.bridge.SLF4JBridgeHandler;

import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.MavenCefAppHandlerAdapter;
import net.litetex.devauthneo.auth.shared.HttpClientUtil;
import net.litetex.devauthneo.config.microsoft.oauth2.authcode.EmbeddedAuthCodeGrantFlowConfig;


// See also: https://learn.microsoft.com/en-us/advertising/shopping-content/code-example-authentication-oauth
public class EmbeddedAuthCodeGrantFlow extends AuthCodeGrantFlow
{
	private final Path stateDir;
	private final boolean cefUseTemporaryCacheDir;
	
	public EmbeddedAuthCodeGrantFlow(final EmbeddedAuthCodeGrantFlowConfig config, final Path stateDir)
	{
		super(
			requireNonNullElse(config.clientId(), "00000000402b5328"), // Minecraft launcher
			createScopes(config.scopes(), DEFAULT_SCOPES),
			requireNonNullElse(config.redirectUri(), "https://login.live.com/oauth20_desktop.srf"));
		this.stateDir = stateDir;
		this.cefUseTemporaryCacheDir = config.cefUseTemporaryCacheDir();
	}
	
	@SuppressWarnings("checkstyle:MagicNumber")
	@Override
	protected String getAuthorizationCode(final String codeVerifier)
	{
		final CompletableFuture<String> cfCode = new CompletableFuture<>();
		
		this.logger.info("Booting CEF, downloading of native assets may take a moment...");
		installJULToSLF4JBridgeIfRequired();
		// The game is headless by default
		setHeadless(false);
		
		try
		{
			final CefAppBuilder builder = new CefAppBuilder();
			builder.setInstallDir(this.stateDir.resolve("jcef").toFile());
			
			final CefSettings cefSettings = builder.getCefSettings();
			cefSettings.windowless_rendering_enabled = false;
			if(this.cefUseTemporaryCacheDir)
			{
				cefSettings.cache_path = createTempCefCacheDir();
				this.logger.info("CEF temp cache dir to use: {}", cefSettings.cache_path);
			}
			builder.setAppHandler(new MavenCefAppHandlerAdapter()
			{
				@Override
				public void stateHasChanged(final CefApp.CefAppState state)
				{
					EmbeddedAuthCodeGrantFlow.this.logger.info("CEF state changed to {}", state);
					if(state == CefApp.CefAppState.TERMINATED)
					{
						if(!cfCode.isDone())
						{
							cfCode.completeExceptionally(new RuntimeException(
								"CEF was shut down before receiving any login information"));
						}
						setHeadless(true);
					}
				}
			});
			
			final CefApp app;
			try
			{
				app = builder.build();
				this.logger.info("App created: {}", app.getVersion());
			}
			catch(final Exception e)
			{
				throw new RuntimeException("Failed to build CEF", e);
			}
			
			final CefClient client = app.createClient();
			client.addDisplayHandler(new CefDisplayHandlerAdapter()
			{
				@SuppressWarnings("PMD.NonExhaustiveSwitch")
				@Override
				public void onAddressChange(final CefBrowser browser, final CefFrame frame, final String url)
				{
					final URI uri = URI.create(url);
					switch(uri.getPath())
					{
						case "/oauth20_desktop.srf" -> this.handleFinalPage(browser, uri, true);
						case "/err.srf" -> this.handleFinalPage(browser, uri, false);
					}
				}
				
				private void handleFinalPage(
					final CefBrowser browser,
					final URI uri,
					final boolean isRedirectPage)
				{
					EmbeddedAuthCodeGrantFlow.this.logger.info("Handling final page: {}", uri);
					final Map<String, String> queryParams = HttpClientUtil.parseQuery(uri.getQuery());
					if(isRedirectPage && queryParams.containsKey("code"))
					{
						cfCode.complete(queryParams.get("code"));
					}
					else
					{
						cfCode.completeExceptionally(new IllegalStateException(
							"Encountered error during oauth flow: " + requireNonNullElse(
								queryParams.get("error_description"),
								"no error description provided")));
					}
					browser.close(false);
				}
			});
			
			this.logger.info("Client created");
			
			final CefBrowser browser = client.createBrowser(
				this.buildAuthorizeUrl(codeVerifier),
				false,
				false);
			
			this.logger.info("Showing sign in popup");
			
			final JFrame popup = new JFrame();
			popup.add(browser.getUIComponent());
			popup.setTitle("DevAuth Neo: Sign in to Microsoft account");
			popup.pack();
			popup.setSize(600, 700);
			popup.setLocationRelativeTo(null); // Center
			popup.addWindowListener(new WindowAdapter()
			{
				@Override
				public void windowClosing(final WindowEvent e)
				{
					popup.dispose();
					app.dispose();
				}
			});
			popup.setVisible(true);
			
			try
			{
				return cfCode.join();
			}
			finally
			{
				popup.dispose();
			}
		}
		finally
		{
			setHeadless(true);
		}
	}
	
	private static String createTempCefCacheDir()
	{
		try
		{
			return Files.createTempDirectory("dev-auth-neo-cef").toAbsolutePath().toString();
		}
		catch(final IOException e)
		{
			throw new UncheckedIOException("Failed to create temp dir", e);
		}
	}
	
	private static void setHeadless(final boolean value)
	{
		System.setProperty("java.awt.headless", String.valueOf(value));
	}
	
	private static void installJULToSLF4JBridgeIfRequired()
	{
		if(!SLF4JBridgeHandler.isInstalled())
		{
			SLF4JBridgeHandler.removeHandlersForRootLogger();
			SLF4JBridgeHandler.install();
		}
	}
}
