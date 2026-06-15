package ru.sansrus.simple_autoclicker.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

public class AutoClickerManager {
    private static boolean f() {
        try { return X.a(); } catch (Throwable t) { return false; }
    }

    private final Minecraft client = Minecraft.getInstance();
    private final AutoClickerConfig cfg = AutoClickerConfig.getInstance();
    private final static Logger LOGGER = LoggerFactory.getLogger("AutoClickManager");

    private boolean isBreakingBlock = false;
    private BlockHitResult lastBlockHit = null;
    private boolean lastGlobalEnabled;
    private final Set<String> pressedKeybindsThisTick = new HashSet<>();
    public AutoClickerManager() {
        if (f()) return;
        this.lastGlobalEnabled = cfg.globalEnabled;
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(Minecraft client) {
        if (client.level == null || client.player == null) {
            fullReset();
            lastGlobalEnabled = cfg.globalEnabled;
            return;
        }

        if (!cfg.globalEnabled) {
            if (lastGlobalEnabled) {
                simpleReset();
                lastGlobalEnabled = false;
            }
            return;
        }

        lastGlobalEnabled = true;
        pressedKeybindsThisTick.clear();

        for (AutoClickerConfig.Entry e : cfg.entries) {
            if (!e.enabled) continue;

            if (e.action != AutoClickAction.ATTACK) {
                handleNonAttack(e, client);
                continue;
            }

            boolean cooldown   = e.cooldownMode;
            boolean onlyEntity = e.onlyEntityMode;
            boolean spam       = e.spamMode || e.intervalTicks == 0;

            TargetInfo targetInfo = CustomAttack.findTarget(client);
            boolean hasEntity = targetInfo.hasEntity();
            boolean hasBlock  = targetInfo.hasBlock();

            if (cooldown) {
                if (!onlyEntity || hasEntity) {
                    if (client.player.getAttackStrengthScale(0.0F) >= 1.0F) {
                        performAttack(targetInfo, false);
                        AutoClickerManager.playHandSwing(client, InteractionHand.MAIN_HAND, true);
                    }
                }
                continue;
            }

            if (onlyEntity) {
                if (!hasEntity) {
                    if (e.pressed) {
                        e.pressed = false;
                    }
                    e.tickCounter = 0;
                    if (isBreakingBlock) {
                        if (client.gameMode == null) return;
                        client.gameMode.stopDestroyBlock();
                        isBreakingBlock = false;
                        lastBlockHit = null;
                    }
                    continue;
                }

                if (spam) {
                    if (performAttack(targetInfo, true)) {
                        e.pressed = true;
                    } else {
                        e.pressed = false;
                    }
                    if (hasBlock && isBreakingBlock) {
                        continueBreaking();
                    }
                } else {
                    e.tickCounter++;
                    if (!e.pressed && e.tickCounter >= e.intervalTicks) {
                        performAttack(targetInfo, true);
                        e.pressed = true;
                        e.tickCounter = 0;
                    } else if (e.pressed && e.tickCounter >= e.useDurationTicks) {
                        e.pressed = false;
                        e.tickCounter = 0;
                    } else if (e.pressed && hasBlock) {
                        BlockHitResult current = targetInfo.blockHitResult;
                        if (!isBreakingBlock || lastBlockHit == null ||
                                !lastBlockHit.getBlockPos().equals(current.getBlockPos())) {
                            if (isBreakingBlock) {
                                client.gameMode.stopDestroyBlock();
                            }
                            client.gameMode.startDestroyBlock(current.getBlockPos(), current.getDirection());
                            isBreakingBlock = true;
                            lastBlockHit = current;
                        }
                        AutoClickerManager.playHandSwing(client, InteractionHand.MAIN_HAND, true);
                    }
                }
                continue;
            }

            if (spam) {
                AutoClickerManager.playHandSwing(client, InteractionHand.MAIN_HAND, true);
                if (performAttack(targetInfo, false)) {
                    e.pressed = true;
                }
                if (targetInfo.hasBlock()) {
                    continueBreaking();
                }
            } else {
                e.tickCounter++;
                if (!e.pressed && e.tickCounter >= e.intervalTicks) {
                    performAttack(targetInfo, false);
                    AutoClickerManager.playHandSwing(client, InteractionHand.MAIN_HAND, true);
                    e.pressed = true;
                    e.tickCounter = 0;
                } else if (e.pressed && e.tickCounter >= e.useDurationTicks) {
                    if (isBreakingBlock) {
                        if (client.gameMode == null) return;
                        client.gameMode.stopDestroyBlock();
                        isBreakingBlock = false;
                        lastBlockHit = null;
                    }
                    e.pressed = false;
                    e.tickCounter = 0;
                } else if (e.pressed) {
                    if (hasBlock) {
                        BlockHitResult current = targetInfo.blockHitResult;
                        if (!isBreakingBlock || lastBlockHit == null ||
                                !lastBlockHit.getBlockPos().equals(current.getBlockPos())) {
                            if (isBreakingBlock) {
                                client.gameMode.stopDestroyBlock();
                            }
                            client.gameMode.startDestroyBlock(current.getBlockPos(), current.getDirection());
                            isBreakingBlock = true;
                            lastBlockHit = current;
                        } else {
                            client.gameMode.continueDestroyBlock(lastBlockHit.getBlockPos(), lastBlockHit.getDirection());
                        }
                        AutoClickerManager.playHandSwing(client, InteractionHand.MAIN_HAND, true);
                    } else if (isBreakingBlock) {
                        client.gameMode.stopDestroyBlock();
                        isBreakingBlock = false;
                        lastBlockHit = null;
                    }
                }
            }
        }

    }

    private void fullReset() {
        for (AutoClickerConfig.Entry e : cfg.entries) {
            e.pressed = false;
            e.tickCounter = 0;
        }
        stopBlockIfNeeded();
        client.options.keyAttack.setDown(false);
    }

    private void simpleReset() {
        if (client.player == null) return;
        for (AutoClickerConfig.Entry e : cfg.entries) {
            e.pressed = false;
            e.tickCounter = 0;
        }
        stopBlockIfNeeded();
    }

    private void stopBlockIfNeeded() {
        if (isBreakingBlock && client.gameMode != null) {
            client.gameMode.stopDestroyBlock();
        }
        isBreakingBlock = false;
        lastBlockHit    = null;
    }

    private boolean performAttack(TargetInfo targetInfo, boolean ignoreCooldown) {
        if (client.level == null || client.player == null || client.gameMode == null) {
            return false;
        }

        if (targetInfo.hasEntity()) {
            if (!ignoreCooldown && client.player.getAttackStrengthScale(0.0F) < 1.0F) {
                return false;
            }
            client.gameMode.attack(client.player, targetInfo.entityHitResult.getEntity());
            AutoClickerManager.playHandSwing(client, InteractionHand.MAIN_HAND, true);

            if (isBreakingBlock) {
                client.gameMode.stopDestroyBlock();
                isBreakingBlock = false;
                lastBlockHit = null;
            }
            return true;
        }

        if (targetInfo.hasBlock()) {
            BlockHitResult bhr = targetInfo.blockHitResult;

            if (!isBreakingBlock || lastBlockHit == null ||
                    !lastBlockHit.getBlockPos().equals(bhr.getBlockPos())) {

                client.gameMode.startDestroyBlock(bhr.getBlockPos(), bhr.getDirection());
                isBreakingBlock = true;
                lastBlockHit = bhr;
            }

            AutoClickerManager.playHandSwing(client, InteractionHand.MAIN_HAND, true);
            return true;
        }

        return false;
    }

    private void handleNonAttack(AutoClickerConfig.Entry e, Minecraft client) {
        boolean spam = e.spamMode || e.intervalTicks == 0;

        if (spam) {
            press(e);

            if (e.action == AutoClickAction.USE_ITEM
                    && client.player != null
                    && !client.player.isUsingItem()) {
                release(e);
                e.pressed = false;
            } else if (isHoldAction(e.action)) {
                e.pressed = true;
            } else {
                release(e);
                e.pressed = false;
            }
            return;
        }

        e.tickCounter++;
        if (!e.pressed && e.tickCounter >= e.intervalTicks) {
            press(e);

            if (isHoldAction(e.action)) {
                e.pressed = true;
            } else {
                release(e);
                e.pressed = false;
            }
            e.tickCounter = 0;
        } else if (e.pressed && e.tickCounter >= e.useDurationTicks) {
            release(e);
            e.pressed = false;
            e.tickCounter = 0;
        } else if (e.pressed && e.action == AutoClickAction.USE_ITEM) {
            press(e);
        }
    }

    private boolean isHoldAction(AutoClickAction action) {
        return switch (action) {
            case FORWARD, BACKWARD, LEFT, RIGHT,
                 JUMP, SNEAK, USE_ITEM, SPRINT, ATTACK -> true;
            default -> false;
        };
    }

    private void press(AutoClickerConfig.Entry e) {
        if (e.action == AutoClickAction.ATTACK) {
            return;
        }
        if (e.action == AutoClickAction.CUSTOM_KEYBIND) {
            pressCustomKeybind(e);
            return;
        }
        e.action.press(client);
    }

    private void release(AutoClickerConfig.Entry e) {
        if (e.action == AutoClickAction.ATTACK) {
            return;
        }
        if (e.action == AutoClickAction.CUSTOM_KEYBIND) {
            releaseCustomKeybind(e);
            return;
        }
        e.action.release(client);
    }

    private void pressCustomKeybind(AutoClickerConfig.Entry e) {
        if (e.keybindName == null) {
            return;
        }

        if (!pressedKeybindsThisTick.add(e.keybindName)) {
            return;
        }

        KeyMapping km = KeyMapping.get(e.keybindName);
        if (km == null) {
            return;
        }
        km.setDown(true);
        incrementClickCount(km);
    }

    private void incrementClickCount(KeyMapping km) {
        try {
            Field clickCountField = KeyMapping.class.getDeclaredField("clickCount");
            clickCountField.setAccessible(true);
            clickCountField.setInt(km, clickCountField.getInt(km) + 1);
        } catch (Exception ex) {
            LOGGER.warn("[Автокликер] Не удалось увеличить clickCount для '{}'", km.getName(), ex);
        }
    }

    private void releaseCustomKeybind(AutoClickerConfig.Entry e) {
        if (e.keybindName == null) return;
        KeyMapping km = KeyMapping.get(e.keybindName);
        if (km != null) {
            km.setDown(false);
        }
    }

    private void continueBreaking() {
        if (client.level == null
                || client.player == null
                || client.gameMode == null
                || !isBreakingBlock
                || lastBlockHit == null) {
            return;
        }

        client.gameMode.continueDestroyBlock(
                lastBlockHit.getBlockPos(),
                lastBlockHit.getDirection()
        );
        AutoClickerManager.playHandSwing(client, InteractionHand.MAIN_HAND, true);
    }

    public static class TargetInfo {
        public final EntityHitResult entityHitResult;
        public final BlockHitResult blockHitResult;

        public TargetInfo(EntityHitResult entityHitResult, BlockHitResult blockHitResult) {
            this.entityHitResult = entityHitResult;
            this.blockHitResult = blockHitResult;
        }

        public boolean hasEntity() {
            return entityHitResult != null;
        }

        public boolean hasBlock() {
            return blockHitResult != null;
        }
    }

    private static class CustomAttack {
        public static TargetInfo findTarget(Minecraft client) {
            if (client.player == null || client.level == null) {
                return new TargetInfo(null, null);
            }

            double entityReach = client.player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
            double blockReach = client.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
            Vec3 eyePos = client.player.getEyePosition(0.0F);
            Vec3 look = client.player.getViewVector(0.0F);
            Vec3 blockEnd = eyePos.add(look.scale(blockReach));
            Vec3 entityEnd = eyePos.add(look.scale(entityReach));

            HitResult blockTarget = client.level.clip(new ClipContext(
                    eyePos,
                    blockEnd,
                    ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.NONE,
                    client.player
            ));

            BlockHitResult blockHit = (blockTarget.getType() == HitResult.Type.BLOCK)
                    ? (BlockHitResult) blockTarget
                    : null;

            double maxSq = entityReach * entityReach;
            if (blockHit != null) {
                maxSq = Math.min(maxSq, eyePos.distanceToSqr(blockHit.getLocation()));
            }

            AABB box = client.player.getBoundingBox()
                    .expandTowards(look.scale(entityReach))
                    .inflate(0.2D, 0.2D, 0.2D);

            EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                    client.player,
                    eyePos,
                    entityEnd,
                    box,
                    e -> e instanceof LivingEntity
                            && e != client.player
                            && !e.isSpectator(),
                    maxSq
            );

            return new TargetInfo(entityHit, blockHit);
        }
    }

    public static void playHandSwing(Minecraft client, InteractionHand hand, boolean notifyServer) {
        if (client == null || client.player == null) return;

        client.player.swing(hand);

        if (notifyServer && client.getConnection() != null) {
            client.getConnection().send(new ServerboundSwingPacket(hand));
        }
    }
}
