# Plan: Item Quantity + Bag Tracking Features

## Context

The Pack app is a native Android (Java) packing list manager with SQLite/ContentProvider data layer. It has a 3-tab packing flow (Should Pack / Pack / Repack) displayed in an ExpandableListView. The user wants two new features: per-trip item quantities, and a global bag tracking system with per-trip opt-in.

---

## Feature 1: Item Quantity (Trip-Specific)

### Database
- Add `quantity INTEGER NOT NULL DEFAULT 1` column to `trip_item` table
- Bump DB version to 2, replace destructive `onUpgrade` with `ALTER TABLE` migration

### Data Layer
- `TripItem.java`: add `quantity` field + getter/setter, update Parcelable
- `TripItemContentProvider.java`: add `"quantity"` to `validColumns`

### UI: Long-press -> Quantity Modal
- Add `setOnLongClickListener` on item rows in `PackingListExpandableAdapter.getChildView` (alongside existing click listener at line 123)
- Show an `AlertDialog` with:
  - Title: item name
  - `EditText` (inputType=number), pre-filled with current quantity
  - Horizontal row of up to 5 "quick add" buttons showing recent quantities (stored in SharedPreferences as comma-separated ints)
  - OK/Cancel buttons
- On confirm: update `trip_item.quantity` via ContentResolver, refresh adapter
- Display: when quantity > 1, show `"Item Name x5"` in the TextView (line 117 of adapter)

### Query Changes
- `TripDetailActivity.fillData` raw query (line 79): add `COALESCE(TI.quantity, 1) as quantity` to SELECT
- TripItem constructor call (line 99): pass quantity
- `showCopyTripDialog`: copy quantity when duplicating trip items

---

## Feature 2: Bag Tracking (Global Bags, Per-Trip Opt-In)

### Database (all in same v1->v2 migration)
- New `bag` table: `_id INTEGER PRIMARY KEY, name TEXT NOT NULL, color TEXT NOT NULL DEFAULT '#808080'`
- New `trip_bag` table: `_id, trip_id (FK trip), bag_id (FK bag)` with CASCADE deletes
- Add `bag_hint_id INTEGER REFERENCES bag(_id) ON DELETE SET NULL` to `item` table
- Add `bag_id INTEGER REFERENCES bag(_id) ON DELETE SET NULL` to `trip_item` table

### Data Layer
- New `Bag.java`: simple data class (id, name, color)
- New `BagContentProvider.java`: extends `AbstractSqliteContentProvider` for `bag` table
- New `TripBagContentProvider.java`: extends `AbstractSqliteContentProvider` for `trip_bag` table
- `ItemContentProvider.java`: add `"bag_hint_id"` to validColumns
- `TripItemContentProvider.java`: add `"bag_id"` to validColumns
- `TripItem.java`: add `bagId`, `bagName`, `bagColor`, `bagHintId` fields + Parcelable updates
- Register both new providers in `AndroidManifest.xml`

### Global Bag Management
- New `BagManagementActivity.java` with ListView of bags (name + color swatch per row)
- Add/edit via AlertDialog: EditText for name + grid of ~12 preset color buttons
- Delete with confirmation
- New "Bags" button on `activity_main.xml` (same pattern as Lists/Sets/Trips buttons)
- Add `openBagsActivity` method to `MainActivity.java`

### Item Bag Hint
- On `ListItemDetailActivity` (item edit screen): add a Spinner to optionally assign a bag hint
- Query all bags for the spinner, include a "None" option
- Save to `item.bag_hint_id`

### Trip Bag Selection
- New menu item in `menu_trip_detail.xml`: "Bags" icon shown `ifRoom` in toolbar
- Clicking opens an AlertDialog with multi-choice list of all global bags
- Pre-check bags already in `trip_bag` for this trip
- On confirm: sync `trip_bag` rows (insert new, delete removed)
- Edge case: if removing a bag that items are packed into, show confirmation and null out those `trip_item.bag_id` values

### Bag-Aware Pack Flow
- `PackingListExpandableAdapter` constructor: accept `List<Bag> tripBags` parameter
- Pass through: `TripDetailActivity` -> `TripDetailPagerAdapter` -> `AbstractPackingFragment` -> adapter
- Modified click handler (line 123-161):
  - When transitioning to STATUS_PACKED or STATUS_REPACKED **and** `tripBags` is non-empty:
    - Show bag picker dialog before completing the status change
    - Bag picker: single-choice list of trip's active bags, each with colored circle + name
    - Pre-select: use `tripItem.bagId` if non-zero (repack case), else `item.bag_hint_id` if that bag is active in the trip
    - On selection: set `trip_item.bag_id` alongside the status update
    - On cancel: abort the pack action (item stays at current status)
  - When `tripBags` is empty: existing behavior unchanged

### Bag Chip Display
- In `getChildView`: when item has a `bagId`, add a small colored oval View between the text and checkbox
  - 12dp circle, background color from `bagColor`, using `GradientDrawable`
  - GONE when no bag assigned
- Adjust `getChildAt` indices accordingly (text=0, bagChip=1, checkbox=2)

### Query Changes
- `TripDetailActivity.fillData`: LEFT JOIN `bag B ON B._id=TI.bag_id` to get bag name/color; also select `I.bag_hint_id`
- Separate query to load trip's active bags into `ArrayList<Bag>` for the adapter
- Pass `tripBags` and `bagHintId` through the data pipeline

---

## Implementation Order

**Phase 1 - Database & Data Layer**
1. `DatabaseHelper.java` - version 2 migration with all new tables/columns
2. `Bag.java` - new data class
3. `BagContentProvider.java` + `TripBagContentProvider.java` - new providers
4. Update `TripItemContentProvider`, `ItemContentProvider` valid columns
5. Update `TripItem.java` - add quantity, bag fields, Parcelable
6. Register providers in `AndroidManifest.xml`

