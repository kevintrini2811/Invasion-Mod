package com.invasion.nexus;

import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import com.invasion.InvasionConfig;
import com.invasion.InvSounds;
import com.invasion.InvasionMod;
import com.invasion.block.NexusBlock;
import com.invasion.block.InvBlocks;
import com.invasion.entity.ElectricityBoltEntity;
import com.invasion.entity.InvEntities;
import com.invasion.entity.SpawnProxyEntity;
import com.invasion.item.InvItems;
import com.invasion.nexus.ai.AttackerAI;
import com.invasion.nexus.spawns.IMWaveSpawner;
import com.invasion.nexus.wave.WaveBuilder;
import com.invasion.nexus.wave.Wave;
import com.invasion.nexus.wave.WaveSpawnerException;
import com.invasion.network.NexusHudPayload;
import net.neoforged.neoforge.network.PacketDistributor;

public class Nexus implements ControllableNexusAccess {
    private static final int INITIAL_SPAWN_RADIUS = 52;
    private static final int MAX_POWER_LEVEL = 2200;
    private static final int MAX_ACTIVAION_TIME = 400;
    private static final int MAX_HEALTH = 100;

    private int activationTimer;

    private int currentWave;
    private int nexusLevel = 1;
    private int nexusKills;

    private int hp = MAX_HEALTH;

    private Mode mode = Mode.STOPPED;
    private int powerLevel;

    private int lastPowerLevel;
    private int powerLevelTimer;

    private int mobsLeftInWave;
    private int lastMobsLeftInWave;

    private int mobsToKillInWave;

    private int nextAttackTime;

    private int daysToAttack;

    private long lastWorldTime;

    private int zapTimer;

    private int tickCount;

    private long waveDelayTimer;
    private long waveDelay;

    private boolean continuousAttack;
    private int continuousAttackCount;

    private boolean activated;
    private boolean discarded;
    private boolean paused;


    private final IMWaveSpawner waveSpawner = new IMWaveSpawner(this, INITIAL_SPAWN_RADIUS);
    private final WaveBuilder waveBuilder = new WaveBuilder();
    private final NexusInventory nexusItemStacks = new NexusInventory();

    private final Participants boundPlayers = new Participants(this);
    private final Combatants mobList;
    private final AttackerAI attackerAI = new AttackerAI(this);
    private NexusHudPayload lastHudPayload = NexusHudPayload.hidden();

    private final InvasionConfig config = InvasionMod.getConfig();

    private AABB boundingBoxToRadius;

    private BlockPos pos;
    private UUID uuid;

    private final ServerLevel world;
    private final WorldNexusStorage storage;

