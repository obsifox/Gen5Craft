package com.gen5craft.core;

/** Phase 8: player inventory basics — 36 slots (9 hotbar + 27 main),
 *  vanilla stacking rules (max 64 per stack unless an item overrides it
 *  later), and a selected hotbar slot. Items are numeric registry IDs with
 *  counts; no data components yet, so two stacks merge iff the item ID
 *  matches. This is the inventory *model*: the HUD renders it, and server
 *  container synchronization fills it (container packet decoding is a
 *  later slice — contents are never faked). */
public final class Inventory {
    public static final int SLOTS = 36;
    public static final int HOTBAR = 9;
    public static final int MAX_STACK = 64;

    private final int[] items = new int[SLOTS];   // 0 = empty
    private final int[] counts = new int[SLOTS];
    private int selected;                          // hotbar index 0..8

    public int slotItem(int slot) { return items[slot]; }
    public int slotCount(int slot) { return counts[slot]; }
    public int selectedSlot() { return selected; }
    public void select(int hotbarIndex) {
        if (hotbarIndex < 0 || hotbarIndex >= HOTBAR) throw new IllegalArgumentException("hotbar index " + hotbarIndex);
        selected = hotbarIndex;
    }
    public int selectedItem() { return items[selected]; }

    public void setSlot(int slot, int itemId, int count) {
        if (slot < 0 || slot >= SLOTS) throw new IllegalArgumentException("slot " + slot);
        if (itemId <= 0 || count <= 0) { items[slot] = 0; counts[slot] = 0; return; }
        items[slot] = itemId;
        counts[slot] = Math.min(count, MAX_STACK);
    }

    /** Add up to count of an item; returns the leftover that did not fit. */
    public int add(int itemId, int count) {
        if (itemId <= 0 || count <= 0) return Math.max(0, count);
        int left = count;
        for (int i = 0; i < SLOTS && left > 0; i++) {
            if (items[i] == itemId && counts[i] < MAX_STACK) {
                int put = Math.min(MAX_STACK - counts[i], left);
                counts[i] += put; left -= put;
            }
        }
        for (int i = 0; i < SLOTS && left > 0; i++) {
            if (items[i] == 0) {
                int put = Math.min(MAX_STACK, left);
                items[i] = itemId; counts[i] = put; left -= put;
            }
        }
        return left;
    }

    /** Remove up to count of an item; returns how many were removed. */
    public int remove(int itemId, int count) {
        int removed = 0;
        for (int i = 0; i < SLOTS && removed < count; i++) {
            if (items[i] == itemId) {
                int take = Math.min(counts[i], count - removed);
                counts[i] -= take; removed += take;
                if (counts[i] == 0) items[i] = 0;
            }
        }
        return removed;
    }

    public int countOf(int itemId) {
        int c = 0;
        for (int i = 0; i < SLOTS; i++) if (items[i] == itemId) c += counts[i];
        return c;
    }
    public int usedSlots() {
        int c = 0;
        for (int i = 0; i < SLOTS; i++) if (items[i] != 0) c++;
        return c;
    }
    public String summary() {
        return "inventory used=" + usedSlots() + "/" + SLOTS + " selected slot=" + (selected + 1)
                + (selectedItem() > 0 ? " item=" + selectedItem() + " x" + counts[selected] : " (empty hand)");
    }
}
