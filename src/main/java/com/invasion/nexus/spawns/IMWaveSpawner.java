package com.invasion.nexus.spawns;

import com.invasion.entity.ai.goal.ExternalAttackNexusGoal;
import com.invasion.mixin.MobEntityAccessor;
import com.invasion.nexus.wave.*;
import com.invasion.util.ChatUtils;
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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.jetbrains.annotations.Nullable;

import com.invasion.InvasionMod;
import com.invasion.entity.EntityIMZombie;
import com.invasion.entity.InvEntities;
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
        killExternalInvasionMobs();
    }

    /**
     * Killt alle externen Invasions-Mobs (Mutant Monsters, Giant, etc.) in der Nähe des Nexus,
     * wenn die Invasion endet oder der Nexus zerstört wurde.
     */
    private void killExternalInvasionMobs() {
        if (!(nexus.getWorld() instanceof ServerLevel world)) {
            return;
        }

        // Bereich um den Nexus, in dem wir nach Zusatzmobs suchen
        BlockPos origin = nexus.getOrigin();
        double radius = this.spawnRadius + 32; // etwas größer als Spawnradius
        AABB searchBox = new AABB(
                origin.getX() - radius, origin.getY() - radius, origin.getZ() - radius,
                origin.getX() + radius, origin.getY() + radius, origin.getZ() + radius
        );

        List<Mob> mobs = world.getEntitiesOfClass(
                Mob.class,
                searchBox,
                mob -> EntityPatterns.isExternalInvasionMob(mob.getType())
        );

        for (Mob mob : mobs) {
            // "sterben" lassen – entweder kill() oder discard()
            //mob.kill();        // versucht normalen Tod (Death-Events etc.)
            mob.discard();  // Alternative: einfach verschwinden lassen
        }

        InvasionMod.LOGGER.info("Killed {} external invasion mobs after nexus end.", mobs.size());
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

		Mob mob = mobConstruct.createMob(nexus);
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
				    InvasionMod.LOGGER.info("[Spawn] Time: " + currentWave.getTimeInWave() / 1000 + "  Type: " + mob + "  Coords: " + spawnPoint + "  Specified: " + angle);
				}

				return true;
			}

            if (spawnPoint.trySpawnEntity((ServerLevel) nexus.getWorld(), mob)) {
                successfulSpawns++;

                // ➜ HIER: nach erfolgreichem Spawn ins Team packen
                if (EntityPatterns.isExternalInvasionMob(mob.getType())) {
                    MobEntityAccessor accessor = (MobEntityAccessor)(Object)mob;
                    accessor.getGoalSelector().addGoal(2, new ExternalAttackNexusGoal(mob, nexus));
                    mob.setPersistenceRequired();

                }

                markAsInvasionAlly(mob);
                if (EntityPatterns.isExternalInvasionMob(mob.getType())) {
                    ChatUtils.broadcastGlobal("Ein Mutant ist gespawnt: " + mob.getName().getString(), ChatFormatting.DARK_RED);
                }
                if (debugMode) {
                    InvasionMod.LOGGER.info("[Spawn] Time: " + currentWave.getTimeInWave()
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

		InvasionMod.LOGGER.info("Found {} spawn points for next nexus wave", spawnPointContainer.getNumberOfSpawnPoints(SpawnType.HUMANOID));
	}

	private void addValidSpawn(Mob entity, List<SpawnPoint> spawnPoints, BlockPos pos) {
	    if (nexus.getWorld().isOutsideBuildHeight(pos)) {
	        InvasionMod.LOGGER.info("[Spawn] Spawn point was outside of build limit {}", pos);
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
        mob.setLastHurtByPlayer((java.util.UUID) null, 0);
        mob.setLastHurtByMob(null);
    }





}
