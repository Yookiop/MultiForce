package com.example;

import com.google.inject.Provides;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import java.awt.Frame;
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
	description = "Brings all RuneLite clients to the front when a force-focus notification is triggered",
	tags = {"notification", "focus", "multi", "client"}
)
public class ExamplePlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ExampleConfig config;

	@Override
	protected void startUp() throws Exception
	{
		log.debug("MultiForce started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.debug("MultiForce stopped!");
	}

	@Subscribe
	public void onNotificationFired(NotificationFired notificationFired)
	{
		// Only process FORCE focus notifications
		if (notificationFired.getNotification().getRequestFocus() != RequestFocusType.FORCE)
		{
			return;
		}

		// Only Windows supports multi-window focusing via JNA
		if (OSType.getOSType() != OSType.Windows)
		{
			log.debug("MultiForce: Skipping multi-window focus on non-Windows OS");
			return;
		}

		try
		{
			bringAllClientWindowsToFront();
		}
		catch (Exception e)
		{
			log.warn("MultiForce: Failed to bring all windows to front", e);
		}
	}

	/**
	 * Finds all RuneLite client windows and brings them to the foreground
	 */
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

		// For each window, simulate input and set it to foreground
		for (WinDef.HWND hwnd : runeliteWindows)
		{
			try
			{
				// Send a F22 key event to allow SetForegroundWindow to work
				// (Windows security restriction)
				WinUser.INPUT input = new WinUser.INPUT();
				input.type = new WinDef.DWORD(WinUser.INPUT.INPUT_KEYBOARD);
				input.input.ki.wVk = new WinDef.WORD(0x85); // VK_F22
				user32.SendInput(new WinDef.DWORD(1), (WinUser.INPUT[]) input.toArray(1), input.size());

				// Bring the window to foreground
				user32.SetForegroundWindow(hwnd);
				log.debug("MultiForce: Brought window to front");
			}
			catch (Exception e)
			{
				log.warn("MultiForce: Failed to focus window", e);
			}
		}
	}

	/**
	 * Finds all RuneLite client windows by enumerating all windows and checking titles
	 */
	private List<WinDef.HWND> findAllRuneliteWindows()
	{
		List<WinDef.HWND> windows = new ArrayList<>();
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
					// Check if this is a RuneLite window
					if (title.contains("RuneLite"))
					{
						windows.add(hWnd);
					}
				}

				return true; // Continue enumeration
			}, null);
		}
		catch (Exception e)
		{
			log.warn("MultiForce: Failed to enumerate windows", e);
		}

		return windows;
	}

	@Provides
	ExampleConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ExampleConfig.class);
	}
}
