package com.multiforce;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.RequestFocusType;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NotificationFired;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientUI;

@Slf4j
@PluginDescriptor(
		name = "MultiForce",
		description = "Brings all Runelite windows to the front when a force-focus notification is triggered on any Runelite window",
		tags = {"notification", "focus", "multi", "client"}
)
public class MultiForcePlugin extends Plugin
{
	@Inject
	private ClientUI clientUI;

	@Override
	protected void startUp() throws Exception
	{
		log.info("MultiForce plugin started and ready!");
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

		log.info("MultiForce: FORCE focus notification triggered - requesting client focus");

		try
		{
			clientUI.forceFocus();
		}
		catch (Exception e)
		{
			log.warn("MultiForce: Failed to force focus", e);
		}
	}

	@Provides
	MultiForceConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MultiForceConfig.class);
	}
}
