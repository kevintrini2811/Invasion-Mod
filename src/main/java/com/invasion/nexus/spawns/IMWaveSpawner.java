package com.invasion.nexus.spawns;

import com.invasion.nexus.wave.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.critereon.MinMaxBounds.Ints;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.jetbrains.annotations.Nullable;

import com.invasion.InvasionMod;
import com.invasion.entity.EntityIMZombie;
import com.invasion.entity.EntityIMZombiePigman;
import com.invasion.entity.EquipmentUtil;
import com.invasion.entity.ImpEnitty;
import com.invasion.entity.IMBlazeEntity;
import com.invasion.entity.IMCreeperEntity;
import com.invasion.entity.IMEndermanEntity;
import com.invasion.entity.IMGhastEntity;
import com.invasion.entity.InvEntities;
import com.invasion.entity.IMSkeletonEntity;
import com.invasion.entity.IMWitherSkeletonEntity;
import com.invasion.entity.IMZombifiedPiglinEntity;
import com.invasion.entity.NexusSpiderEntity;
import com.invasion.entity.PigmanEngineerEntity;
import com.invasion.nexus.Combatant;
import com.invasion.nexus.EntityConstruct;
import com.invasion.nexus.NexusAccess;

public class IMWaveSpawner implements Spawner {
	private static final int MAX_SPAWN_TRIES = 20;
	public static final int MIN_SPAWN_RADIUS = 8;
	private static final int NORMAL_SPAWN_HEIGHT = 30;
	private static final int MIN_SPAWN_POINTS_TO_KEEP = 15;
	private static final int MIN_SPAWN_POINTS_TO_KEEP_BELOW_HEIGHT_CUTOFF = 20;
	private static final int HEIGHT_CUTOFF = 35;
	private static final float SPAWN_POINT_CULL_RATE = 0.3F;

	private SpawnPointContainer spawnPointContainer = new SpawnPointContainer();

	private final NexusAccess nexus;
	private final List<Item> randomMeleeWaveWeapons;
	private final List<Item> randomRangedWaveWeapons;
	private final List<Item> randomWaveWeapons;
	private final List<Item> randomWaveArmor;

	@Nullable
	private Wave currentWave;

	private boolean active;
	private boolean waveComplete;
	private boolean permitSpawns = true;
	private boolean debugMode;

	private int spawnRadius;
	private int successfulSpawns;
	private long elapsed;

	public IMWaveSpawner(NexusAccess nexus, int radius) {
		this.nexus = nexus;
		this.spawnRadius = radius;
		randomMeleeWaveWeapons = findRegisteredItems(
				EquipmentUtil::isMeleeWeapon);
		randomRangedWaveWeapons = findRegisteredItems(
				EquipmentUtil::isRangedWeapon);
		randomWaveWeapons = Stream.concat(
						randomMeleeWaveWeapons.stream(),
						randomRangedWaveWeapons.stream())
				.distinct()
				.toList();
		randomWaveArmor = findRegisteredItems(
				EquipmentUtil::isHumanoidArmor);
	}

	private static List<Item> findRegisteredItems(
			java.util.function.Predicate<net.minecraft.world.item.ItemStack>
					predicate) {
		return BuiltInRegistries.ITEM.stream()
				.filter(item -> predicate.test(item.getDefaultInstance()))
				.toList();
	}

    @Override
    public RandomSource getRandom() {
        return nexus.getWorld().getRandom();
    }

	public long getElapsedTime() {
		return elapsed;
	}

	public boolean setRadius(int radius) {
	    radius = Math.max(8, radius);
	    spawnRadius = radius;
	    return spawnRadius != radius;
	}

	public int getRadius() {
	    return spawnRadius;
	}

	public void beginNextWave(int waveNumber) throws WaveSpawnerException {
		beginNextWave(WaveBuilder.generateMainInvasionWave(waveNumber));
	}