    private final ContainerData properties = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> activationTimer;
                case 1 -> getMode().ordinal();
                case 2 -> getCurrentWave();
                case 3 -> nexusLevel;
                case 4 -> nexusKills;
                case 5 -> getSpawnRadius();
                case 6 -> nexusItemStacks.getFluxProgress();
                case 7 -> powerLevel;
                case 8 -> nexusItemStacks.getCookTime();
                case 9 -> isActivating() ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int j) {
            if (index == 0) {
                activationTimer = j;
            } else if (index == 1) {
                setMode(Mode.forId(j));
            } else if (index == 2) {
                currentWave = j;
            } else if (index == 3) {
                nexusLevel = j;
            } else if (index == 4) {
                nexusKills = j;
            } else if (index == 5) {
                setSpawnRadius(j);
            } else if (index == 6) {
                nexusItemStacks.setFlugProgress(j);
            } else if (index == 7) {
                powerLevel = j;
            } else if (index == 8) {
                nexusItemStacks.setCookTime(j);
            }
        }

        @Override
        public int getCount() {
            return 10;
        }
    };

    Nexus(ServerLevel world, WorldNexusStorage storage, UUID id, BlockPos pos) {
        this.uuid = id;
        this.world = world;
        this.storage = storage;
        this.pos = pos;
        mobList = new Combatants(this);
        boundingBoxToRadius = computeSpawnArea();
        nexusItemStacks.setChangeListener(storage::setDirty);
    }

    @Override
    public UUID getUuid() {
        return uuid;
    }

    @Override
    public boolean isDiscarded() {
        return discarded;
    }

    void discard() {
        discarded = true;
    }

    public Container getHeldItems() {
        return nexusItemStacks;
    }

    public ContainerData getProperties() {
        return properties;
    }

    @Override
    public Participants getParticipants() {
        return boundPlayers;
    }

    private AABB computeSpawnArea() {
        return new AABB(pos).inflate(getSpawnRadius() + 10, getSpawnRadius() + 40, getSpawnRadius() + 10);
    }

    private AABB getChunkBox(Level world) {
        return new AABB(pos).inflate(getSpawnRadius() + 10, getSpawnRadius() + 40, getSpawnRadius() + 10)
                .setMinY(world.getMinBuildHeight()).setMaxY(world.getMaxBuildHeight());
    }

    @Override
    public boolean isActive() {
        return activated;
    }

    @Override
    public boolean isActivating() {
        return activationTimer > 0 && activationTimer < MAX_ACTIVAION_TIME;
    }

    @Override
    public Mode getMode() {
        return mode;
    }

    @Override
    public int getLevel() {
        return nexusLevel;
    }

    @Override
    public int getSpawnRadius() {
        return waveSpawner.getRadius();
    }

    @Override
    public Level getWorld() {
        return world;
    }

    @Override
    public BlockPos getOrigin() {
        return pos;
    }

    public Combatants getCombatants() {
        return mobList;
    }

    @Override
    public AttackerAI getAttackerAI() {
        return attackerAI;
    }

    @Override
    public int getCurrentWave() {
        return currentWave;
    }

    @Override
    public int getProgressionLevel() {
        return mode == Mode.CONTINUOUS ? continuousAttackCount : currentWave;
    }

    @Override
    public int getChargedCreeperChancePercent() {
        return mode == Mode.CONTINUOUS
                ? Math.clamp(continuousAttackCount, 1, 100)
                : ControllableNexusAccess.super
                        .getChargedCreeperChancePercent();
    }

    @Override
    public int getRandomEquipmentChancePercent() {
        return mode == Mode.CONTINUOUS
                ? Math.clamp(continuousAttackCount - 1, 0, 100)
                : ControllableNexusAccess.super
                        .getRandomEquipmentChancePercent();
    }

    @Override
    public int getMobsLeftInWave() {
        return Math.max(0, mobsLeftInWave);
    }

    @Override
    public int getMobsToKillInWave() {
        return Math.max(0, mobsToKillInWave);
    }

    @Override
    public int getHealthPercent() {
        return Math.max(0, Math.min(100, hp * 100 / MAX_HEALTH));
    }

    public void onPlayerJoined(ServerPlayer player) {
        if (mode.isActive()) {
            sendWaveProgressHud(player, createHudPayload());
        }
        boundPlayers.reconnect(player);
    }

    public void tick() {
        updateWaveProgressHud();
        if (!mode.isActive() || paused) {
            return;
        }
        try {
            tickCount = (tickCount + 1) % 60;
            if (tickCount == 0) {
                boundPlayers.bindPlayers(boundingBoxToRadius);
                mobList.updateMobList(boundingBoxToRadius);
            }

            if (mode == Mode.STARTED || mode == Mode.WAITING) {
                doInvasion(50);
            } else if (mode == Mode.CONTINUOUS) {
                doContinuous(50);
            }
            storage.setActiveNexus(this);
            storage.setDirty();
        } catch (WaveSpawnerException e) {
            InvasionMod.LOGGER.error("Exception occured whilst updating invasion", e);
            stop(false);
        }
    }

    public void onLoaded() {
        if (!mode.isActive() || paused) {
            return;
        }
        boundingBoxToRadius = getChunkBox(world);
        if (mode == Mode.CONTINUOUS && continuousAttack) {
            if (resumeSpawnerContinuous()) {
                mobsLeftInWave = (lastMobsLeftInWave += acquireEntities());
            }
        } else if (mode != Mode.DEBUG) {
            resumeSpawnerInvasion();
        }
    }

    @Override
    public void stop(boolean killEnemies) {
        if (mode == Mode.WAITING) {
            setMode(Mode.CONTINUOUS);
            int days = getWorld().getRandom().nextIntBetweenInclusive(config.minContinuousModeDays, config.maxContinuousModeDays);
            nextAttackTime = (int) ((getWorld().getGameTime() / TICKS_PER_DAY * TICKS_PER_DAY) + HALF_DAY_TIME + days * TICKS_PER_DAY);
        } else {
            setMode(Mode.STOPPED);
        }

        waveSpawner.stop();
        activationTimer = 0;
        currentWave = 0;
        activated = false;
        paused = false;
        updateWaveProgressHud();

        if (killEnemies) {
            killAllMobs();
        }
        storage.setDirty();
    }
    @Override
    public boolean togglePause() {
        if (!mode.isActive() && !paused) {
            return false;
        }

        if (!paused) {
            paused = true;
            setInvasionMobsPaused(true);
            storage.setDirty();
            return true;
        }

        paused = false;
        if (!waveSpawner.isActive()) {
            onLoaded();
        }
        setInvasionMobsPaused(false);
        storage.setDirty();
        return false;
    }

    private void setInvasionMobsPaused(boolean pause) {
        AABB area = boundingBoxToRadius != null ? boundingBoxToRadius : computeSpawnArea();
        for (Mob mob : getWorld().getEntitiesOfClass(Mob.class, area, entity ->
                entity instanceof Combatant<?> combatant && combatant.getNexus() == this)) {
            mob.setNoAi(pause);
        }
    }

    @Override
    public boolean isPaused() {
        return paused;
    }

    @Override
    public List<Component> getStatus() {
        return List.of(
                Component.literal("Current Time: " + getWorld().getGameTime()),
                Component.literal("Time to next: " + nextAttackTime),
                Component.literal("Days to attack: " + daysToAttack),
                Component.literal("Mobs left: " + mobsLeftInWave),
                Component.literal("Mode: " + mode)
        );
    }

    @Override
    public boolean setSpawnRadius(int radius) {
        if (!waveSpawner.isActive() && waveSpawner.setRadius(radius)) {
            boundingBoxToRadius = getChunkBox(getWorld());
            storage.setDirty();
            return true;
        }

        return false;
    }

    @Override
    public boolean setWave(int wave) {
        if (wave < 1
                || !activated
                || (mode != Mode.STARTED && mode != Mode.WAITING)) {
            return false;
        }

        try {
            killAllMobs();
            waveSpawner.stop();
            currentWave = wave;
            beginWave(currentWave);
            initializeWaveProgress();
            waveDelayTimer = -1L;
            nexusLevel = Math.max(nexusLevel, currentWave);
            updateWaveProgressHud();
            storage.setDirty();
            return true;
        } catch (WaveSpawnerException e) {
            InvasionMod.LOGGER.error("Unable to set invasion wave to {}", wave, e);
            return false;
        }
    }

    @Override
    public void damage(DamageSource source, int amount) {
        if (paused || amount <= 0 || hp <= 0) {
            return;
        }

        hp = Math.max(0, hp - amount);
        storage.setDirty();
        updateWaveProgressHud();
        boundPlayers.playSoundForBoundPlayers(SoundEvents.BLAZE_HURT);

        if (hp <= 0) {
            if (mode == Mode.STARTED || mode == Mode.DEBUG) {
                theEnd();
                SpawnProxyEntity mob = InvEntities.SPAWN_PROXY.create(getWorld());
                mob.setCustomName(InvBlocks.NEXUS_CORE.getName());
                boundPlayers.sendMessage(source.getLocalizedDeathMessage(mob));
            }
        }
    }

    @Override
    public void notifyCombatantRemoved(Combatant<?> combatant, RemovalReason reason) {
        if (reason == RemovalReason.KILLED) {
            nexusKills++;
            mobsLeftInWave--;
            storage.setDirty();
            updateWaveProgressHud();
            if (mobsLeftInWave <= 0) {
                if (lastMobsLeftInWave > 0) {
                    boundPlayers.sendMessage(ChatFormatting.GREEN, "invmod.message.nexus.stableagain");
                    lastMobsLeftInWave = mobsLeftInWave;
                }
                return;
            }
        } else if (reason == RemovalReason.DISCARDED) {
            if (combatant.asEntity().getType().create(getWorld()) instanceof Combatant<?> copy) {
                copy.asEntity().restoreFrom(combatant.asEntity());
                copy.setNexus(this);
                waveSpawner.askForRespawn(copy);
            }
        }
    }

    // TODO: Generate warning when a mob is nearby
    public void registerMobClose() {
    }

    private void updateWaveProgressHud() {
        NexusHudPayload payload = createHudPayload();
        if (payload.equals(lastHudPayload)) {
            return;
        }

        lastHudPayload = payload;
        for (ServerPlayer player : world.players()) {
            sendWaveProgressHud(player, payload);
        }
    }

    private NexusHudPayload createHudPayload() {
        if (!mode.isActive()) {
            return NexusHudPayload.hidden();
        }
        int total = Math.max(0, mobsToKillInWave);
        int defeated = Math.min(total, Math.max(0, total - mobsLeftInWave));
        int healthPercent = getHealthPercent();
        return new NexusHudPayload(true, mode == Mode.CONTINUOUS,
                getProgressionLevel(), defeated, total, healthPercent);
    }

    private void sendWaveProgressHud(ServerPlayer player, NexusHudPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    @Override
    public boolean start(int startWave) {
        if (!storage.setActiveNexus(this)) {
            InvasionMod.log("Another nexus is already active in this world");
        }
        if (mode == Mode.CONTINUOUS && continuousAttack) {
            boundPlayers.sendWarning("invmod.message.nexus.alreadyactivated");
            return false;
        }

        if (mode != Mode.STOPPED && mode != Mode.CONTINUOUS) {
            InvasionMod.log("Tried to activate Nexus while already active");
            return false;
        }

        if (!waveSpawner.isReady()) {
            InvasionMod.log("Wave spawner is not in ready state");
            return false;
        }

        try {
            paused = false; // falls vorher pausiert war
            boundingBoxToRadius = computeSpawnArea();
            bindExistingImMobs();
            currentWave = startWave;
            beginWave(currentWave);
            initializeWaveProgress();
            setMode(mode == Mode.STOPPED ? Mode.STARTED : Mode.WAITING);
            boundPlayers.bindPlayers(boundingBoxToRadius);
            regenerateHealth();
            waveDelayTimer = -1L;
            boundPlayers.sendMessage(boundPlayers.getParticipantsList());
            boundPlayers.sendWarning("invmod.message.nexus.firstwavesoon");
            boundPlayers.playSoundForBoundPlayers(InvSounds.BLOCK_NEXUS_RUMBLE);
            activated = true;
            return true;
        } catch (WaveSpawnerException e) {
            stop(false);
            InvasionMod.log(e.getMessage());
            boundPlayers.sendNotice(e.getMessage());
            return false;
        }
    }

    public boolean startDebugMode() {
        if (mode != Mode.STOPPED || !storage.setActiveNexus(this)) {
            return false;
        }

        waveSpawner.stop();
        paused = false;
        activationTimer = 0;
        currentWave = 0;
        mobsToKillInWave = 0;
        mobsLeftInWave = 0;
        lastMobsLeftInWave = 0;
        boundingBoxToRadius = computeSpawnArea();
        bindExistingImMobs();
        boundPlayers.bindPlayers(boundingBoxToRadius);
        regenerateHealth();
        activated = true;
        setMode(Mode.DEBUG);
        boundPlayers.sendMessage(boundPlayers.getParticipantsList());
        return true;
    }

    private void startContinuousPlay() {
        if (mode != Mode.STABLE || !waveSpawner.isReady()) {
            boundPlayers.sendWarning("invmod.message.nexus.couldnotactivate");
            return;
        }
        boundingBoxToRadius = getChunkBox(getWorld());
        bindExistingImMobs();
        setMode(Mode.CONTINUOUS);
        regenerateHealth();
        lastPowerLevel = powerLevel;
        lastWorldTime = getWorld().getGameTime();
        nextAttackTime = (int) ((lastWorldTime / TICKS_PER_DAY * TICKS_PER_DAY) + HALF_DAY_TIME);
        if (lastWorldTime % TICKS_PER_DAY > SUNSET_TIME && lastWorldTime % TICKS_PER_DAY < NIGHT_TIME) {
            boundPlayers.sendWarning("invmod.message.nexus.nightlooming");
        } else {
            boundPlayers.sendWarning("invmod.message.nexus.activatedandstable");
        }
    }

    private void doInvasion(int elapsed) throws WaveSpawnerException {
        if (waveSpawner.isActive()) {
            if (hp <= 0) {
                theEnd();
            } else {
                nexusItemStacks.generateFlux(1);
                if (waveSpawner.isWaveComplete()) {
                    if (waveDelayTimer == -1L) {
                        boundPlayers.playSoundForBoundPlayers(InvSounds.BLOCK_NEXUS_CHIME);
                        waveDelayTimer = 0L;
                        waveDelay = waveSpawner.getWaveRestTime();
                        InvasionMod.LOGGER.debug("Next wave begins in: {}ticks", waveDelay);
                    } else {
                        waveDelayTimer += elapsed;
                        if (waveDelayTimer > waveDelay) {
                            currentWave += 1;
                            beginWave(currentWave);
                            initializeWaveProgress();
                            waveDelayTimer = -1L;
                            boundPlayers.playSoundForBoundPlayers(InvSounds.BLOCK_NEXUS_RUMBLE);
                            if (currentWave > nexusLevel) {
                                nexusLevel = currentWave;
                            }
                        }
                    }
                } else {
                    waveSpawner.spawn(elapsed);
                }
            }
        }
    }

    private void doContinuous(int elapsed) {
        powerLevelTimer += elapsed;
        if (powerLevelTimer > MAX_POWER_LEVEL) {
            powerLevelTimer -= MAX_POWER_LEVEL;
            nexusItemStacks.generateFlux(5 + (int) (5 * powerLevel / 1550F));
            if (!nexusItemStacks.getItem(0).is(InvItems.DAMPING_AGENT)) {
                powerLevel++;
            }
        }

        if (nexusItemStacks.getItem(0).is(InvItems.STRONG_DAMPING_AGENT) && powerLevel >= 0 && !continuousAttack && --powerLevel < 0) {
            stop(false);
        }

        if (!continuousAttack) {
            long currentTime = getWorld().getGameTime();
            int timeOfDay = (int) (this.lastWorldTime % TICKS_PER_DAY);
            if (timeOfDay < SUNSET_TIME && currentTime % TICKS_PER_DAY >= SUNSET_TIME && currentTime + SUNSET_TIME > nextAttackTime) {
                boundPlayers.sendWarning("invmod.message.nexus.nightlooming");
            }
            if (lastWorldTime > currentTime) {
                nextAttackTime = ((int) (nextAttackTime - (lastWorldTime - currentTime)));
            }
            lastWorldTime = currentTime;

            if (lastWorldTime >= nextAttackTime) {
                try {
                    float difficulty = 1 + powerLevel / 4500;
                    float tierLevel = 1 + powerLevel / 4500;
                    Wave wave = waveBuilder.generateWave(difficulty, tierLevel, WAVE_DURATION);
                    continuousAttackCount++;
                    mobsLeftInWave = (lastMobsLeftInWave = mobsToKillInWave = (int) (wave.getTotalMobAmount() * 0.8F));
                    beginWave(wave);
                    continuousAttack = true;
                    int days = getWorld().getRandom().nextIntBetweenInclusive(config.minContinuousModeDays, config.maxContinuousModeDays);
                    nextAttackTime = (int) ((currentTime / TICKS_PER_DAY * TICKS_PER_DAY) + HALF_DAY_TIME + days * TICKS_PER_DAY);
                    regenerateHealth();
                    zapTimer = 0;
                    waveDelayTimer = -1L;
                    boundPlayers.sendWarning("invmod.message.nexus.destabilizing");
                    boundPlayers.playSoundForBoundPlayers(InvSounds.BLOCK_NEXUS_RUMBLE);
                } catch (WaveSpawnerException e) {
                    InvasionMod.LOGGER.error("Exception whilst updating spawner", e);
                    stop(false);
                }
            }

        } else if (hp <= 0) {
            continuousAttack = false;
            continuousNexusHurt();
        } else if (waveSpawner.isWaveComplete()) {
            if (waveDelayTimer == -1L) {
                waveDelayTimer = 0L;
                waveDelay = waveSpawner.getWaveRestTime();
            } else {
                waveDelayTimer += elapsed;
                if (waveDelayTimer > waveDelay && zapTimer < -200) {
                    waveDelayTimer = -1L;
                    continuousAttack = false;
                    waveSpawner.stop();
                    regenerateHealth();
                    lastPowerLevel = powerLevel;
                }
            }

            zapTimer--;
            if (mobsLeftInWave <= 0) {
                if (zapTimer <= 0 && zapEnemy(true)) {
                    zapEnemy(false);
                    zapTimer = 23;
                }
            }
        } else {
            try {
                waveSpawner.spawn(elapsed);
            } catch (WaveSpawnerException e) {
                InvasionMod.LOGGER.error("Exception occured whilst spawning wave", e);
                stop(false);
            }
        }
    }

    private void regenerateHealth() {
        hp = MAX_HEALTH;
    }

    public void tickInventory() {
        int previousActivationTimer = activationTimer;
        Mode previousMode = mode;
        nexusItemStacks.tick(this);

        if (!storage.canActivate(this)) {
            return;
        }

        ItemStack catalyst = nexusItemStacks.getItem(0);

        if (activationTimer >= MAX_ACTIVAION_TIME) {
            activationTimer = 0;
            if (!catalyst.isEmpty()) {
                if (catalyst.is(InvItems.NEXUS_CATALYST)) {
                    catalyst.shrink(1);
                    start(1);
                } else if (catalyst.is(InvItems.STRONG_NEXUS_CATALYST)) {
                    catalyst.shrink(1);
                    start(10);
                } else if (catalyst.is(InvItems.STABLE_NEXUS_CATALYST)) {
                    catalyst.shrink(1);
                    activated = true;
                    startContinuousPlay();
                }
            }
        } else if (mode.isIdle()) {
            if (!catalyst.isEmpty()) {
                if (catalyst.is(InvItems.NEXUS_CATALYST) || catalyst.is(InvItems.STRONG_NEXUS_CATALYST)) {
                    activationTimer++;
                    if (activationTimer % 100 == world.getRandom().nextInt(100)) {
                        world.playSound(null, pos, InvSounds.BLOCK_NEXUS_RUMBLE, SoundSource.BLOCKS, 1, 1);
                    }
                    setMode(Mode.STOPPED);
                } else if (catalyst.is(InvItems.STABLE_NEXUS_CATALYST)) {
                    activationTimer++;
                    if (activationTimer % 100 == world.getRandom().nextInt(100)) {
                        world.playSound(null, pos, InvSounds.BLOCK_NEXUS_RUMBLE, SoundSource.BLOCKS, 1, 1);
                    }
                    setMode(Mode.STABLE);
                }
            } else {
                activationTimer = 0;
            }
        } else if (mode == Mode.CONTINUOUS) {
            if (!catalyst.isEmpty()) {
                if (catalyst.is(InvItems.NEXUS_CATALYST) || catalyst.is(InvItems.STRONG_NEXUS_CATALYST)) {
                    activationTimer++;
                }
            } else {
                activationTimer = 0;
            }
        }
        if (activationTimer != previousActivationTimer || mode != previousMode) {
            storage.setDirty();
        }
    }

    protected void setMode(Mode mode) {
        if (mode == this.mode) {
            return;
        }
        InvasionMod.LOGGER.debug("Nexus {} changing mode from {} to {}", this.getUuid(), this.mode, mode);
        this.mode = mode;
        storage.setDirty();
        if (getWorld() instanceof ServerLevel sw) {
            if (sw.getBlockState(pos).is(InvBlocks.NEXUS_CORE)) {
                sw.setBlockAndUpdate(pos, InvBlocks.NEXUS_CORE.defaultBlockState().setValue(NexusBlock.LIT, mode != Mode.STOPPED));
            } else {
                discard();
            }
        }
    }

    private int acquireEntities() {
        List<PathfinderMob> entities = getWorld().getEntitiesOfClass(PathfinderMob.class, boundingBoxToRadius.inflate(10, 128, 10), Combatant.PREDICATE);
        InvasionMod.log("Acquired " + entities.size() + " entities after state restore");
        return entities.size();
    }

    private void bindExistingImMobs() {
        for (Combatant<?> combatant
                : com.invasion.entity.BoundIMMobRegistry.loaded(
                        (ServerLevel)getWorld())) {
            net.minecraft.world.entity.Entity entity = combatant.asEntity();
            if (entity instanceof LivingEntity living
                    && living.isAlive()
                    && !entity.isRemoved()
                    && !(entity instanceof com.invasion.entity.IMWolfEntity)) {
                combatant.setNexus(this);
            }
        }
    }

    private void beginWave(int wave) throws WaveSpawnerException {
        bindExistingImMobs();
        waveSpawner.beginNextWave(wave);
    }

    private void beginWave(Wave wave) throws WaveSpawnerException {
        bindExistingImMobs();
        waveSpawner.beginNextWave(wave);
    }

    private void initializeWaveProgress() {
        mobsToKillInWave = Math.max(1,
                (int)(waveSpawner.getTotalDefinedMobsThisWave() * 0.8F));
        mobsLeftInWave = mobsToKillInWave;
        lastMobsLeftInWave = mobsToKillInWave;
    }

    private void theEnd() {
        if (!getWorld().isClientSide()) {
            boundPlayers.sendWarning("invmod.message.nexus.destroyed");
            stop(false);
            boundPlayers.release();
            killAllMobs();
        }
    }

    private void continuousNexusHurt() {
        boundPlayers.sendWarning("invmod.message.nexus.severelydamaged");
        boundPlayers.playSoundForBoundPlayers(SoundEvents.ENDER_DRAGON_DEATH, 4, 1);
        killAllMobs();
        waveSpawner.stop();
        powerLevel = ((int) ((powerLevel - (powerLevel - lastPowerLevel)) * 0.7F));
        lastPowerLevel = powerLevel;
        if (powerLevel < 0) {
            powerLevel = 0;
            stop(false);
        }
    }

    private void killAllMobs() {
        DamageSource source = getWorld().damageSources().magic();
        for (Combatant<?> combatant
                : com.invasion.entity.BoundIMMobRegistry.bound(
                        (ServerLevel)getWorld())) {
            net.minecraft.world.entity.Entity entity = combatant.asEntity();
            if (entity instanceof LivingEntity mob
                    && !(entity instanceof com.invasion.entity.IMWolfEntity)
                    && combatant.getNexus() == this) {
                mob.hurt(source, mob.getMaxHealth());
                mob.kill();
            }
        }
    }

    private boolean zapEnemy(boolean sfx) {
        Combatant<?> mob = mobList.removeNearestCombatant();
        if (mob == null) {
            return false;
        }
        mob.asEntity().hurt(mob.asEntity().damageSources().magic(), 500);
        getWorld().addFreshEntity(new ElectricityBoltEntity(getWorld(), com.invasion.util.math.PosUtils.center(pos), mob.asEntity().getEyePosition(), 15, sfx));
        return true;
    }

    private boolean resumeSpawnerContinuous() {
        try {
            float difficulty = 1 + powerLevel / 4500F;
            float tierLevel = 1 + powerLevel / 4500F;
            Wave wave = waveBuilder.generateWave(difficulty, tierLevel, WAVE_DURATION);
            this.mobsToKillInWave = ((int) (wave.getTotalMobAmount() * 0.8F));
            InvasionMod.log("Original mobs to kill: " + mobsToKillInWave);
            lastMobsLeftInWave = mobsToKillInWave - waveSpawner.resumeFromState(wave);
            mobsLeftInWave = lastMobsLeftInWave;
            return true;
        } catch (WaveSpawnerException e) {
            InvasionMod.LOGGER.error("Error resuming spawner", e);
            stop(false);
            return false;
        }
    }

    private boolean resumeSpawnerInvasion() {
        try {
            waveSpawner.resumeFromState(currentWave);
            if (mobsToKillInWave <= 0) {
                initializeWaveProgress();
            }
            return true;
        } catch (WaveSpawnerException e) {
            InvasionMod.LOGGER.error("Error resuming spawner", e);
            stop(false);
            return false;
        }
    }

    Nexus(ServerLevel world, WorldNexusStorage storage, CompoundTag compound, HolderLookup.Provider lookup) {
        this(world, storage,
                compound.getUUID("uuid"),
                net.minecraft.nbt.NbtUtils.readBlockPos(compound, "pos").orElseThrow());
        activationTimer = compound.getInt("activationTimer");
        mode = Mode.forId(compound.getInt("mode"));
        currentWave = compound.getInt("currentWave");
        nexusLevel = compound.getInt("nexusLevel");
        hp = compound.getInt("hp");
        nexusKills = compound.getInt("nexusKills");
        powerLevel = compound.getInt("powerLevel");
        lastPowerLevel = compound.getInt("lastPowerLevel");
        nextAttackTime = compound.getInt("nextAttackTime");
        daysToAttack = compound.getInt("daysToAttack");
        continuousAttack = compound.getBoolean("continuousAttack");
        continuousAttackCount = compound.getInt("continuousAttackCount");
        activated = compound.getBoolean("activated");
        paused = compound.getBoolean("paused");
        mobsLeftInWave = compound.getInt("mobsLeftInWave");
        lastMobsLeftInWave = compound.contains("lastMobsLeftInWave")
                ? compound.getInt("lastMobsLeftInWave") : mobsLeftInWave;
        mobsToKillInWave = compound.getInt("mobsToKillInWave");

        nexusItemStacks.readNbt(compound.getCompound("inventory"), lookup);
        boundPlayers.readNbt(compound.getCompound("boundPlayers"), lookup);
        waveSpawner.readNbt(compound.getCompound("waveSpawner"), lookup);
        attackerAI.readNbt(compound.getCompound("ai"), lookup);

        boundingBoxToRadius = computeSpawnArea();
    }

    public CompoundTag writeNbt(CompoundTag compound, HolderLookup.Provider lookup) {
        compound.putUUID("uuid", uuid);
        compound.put("pos", net.minecraft.nbt.NbtUtils.writeBlockPos(pos));
        compound.putInt("activationTimer", activationTimer);
        compound.putInt("mode", getMode().ordinal());
        compound.putInt("currentWave", getCurrentWave());
        compound.putInt("nexusLevel", getLevel());
        compound.putInt("hp", hp);
        compound.putInt("nexusKills", nexusKills);
        compound.putInt("powerLevel", powerLevel);
        compound.putInt("lastPowerLevel", lastPowerLevel);
        compound.putInt("nextAttackTime", nextAttackTime);
        compound.putInt("daysToAttack", daysToAttack);
        compound.putBoolean("continuousAttack", continuousAttack);
        compound.putInt("continuousAttackCount", continuousAttackCount);
        compound.putBoolean("activated", isActive());
        compound.putBoolean("paused", paused);
        compound.putInt("mobsLeftInWave", mobsLeftInWave);
        compound.putInt("lastMobsLeftInWave", lastMobsLeftInWave);
        compound.putInt("mobsToKillInWave", mobsToKillInWave);


        compound.put("inventory", nexusItemStacks.writeNbt(new CompoundTag(), lookup));
        compound.put("boundPlayers", boundPlayers.writeNbt(new CompoundTag(), lookup));
        compound.put("waveSpawner", waveSpawner.writeNbt(new CompoundTag(), lookup));
        compound.put("ai", attackerAI.writeNbt(new CompoundTag(), lookup));
        return compound;
    }
}
