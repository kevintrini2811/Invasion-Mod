package com.invasion.nexus.spawns;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.invasion.compat.ConfiguredModMobs;
import com.invasion.entity.EntityIMZombie;
import com.invasion.entity.IMCreeperEntity;
import com.invasion.entity.IMFatZombieEntity;
import com.invasion.entity.IMSkeletonEntity;
import com.invasion.entity.IMZombifiedPiglinEntity;
import com.invasion.nexus.NexusAccess;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class WaveArmorTest {
    private final NexusAccess nexus = mock(NexusAccess.class);
    private final RandomSource random = mock(RandomSource.class);
    private final Map<EquipmentSlot, ItemStack> equipment = new EnumMap<>(EquipmentSlot.class);
    private final Map<ItemStack, EquipmentSlot> slots = new IdentityHashMap<>();
    private MockedStatic<ConfiguredModMobs> configured;
    private IMWaveSpawner spawner;

    @BeforeEach
    void setUp() throws ReflectiveOperationException {
        configured = mockStatic(ConfiguredModMobs.class);
        configured.when(() -> ConfiguredModMobs.allowsArmor(any(), anyBoolean()))
                .thenAnswer(invocation -> invocation.getArgument(1));
        when(nexus.getCurrentWave()).thenReturn(25);
        spawner = mock(IMWaveSpawner.class, CALLS_REAL_METHODS);
        doReturn(random).when(spawner).getRandom();
        List<Item> armor = new ArrayList<>();
        for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
            // Two choices per slot prove that equipping removes every same-slot candidate.
            for (int i = 0; i < 2; i++) {
                Item item = mock(Item.class);
                ItemStack stack = mock(ItemStack.class);
                when(item.getDefaultInstance()).thenReturn(stack);
                slots.put(stack, slot);
                armor.add(item);
            }
        }
        setField("nexus", nexus);
        setField("randomWaveArmor", armor);
    }

    private void setField(String name, Object value) throws ReflectiveOperationException {
        var field = IMWaveSpawner.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(spawner, value);
    }

    @AfterEach
    void tearDown() {
        configured.close();
    }

    private <T extends Mob> T mob(Class<T> type) {
        T mob = mock(type);
        when(mob.getEquipmentSlotForItem(any())).thenAnswer(invocation ->
                slots.get(invocation.<ItemStack>getArgument(0)));
        when(mob.getItemBySlot(any())).thenAnswer(invocation ->
                equipment.getOrDefault(invocation.getArgument(0), ItemStack.EMPTY));
        doAnswer(invocation -> {
            EquipmentSlot slot = invocation.getArgument(0);
            assertFalse(equipment.containsKey(slot), "Existing armor must not be replaced");
            equipment.put(slot, invocation.getArgument(1));
            return null;
        }).when(mob).setItemSlot(any(), any());
        return mob;
    }

    @Test
    void firstFailureAddsNothing() {
        when(random.nextInt(100)).thenReturn(25);
        spawner.equipRandomWaveArmor(mob(IMSkeletonEntity.class));
        assertTrue(equipment.isEmpty());
        verify(random).nextInt(100);
    }

    @Test
    void successesFillDifferentSlotsUntilFirstFailure() {
        when(random.nextInt(100)).thenReturn(24, 0, 25);
        spawner.equipRandomWaveArmor(mob(IMSkeletonEntity.class));
        assertEquals(2, equipment.size());
        verify(random, times(3)).nextInt(100);
    }

    @Test
    void fullChanceFillsRemainingSlotsWithoutReplacingEquipment() {
        when(nexus.getCurrentWave()).thenReturn(150);
        when(random.nextInt(100)).thenReturn(99);
        ItemStack helmet = mock(ItemStack.class);
        equipment.put(EquipmentSlot.HEAD, helmet);
        Mob mob = mob(IMSkeletonEntity.class);
        spawner.equipRandomWaveArmor(mob);
        assertEquals(4, equipment.size());
        assertSame(helmet, equipment.get(EquipmentSlot.HEAD));
        verify(random, times(3)).nextInt(100);
        clearInvocations(random);
        spawner.equipRandomWaveArmor(mob);
        verifyNoInteractions(random);
    }

    @Test
    void helmetOnlyMobStopsAfterOneSuccess() {
        when(nexus.getCurrentWave()).thenReturn(100);
        spawner.equipRandomWaveArmor(mob(IMCreeperEntity.class));
        assertEquals(java.util.Set.of(EquipmentSlot.HEAD), equipment.keySet());
        verify(random).nextInt(100);
    }

    @Test
    void bruteCannotReceiveChestArmor() {
        when(nexus.getCurrentWave()).thenReturn(100);
        EntityIMZombie mob = mob(EntityIMZombie.class);
        when(mob.isBrute()).thenReturn(true);
        spawner.equipRandomWaveArmor(mob);
        assertEquals(java.util.Set.of(EquipmentSlot.HEAD, EquipmentSlot.LEGS, EquipmentSlot.FEET), equipment.keySet());
    }

    @Test
    void zombifiedPiglinCannotReceiveHelmet() {
        when(nexus.getCurrentWave()).thenReturn(100);
        spawner.equipRandomWaveArmor(mob(IMZombifiedPiglinEntity.class));
        assertEquals(java.util.Set.of(EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET), equipment.keySet());
    }

    @Test
    void zeroWaveAndIneligibleMobsDoNotRoll() {
        when(nexus.getCurrentWave()).thenReturn(0);
        spawner.equipRandomWaveArmor(mob(IMSkeletonEntity.class));
        when(nexus.getCurrentWave()).thenReturn(100);
        spawner.equipRandomWaveArmor(mob(IMFatZombieEntity.class));
        spawner.equipRandomWaveArmor(mob(Mob.class));
        assertTrue(equipment.isEmpty());
        verifyNoInteractions(random);
    }

    @Test
    void configuredArmorPermissionIsRespected() {
        configured.when(() -> ConfiguredModMobs.allowsArmor(any(), anyBoolean())).thenReturn(false);
        spawner.equipRandomWaveArmor(mob(IMSkeletonEntity.class));
        verifyNoInteractions(random);
        configured.when(() -> ConfiguredModMobs.allowsArmor(any(), anyBoolean())).thenReturn(true);
        when(nexus.getCurrentWave()).thenReturn(100);
        spawner.equipRandomWaveArmor(mob(Mob.class));
        assertEquals(4, equipment.size());
    }
}