	public void beginNextWave(Wave wave) throws WaveSpawnerException {
		if (!active) {
			generateSpawnPoints();
		} else if (debugMode) {
		    InvasionMod.log("Successful spawns of last wave: " + successfulSpawns);
		}

		wave.resetWave();
		waveComplete = false;
		active = true;
		currentWave = wave;
		elapsed = 0L;
		successfulSpawns = 0;

		if (debugMode) {
		    InvasionMod.log("Defined mobs this wave: " + getTotalDefinedMobsThisWave());
		}
	}

	public void spawn(int elapsedMillis) throws WaveSpawnerException {
		elapsed += elapsedMillis;
		if (waveComplete || !active) {
			return;
		}

		if (spawnPointContainer.getNumberOfSpawnPoints(SpawnType.HUMANOID) < 10) {
			generateSpawnPoints();
			if (spawnPointContainer.getNumberOfSpawnPoints(SpawnType.HUMANOID) < 10) {
				throw new WaveSpawnerException("Not enough spawn points for type " + SpawnType.HUMANOID);
			}
		}
		currentWave.doNextSpawns(elapsedMillis, this);
		if (currentWave.isComplete()) {
			waveComplete = true;
		}
	}

	public int resumeFromState(Wave wave) throws WaveSpawnerException {
		long savedElapsed = elapsed;
		stop();
		beginNextWave(wave);
		setPermitSpawns(false);
		int numberOfSpawns = 0;
		for (long i = 0; i < savedElapsed; i += 100L) {
			numberOfSpawns += currentWave.doNextSpawns(100, this);
		}
		setPermitSpawns(true);
		elapsed = savedElapsed;
		return numberOfSpawns;
	}

	public int resumeFromState(int waveNumber) throws WaveSpawnerException {
		long savedElapsed = elapsed;
		stop();
		beginNextWave(waveNumber);
		setPermitSpawns(false);
		int numberOfSpawns = 0;
		for (long i = 0; i < savedElapsed; i += 100L) {
		    numberOfSpawns += currentWave.doNextSpawns(100, this);
		}
		setPermitSpawns(true);
		elapsed = savedElapsed;
		return numberOfSpawns;
	}

    public void stop() {
        active = false;
    }


    public boolean isActive() {
		return active;
	}

	public boolean isReady() {
		return !active && nexus != null && nexus.getWorld() != null;
	}

	public boolean isWaveComplete() {
		return waveComplete;
	}

	public int getWaveDuration() {
		return currentWave.getWaveTotalTime();
	}

	public int getWaveRestTime() {
		return currentWave.getWaveBreakTime();
	}

	public int getSuccessfulSpawnsThisWave() {
		return successfulSpawns;
	}

	public int getTotalDefinedMobsThisWave() {
		return currentWave.getTotalMobAmount();
	}

	public void askForRespawn(Combatant<?> entity) {
		if (spawnPointContainer.getNumberOfSpawnPoints(SpawnType.HUMANOID) > 10) {
			SpawnPoint spawnPoint = spawnPointContainer.getRandomSpawnPoint(SpawnType.HUMANOID);
			if (spawnPoint != null) {
			    final byte statusAddDeathParticles = (byte)60;
			    spawnPoint.applyTo(entity.asEntity());
			    entity.resetHealth();
			    entity.asEntity().level().broadcastEntityEvent(entity.asEntity(), statusAddDeathParticles);
			}
		}
	}

	@Override
    public void sendSpawnAlert(String message, ChatFormatting color) {
		if (debugMode) {
		    InvasionMod.log(message);
		}
		nexus.getParticipants().sendMessage(color, message);
	}

	@Override
    public void noSpawnPointNotice() {
	}

	public void debugMode(boolean isOn) {
		debugMode = isOn;
	}

	@Override
    public int getNumberOfPointsInRange(Ints angle, SpawnType type) {
		return spawnPointContainer.getNumberOfSpawnPoints(type, angle);
	}

	public void setPermitSpawns(boolean flag) {
		permitSpawns = flag;
	}

	public void giveSpawnPoints(SpawnPointContainer spawnPointContainer) {
		this.spawnPointContainer = spawnPointContainer;
	}

