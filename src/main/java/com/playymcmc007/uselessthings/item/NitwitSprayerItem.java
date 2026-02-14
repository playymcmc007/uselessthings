package com.playymcmc007.uselessthings.item;

import com.playymcmc007.uselessthings.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.List;
import java.util.ArrayList;

public class NitwitSprayerItem extends Item {
    private static final SoundEvent SPRAY_SOUND = SoundEvent.createVariableRangeEvent(
            new ResourceLocation("uselessthings", "sprayer_use")
    );
    public NitwitSprayerItem(Properties properties) {
        super(properties);
    }
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        tooltip.add(Component.translatable("tooltip.uselessthings.nitwit_sprayer.description"));
    }
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {

        ItemStack itemStack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            List<Entity> targetedEntities = getTargetedEntities(player, level);

            int convertedCount = 0;

            for (Entity entity : targetedEntities) {
                if (entity instanceof Villager villager) {
                    if (processVillagerConversion(villager, level)) {
                        convertedCount++;
                    }
                } else if (entity instanceof WanderingTrader trader) {
                    if (processWanderingTrader(trader, level)) {
                        convertedCount++;
                    }
                }
            }
        }
        spawnSprayParticles(player, level);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SPRAY_SOUND, SoundSource.PLAYERS, 0.7F, 0.9F + level.random.nextFloat() * 0.2F);

        if (!player.isCreative()) {
            itemStack.hurtAndBreak(1, player,
                    (p) -> p.broadcastBreakEvent(hand));
        }

        player.getCooldowns().addCooldown(this, 15);

        return InteractionResultHolder.success(itemStack);
    }

    private List<Entity> getTargetedEntities(Player player, Level level) {
        Vec3 eyePosition = player.getEyePosition(1.0F);
        Vec3 lookVector = player.getViewVector(1.0F);
        Vec3 searchEnd = eyePosition.add(lookVector.x * 3, lookVector.y * 3, lookVector.z * 3);

        AABB searchArea = new AABB(eyePosition, searchEnd).inflate(1.5D);

        List<Entity> entities = new ArrayList<>();
        entities.addAll(level.getEntitiesOfClass(Villager.class, searchArea));
        entities.addAll(level.getEntitiesOfClass(WanderingTrader.class, searchArea));

        return entities.stream()
                .filter(entity -> isInSight(eyePosition, lookVector, entity))
                .toList();
    }

    private boolean processVillagerConversion(Villager villager, Level level) {
        if (villager.getVillagerData().getProfession() != VillagerProfession.NITWIT) {
            villager.setVillagerData(villager.getVillagerData().setProfession(VillagerProfession.NITWIT));

            level.playSound(null, villager.getX(), villager.getY(), villager.getZ(),
                    SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.0F);

            return true;
        }
        return false;
    }

    private boolean processWanderingTrader(Entity target, Level level) {

        if (target instanceof WanderingTrader trader) {
            CompoundTag persistentData = trader.getPersistentData();
            String processedKey = "SprayerProcessed";
            if (persistentData.getBoolean(processedKey)) {
                return false;
            }
            persistentData.putBoolean(processedKey, true);

            float chance = Math.round(level.random.nextFloat() * 100) / 100.0f;
            if (chance < 0.75f) {
                trader.kill();
                level.players().forEach(player ->
                        player.displayClientMessage(
                        Component.translatable("message.uselessthings.nitwit_sprayer.death"),
                        false
                ));
                return true;
            } else if (chance < 0.89f) {
                boolean result = convertToCreeper(trader, level);
                if (result) {
                    level.players().forEach(player ->
                            player.displayClientMessage(
                                    Component.translatable("message.uselessthings.nitwit_sprayer.creeper"),
                                    false
                            ));
                }
                return result;
            } else if (chance < 0.90f) {
                boolean result = replaceTrades(trader, level);
                if (result) {
                    level.players().forEach(player ->
                            player.displayClientMessage(
                                    Component.translatable("message.uselessthings.nitwit_sprayer.trades"),
                                    false
                            ));
                }
                return result;
            } else {
                spawnVillagerAngryParticles(trader, level);
                level.playSound(null, trader.getX(), trader.getY(), trader.getZ(),
                        SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.0F, 1.0F);
                level.players().forEach(player ->
                        player.displayClientMessage(
                                Component.translatable("message.uselessthings.nitwit_sprayer.angry"),
                                false
                        ));
                return true;
            }
        }
        return false;
    }

    private boolean convertToCreeper(WanderingTrader trader, Level level) {
        Creeper creeper = new Creeper(EntityType.CREEPER, level);
        creeper.setPos(trader.getX(), trader.getY(), trader.getZ());

        trader.discard();
        level.addFreshEntity(creeper);

        return true;
    }

    private boolean replaceTrades(WanderingTrader trader, Level level) {
        trader.getOffers().clear();
        trader.getOffers().add(new MerchantOffer(
                new ItemStack(Items.EMERALD, 15),
                createPlainWaterBottle(),
                createVillageHeroPotion(),
                1,
                15,
                0.0f
        ));
        trader.getOffers().add(new MerchantOffer(
                new ItemStack(Items.MUSIC_DISC_PIGSTEP),
                new ItemStack(Items.CHORUS_FLOWER, 4),
                new ItemStack(ModItems.ARCHITECTURAL_GREENERY.get()),
                1,
                15,
                0.0f
        ));
        trader.getOffers().add(new MerchantOffer(
                new ItemStack(Items.DRAGON_HEAD),
                new ItemStack(Items.TOTEM_OF_UNDYING),
                new ItemStack(Items.POISONOUS_POTATO),
                5,
                0,
                0.0f
        ));
        spawnVillagerHappyParticles(trader, level);

        level.playSound(null, trader.getX(), trader.getY(), trader.getZ(),
                SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.0F);
        return true;
    }

    private ItemStack createPlainWaterBottle() {
        ItemStack waterBottle = new ItemStack(Items.POTION);
        PotionUtils.setPotion(waterBottle, Potions.WATER);
        return waterBottle;
    }
    private ItemStack createVillageHeroPotion() {
        ItemStack potion = new ItemStack(Items.POTION);

        List<MobEffectInstance> effects = new ArrayList<>();
        effects.add(new MobEffectInstance(
                MobEffects.HERO_OF_THE_VILLAGE,
                600,
                4,
                false,
                true
        ));

        PotionUtils.setCustomEffects(potion, effects);

        CompoundTag tag = potion.getOrCreateTag();
        tag.putBoolean("Uncraftable", true);

        potion.setHoverName(Component.literal("§6村庄英雄药水 V§r"));

        return potion;
    }
    private void spawnVillagerHappyParticles(WanderingTrader trader, Level level) {
        for (int i = 0; i < 20; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 2.0;
            double offsetY = level.random.nextDouble() * 2.0;
            double offsetZ = (level.random.nextDouble() - 0.5) * 2.0;

            Vec3 particlePos = trader.position().add(offsetX, offsetY, offsetZ);

            if (level.isClientSide) {
                level.addParticle(ParticleTypes.HAPPY_VILLAGER,
                        particlePos.x, particlePos.y, particlePos.z,
                        0, 0, 0);
            } else {
                ((ServerLevel) level).sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        particlePos.x, particlePos.y, particlePos.z,
                        1, 0, 0, 0, 0);
            }
        }
    }
    private void spawnVillagerAngryParticles(WanderingTrader trader, Level level) {
        for (int i = 0; i < 15; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 2.0;
            double offsetY = level.random.nextDouble() * 2.0;
            double offsetZ = (level.random.nextDouble() - 0.5) * 2.0;

            Vec3 particlePos = trader.position().add(offsetX, offsetY, offsetZ);

            if (level.isClientSide) {
                level.addParticle(ParticleTypes.ANGRY_VILLAGER,
                        particlePos.x, particlePos.y, particlePos.z,
                        0, 0, 0);
            } else {
                ((ServerLevel) level).sendParticles(ParticleTypes.ANGRY_VILLAGER,
                        particlePos.x, particlePos.y, particlePos.z,
                        1, 0, 0, 0, 0);
            }
        }
    }
    private void spawnSprayParticles(Player player, Level level) {
        Vec3 playerPos = player.getEyePosition(1.0F);

        Vec3 lookVector = player.getViewVector(1.0F);
        Vec3 direction = lookVector.normalize();

        double rayLength = 3.0D;
        Vec3 rayEnd = playerPos.add(direction.x * rayLength, direction.y * rayLength, direction.z * rayLength);

        SimpleParticleType particleType = ParticleTypes.RAIN;

        int particleCount = 25;

        for (int i = 0; i < particleCount; i++) {
            double t = level.random.nextDouble();
            Vec3 particlePos = new Vec3(
                    playerPos.x + (rayEnd.x - playerPos.x) * t,
                    playerPos.y + (rayEnd.y - playerPos.y) * t,
                    playerPos.z + (rayEnd.z - playerPos.z) * t
            );

            double spread = 0.15;
            particlePos = particlePos.add(
                    (level.random.nextDouble() - 0.5) * spread,
                    (level.random.nextDouble() - 0.5) * spread,
                    (level.random.nextDouble() - 0.5) * spread
            );

            Vec3 velocity = new Vec3(
                    (level.random.nextDouble() - 0.5) * 0.02,
                    -0.1 - level.random.nextDouble() * 0.05,
                    (level.random.nextDouble() - 0.5) * 0.02
            );

            if (level.isClientSide) {
                level.addParticle(particleType,
                        particlePos.x, particlePos.y, particlePos.z,
                        velocity.x, velocity.y, velocity.z);
            } else {
                ((ServerLevel) level).sendParticles(particleType,
                        particlePos.x, particlePos.y, particlePos.z,
                        1, velocity.x, velocity.y, velocity.z, 0.0);
            }
        }
    }

    private boolean isInSight(Vec3 eyePos, Vec3 lookVector, Entity entity) {
        Vec3 toEntity = entity.position().subtract(eyePos).normalize();
        double dotProduct = lookVector.dot(toEntity);
        return dotProduct > 0.7D;
    }
}