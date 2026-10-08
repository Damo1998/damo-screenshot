# Damo Screenshot

A small RuneLite plugin starter for building a user-triggered screenshot workflow.

## Current structure

- `DamoScreenshotPlugin` is the RuneLite plugin entry point and lifecycle hook.
- `DamoScreenshotConfig` is where plugin settings belong. Keep defaults safe and make capture user-triggered.

## A sensible next step

Implement a capture service that obtains a screenshot only after an explicit user action, then stores it locally through RuneLite's `Filepath` utility. Keep screenshot acquisition separate from storage so each piece is easy to understand and change. Add an overlay or hotkey only after you have chosen the capture trigger.

## Build

Run `./gradlew build` from the project root. For in-game validation, launch with `./gradlew run` and log in to the development client using RuneLite's [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts) instructions.


## Suggestions

1. If the user examined an NPC (Monster) which opened a window that shows its current drops/etc,
and you can configure what drops you want to highlight/screenshot, etc.

2. Dropdown for price minimum. Could be like xx yy where yy is the noun.
So you can have 10 M where m (million). or 5 k (thousand).
3. 