	@Override
	public boolean attemptSpawn(EntityConstruct mobConstruct, Ints angle) {
		if ((mobConstruct.rules() & BudgetWavePlan.RULE_PLANNED) == 0) {
			mobConstruct = replaceWithRareWaveVariant(mobConstruct);
			mobConstruct = replaceEngineerWithZombieBuilder(mobConstruct);
		}
		int spawnTries = Math.min(spawnPointContainer.getNumberOfSpawnPoints(SpawnType.HUMANOID, angle), MAX_SPAWN_TRIES);

		for (SpawnPoint spawnPoint : spawnPointContainer.getRandomSpawnPoints(
				SpawnType.HUMANOID, angle, spawnTries)) {
			if (!permitSpawns) {
				successfulSpawns++;
				if (debugMode) {
				    InvasionMod.LOGGER.debug("[Spawn] Time: " + currentWave.getTimeInWave() / 1000 + "  Type: " + mobConstruct.entityType() + "  Coords: " + spawnPoint + "  Specified: " + angle);
				}

				return true;
			}

			EntityConstruct spawnConstruct = (mobConstruct.rules() & BudgetWavePlan.RULE_PLANNED) != 0 ? mobConstruct :
					replaceSlimeWithMagmaCube(
						replaceSkeletonWithEnvironmentalVariant(
							replaceZombieWithEnvironmentalVariant(
									mobConstruct,
									(ServerLevel) nexus.getWorld(),
									spawnPoint.pos()),
							(ServerLevel) nexus.getWorld(),
							spawnPoint.pos()));
			Mob mob = spawnConstruct.createMob(nexus);
			equipRandomWaveWeapon(mob, spawnConstruct);
			equipRandomWaveArmor(mob, spawnConstruct);

            if (spawnPoint.trySpawnEntity(
                    (ServerLevel) nexus.getWorld(), mob)) {
                successfulSpawns++;
				mob.getPersistentData().putBoolean("invmodWaveMob", true);

                equipWitherSkeletonWeapon(mob);
                applyBabyVariant(mob, spawnConstruct);
                markAsInvasionAlly(mob);
                if (debugMode) {
                    InvasionMod.LOGGER.debug("[Spawn] Time: " + currentWave.getTimeInWave()
                            + "  Mob: " + mob.getName().getString()
                            + "  Coords: " + mob.getX() + ", " + mob.getY() + ", " + mob.getZ()
                            + "  θ" + spawnPoint.getAngle() + "  Specified: " + angle);
                }

                return true;
			}
		}
		return false;
	}

	private EntityConstruct replaceEngineerWithZombieBuilder(
			EntityConstruct construct) {
		if (construct.entityType() != InvEntities.PIGMAN_ENGINEER) {
			return construct;
		}
		int chancePercent = nexus.getZombieBuilderChancePercent();
		if (getRandom().nextInt(100) >= chancePercent) {
			return construct;
		}
		return new EntityConstruct(
				InvEntities.ZOMBIE_BUILDER,
				construct.texture(),
				construct.tier(),
				construct.flavour(),
				construct.scaling(),
				construct.minAngle(),
				construct.maxAngle());
	}

	private EntityConstruct replaceSlimeWithMagmaCube(
			EntityConstruct construct) {
		if (construct.entityType() != InvEntities.SLIME
				|| nexus.getCurrentWave() < 8
				|| getRandom().nextInt(5) != 0) {
			return construct;
		}
		return new EntityConstruct(
				InvEntities.MAGMA_CUBE,
				construct.texture(),
				construct.tier(),
				construct.flavour(),
				construct.scaling(),
				construct.minAngle(),
				construct.maxAngle());
	}

