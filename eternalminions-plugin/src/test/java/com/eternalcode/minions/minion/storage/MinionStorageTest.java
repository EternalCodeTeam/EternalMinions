package com.eternalcode.minions.minion.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

class MinionStorageTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void shouldPlaceItemInFirstEmptySlotWhenNothingSimilarIsStored() {
        MinionStorage storage = new MinionStorage(3);

        MinionStorageUpdate update = storage.add(new ItemStack(Material.COBBLESTONE, 10));

        assertThat(update.remaining()).isNull();
        assertThat(update.storage().item(0)).isEqualTo(new ItemStack(Material.COBBLESTONE, 10));
        assertThat(update.storage().item(1)).isNull();
    }

    @Test
    void shouldTopOffAnExistingSimilarStackBeforeUsingAnEmptySlot() {
        MinionStorage storage = new MinionStorage(3)
                .withItem(1, new ItemStack(Material.COBBLESTONE, 10));

        MinionStorageUpdate update = storage.add(new ItemStack(Material.COBBLESTONE, 5));

        assertThat(update.storage().item(1)).isEqualTo(new ItemStack(Material.COBBLESTONE, 15));
        assertThat(update.storage().item(0))
                .as("the earlier empty slot must stay untouched while an existing stack has room")
                .isNull();
    }

    @Test
    void shouldSpillOverIntoANewSlotWhenExistingStackHasNoRoomLeft() {
        int maxStack = new ItemStack(Material.COBBLESTONE).getMaxStackSize();
        MinionStorage storage = new MinionStorage(3)
                .withItem(0, new ItemStack(Material.COBBLESTONE, maxStack));

        MinionStorageUpdate update = storage.add(new ItemStack(Material.COBBLESTONE, 5));

        assertThat(update.storage().item(0).getAmount()).isEqualTo(maxStack);
        assertThat(update.storage().item(1)).isEqualTo(new ItemStack(Material.COBBLESTONE, 5));
        assertThat(update.remaining()).isNull();
    }

    @Test
    void shouldSplitASingleInputAcrossSlotsWhenItExceedsMaxStackSize() {
        int maxStack = new ItemStack(Material.COBBLESTONE).getMaxStackSize();
        MinionStorage storage = new MinionStorage(3);

        MinionStorageUpdate update = storage.add(new ItemStack(Material.COBBLESTONE, maxStack + 10));

        assertThat(update.storage().item(0).getAmount()).isEqualTo(maxStack);
        assertThat(update.storage().item(1).getAmount()).isEqualTo(10);
        assertThat(update.remaining()).isNull();
    }

    @Test
    void shouldReturnLeftoverRemainingWhenStorageHasNoSpaceLeft() {
        int maxStack = new ItemStack(Material.COBBLESTONE).getMaxStackSize();
        MinionStorage storage = new MinionStorage(1)
                .withItem(0, new ItemStack(Material.COBBLESTONE, maxStack));

        MinionStorageUpdate update = storage.add(new ItemStack(Material.COBBLESTONE, 5));

        assertThat(update.remaining()).isEqualTo(new ItemStack(Material.COBBLESTONE, 5));
        assertThat(update.storage().item(0).getAmount())
                .as("a fully-full storage must not silently drop or duplicate the input")
                .isEqualTo(maxStack);
    }

    @Test
    void shouldNotStackDissimilarItemsTogether() {
        MinionStorage storage = new MinionStorage(2)
                .withItem(0, new ItemStack(Material.COBBLESTONE, 10));

        MinionStorageUpdate update = storage.add(new ItemStack(Material.DIRT, 10));

        assertThat(update.storage().item(0)).isEqualTo(new ItemStack(Material.COBBLESTONE, 10));
        assertThat(update.storage().item(1)).isEqualTo(new ItemStack(Material.DIRT, 10));
        assertThat(update.remaining()).isNull();
    }

    @Test
    void shouldGrowCapacityWhilePreservingExistingItems() {
        MinionStorage storage = new MinionStorage(2).withItem(0, new ItemStack(Material.DIAMOND, 1));

        MinionStorage resized = storage.resized(5);

        assertThat(resized.capacity()).isEqualTo(5);
        assertThat(resized.item(0)).isEqualTo(new ItemStack(Material.DIAMOND, 1));
    }

    @Test
    void shouldRejectShrinkingBelowTheCurrentSlotCount() {
        MinionStorage storage = new MinionStorage(5);

        assertThatIllegalArgumentException().isThrownBy(() -> storage.resized(2));
    }

    @Test
    void shouldRejectOutOfBoundsSlotAccess() {
        MinionStorage storage = new MinionStorage(3);

        assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> storage.item(3));
        assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> storage.item(-1));
    }

    @Test
    void shouldRejectNonPositiveInitialCapacity() {
        assertThatIllegalArgumentException().isThrownBy(() -> new MinionStorage(0));
    }
}
