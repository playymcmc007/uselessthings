package com.playymcmc007.uselessthings.block;
import com.playymcmc007.uselessthings.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
public class ArchitecturalGreeneryBlock extends Block implements BonemealableBlock {
    private static final double BRANCH_CHANCE = 0.4;
    private static final double FLOWER_CHANCE = 0.01;
    public static final BooleanProperty WITHERED = BooleanProperty.create("withered");
    private static final int WITHER_DELAY = 100;
    public static final IntegerProperty GROWTH_TIME_LEVEL = IntegerProperty.create("growth_time_level", 0, 30);
    public ArchitecturalGreeneryBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(WITHERED, false).setValue(GROWTH_TIME_LEVEL, 0));
    }
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GROWTH_TIME_LEVEL);
        builder.add(WITHERED);
    }
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide) {
            checkWitherConditions((ServerLevel) level, pos, state);
        }
    }
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide) {
            checkWitherConditions((ServerLevel) level, pos, state);
        }
    }
    private void checkWitherConditions(ServerLevel level, BlockPos pos, BlockState state) {
        boolean isIsolated = isIsolated(level, pos);
        boolean isNight = isNightTime(level);
        boolean isWithered = state.getValue(WITHERED);
        boolean shouldWither = isIsolated || isNight;
        if (shouldWither && !isWithered) {
            BlockState witheredState = state.setValue(WITHERED, true)
                    .setValue(GROWTH_TIME_LEVEL, 0);
            level.setBlock(pos, witheredState, 3);
            level.scheduleTick(pos, this, WITHER_DELAY);
        } else if (!shouldWither && isWithered) {
            level.setBlock(pos, state.setValue(WITHERED, false), 3);
        }
    }
    private boolean isIsolated(ServerLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            if (!level.isEmptyBlock(neighborPos)) {
                return false;
            }
        }
        return true;
    }
    private boolean isNightTime(ServerLevel level) {
        long dayTime = level.getDayTime() % 24000;
        return dayTime > 13000 && dayTime < 23000;
    }
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(WITHERED)) {
            return InteractionResult.PASS;
        }
        ItemStack itemStack = player.getItemInHand(hand);
        if (itemStack.getItem() == Items.NETHER_STAR) {
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(GROWTH_TIME_LEVEL, 30), 3);
                level.scheduleTick(pos, this, 20);
                level.playSound(null, pos,
                        net.minecraft.sounds.SoundEvents.GRASS_PLACE,
                        net.minecraft.sounds.SoundSource.BLOCKS,
                        1.0F, 1.0F);
                if (!player.isCreative()) {
                    itemStack.shrink(1);
                }
                player.swing(hand);
                return InteractionResult.SUCCESS;
            }
        }
        if (itemStack.getItem() instanceof BoneMealItem) {
            if (this.isValidBonemealTarget(level, pos, state, level.isClientSide)) {
                if (!level.isClientSide) {
                    this.performBonemeal((ServerLevel) level, level.random, pos, state);
                    ((ServerLevel) level).sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            10,
                            0.5,
                            0.5,
                            0.5,
                            0.1
                    );
                    level.playSound(null, pos,
                            net.minecraft.sounds.SoundEvents.BONE_MEAL_USE,
                            net.minecraft.sounds.SoundSource.BLOCKS,
                            1.0F, 1.0F);
                    if (!player.isCreative()) {
                        itemStack.shrink(1);
                    }
                }
                player.swing(hand);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(WITHERED)) {
            level.removeBlock(pos, false);
            return;
        }
        int timeLevel = state.getValue(GROWTH_TIME_LEVEL);
        checkWitherConditions(level, pos, state);
        if (level.getBlockState(pos).getValue(WITHERED)) {
            return;
        }
        if (timeLevel > 0) {
            int newTimeLevel = timeLevel - 1;
            level.setBlock(pos, state.setValue(GROWTH_TIME_LEVEL, newTimeLevel), 3);
            double growChance = 0.6 + (timeLevel / 15.0) * 0.3;
            if (random.nextDouble() < growChance) {
                this.growPlant(level, pos, random, timeLevel);
                level.playSound(null, pos,
                        net.minecraft.sounds.SoundEvents.GRASS_PLACE,
                        net.minecraft.sounds.SoundSource.BLOCKS,
                        1.0F, 1.0F);
            }
            if (newTimeLevel > 0) {
                level.scheduleTick(pos, this, 20);
            }
        }
    }
    private void growPlant(ServerLevel level, BlockPos pos, RandomSource random, int parentTimeLevel) {
        tryGrowUpwards(level, pos, parentTimeLevel, random);
    }
    private boolean isGrowthPathBlocked(ServerLevel level, BlockPos startPos, BlockPos endPos) {
        if (startPos.equals(endPos)) {
            return false;
        }

        int dx = Integer.signum(endPos.getX() - startPos.getX());
        int dy = Integer.signum(endPos.getY() - startPos.getY());
        int dz = Integer.signum(endPos.getZ() - startPos.getZ());

        int distanceX = Math.abs(endPos.getX() - startPos.getX());
        int distanceY = Math.abs(endPos.getY() - startPos.getY());
        int distanceZ = Math.abs(endPos.getZ() - startPos.getZ());

        int maxDistance = Math.max(Math.max(distanceX, distanceY), distanceZ);

        BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();
        for (int i = 1; i < maxDistance; i++) {
            int x = startPos.getX() + dx * Math.min(i, distanceX);
            int y = startPos.getY() + dy * Math.min(i, distanceY);
            int z = startPos.getZ() + dz * Math.min(i, distanceZ);
            checkPos.set(x, y, z);
            if (!checkPos.equals(startPos) && !checkPos.equals(endPos)) {
                if (!canGrowInto(level, checkPos)) {
                    return true;
                }
            }
        }

        return false;
    }
    private void tryGrowUpwards(ServerLevel level, BlockPos pos, int parentTimeLevel, RandomSource random) {
        int growDistance = /*random.nextInt(3) +*/ 1;

        BlockPos currentPos = pos;
        for (int i = 1; i <= growDistance; i++) {
            BlockPos nextPos = currentPos.above();

            if (!isGrowthPathBlocked(level, currentPos, nextPos) && canGrowInto(level, nextPos)) {
                int inheritedTimeLevel = Math.max(0, parentTimeLevel - 1);
                level.setBlock(nextPos, ModBlocks.ARCHITECTURAL_GREENERY.get()
                        .defaultBlockState()
                        .setValue(GROWTH_TIME_LEVEL, inheritedTimeLevel), 3);
                if (inheritedTimeLevel > 0) {
                    level.scheduleTick(nextPos, this, 20);
                }

                if (random.nextDouble() < BRANCH_CHANCE) {
                    tryCreateBranch(level, nextPos, inheritedTimeLevel, random);
                }
                if (random.nextDouble() < FLOWER_CHANCE) {
                    tryCreateArchitecturalFlower(level, nextPos);
                }

                currentPos = nextPos;
            } else {
                break;
            }
        }
    }
    private void tryCreateBranch(ServerLevel level, BlockPos pos, int parentTimeLevel, RandomSource random) {
        Direction[] directions = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        for (Direction dir : directions) {
            if (random.nextDouble() < 0.3) {
                int branchDistance = random.nextInt(3) + 1;

                BlockPos currentPos = pos;
                for (int i = 1; i <= branchDistance; i++) {
                    BlockPos nextPos = currentPos.relative(dir);

                    if (!isGrowthPathBlocked(level, currentPos, nextPos) && canGrowInto(level, nextPos)) {
                        int branchTimeLevel = Math.max(0, parentTimeLevel);
                        level.setBlock(nextPos, ModBlocks.ARCHITECTURAL_GREENERY.get()
                                .defaultBlockState()
                                .setValue(GROWTH_TIME_LEVEL, branchTimeLevel), 3);
                        if (branchTimeLevel > 0) {
                            level.scheduleTick(nextPos, this, 20);
                        }
                        currentPos = nextPos;
                    } else {
                        break;
                    }
                }
            }
        }
    }
    private void tryCreateArchitecturalFlower(ServerLevel level, BlockPos pos) {
        BlockPos flowerPos = pos.above();
        if (canGrowInto(level, flowerPos)) {
            level.setBlock(flowerPos, ModBlocks.ARCHITECTURAL_FLOWER.get().defaultBlockState(), 3);
        }
    }
    private boolean canGrowInto(ServerLevel level, BlockPos pos) {
        BlockState existingState = level.getBlockState(pos);
        return existingState.isAir() ||
                existingState.getFluidState().isSource() ||
                existingState.canBeReplaced() ||
                !existingState.isSolid();
    }
    public int getColor(BlockState state) {
        int growthLevel = state.getValue(GROWTH_TIME_LEVEL);
        return getGreenGradientColor(growthLevel);
    }
    private int getGreenGradientColor(int growthLevel) {
        if (growthLevel <= 0) return 0xFFFFFF;
        float factor = Math.min(1.0f, growthLevel / 30.0f);
        int rgbReduction = (int)(255 * factor);
        int red = 255 - rgbReduction;
        int green = 255;
        int blue = 255 - rgbReduction;
        return (red << 16) | (green << 8) | blue;
    }
    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, boolean isClient) {
        return true;
    }
    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }
    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        this.growPlant(level, pos, random, 1);
    }
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.randomTick(state, level, pos, random);
        if (random.nextDouble() < 0.3) {
            checkWitherConditions(level, pos, state);
        }
        if (!state.getValue(WITHERED) && random.nextDouble() < 0.2) {
            this.growPlant(level, pos, random, 1);
        }
    }
    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.core.Direction direction) {
        return 100;
    }
    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, net.minecraft.core.Direction direction) {
        return 300;
    }
    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }
}