	private EntityConstruct replaceZombieWithEnvironmentalVariant(
			EntityConstruct construct, ServerLevel world, BlockPos pos) {
		if (construct.entityType() != InvEntities.ZOMBIE
				|| construct.tier() == 3
				|| construct.tier() == 2
						&& (construct.flavour() == 2
								|| construct.flavour() == 3)) {
			return construct;
		}

		List<EntityType<? extends Mob>> relevantVariants =
				new ArrayList<>(3);
		var biome = world.getBiome(pos);
		if (construct.tier() == 1
				&& (biome.is(Biomes.SWAMP)
						|| biome.is(Biomes.MANGROVE_SWAMP))
				&& getRandom().nextInt(4) == 0) {
			return new EntityConstruct(
					InvEntities.SLIME,
					construct.texture(),
					construct.tier(),
					construct.flavour(),
					construct.scaling(),
					construct.minAngle(),
					construct.maxAngle());
		}
		if (biome.is(BiomeTags.IS_OCEAN)
				|| biome.is(BiomeTags.IS_RIVER)) {
			relevantVariants.add(InvEntities.DROWNED);
		}
		if (biome.is(BiomeTags.HAS_DESERT_PYRAMID)) {
			relevantVariants.add(InvEntities.HUSK);
		}
		if (world.structureManager()
				.getStructureWithPieceAt(pos, StructureTags.VILLAGE)
				.isValid()) {
			relevantVariants.add(InvEntities.ZOMBIE_VILLAGER);
		}

		int chance = relevantVariants.isEmpty() ? 3 : 75;
		if (getRandom().nextInt(100) >= chance) {
			return construct;
		}

		EntityType<? extends Mob> replacement;
		if (relevantVariants.isEmpty()) {
			replacement = switch (getRandom().nextInt(3)) {
				case 0 -> InvEntities.ZOMBIE_VILLAGER;
				case 1 -> InvEntities.DROWNED;
				default -> InvEntities.HUSK;
			};
		} else {
			replacement = relevantVariants.get(
					getRandom().nextInt(relevantVariants.size()));
		}

		return new EntityConstruct(
				replacement,
				construct.texture(),
				construct.tier(),
				construct.flavour(),
				construct.scaling(),
				construct.minAngle(),
				construct.maxAngle());
	}

	private EntityConstruct replaceSkeletonWithEnvironmentalVariant(
			EntityConstruct construct, ServerLevel world, BlockPos pos) {
		if (construct.entityType() != InvEntities.SKELETON) {
			return construct;
		}

		List<EntityType<? extends Mob>> relevantVariants =
				new ArrayList<>(3);
		var biome = world.getBiome(pos);
		if (biome.is(Biomes.SWAMP)
				|| biome.is(Biomes.MANGROVE_SWAMP)) {
			relevantVariants.add(InvEntities.BOGGED);
		}
		if (biome.is(Biomes.SNOWY_PLAINS)
				|| biome.is(Biomes.ICE_SPIKES)) {
			relevantVariants.add(InvEntities.STRAY);
		}

		int chance = relevantVariants.isEmpty() ? 1 : 75;
		if (getRandom().nextInt(100) >= chance) {
			return construct;
		}

		EntityType<? extends Mob> replacement;
		if (relevantVariants.isEmpty()) {
			replacement = switch (getRandom().nextInt(2)) {
				case 0 -> InvEntities.BOGGED;
				default -> InvEntities.STRAY;
			};
		} else {
			replacement = relevantVariants.get(
					getRandom().nextInt(relevantVariants.size()));
		}

		return new EntityConstruct(
				replacement,
				construct.texture(),
				construct.tier(),
				construct.flavour(),
				construct.scaling(),
				construct.minAngle(),
				construct.maxAngle());
	}

