package com.multiforce;

import com.google.inject.Provides;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.RequestFocusType;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NotificationFired;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.OSType;

@Slf4j
@PluginDescriptor(
		name = "MultiForce",
		description = "Brings all Runelite windows to the front when a force-focus notification is triggered on any Runelite window",
		tags = {"notification", "focus", "multi", "client"}
)
public class MultiForcePlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private MultiForceConfig config;

	@Override
	protected void startUp() throws Exception
	{
		log.info("MultiForce plugin started and ready!");

		if (OSType.getOSType() == OSType.Windows)
		{
			List<WinDef.HWND> runeliteWindows = findAllRuneliteWindows();
			log.info("MultiForce: Current RuneLite windows on startup: {}", runeliteWindows.size());
		}
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.debug("MultiForce stopped!");
	}

	@Subscribe
	public void onNotificationFired(NotificationFired notificationFired)
	{
		log.info("MultiForce: Notification event received!");

		// Only process FORCE focus notifications
		if (notificationFired.getNotification().getRequestFocus() != RequestFocusType.FORCE)
		{
			log.debug("MultiForce: Notification focus type is {} (not FORCE), skipping",
					notificationFired.getNotification().getRequestFocus());
			return;
		}

		if (OSType.getOSType() != OSType.Windows)
		{
			log.debug("MultiForce: Skipping multi-window focus on non-Windows OS");
			return;
		}

		log.info("MultiForce: FORCE focus notification triggered - attempting to bring all RuneLite windows to front");

		try
		{
			bringAllClientWindowsToFront();
		}
		catch (Exception e)
		{
			log.warn("MultiForce: Failed to bring all windows to front", e);
		}
	}

	private void bringAllClientWindowsToFront()
	{
		List<WinDef.HWND> runeliteWindows = findAllRuneliteWindows();

		if (runeliteWindows.isEmpty())
		{
			log.debug("MultiForce: No RuneLite windows found");
			return;
		}

		log.debug("MultiForce: Found {} RuneLite window(s), bringing all to front", runeliteWindows.size());

		User32 user32 = User32.INSTANCE;

		for (int i = 0; i < runeliteWindows.size(); i++)
		{
			WinDef.HWND hwnd = runeliteWindows.get(i);
			try
			{
				log.debug("MultiForce: Bringing window {} of {} to front", i + 1, runeliteWindows.size());

				user32.SetForegroundWindow(hwnd);
				log.debug("MultiForce: Successfully brought window {} to front", i + 1);
			}
			catch (Exception e)
			{
				log.warn("MultiForce: Failed to focus window {} of {}", i + 1, runeliteWindows.size(), e);
			}
		}

		log.debug("MultiForce: Completed bringing {} RuneLite window(s) to front", runeliteWindows.size());
	}


	private List<WinDef.HWND> findAllRuneliteWindows()
	{
		List<WinDef.HWND> windows = new ArrayList<>();
		List<String> allRuneliteMatches = new ArrayList<>();
		User32 user32 = User32.INSTANCE;

		try
		{
			user32.EnumWindows((hWnd, data) ->
			{
				char[] titleChars = new char[512];
				int length = user32.GetWindowText(hWnd, titleChars, titleChars.length);

				if (length > 0)
				{
					String title = new String(titleChars, 0, length);

					if (title.startsWith("RuneLite - "))
					{
						windows.add(hWnd);
						allRuneliteMatches.add(title);
						log.info("MultiForce: Found RuneLite client window: '{}'", title);
					}
				}

				return true;
			}, null);
		}
		catch (Exception e)
		{
			log.warn("MultiForce: Failed to enumerate windows", e);
		}

		log.info("MultiForce: RuneLite client windows found: {}", windows.size());
		if (windows.isEmpty())
		{
			log.warn("MultiForce: No RuneLite client windows found");
		}

		return windows;
	}

	@Provides
	MultiForceConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MultiForceConfig.class);
	}
}
