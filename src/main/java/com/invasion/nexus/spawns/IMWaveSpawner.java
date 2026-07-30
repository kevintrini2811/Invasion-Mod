package com.invasion.nexus.spawns;

import com.invasion.nexus.wave.*;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.predicates.MinMaxBounds.Ints;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.jetbrains.annotations.Nullable;

import com.invasion.InvasionMod;
import com.invasion.entity.EntityIMZombie;
import com.invasion.entity.EntityIMZombiePigman;
import com.invasion.entity.ImpEnitty;
import com.invasion.entity.IMCreeperEntity;
import com.invasion.entity.InvEntities;
import com.invasion.entity.IMSkeletonEntity;
import com.invasion.entity.IMWitherSkeletonEntity;
import com.invasion.entity.PigmanEngineerEntity;
import com.invasion.item.InvItems;
import com.invasion.nexus.Combatant;
import com.invasion.nexus.EntityConstruct;
import com.invasion.nexus.NexusAccess;

public class IMWaveSpawner implements Spawner {
	private static final int MAX_SPAWN_TRIES = 20;
	private static final List<Item> RANDOM_WAVE_WEAPONS = List.of(
			Items.WOODEN_SWORD,
			Items.STONE_SWORD,
			Items.IRON_SWORD,
			Items.GOLDEN_SWORD,
			Items.DIAMOND_SWORD,
			Items.NETHERITE_SWORD,
			Items.WOODEN_AXE,
			Items.STONE_AXE,
			Items.IRON_AXE,
			Items.GOLDEN_AXE,
			Items.DIAMOND_AXE,
			Items.NETHERITE_AXE,
			Items.TRIDENT,
			Items.MACE,
			Items.BOW,
			Items.CROSSBOW,
			InvItems.INFUSED_SWORD,
			InvItems.SEARING_BOW);
	private static final List<Item> RANDOM_RANGED_WAVE_WEAPONS = List.of(
			Items.BOW,
			Items.CROSSBOW,
			InvItems.SEARING_BOW);
	private static final List<Item> RANDOM_MELEE_WAVE_WEAPONS =
			RANDOM_WAVE_WEAPONS.stream()
					.filter(item -> !RANDOM_RANGED_WAVE_WEAPONS.contains(item))
					.toList();
	private static final List<Item> RANDOM_WAVE_ARMOR = List.of(
			Items.LEATHER_HELMET,
			Items.LEATHER_CHESTPLATE,
			Items.LEATHER_LEGGINGS,
			Items.LEATHER_BOOTS,
			Items.CHAINMAIL_HELMET,
			Items.CHAINMAIL_CHESTPLATE,
			Items.CHAINMAIL_LEGGINGS,
			Items.CHAINMAIL_BOOTS,
			Items.IRON_HELMET,
			Items.IRON_CHESTPLATE,
			Items.IRON_LEGGINGS,
			Items.IRON_BOOTS,
			Items.GOLDEN_HELMET,
			Items.GOLDEN_CHESTPLATE,
			Items.GOLDEN_LEGGINGS,
			Items.GOLDEN_BOOTS,
			Items.DIAMOND_HELMET,
			Items.DIAMOND_CHESTPLATE,
			Items.DIAMOND_LEGGINGS,
			Items.DIAMOND_BOOTS,
			Items.NETHERITE_HELMET,
			Items.NETHERITE_CHESTPLATE,
			Items.NETHERITE_LEGGINGS,
			Items.NETHERITE_BOOTS);
	public static final int MIN_SPAWN_RADIUS = 8;
	private static final int NORMAL_SPAWN_HEIGHT = 30;
	private static final int MIN_SPAWN_POINTS_TO_KEEP = 15;
	private static final int MIN_SPAWN_POINTS_TO_KEEP_BELOW_HEIGHT_CUTOFF = 20;
	private static final int HEIGHT_CUTOFF = 35;
	private static final float SPAWN_POINT_CULL_RATE = 0.3F;

	private SpawnPointContainer spawnPointContainer = new SpawnPointContainer();

	private final NexusAccess nexus;

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
		if (!permitSpawns) {
			return false;
		}

		mobConstruct = replaceWithRareWaveVariant(mobConstruct);
		Mob mob = mobConstruct.createMob(nexus);
		equipRandomWaveWeapon(mob);
		equipRandomWaveArmor(mob);
		int spawnTries = Math.min(spawnPointContainer.getNumberOfSpawnPoints(SpawnType.HUMANOID, angle), MAX_SPAWN_TRIES);