	private EntityConstruct replaceWithRareWaveVariant(
			EntityConstruct construct) {
		if (construct.entityType() == InvEntities.WITHER
				&& getRandom().nextBoolean()) {
			return new EntityConstruct(
					InvEntities.WARDEN,
					construct.texture(),
					construct.tier(),
					construct.flavour(),
					construct.scaling(),
					construct.minAngle(),
					construct.maxAngle());
		}

		if (construct.entityType() == InvEntities.ZOMBIE
				&& getRandom().nextInt(100) == 0) {
			return new EntityConstruct(
					InvEntities.SPEEDY_ZOMBIE,
					construct.texture(),
					construct.tier(),
					construct.flavour(),
					construct.scaling(),
					construct.minAngle(),
					construct.maxAngle());
		}

		if (construct.entityType() == InvEntities.ZOMBIE_PIGMAN
				&& construct.tier() != 3
				&& getRandom().nextBoolean()) {
			return new EntityConstruct(
					InvEntities.ZOMBIFIED_PIGLIN,
					construct.texture(),
					construct.tier(),
					construct.flavour(),
					construct.scaling(),
					construct.minAngle(),
					construct.maxAngle());
		}

		int witherSkeletonChance = nexus.getWitherSkeletonChancePercent();
		if (witherSkeletonChance > 0
				&& construct.entityType() == InvEntities.SKELETON
				&& getRandom().nextInt(100) < witherSkeletonChance) {
			return new EntityConstruct(
					InvEntities.WITHER_SKELETON,
					construct.texture(),
					construct.tier(),
					construct.flavour(),
					construct.scaling(),
					construct.minAngle(),
					construct.maxAngle());
		}

		int chargedChance = nexus.getChargedCreeperChancePercent();
		if (chargedChance > 0
				&& construct.entityType() == InvEntities.CREEPER
				&& construct.tier() == 1
				&& IMCreeperEntity.rollChargedVariant(
						getRandom(), chargedChance)) {
			return new EntityConstruct(
					construct.entityType(),
					construct.texture(),
					2,
					construct.flavour(),
					construct.scaling(),
					construct.minAngle(),
					construct.maxAngle());
		}
		return construct;
	}

	private void equipWitherSkeletonWeapon(Mob mob) {
		if (!(mob instanceof IMWitherSkeletonEntity)) {
			return;
		}
		List<Item> weaponPool = getRandom().nextBoolean()
				? randomMeleeWaveWeapons
				: randomRangedWaveWeapons;
		if (weaponPool.isEmpty()) {
			weaponPool = randomWaveWeapons;
		}
		if (weaponPool.isEmpty()) {
			return;
		}
		Item weapon = weaponPool.get(getRandom().nextInt(weaponPool.size()));
		mob.setItemSlot(EquipmentSlot.MAINHAND, weapon.getDefaultInstance());
	}

	private void applyBabyVariant(Mob mob, EntityConstruct construct) {
		boolean planned = (construct.rules() & BudgetWavePlan.RULE_PLANNED) != 0;
		boolean baby = (construct.rules() & BudgetWavePlan.RULE_BABY) != 0
				|| !planned && getRandom().nextInt(100) < nexus.getBabyZombieChancePercent();
		if (!baby) return;
		if (mob instanceof EntityIMZombie zombie && !zombie.isBrute()) zombie.setBaby(true);
		if (mob instanceof IMSkeletonEntity skeleton
				&& com.invasion.compat.TinySkeletonsCompatibility.isLoaded()) skeleton.setBaby(true);
	}

	private void equipRandomWaveWeapon(Mob mob, EntityConstruct construct) {
		if (!(mob instanceof EntityIMZombie
				|| mob instanceof EntityIMZombiePigman
				|| mob instanceof IMZombifiedPiglinEntity
				|| mob instanceof ImpEnitty)
				|| !mob.getMainHandItem().isEmpty()) {
			return;
		}

		boolean planned = (construct.rules() & BudgetWavePlan.RULE_PLANNED) != 0;
		int chancePercent = (construct.rules() & (BudgetWavePlan.RULE_RANGED | BudgetWavePlan.RULE_WEAPON)) != 0
				? 100 : planned ? 0 : nexus.getRandomEquipmentChancePercent();
		if (getRandom().nextInt(100) >= chancePercent) {
			return;
		}

		List<Item> weaponPool = (construct.rules() & BudgetWavePlan.RULE_RANGED) != 0
				? randomRangedWaveWeapons : randomWaveWeapons;
		if (weaponPool.isEmpty()) {
			return;
		}
		Item weapon = weaponPool.get(getRandom().nextInt(weaponPool.size()));
		mob.setItemSlot(
				net.minecraft.world.entity.EquipmentSlot.MAINHAND,
				weapon.getDefaultInstance());
	}

