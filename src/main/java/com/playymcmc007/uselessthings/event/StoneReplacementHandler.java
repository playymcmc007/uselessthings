package com.playymcmc007.uselessthings.event;

import com.playymcmc007.uselessthings.UselessThings;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = UselessThings.MODID)
public class StoneReplacementHandler {

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        LevelAccessor levelAccessor = event.getLevel();

        // 确保是服务端并且是我们自己的维度
        if (levelAccessor instanceof Level level &&
                level.dimension().location().equals(UselessThings.NO_STONE_DIMENSION_ID)) {

            ChunkAccess chunk = event.getChunk();

            // 当区块加载时，替换所有石头
            replaceAllStoneWithDirt(chunk);
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        Level level = (Level) event.getLevel();

        // 只在我们自己的维度中生效
        if (level.dimension().location().equals(UselessThings.NO_STONE_DIMENSION_ID)) {
            BlockState placedState = event.getPlacedBlock();

            // 如果玩家放置的是石头类方块，直接取消操作
            if (placedState.is(BlockTags.BASE_STONE_OVERWORLD)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onBlockMultiPlace(BlockEvent.EntityMultiPlaceEvent event) {
        Level level = (Level) event.getLevel();

        if (level.dimension().location().equals(UselessThings.NO_STONE_DIMENSION_ID)) {
            // 检查是否有石头类方块
            boolean hasStone = false;
            for (BlockSnapshot snapshot : event.getReplacedBlockSnapshots()) {
                BlockState placedState = snapshot.getReplacedBlock();
                if (placedState.is(BlockTags.BASE_STONE_OVERWORLD)) {
                    hasStone = true;
                    break;
                }
            }

            // 如果有石头，取消整个放置操作
            if (hasStone) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onFluidPlaceBlock(BlockEvent.FluidPlaceBlockEvent event) {
        LevelAccessor levelAccessor = event.getLevel();

        if (levelAccessor instanceof Level level &&
                level.dimension().location().equals(UselessThings.NO_STONE_DIMENSION_ID)) {

            BlockPos pos = event.getPos();
            BlockState newState = event.getNewState();

            // 如果流体生成了石头（比如岩浆遇水），变成泥土
            if (newState.is(BlockTags.BASE_STONE_OVERWORLD)) {
                event.setNewState(Blocks.DIRT.defaultBlockState());
            }
        }
    }

    /**
     * 将区块中的所有石头变成泥土
     */
    private static void replaceAllStoneWithDirt(ChunkAccess chunk) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getMaxBuildHeight();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y < maxY; y++) {
                    pos.set(x, y, z);
                    BlockState state = chunk.getBlockState(pos);

                    if (state.is(BlockTags.BASE_STONE_OVERWORLD)) {
                        chunk.setBlockState(pos, Blocks.DIRT.defaultBlockState(), false);
                    }
                }
            }
        }
    }
}