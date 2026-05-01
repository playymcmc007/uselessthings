package com.playymcmc007.uselessthings.block;

import com.playymcmc007.uselessthings.ModItems;
import com.playymcmc007.uselessthings.world.feature.GiantTreeFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class SuperSaplingBlock extends Block {

    public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.values());
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 3);

    protected static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 12.0, 14.0);

    public SuperSaplingBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.UP)
                .setValue(AGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, AGE);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        BlockPos clickedPos = context.getClickedPos();
        BlockPos placePos = clickedPos.relative(clickedFace);

        if (!context.getLevel().getBlockState(placePos).canBeReplaced(context)) {
            return null;
        }

        return this.defaultBlockState().setValue(FACING, clickedFace);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos attachedPos = pos.relative(facing.getOpposite());
        BlockState attachedState = level.getBlockState(attachedPos);

        if (!attachedState.isFaceSturdy(level, attachedPos, facing)) {
            return false;
        }

        Block attachedBlock = attachedState.getBlock();
        return attachedBlock == Blocks.GRASS_BLOCK ||
                attachedBlock == Blocks.DIRT ||
                attachedBlock == Blocks.COARSE_DIRT ||
                attachedBlock == Blocks.PODZOL ||
                attachedBlock == Blocks.FARMLAND ||
                attachedBlock == Blocks.MOSS_BLOCK ||
                attachedBlock == Blocks.ROOTED_DIRT;
    }

    // ========== 右键交互（使用超级骨粉） ==========
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        ItemStack itemStack = player.getItemInHand(hand);

        if (itemStack.getItem() == ModItems.SUPER_BONE_MEAL.get()) {
            if (level.isClientSide) {
                for (int i = 0; i < 30; i++) {
                    double offsetX = level.random.nextDouble() - 0.5;
                    double offsetY = level.random.nextDouble() - 0.5;
                    double offsetZ = level.random.nextDouble() - 0.5;
                    level.addParticle(
                            net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                            pos.getX() + 0.5 + offsetX,
                            pos.getY() + 0.5 + offsetY,
                            pos.getZ() + 0.5 + offsetZ,
                            0, 0, 0
                    );
                }
            } else {
                // 服务端：播放音效
                level.playSound(
                        null,
                        pos,
                        SoundEvents.BONE_MEAL_USE,
                        SoundSource.BLOCKS,
                        1.0f,
                        1.0f
                );
            }

            if (!level.isClientSide) {
                ServerLevel serverLevel = (ServerLevel) level;

                if (!player.isCreative()) {
                    itemStack.shrink(1);
                }

                int age = state.getValue(AGE);
                if (age < 3) {
                    level.setBlock(pos, state.setValue(AGE, age + 1), 3);
                } else {
                    growTree(serverLevel, pos, state, RandomSource.create());
                }
            }

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    // ========== 大树生长 ==========
    private void growTree(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        level.playSound(
                null,
                pos,
                SoundEvents.WITHER_SPAWN,
                SoundSource.BLOCKS,
                1.0f,
                0.5f
        );
        GiantTreeFeature feature = new GiantTreeFeature(NoneFeatureConfiguration.CODEC);

        FeaturePlaceContext<NoneFeatureConfiguration> context = new FeaturePlaceContext<>(
                java.util.Optional.empty(),
                level,
                null,
                random,
                pos,
                NoneFeatureConfiguration.INSTANCE
        );

        feature.place(context);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        Direction facing = state.getValue(FACING);
        if (direction == facing.getOpposite() && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return state;
    }
}