package com.playymcmc007.uselessthings.block;

import com.playymcmc007.uselessthings.ModItems;
import com.playymcmc007.uselessthings.UselessThings;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Mod.EventBusSubscriber
public class SuperChunkBlock extends Block {
    public static final BooleanProperty DIRECT_MODE = BooleanProperty.create("direct_mode");

    private static final int LOAD_DELAY_MS = 1;
    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(4);
    private static final ConcurrentHashMap<BlockPos, LoadingState> LOADING_STATES = new ConcurrentHashMap<>();

    public SuperChunkBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(DIRECT_MODE, false));
    }
    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
        return new ItemStack(
                state.getValue(DIRECT_MODE) ?
                        ModItems.SUPER_CHUNK_BLOCK_DIRECT.get() :
                        ModItems.SUPER_CHUNK_BLOCK_TICKET.get()
        );
    }
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DIRECT_MODE);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            ItemStack itemInHand = player.getItemInHand(hand);

            if (itemInHand.getItem() == Items.STICK) {
                showLoadingStatus((ServerLevel) level, pos, player);
                return InteractionResult.SUCCESS;
            }
            else if (itemInHand.getItem() == Items.COMPASS) {
                showCurrentRadius((ServerLevel) level, pos, player);
                return InteractionResult.SUCCESS;
            }
        }
        return super.use(state, level, pos, player, hand, hit);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!level.isClientSide) {
            startForceLoading((ServerLevel) level, pos);
        }
        super.onPlace(state, level, pos, oldState, isMoving);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide) {
            stopForceLoading(pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    private void startForceLoading(ServerLevel level, BlockPos pos) {
        stopForceLoading(pos);

        BlockState state = level.getBlockState(pos);
        boolean directMode = state.getValue(DIRECT_MODE);

        LoadingState loadingState = new LoadingState(level, pos, directMode);

        ScheduledFuture<?> future = SCHEDULER.scheduleAtFixedRate(
                new ChunkLoaderTask(loadingState),
                0, LOAD_DELAY_MS, TimeUnit.MILLISECONDS
        );

        loadingState.setFuture(future);
        LOADING_STATES.put(pos, loadingState);

        String modeName = directMode ? "直接模式" : "票证模式";
        UselessThings.LOGGER.info("SuperChunkBlock started {} at {}", modeName, pos);

        broadcastMessage(level, pos,
                Component.literal("§a[SuperChunk] §f开始" + (directMode ? "§c直接" : "§a票证") + "§f模式加载区块！"));
    }

    private static void stopForceLoading(BlockPos pos) {
        LoadingState loadingState = LOADING_STATES.remove(pos);
        if (loadingState != null) {
            long totalLoaded = loadingState.getTotalLoadedChunks();
            long currentRadius = loadingState.getCurrentRadius();
            loadingState.stop();

            if (loadingState.getLevel() != null) {
                broadcastMessage(loadingState.getLevel(), pos,
                        Component.literal("§c[SuperChunk] §f停止加载区块，共加载了 §e" +
                                totalLoaded + " §f个区块，最远到达半径 §e" + currentRadius + "§f 格区块"));
            }

            UselessThings.LOGGER.info("SuperChunkBlock stopped force loading at {}, total chunks: {}, max radius: {}",
                    pos, totalLoaded, currentRadius);
        }
    }

    private void showLoadingStatus(ServerLevel level, BlockPos pos, Player player) {
        LoadingState state = LOADING_STATES.get(pos);
        if (state != null) {
            String modeName = state.isDirectMode() ? "§c直接模式" : "§a票证模式";
            player.sendSystemMessage(Component.literal("§6========== SuperChunk " + modeName + " §6=========="));
            player.sendSystemMessage(Component.literal("§e当前加载: §a" +
                    String.format("%-8d", state.getCurrentLoadedChunks()) + " §7个区块"));
            player.sendSystemMessage(Component.literal("§e总计加载: §b" +
                    String.format("%-8d", state.getTotalLoadedChunks()) + " §7个区块"));
            player.sendSystemMessage(Component.literal("§e当前半径: §d" +
                    String.format("%-8d", state.getCurrentRadius()) + " §7格区块"));
            player.sendSystemMessage(Component.literal("§e运行时间: §f" + state.getRunningTime()));
            player.sendSystemMessage(Component.literal("§6========================================"));
        } else {
            player.sendSystemMessage(Component.literal("§c这个方块没有在加载区块！"));
        }
    }

    private void showCurrentRadius(ServerLevel level, BlockPos pos, Player player) {
        LoadingState state = LOADING_STATES.get(pos);
        if (state != null) {
            player.sendSystemMessage(Component.literal("§6========== 无限加载进度 =========="));
            player.sendSystemMessage(Component.literal("§e中心坐标: §fX=" + pos.getX() + " Y=" + pos.getY() + " Z=" + pos.getZ()));
            player.sendSystemMessage(Component.literal("§e中心区块: §f(" + state.getCenterChunk().x + ", " + state.getCenterChunk().z + ")"));
            player.sendSystemMessage(Component.literal("§e当前半径: §f" + state.getCurrentRadius() + " 格区块"));
            player.sendSystemMessage(Component.literal("§e已加载区块: §f" + state.getCurrentLoadedChunks() + " 个"));
            player.sendSystemMessage(Component.literal("§6=================================="));
        }
    }

    private static void broadcastMessage(ServerLevel level, BlockPos pos, Component message) {
        level.getPlayers(player -> player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 10000)
                .forEach(player -> player.sendSystemMessage(message));
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!event.getLevel().isClientSide()) {
            BlockState state = event.getState();
            if (state.getBlock() instanceof SuperChunkBlock) {
                BlockPos pos = event.getPos();
                stopForceLoading(pos);
            }
        }
    }

    private static class LoadingState {
        private final ServerLevel level;
        private final BlockPos pos;
        private final ChunkPos centerChunk;
        private final Set<ChunkPos> loadedChunks = ConcurrentHashMap.newKeySet();
        private final AtomicLong totalLoadedChunks = new AtomicLong(0);
        private final long startTime;
        private final boolean directMode;

        private volatile long currentRadius = 0;
        private ScheduledFuture<?> future;
        private volatile boolean isRunning = true;

        public LoadingState(ServerLevel level, BlockPos pos, boolean directMode) {  // 改名
            this.level = level;
            this.pos = pos;
            this.centerChunk = new ChunkPos(pos);
            this.startTime = System.currentTimeMillis();
            this.directMode = directMode;
        }

        public void setFuture(ScheduledFuture<?> future) {
            this.future = future;
        }

        public void stop() {
            isRunning = false;
            if (future != null) {
                future.cancel(false);
            }

            new Thread(() -> {
                UselessThings.LOGGER.info("Starting to unload {} forced chunks from {}", loadedChunks.size(), pos);
                int count = 0;
                for (ChunkPos chunkPos : loadedChunks) {
                    try {
                        if (directMode) {
                            level.setChunkForced(chunkPos.x, chunkPos.z, false);
                        } else {
                            level.getChunkSource().removeRegionTicket(
                                    net.minecraft.server.level.TicketType.FORCED,
                                    chunkPos,
                                    2,
                                    chunkPos
                            );
                        }
                        count++;
                    } catch (Exception e) {
                        UselessThings.LOGGER.error("Error unloading chunk {}: {}", chunkPos, e.getMessage());
                    }
                }
                loadedChunks.clear();
                UselessThings.LOGGER.info("Finished unloading {} chunks from {}", count, pos);
            }).start();
        }

        public boolean addLoadedChunk(ChunkPos chunkPos) {
            if (loadedChunks.add(chunkPos)) {
                totalLoadedChunks.incrementAndGet();
                return true;
            }
            return false;
        }

        public boolean isChunkLoaded(ChunkPos chunkPos) {
            return loadedChunks.contains(chunkPos);
        }

        public ServerLevel getLevel() { return level; }
        public BlockPos getPos() { return pos; }
        public ChunkPos getCenterChunk() { return centerChunk; }
        public long getCurrentRadius() { return currentRadius; }
        public void setCurrentRadius(long radius) { this.currentRadius = radius; }
        public int getCurrentLoadedChunks() { return loadedChunks.size(); }
        public long getTotalLoadedChunks() { return totalLoadedChunks.get(); }
        public boolean isRunning() { return isRunning; }
        public boolean isDirectMode() { return directMode; }  // 改名

        public String getRunningTime() {
            long seconds = (System.currentTimeMillis() - startTime) / 1000;
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            long secs = seconds % 60;
            if (hours > 0) {
                return String.format("%d:%02d:%02d", hours, minutes, secs);
            }
            return String.format("%d:%02d", minutes, secs);
        }
    }

    private static class ChunkLoaderTask implements Runnable {
        private final LoadingState state;

        public ChunkLoaderTask(LoadingState state) {
            this.state = state;
        }

        @Override
        public void run() {
            try {
                if (!state.isRunning() || state.getLevel().getBlockState(state.getPos()).isAir()) {
                    stopForceLoading(state.getPos());
                    return;
                }

                forceLoadNextRing();
            } catch (Exception e) {
                UselessThings.LOGGER.error("Error in ChunkLoaderTask at {}", state.getPos(), e);
            }
        }

        private void forceLoadNextRing() {
            long currentRadius = state.getCurrentRadius();
            ChunkPos centerChunk = state.getCenterChunk();

            if (currentRadius == 0) {
                forceLoadChunk(centerChunk.x, centerChunk.z);
                state.setCurrentRadius(1);

                if (state.getTotalLoadedChunks() % 1000 == 0) {
                    broadcastProgress();
                }
                return;
            }

            for (long x = -currentRadius; x <= currentRadius; x++) {
                for (long z = -currentRadius; z <= currentRadius; z++) {
                    if (Math.abs(x) == currentRadius || Math.abs(z) == currentRadius) {
                        long chunkX = centerChunk.x + x;
                        long chunkZ = centerChunk.z + z;

                        if (chunkX > Integer.MAX_VALUE || chunkX < Integer.MIN_VALUE ||
                                chunkZ > Integer.MAX_VALUE || chunkZ < Integer.MIN_VALUE) {
                            continue;
                        }

                        ChunkPos chunkPos = new ChunkPos((int)chunkX, (int)chunkZ);

                        if (!state.isChunkLoaded(chunkPos)) {
                            forceLoadChunk((int)chunkX, (int)chunkZ);
                            state.addLoadedChunk(chunkPos);

                            if (state.getTotalLoadedChunks() % 1000 == 0) {
                                broadcastProgress();
                            }
                            return;
                        }
                    }
                }
            }

            state.setCurrentRadius(currentRadius + 1);

            if (currentRadius % 10 == 0) {
                BlockPos pos = state.getPos();
                broadcastMessage(state.getLevel(), state.getPos(),
                        Component.literal("§a[SuperChunk] §f已达到半径 §e" + currentRadius +
                                "§f，已加载 §e" + state.getTotalLoadedChunks() + "§f 个区块"  +
                                "§f，运行时间 §e" + state.getRunningTime() +
                                "§f 于 §e[" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]"));
            }
        }

        private void forceLoadChunk(int chunkX, int chunkZ) {
            try {
                ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);

                if (state.isDirectMode()) {
                    state.getLevel().setChunkForced(chunkX, chunkZ, true);
                } else {
                    state.getLevel().getChunkSource().addRegionTicket(
                            net.minecraft.server.level.TicketType.FORCED,
                            chunkPos,
                            2,
                            chunkPos
                    );
                }

                state.getLevel().getChunk(chunkX, chunkZ);

                if (UselessThings.LOGGER.isDebugEnabled()) {
                    UselessThings.LOGGER.debug("Force loaded chunk ({}, {}) from {}",
                            chunkX, chunkZ, state.getPos());
                }
            } catch (Exception e) {
                UselessThings.LOGGER.error("Failed to force load chunk ({}, {})", chunkX, chunkZ, e);
            }
        }

        private void broadcastProgress() {
            BlockPos pos = state.getPos();
            broadcastMessage(state.getLevel(), state.getPos(),
                    Component.literal("§a[SuperChunk] §f已加载 §e" + state.getTotalLoadedChunks() +
                            "§f 个区块，当前半径 §e" + state.getCurrentRadius() +
                            "§f，运行时间 §e" + state.getRunningTime() +
                            "§f 于 §e[" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]"));
        }
    }

    public static void shutdown() {
        LOADING_STATES.values().forEach(LoadingState::stop);
        LOADING_STATES.clear();
    }
}