**Phase 2 - Quantity Feature**
7. Quantity dialog helper (AlertDialog with quick-add buttons)
8. Long-press handler in `PackingListExpandableAdapter`
9. Quantity display in adapter (`"x5"` suffix)
10. Update `fillData` query and TripItem construction
11. Update `showCopyTripDialog` to preserve quantity

**Phase 3 - Bag Management**
12. `BagManagementActivity` + layout + color picker dialog
13. "Bags" button on MainActivity
14. Bag hint spinner on `ListItemDetailActivity`

**Phase 4 - Trip Bag Integration**
15. Trip bag selection dialog (from TripDetailActivity toolbar)
16. Bag picker dialog (shown during pack action)
17. Modify adapter click handler for bag-aware packing
18. Bag chip display in item rows
19. Update `fillData` query with bag JOINs
20. Wire trip bags through pager adapter and fragments

**Phase 5 - Edge Cases & Polish**
21. Handle bag removal from trip (null out packed bag_ids)
22. Copy trip: also copy `trip_bag` entries and `trip_item.bag_id`
23. Verify cloud backup/restore compatibility (schema travels with DB file; `onUpgrade` handles old->new)

---

## Key Files to Modify

| File | Changes |
|------|---------|
| `data/DatabaseHelper.java` | Version bump, new tables, migration |
| `data/TripItem.java` | Add quantity, bag fields, update Parcelable |
| `data/TripItemContentProvider.java` | Add quantity, bag_id to valid columns |
| `data/ItemContentProvider.java` | Add bag_hint_id to valid columns |
| `adapter/PackingListExpandableAdapter.java` | Long-press, quantity display, bag indicator, bag-aware click |
| `adapter/TripDetailPagerAdapter.java` | Pass tripBags through to fragments |
| `fragment/AbstractPackingFragment.java` | Receive and pass tripBags |
| `TripDetailActivity.java` | Load bags, modified query, menu handler, copy-trip changes |
| `MainActivity.java` | Add Manage Bags button |
| `AndroidManifest.xml` | Register new providers and activity |
| `res/menu/menu_trip_detail.xml` | Add bags menu item |
| `res/layout/activity_main.xml` | Add Manage Bags button |

## New Files

| File | Purpose |
|------|---------|
| `data/Bag.java` | Data class |
| `data/BagContentProvider.java` | Content provider for `bag` table |
| `data/TripBagContentProvider.java` | Content provider for `trip_bag` table |
| `BagManagementActivity.java` | Global bag CRUD activity |
| `res/layout/activity_bag_management.xml` | Bag management layout |
| `res/drawable/ic_bag.xml` | Bag icon vector drawable |

Note: `ic_bag.xml` was not created — the "Bags" menu item uses text only (`ifRoom` shows title). A vector drawable could be added later for a cleaner toolbar.

---

## Verification
1. ✅ Build and install on device/emulator
2. ✅ Test quantity: long-press item -> set quantity -> verify display, verify persistence across tab switches
3. ✅ Test bags: create bags globally -> assign to trip -> pack item -> verify bag picker appears -> verify chip shows
4. ✅ Test repack: verify bag pre-selects the pack-phase bag
5. ✅ Test bag hint: set hint on item -> pack in trip with that bag active -> verify pre-selection
6. ✅ Test edge cases: remove bag from trip, delete bag globally, copy trip with bags
7. ✅ Test migration: install old version, add data, upgrade to new version, verify data intact

---

## Implementation Notes & Follow-ups

### Ambiguities / Decisions Made
- **Bag menu in toolbar**: Used text-only "Bags" menu item (showAsAction=ifRoom) rather than an icon since no vector drawable was created. Could add `ic_bag.xml` later for a cleaner look.
  - Fine as is
- **Quantity on unpacked items**: Setting quantity on an item with status 0 auto-marks it as SHOULD_PACK (since trip_item row must exist to store quantity). This seemed like the right UX — if you're setting a quantity you intend to pack it.
  - Yep definitely this is great, lets you long-press for everything you want quantity for.
- **Bag chip**: Shows a 12dp colored circle between item name and checkbox. No bag name text to keep rows compact. Tooltip or long-press could show bag name in the future.
  - Added legend to solve for this 
- **Bag removal from trip**: When unchecking a bag in the trip bag selection dialog, all trip_item.bag_id values for that bag are nulled. Items stay packed, just lose their bag assignment. No confirmation dialog — done silently since user explicitly unchecked it.
  - Fine as is
- **Repack bag pre-selection**: Uses the already-assigned bag_id from the pack phase. Falls back to bag_hint_id if no bag was assigned.
  - Makes sense.
- **Should Pack tab**: Bag picker is NOT shown on Should Pack since it's just marking items to pack, not actually packing them. Bags only asked during Pack and Repack actions.
  - Makes sense.

### Potential Follow-ups
- Add a vector drawable icon for the Bags toolbar menu item
  - No need, but we added nice tiles to landing page
- Consider showing bag name on long-press of the bag chip
  - No need, added always visible legend
- The bag hint spinner on item edit screen loads bags synchronously — for very large bag lists this could be slow (unlikely in practice)
  - Seems fine
- No uniqueness constraint on bag names — user could create duplicate-named bags
  - Fixed
- The quantity quick-add buttons are global (SharedPreferences), not per-trip or per-list
  - Makes sense
- Consider adding a "clear all bag assignments" option when removing bags from a trip
  - No need
- The bag color picker uses a fixed grid of 12 colors — could be expanded or support custom hex input
  - No need, gave nice pastels
- There is no way to delete a bag
  - Fine for now