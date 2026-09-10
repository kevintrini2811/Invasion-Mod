package com.invasion.nexus.wave;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;

class BudgetWavePlanTest {
    @Test
    void preservesPlanAndCurrentPhaseAcrossRoundTrip() {
        CompoundTag saved = planTag();

        BudgetWavePlan plan = BudgetWavePlan.load(saved, null).orElseThrow();
        CompoundTag roundTrip = plan.save(null);
        BudgetWavePlan reloaded = BudgetWavePlan.load(roundTrip, null).orElseThrow();

        assertEquals(12, reloaded.wave());
        assertEquals(2, reloaded.phaseCount());
        assertEquals(1, reloaded.phaseIndex());
        assertEquals(BudgetWavePlan.Theme.RANGED, reloaded.currentPhase().theme());
        assertEquals(3, reloaded.currentPhase().purchases().getFirst().tier());
        assertEquals(2, reloaded.currentPhase().purchases().getFirst().flavour());
        assertEquals(7, reloaded.currentPhase().purchases().getFirst().cost());
        assertEquals(BudgetWavePlan.RULE_RANGED, reloaded.currentPhase().purchases().getFirst().rules());
    }

    @Test
    void clampsInvalidPhaseAndRejectsPlanWithoutPhases() {
        CompoundTag saved = planTag();
        saved.putInt("phase", 20);

        BudgetWavePlan plan = BudgetWavePlan.load(saved, null).orElseThrow();

        assertEquals(1, plan.phaseIndex());
        assertFalse(plan.advance());
        assertTrue(BudgetWavePlan.load(new CompoundTag(), null).isEmpty());
    }

    private static CompoundTag planTag() {
        CompoundTag plan = new CompoundTag();
        plan.putInt("wave", 12);
        plan.putInt("phase", 1);
        ListTag phases = new ListTag();
        phases.add(phase("SWARM", 1, 0, 1, BudgetWavePlan.RULE_PLANNED));
        phases.add(phase("RANGED", 3, 2, 7, BudgetWavePlan.RULE_RANGED));
        plan.put("phases", phases);
        return plan;
    }

    private static CompoundTag phase(String theme, int tier, int flavour, int cost, int rules) {
        CompoundTag phase = new CompoundTag();
        phase.putString("theme", theme);
        CompoundTag purchase = new CompoundTag();
        purchase.putString("type", "minecraft:zombie");
        purchase.putInt("tier", tier);
        purchase.putInt("flavour", flavour);
        purchase.putInt("cost", cost);
        purchase.putInt("rules", rules);
        ListTag purchases = new ListTag();
        purchases.add(purchase);
        phase.put("purchases", purchases);
        return phase;
    }
}