	private void equipRandomWaveArmor(Mob mob, EntityConstruct construct) {
		boolean canWearWaveArmor = mob instanceof IMSkeletonEntity
				|| mob instanceof PigmanEngineerEntity
				|| mob instanceof EntityIMZombie
				|| mob instanceof EntityIMZombiePigman
				|| mob instanceof IMZombifiedPiglinEntity
				|| mob instanceof IMCreeperEntity
				|| mob instanceof NexusSpiderEntity
				|| mob instanceof IMEndermanEntity
				|| mob instanceof IMBlazeEntity
				|| mob instanceof IMGhastEntity;
		if (!canWearWaveArmor) {
			return;
		}

		boolean planned = (construct.rules() & BudgetWavePlan.RULE_PLANNED) != 0;
		int chancePercent = (construct.rules() & (BudgetWavePlan.RULE_ARMORED | BudgetWavePlan.RULE_ARMOR)) != 0
				? 100 : planned ? 0 : nexus.getRandomEquipmentChancePercent();
		if (getRandom().nextInt(100) >= chancePercent) {
			return;
		}

		List<Item> availableArmor = new ArrayList<>();
		for (Item armor : randomWaveArmor) {
			EquipmentSlot slot = mob.getEquipmentSlotForItem(armor.getDefaultInstance());
			boolean helmetOnly = mob instanceof IMCreeperEntity
					|| mob instanceof NexusSpiderEntity
					|| mob instanceof IMEndermanEntity
					|| mob instanceof IMBlazeEntity
					|| mob instanceof IMGhastEntity;
			boolean bruteArmorSlot =
					mob instanceof EntityIMZombie zombie && zombie.isBrute()
					|| mob instanceof EntityIMZombiePigman pigman && pigman.isBrute();
			boolean disallowHelmet =
					mob instanceof IMZombifiedPiglinEntity
					&& slot == EquipmentSlot.HEAD;
			if (slot.isArmor()
					&& !disallowHelmet
					&& (!helmetOnly || slot == EquipmentSlot.HEAD)
					&& (!bruteArmorSlot
							|| slot == EquipmentSlot.HEAD
							|| slot == EquipmentSlot.LEGS
							|| slot == EquipmentSlot.FEET)
					&& mob.getItemBySlot(slot).isEmpty()) {
				availableArmor.add(armor);
			}
		}
		if (availableArmor.isEmpty()) {
			return;
		}

		if ((construct.rules() & BudgetWavePlan.RULE_ARMORED) != 0) {
			for (EquipmentSlot desired : EquipmentSlot.values()) {
				if (!desired.isArmor()) continue;
				List<Item> slotArmor = availableArmor.stream().filter(item -> mob.getEquipmentSlotForItem(item.getDefaultInstance()) == desired).toList();
				if (!slotArmor.isEmpty()) {
					Item armor = slotArmor.get(getRandom().nextInt(slotArmor.size()));
					mob.setItemSlot(desired, armor.getDefaultInstance());
				}
			}
		} else {
			Item armor = availableArmor.get(getRandom().nextInt(availableArmor.size()));
			EquipmentSlot slot = mob.getEquipmentSlotForItem(armor.getDefaultInstance());
			mob.setItemSlot(slot, armor.getDefaultInstance());
		}
	}