		for (int j = 0; j < spawnTries; j++) {
		    @Nullable
			final SpawnPoint spawnPoint = angle.max().orElse(EntityPattern.MAX_VALID_ANGLE) - angle.min().orElse(EntityPattern.MAX_ANGLE) >= 360
				        ? spawnPointContainer.getRandomSpawnPoint(SpawnType.HUMANOID)
		                : spawnPointContainer.getRandomSpawnPoint(SpawnType.HUMANOID, angle);

			if (spawnPoint == null) {
				return false;
			}
			if (!permitSpawns) {
				successfulSpawns++;
				if (debugMode) {
				    InvasionMod.LOGGER.debug("[Spawn] Time: " + currentWave.getTimeInWave() / 1000 + "  Type: " + mob + "  Coords: " + spawnPoint + "  Specified: " + angle);
				}

				return true;
			}

            if (spawnPoint.trySpawnEntity(
                    (ServerLevel) nexus.getWorld(), mob)) {
                successfulSpawns++;

                equipWitherSkeletonWeapon(mob);
                applyBabyZombieVariant(mob);
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
		InvasionMod.LOGGER.error("Could not find valid spawn for '" + mob.getName().getString() + "' after " + spawnTries + " tries");
		return false;
	}

	private EntityConstruct replaceWithRareWaveVariant(
			EntityConstruct construct) {
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
				? RANDOM_MELEE_WAVE_WEAPONS
				: RANDOM_RANGED_WAVE_WEAPONS;
		Item weapon = weaponPool.get(getRandom().nextInt(weaponPool.size()));
		mob.setItemSlot(EquipmentSlot.MAINHAND, weapon.getDefaultInstance());
	}

	private void applyBabyZombieVariant(Mob mob) {
		if (!(mob instanceof EntityIMZombie zombie)) {
			return;
		}

		int chancePercent = nexus.getBabyZombieChancePercent();
		if (getRandom().nextInt(100) < chancePercent) {
			zombie.setBaby(true);
		}
	}

	private void equipRandomWaveWeapon(Mob mob) {
		if (!(mob instanceof EntityIMZombie
				|| mob instanceof EntityIMZombiePigman
				|| mob instanceof ImpEnitty)
				|| !mob.getMainHandItem().isEmpty()) {
			return;
		}

		int chancePercent = nexus.getRandomEquipmentChancePercent();
		if (getRandom().nextInt(100) >= chancePercent) {
			return;
		}

		Item weapon = RANDOM_WAVE_WEAPONS.get(
				getRandom().nextInt(RANDOM_WAVE_WEAPONS.size()));
		mob.setItemSlot(
				net.minecraft.world.entity.EquipmentSlot.MAINHAND,
				weapon.getDefaultInstance());
	}

	private void equipRandomWaveArmor(Mob mob) {
		boolean canWearWaveArmor = mob instanceof IMSkeletonEntity
				|| mob instanceof PigmanEngineerEntity
				|| mob instanceof EntityIMZombie zombie && !zombie.isBrute()
				|| mob instanceof EntityIMZombiePigman pigman && !pigman.isBrute();
		if (!canWearWaveArmor) {
			return;
		}

		int chancePercent = nexus.getRandomEquipmentChancePercent();
		if (getRandom().nextInt(100) >= chancePercent) {
			return;
		}

		List<Item> availableArmor = new ArrayList<>();
		for (Item armor : RANDOM_WAVE_ARMOR) {
			EquipmentSlot slot = mob.getEquipmentSlotForItem(armor.getDefaultInstance());
			if (mob.getItemBySlot(slot).isEmpty()) {
				availableArmor.add(armor);
			}
		}
		if (availableArmor.isEmpty()) {
			return;
		}

		Item armor = availableArmor.get(getRandom().nextInt(availableArmor.size()));
		EquipmentSlot slot = mob.getEquipmentSlotForItem(armor.getDefaultInstance());
		mob.setItemSlot(slot, armor.getDefaultInstance());
	}

	private void generateSpawnPoints() {
		EntityIMZombie zombie = InvEntities.ZOMBIE.create(nexus.getWorld(), net.minecraft.world.entity.EntitySpawnReason.EVENT);
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
		entity.absSnapTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0, 0);
		if (entity.checkSpawnObstruction(nexus.getWorld()) && nexus.getWorld().noCollision(entity)) {
			int angle = (int) (Math.atan2(nexus.getOrigin().getZ() - pos.getZ(), nexus.getOrigin().getX() - pos.getX()) * Mth.RAD_TO_DEG);
			spawnPoints.add(new SpawnPoint(pos.immutable(), angle, SpawnType.HUMANOID));
		}
	}



    public void readNbt(CompoundTag compound, HolderLookup.Provider lookup) {
        setRadius(compound.getIntOr("spawnRadius", 0));
        elapsed = compound.getLongOr("elapsed", 0L);
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
