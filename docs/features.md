---
layout: page
title: Features
permalink: /features/
---

## Packing Lists

Packing lists are the building blocks of Pack. Each list represents a category of items -- "Toiletries", "Clothes", "Tech Gear", "Kids' Stuff" -- and contains individual items you might bring on a trip.

- **Create and manage lists** with a name and priority (higher priority lists appear first).
- **Add items** to each list with a name and optional priority.
- **Search across all lists** using the search icon in the action bar. Tapping a result navigates directly to that list and highlights the matching item.
- **Set a default bag** for an entire list. When you assign a default bag, all items without an existing bag hint are updated, and new items inherit the default automatically.
- **Suggest bags from recent trips** -- If you've packed this list before, Pack can look at your most recent trip and suggest bag assignments based on what you actually did last time.

---

## Bags

Bags represent your real, physical luggage. Each bag has a name and a color, so you can visually identify which bag an item belongs to throughout the packing process.

- **12 preset colors** to choose from when creating a bag.
- **Color chips** appear next to packed items, making it easy to see bag assignments at a glance.
- **Bag legend** on the Pack and Repack tabs shows all active bags. Tap a bag in the legend to see every item assigned to it, grouped by packing list.
- **Bag hints** -- Assign a default bag to individual items or entire lists. These hints are remembered and pre-selected the next time you pack.

---

## List Sets

A List Set bundles multiple packing lists together for a particular type of trip.

For example:
- **"Weekend Getaway"** = Clothes + Toiletries
- **"Beach Holiday"** = Clothes + Toiletries + Beach Gear + Kids' Stuff
- **"Business Trip"** = Clothes + Toiletries + Work Equipment

When you create a trip, you choose a List Set. All the items from the included lists become available for that trip. This way you define your lists once and mix-and-match them for different trip types.

---

## Trips

Trips are where the packing happens. Each trip is built from a List Set and provides a four-tab workflow.

### Tab 1: Should Pack

Select which items from your lists you actually want to bring on this specific trip. Items are grouped by their parent list (collapsible groups).

- **Tap** an item to select it for the trip.
- **Long-press** to set a quantity (e.g., 3x T-shirts). The dialog remembers your recent quantities for quick selection.

### Tab 2: Pack

Work through your selected items and check them off as you physically pack them.

- **Tap** an item to mark it as packed. If bags are enabled, a bag picker dialog appears so you can record which bag the item goes into.
- **Bag pre-selection** is smart: it uses the item's current assignment, then falls back to its bag hint, then to the last bag you selected.
- **Bag legend** at the top shows all trip bags. Tap one to see its contents.
- **Long-press** to change quantity or reassign an item to a different bag.

### Tab 3: Repack

For the journey home. Check off items as you repack them to make sure nothing gets left behind at your destination.

- **Tap** to mark an item as repacked.
- **Clear Repack** menu action resets all repacked items back to packed status, useful if you need to repack again (e.g., moving between hotels).

### Tab 4: Notes

A free-form area for trip-related information that doesn't fit into packing lists.

- **Notes** -- A text area for anything: hotel addresses, flight details, reminders. Toggle between read and edit mode.
- **To-do list** -- A checklist for tasks like "charge camera batteries" or "print boarding passes".
  - Tap to mark items as done (strikethrough and sorted to the bottom).
  - Long-press and drag to reorder.
  - Swipe left to delete.

### Trip Actions

- **Search** -- Find items within the trip. Results show the item name, quantity, category, bag (with color chip), and packing status.
- **Copy Trip** -- Duplicate a trip as a starting point for a new one. Optionally copy bag assignments and/or notes and to-dos.
- **Delete Trip** -- Remove a trip and all its data.
- **Smart tab selection** -- When you open a trip, Pack automatically selects the most relevant tab based on your current packing progress.

---

## Cloud Backup

Back up your entire Pack database to Google Drive so your lists and trip history survive device changes.

- **Backup Now** -- Uploads your database to Google Drive. Each backup is timestamped.
- **Restore from Backup** -- Lists all available backups with date and file size. Select one to restore.
- **Import Database File** -- Import a `.db` file from your device storage (validated before replacing local data).
- **Clear Database** -- Delete all local data (double confirmation required).
- **Sign Out** -- Sign out of Google Drive.

---

## Theme

Tap the Theme tile on the home screen to cycle between three modes:

- **Auto** -- Follows your device's system theme.
- **Light** -- Always light.
- **Dark** -- Always dark, with carefully tuned colors for readability.