	private void generateSpawnPoints() {
		EntityIMZombie zombie = InvEntities.ZOMBIE.create(nexus.getWorld());
		zombie.setNexus(nexus);
		List<SpawnPoint> spawnPoints = new ArrayList<>();
		BlockPos origin = nexus.getOrigin();
		BlockPos.MutableBlockPos mutable = origin.mutable();

		for (int vertical = 0;
		         Math.abs(vertical) < spawnRadius && !nexus.getWorld().isOutsideBuildHeight(origin.getY() + vertical);
		         vertical = vertical > 0 ? vertical * -1 : vertical * -1 + 1) {
			for (int i = 0; i <= spawnRadius * 0.7D + 1; i++) {
				int j = (int) Math.round(spawnRadius * Math.cos(Math.asin(i / spawnRadius)));

				addValidSpawn(zombie, spawnPoints, mutable.set(origin).move( i, vertical, j));
				addValidSpawn(zombie, spawnPoints, mutable.set(origin).move( i, vertical,-j));
				addValidSpawn(zombie, spawnPoints, mutable.set(origin).move(-i, vertical, j));
				addValidSpawn(zombie, spawnPoints, mutable.set(origin).move(-i, vertical,-j));

                addValidSpawn(zombie, spawnPoints, mutable.set(origin).move( j, vertical, i));
                addValidSpawn(zombie, spawnPoints, mutable.set(origin).move( j, vertical,-i));
                addValidSpawn(zombie, spawnPoints, mutable.set(origin).move(-j, vertical, i));
                addValidSpawn(zombie, spawnPoints, mutable.set(origin).move(-j, vertical,-i));
			}
		}

		if (spawnPoints.size() > MIN_SPAWN_POINTS_TO_KEEP) {
			int i;
			int amountToRemove = (int) ((spawnPoints.size() - MIN_SPAWN_POINTS_TO_KEEP) * SPAWN_POINT_CULL_RATE);
			for (i = spawnPoints.size() - 1; i >= spawnPoints.size() - amountToRemove; i--) {
				if (Math.abs(spawnPoints.get(i).pos().getY() - origin.getY()) < NORMAL_SPAWN_HEIGHT) {
					break;
				}
			}
			for (; i >= MIN_SPAWN_POINTS_TO_KEEP_BELOW_HEIGHT_CUTOFF; i--) {
				SpawnPoint spawnPoint = spawnPoints.get(i);
				if (spawnPoint.pos().getY() - origin.getY() <= HEIGHT_CUTOFF) {
					spawnPointContainer.addSpawnPointXZ(spawnPoint);
				}

			}
			for (; i >= 0; i--) {
				spawnPointContainer.addSpawnPointXZ(spawnPoints.get(i));
			}

		}

		InvasionMod.LOGGER.debug("Found {} spawn points for next nexus wave", spawnPointContainer.getNumberOfSpawnPoints(SpawnType.HUMANOID));
	}

	private void addValidSpawn(Mob entity, List<SpawnPoint> spawnPoints, BlockPos pos) {
	    if (nexus.getWorld().isOutsideBuildHeight(pos)) {
	        InvasionMod.LOGGER.debug("[Spawn] Spawn point was outside of build limit {}", pos);
	        return;
	    }
		entity.moveTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0, 0);
		if (entity.checkSpawnObstruction(nexus.getWorld()) && nexus.getWorld().noCollision(entity)) {
			int angle = (int) (Math.atan2(nexus.getOrigin().getZ() - pos.getZ(), nexus.getOrigin().getX() - pos.getX()) * Mth.RAD_TO_DEG);
			spawnPoints.add(new SpawnPoint(pos.immutable(), angle, SpawnType.HUMANOID));
		}
	}



    public void readNbt(CompoundTag compound, HolderLookup.Provider lookup) {
        setRadius(compound.getInt("spawnRadius"));
        elapsed = compound.getLong("elapsed");
    }

    public CompoundTag writeNbt(CompoundTag compound, HolderLookup.Provider lookup) {
        compound.putInt("spawnRadius", spawnRadius);
        compound.putLong("elapsed", elapsed);
        return compound;
    }
    private void markAsInvasionAlly(Mob mob) {
        ServerLevel world = (ServerLevel) mob.level();
        Scoreboard scoreboard = world.getScoreboard();

        // Team holen oder erstellen
        PlayerTeam team = scoreboard.getPlayerTeam("invasion_allies");
        if (team == null) {
            team = scoreboard.addPlayerTeam("invasion_allies");
            team.setAllowFriendlyFire(false);              // kein Damage untereinander
            team.setCollisionRule(PlayerTeam.CollisionRule.NEVER); // optional: keine Kollision
        }

        // WICHTIG: eindeutigen ScoreHolder-Namen benutzen
        String holderName = mob.getScoreboardName();
        scoreboard.addPlayerToTeam(holderName, team);

        // existende Aggro resetten
        mob.setTarget(null);
        mob.setLastHurtByMob(null);
    }





}
