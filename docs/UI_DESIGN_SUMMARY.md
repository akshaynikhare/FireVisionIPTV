# FireVision IPTV - Netflix-Style Android TV App

## Design Brief
Android TV IPTV streaming app with Netflix-inspired dark UI. Pure black (#000000) background, unique blue (#2196F3) accents, white (#FFFFFF) text. Built with Android Leanback library for TV D-pad navigation.

## Core Layout
**Vertical Sidebar (89dp width)**: Netflix-style left navigation with 5 icons - Home, Search, Categories, Favorites, Settings. Dark background (#0A0A0A), focused items show lighter background (#1A1A1A).

**Main Content Area**: Horizontal scrolling channel grids organized by category. Channel cards display logo and name. Focus states use animated blue glow border.

## Key Screens
- **Home**: Browse channels by category
- **Pairing**: PIN display + QR code for account registration
- **Playback**: Full-screen video with dismissible bottom channel switcher overlay
- **Search**: Search input with results grid
- **Settings**: Server URL, pairing code, auto-load channel configuration

## Colors
- Background: #000000 (pure black)
- Sidebar: #0A0A0A
- Accent: #2196F3 (Unique blue)
- Text: #FFFFFF (white), #B3B3B3 (secondary)
- Focus: Blue glow (#602196F3, #802196F3, #FF2196F3 layered)

## Features
- PIN-based TV pairing system
- Channel favorites and search
- Auto-load channel on launch
- Node.js/MongoDB backend sync
- ExoPlayer for HLS/RTSP streaming
