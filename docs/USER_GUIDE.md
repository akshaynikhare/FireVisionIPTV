# FireVision IPTV -- User Guide

This guide walks you through setting up and using FireVision IPTV on your Amazon Fire TV, Android TV, or Android device.

## Table of Contents

1. [Getting Started](#getting-started)
2. [Connecting to Your Server](#connecting-to-your-server)
3. [Device Pairing (PIN and QR Code)](#device-pairing-pin-and-qr-code)
4. [Browsing Channels](#browsing-channels)
5. [Favorites](#favorites)
6. [Search](#search)
7. [Watching a Channel](#watching-a-channel)
8. [Settings Reference](#settings-reference)
9. [Troubleshooting](#troubleshooting)

---

## Getting Started

FireVision IPTV is a streaming app for live TV channels. It works on:

- Amazon Fire TV Stick and Fire TV Cube
- Android TV devices (Shield, Chromecast with Google TV, smart TVs)
- Android phones and tablets (Android 9 or later)

The app connects to a FireVision IPTV Server that provides your channel list. You will need a running server instance and a device pairing code to get started.

### First Launch

1. Install the app on your device.
2. A splash screen appears briefly while the app loads.
3. If this is your first time, you will be taken to the **Pairing Screen** to connect to your server.
4. After pairing, the app loads your channel list and takes you to the **Home Screen**.

---

## Connecting to Your Server

Before you can watch channels, the app needs to know where your FireVision IPTV Server is running.

### Setting the Server URL

1. On the **Settings** screen, find the **Device Setup** section.
2. In the **Server URL** field, enter the address of your server.
   - If the server is on your local network: `http://192.168.1.100:3000` (use your server's local IP and port)
   - If the server is accessible over the internet: `https://tv.example.com`
3. Select **Save Settings**.

The default server URL is `https://tv.cadnative.com`. Change this only if you are running your own server.

---

## Device Pairing (PIN and QR Code)

Pairing links your TV device to your account on the FireVision server. There are two ways to pair.

### Method 1: PIN Code

1. Open FireVision IPTV on your TV.
2. If unpaired, the Pairing screen appears automatically. Otherwise, go to **Settings** and select **Pair with PIN**.
3. A 6-digit PIN is displayed on the TV screen in large text.
4. On your phone or computer, open the FireVision server dashboard in a web browser.
5. Go to the device pairing page and enter the 6-digit PIN.
6. Confirm the pairing on the dashboard.
7. The TV app detects the pairing automatically and shows a welcome message with your username.
8. Your channel list loads and the app navigates to the Home screen.

### Method 2: QR Code

1. On the Pairing screen (or Settings if unpaired), a QR code is displayed next to the PIN.
2. Scan the QR code with your phone's camera.
3. The QR code opens the pairing page in your phone's browser with the PIN already filled in.
4. Sign in or create an account on the dashboard.
5. Confirm the pairing.
6. The TV app detects the pairing automatically.

### Pairing Tips

- The PIN expires after 10 minutes. If it expires, select **Generate New PIN** to get a fresh one.
- Make sure your TV and the device you are pairing from can both reach the server (same network or internet access).
- If pairing gets stuck, check that the server URL is correct in Settings.
- You can select **Skip -- Use Default Channels** at the bottom of the pairing screen to browse default channels without pairing. You can pair later from Settings.

---

## Browsing Channels

### Home Screen

The Home screen is the main hub of the app. It shows:

- **Featured Channels** -- A top banner carousel highlighting popular or most-watched channels. Scroll left and right to browse.
- **Recently Watched** -- Channels you have watched in the current session. Only appears after you start watching.
- **Popular Categories** -- A horizontal row of category cards showing the category name and how many channels are in it. Select a category to filter channels.
- **Category Rows** -- Below the categories, channels are organized into rows by category (News, Sports, Movies, Entertainment, etc.). Each row shows up to 10 channels with a "See All" link to view the full category.

### Side Navigation

A navigation bar runs along the left side of the screen. Use the D-pad to move focus to the sidebar, then navigate between sections:

| Item | What it does |
|------|-------------|
| Home | Returns to the Home screen with featured channels and category rows |
| Search | Opens the search screen to find channels by name |
| Channels | Shows all channels in a browsable grid |
| Categories | Shows all categories as a grid of cards |
| Favorites | Shows your saved favorite channels |
| Settings | Opens app settings (at the bottom of the sidebar) |

The sidebar collapses to icons when you move focus away from it, giving more room for content.

### Channels Screen

Select **Channels** from the sidebar to see all channels in a grid.

- **Filter by category** -- A row of category chips appears at the top. Select a category to filter. Select **All** to show everything.
- **Channel cards** -- Each card shows the channel logo, name, and optionally a health status indicator (green for online, red for offline).
- **Current program** -- If EPG data is available, the currently airing program title is shown on each channel card.

### Categories Screen

Select **Categories** from the sidebar to see all channel categories.

- Each category card shows an image, the category name, and how many channels are in it.
- Select a category card to see all channels in that category.

---

## Favorites

You can save channels as favorites for quick access.

### Adding a Favorite

- On any channel card (Home, Channels, or Search results), press the **Menu** button on your remote to toggle the favorite status.
- In the player, a heart button appears in the bottom-left corner. Select it to add or remove the current channel from favorites.
- A filled heart means the channel is a favorite. An outlined heart means it is not.

### Viewing Favorites

Select **Favorites** from the sidebar to see all your saved channels in one place.

### Removing a Favorite

- On the Favorites screen, select the heart icon on any channel card to remove it.
- In the player, select the heart button to unfavorite the current channel.

### Sync with Server

If your device is paired, favorites sync with the server. Changes you make on one device will appear on other devices paired to the same account.

---

## Search

Select **Search** from the sidebar to find channels by name.

1. A search box appears at the top of the screen.
2. Press the center button on your remote to activate the search field and bring up the on-screen keyboard.
3. Type your search term. Results update as you type.
4. Results appear in a grid below the search box. The number of results is shown (e.g., "5 results for 'news'").
5. Select a channel from the results to start playing it.

### Search History

- Previous searches appear as chips below the search box when it is empty.
- Select a chip to re-run that search.
- Select **Clear** to erase your search history.

---

## Watching a Channel

Select any channel card to start watching. The player opens in full screen.

### Playback Controls

| Remote Button | Action |
|---------------|--------|
| Play/Pause | Pause or resume playback |
| Center button (D-pad) | Pause or resume playback; briefly shows the favorite button |
| Menu | Show or hide the channel overlay |
| Left / Channel Down | Switch to the previous channel |
| Right / Channel Up | Switch to the next channel |
| Back | If the overlay is open, closes it. If closed, exits the player and returns to the previous screen. |

### Channel Overlay

Press **Menu** on your remote to bring up the channel overlay at the bottom of the screen.

The overlay shows:

- **Now playing info** -- The current channel name, the current program title, and the next program (if EPG data is available).
- **Category chips** -- Filter the channel list by category.
- **Channel grid** -- Browse and switch to other channels without leaving the player. The currently playing channel is highlighted.

Press **Menu** or **Back** to dismiss the overlay.

### Stream Errors and Recovery

If a stream fails to load or drops:

1. The app automatically tries to reconnect. You will see a "Reconnecting..." message with the attempt count.
2. If the primary stream URL fails, the app tries alternate streams if available.
3. If all attempts fail, a "Stream Offline" message appears with a 5-second countdown. Press any button to stay on the screen, or wait for the app to navigate back automatically.

### Resuming Playback

The app saves your playback position periodically. When you return to a channel, playback resumes from where you left off.

---

## Settings Reference

Open **Settings** from the sidebar to configure the app.

### Device Setup (when not paired)

| Setting | Description |
|---------|-------------|
| Server URL | The address of your FireVision IPTV server. Enter a URL like `https://tv.cadnative.com` or a local address like `http://192.168.1.100:3000`. |
| TV Pairing Code | Enter a pairing code manually if you already have one from the server dashboard. |
| Save Settings | Saves the server URL and pairing code. |
| Pair with PIN | Starts the PIN-based pairing flow to connect this device to your account. |

A QR code is also displayed for quick pairing from a phone.

### Device Setup (when paired)

| Setting | Description |
|---------|-------------|
| Paired status | Shows a green indicator and your TV code. |
| Reset Pairing | Removes the pairing from this device. You will need to pair again to access your channels and favorites. |

### Stream Health

| Setting | Description |
|---------|-------------|
| Check Liveliness | Scans all channels to check which streams are online or offline. Progress is shown during the scan. After scanning, channel cards display a green (online) or red (offline) status indicator. |

### About

| Setting | Description |
|---------|-------------|
| App Version | Shows the currently installed version. |
| Check for Updates | Checks the server for a newer version of the app. If an update is available, shows the version number, release notes, and file size. Mandatory updates are flagged. Select **Update Now** to download and install. |

---

## Troubleshooting

### "No channels found" after pairing

- Make sure your server has channels configured. Log in to the server dashboard and verify channels are listed.
- Go to Settings and confirm the server URL is correct.
- Try closing and reopening the app to trigger a fresh sync.

### Channels not loading or "Connection error"

- Check that your device is connected to the internet or local network.
- Verify the server URL in Settings. If the server is on your local network, make sure the device is on the same network.
- If the server uses HTTPS, make sure the certificate is valid.

### Stream buffering or not loading

- Use **Check Liveliness** in Settings to identify offline channels. Channels marked red are not currently streaming.
- Try switching to a different channel and then back.
- Check your internet connection speed. Live streams typically require at least 5 Mbps.
- If a channel has alternate streams, the app tries them automatically. Wait for the reconnection attempts to complete.

### PIN expired during pairing

- Select **Generate New PIN** on the pairing screen to get a fresh code.
- Complete the pairing within 10 minutes of generating the PIN.

### Pairing stuck on "Waiting for confirmation"

- Make sure you completed the pairing on the server dashboard (entered the PIN or scanned the QR code and confirmed).
- Check that the TV and your phone/computer can both reach the server.
- Try generating a new PIN and pairing again.

### App crashes on specific channels

- Some streams may use formats that are not fully supported. Try a different channel.
- If the problem persists, check the server for updated stream URLs.
- Make sure your app is on the latest version (Settings > Check for Updates).

### Remote control buttons not working as expected

- FireVision IPTV is designed for D-pad navigation. All actions are reachable using the directional buttons, center/select button, back button, and menu button.
- On some devices, the Menu button may be labeled differently (hamburger icon, three lines, or options).
- If you have a voice remote, the microphone button does not control the app. Use the standard navigation buttons.

### How to report a bug

If you encounter a problem that is not covered here:

1. Note the channel you were watching (if applicable) and what you were doing when the issue occurred.
2. Check the app version in Settings > About.
3. Report the issue to your server administrator or through the FireVision support channels.
