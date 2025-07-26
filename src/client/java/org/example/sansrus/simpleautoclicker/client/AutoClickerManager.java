package org.example.sansrus.simpleautoclicker.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Optional;

public class AutoClickerManager {
    private final MinecraftClient client = MinecraftClient.getInstance();
    private final AutoClickerConfig cfg = AutoClickerConfig.getInstance();

    // Переменные для отслеживания состояния импульсного нажатия
    private boolean needsAttackPress = false;
    private boolean needsAttackRelease = false;
    // Переменные для отслеживания состояния ломания блока
    private boolean isBreakingBlock = false;
    private BlockHitResult lastBlockHit = null;

    public AutoClickerManager() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(MinecraftClient client) {
        if (client.world == null || client.player == null) {
            fullReset();
            return;
        }

        if (!cfg.globalEnabled) {
            simpleReset();
            return;
        }

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

            // ——— Режим КУЛДАУНА ———
            if (cooldown) {
                if (!onlyEntity || hasEntity) {
                    if (client.player.getAttackCooldownProgress(0.0F) >= 1.0F) {
                        performAttack(targetInfo);
                    }
                }
                continue;
            }

            // ——— Режим ТОЛЬКО-СУЩНОСТИ ———
            if (onlyEntity) {
                if (!hasEntity) {
                    if (e.pressed) {
                        e.pressed = false;
                    }
                    e.tickCounter = 0;
                    // Остановить ломание блока, если начато
                    if (isBreakingBlock) {
                        client.interactionManager.cancelBlockBreaking();
                        isBreakingBlock = false;
                        lastBlockHit = null;
                    }
                    continue;
                }

                if (spam) {
                    performAttack(targetInfo);
                    e.pressed = true;
                    if (hasBlock) {
                        continueBreaking();
                    }
                } else {
                    e.tickCounter++;
                    if (!e.pressed && e.tickCounter >= e.intervalTicks) {
                        performAttack(targetInfo);
                        e.pressed = true;
                        e.tickCounter = 0;
                    } else if (e.pressed && e.tickCounter >= e.useDurationTicks) {
                        e.pressed = false;
                        e.tickCounter = 0;
                    }
                }
                continue;
            }

            // ——— ОБЫЧНЫЙ РЕЖИМ (можно бить и сущности, и блоки) ———
            if (spam) {
                performAttack(targetInfo);
                e.pressed = true;
                if (targetInfo.hasBlock()) {
                    continueBreaking();
                }
            } else {
                e.tickCounter++;
                if (!e.pressed && e.tickCounter >= e.intervalTicks) {
                    performAttack(targetInfo);
                    e.pressed = true;
                    e.tickCounter = 0;
                } else if (e.pressed && e.tickCounter >= e.useDurationTicks) {
                    // Остановить ломание, если было
                    if (isBreakingBlock) {
                        client.interactionManager.cancelBlockBreaking();
                        isBreakingBlock = false;
                        lastBlockHit = null;
                    }
                    e.pressed = false;
                    e.tickCounter = 0;
                } else if (e.pressed && hasBlock) {
                    continueBreaking();
                }
            }
        }
    }

    /**
     * Полный сброс: и внутренние флаги, и сброс нажатий всех клавиш (использовался до этого).
     */
    private void fullReset() {
        for (AutoClickerConfig.Entry e : cfg.entries) {
            e.pressed = false;
            e.tickCounter = 0;
        }
        stopBlockIfNeeded();
        // **ЗДЕСЬ** сбрасываем состояние клавиши атаки
        client.options.attackKey.setPressed(false);
    }

    /**
     * Упрощённый сброс при отключении мода:
     * — сбрасываем свои флаги и прерываем ломание, но НЕ сбрасываем attackKey.
     */
    private void simpleReset() {
        for (AutoClickerConfig.Entry e : cfg.entries) {
            e.pressed = false;
            e.tickCounter = 0;
        }
        stopBlockIfNeeded();
    }

    /**
     * Общий метод отмены текущего ломания блока.
     */
    private void stopBlockIfNeeded() {
        if (isBreakingBlock && client.interactionManager != null) {
            client.interactionManager.cancelBlockBreaking();
        }
        isBreakingBlock = false;
        lastBlockHit    = null;
    }


    /**
     * Выполняет мгновенную атаку по сущности или начало ломания блока на сервере.
     */
    private void performAttack(TargetInfo targetInfo) {
        if (client.world == null || client.player == null || client.interactionManager == null) {
            return;
        }

        // Атакуем сущность
        if (targetInfo.hasEntity()) {
            client.interactionManager.attackEntity(
                    client.player,
                    targetInfo.entityHitResult.getEntity()
            );
            // Отменяем любое текущее ломание блока
            if (isBreakingBlock) {
                client.interactionManager.cancelBlockBreaking();
                isBreakingBlock = false;
                lastBlockHit = null;
            }
            return;
        }

        // Начинаем ломание блока
        if (targetInfo.hasBlock()) {
            BlockHitResult bhr = targetInfo.blockHitResult;
            client.interactionManager.attackBlock(
                    bhr.getBlockPos(),
                    bhr.getSide()
            );
            isBreakingBlock = true;
            lastBlockHit   = bhr;
            return;
        }
    }

    private void handleNonAttack(AutoClickerConfig.Entry e, MinecraftClient client) {
        boolean spam = e.spamMode || e.intervalTicks == 0;
        if (spam) {
            press(e);
            e.pressed = true;
        } else {
            e.tickCounter++;
            if (!e.pressed && e.tickCounter >= e.intervalTicks) {
                press(e);
                e.pressed     = true;
                e.tickCounter = 0;
            } else if (e.pressed && e.tickCounter >= e.useDurationTicks) {
                release(e);
                e.pressed     = false;
                e.tickCounter = 0;
            }
        }
    }

    private void press(AutoClickerConfig.Entry e) {
        if (e.action == AutoClickAction.ATTACK) {
            // Вся логика теперь в scheduleAttack и onTick
        } else {
            KeyBinding key = map(e.action);
            if (key != null) key.setPressed(true);
        }
    }

    private void release(AutoClickerConfig.Entry e) {
        if (e.action == AutoClickAction.ATTACK) {
            // nothing
        } else {
            KeyBinding key = map(e.action);
            if (key != null) key.setPressed(false);
        }
    }

    private KeyBinding map(AutoClickAction action) {
        return switch (action) {
            case FORWARD -> client.options.forwardKey;
            case BACKWARD -> client.options.backKey;
            case LEFT -> client.options.leftKey;
            case RIGHT -> client.options.rightKey;
            case JUMP -> client.options.jumpKey;
            case SNEAK -> client.options.sneakKey;
            case USE_ITEM -> client.options.useKey;
            default -> null;
        };
    }

    private void resetAll() {
        for (AutoClickerConfig.Entry e : cfg.entries) {
            if (e.pressed) {
                if (e.action != AutoClickAction.ATTACK) {
                    KeyBinding key = map(e.action);
                    if (key != null) key.setPressed(false);
                }
                e.pressed = false;
                e.tickCounter = 0;
            }
        }
        // Убедимся, что клавиша атаки отпущена
        if (needsAttackPress || needsAttackRelease || client.options.attackKey.isPressed()) {
            client.options.attackKey.setPressed(false);
            needsAttackPress = false;
            needsAttackRelease = false;
        }
        // Отменяем ломание блока при сбросе
        if (client.interactionManager != null && isBreakingBlock) {
            client.interactionManager.cancelBlockBreaking();
        }
        isBreakingBlock = false;
        lastBlockHit = null;
    }

    /**
     * Продолжает ломание уже начатого блока, вызывая updateBlockBreakingProgress.
     */
    private void continueBreaking() {
        if (client.world == null
                || client.player == null
                || client.interactionManager == null
                || !isBreakingBlock
                || lastBlockHit == null) {
            return;
        }

        // 1) обновляем прогресс ломания
        client.interactionManager.updateBlockBreakingProgress(
                lastBlockHit.getBlockPos(),
                lastBlockHit.getSide()
        );
        // 2) посылаем мах рукой — без этого сервер не «продолжит» ломать
        client.player.networkHandler.sendPacket(
                new HandSwingC2SPacket(Hand.MAIN_HAND)
        );
    }

    // Вспомогательный класс для передачи информации о цели
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
        // Большая часть логики перенесена в onTick и scheduleAttack
        // Оставляем только findTarget для определения цели

        /**
         * Находит ближайшую цель (сущность или блок) для атаки.
         * Использует комбинацию raycast для блоков и поиска сущностей в области для точности.
         */
        public static TargetInfo findTarget(MinecraftClient client) {
            if (client.player == null || client.world == null) {
                return new TargetInfo(null, null);
            }

            double reach = client.player.getAttributeValue(EntityAttributes.ENTITY_INTERACTION_RANGE);
            // 1) Блоковый рэйкаст
            HitResult rawHit = client.player.raycast(reach, 0.0F, false);
            BlockHitResult blockHit = rawHit instanceof BlockHitResult bhr ? bhr : null;
            double blockDistSq = blockHit != null
                    ? client.player.getCameraPosVec(0.0F).squaredDistanceTo(blockHit.getPos())
                    : Double.MAX_VALUE;

            // 2) Собираем энтити в зоне перед игроком
            Vec3d eyePos = client.player.getCameraPosVec(0.0F);
            Vec3d look   = client.player.getRotationVec(0.0F);
            Vec3d end    = eyePos.add(look.multiply(reach));
            Box searchBox = client.player.getBoundingBox()
                    .stretch(look.multiply(reach))
                    .expand(1.0D, 1.0D, 1.0D);
            double closestEntityDistSq = Double.MAX_VALUE;
            EntityHitResult closestEntityHit = null;

            for (LivingEntity ent : client.world.getEntitiesByClass(LivingEntity.class, searchBox,
                    e -> e != client.player && !e.isSpectator())) {
                // Расширяем немного box, чтобы «захватить» края сущности
                Box entBox = ent.getBoundingBox().expand(0.3D);
                Optional<Vec3d> optHit = entBox.raycast(eyePos, end);
                if (optHit.isPresent()) {
                    Vec3d hitPos = optHit.get();
                    double distSq = eyePos.squaredDistanceTo(hitPos);
                    if (distSq < closestEntityDistSq) {
                        closestEntityDistSq = distSq;
                        // сохраняем EntityHitResult с реальной точкой попадания
                        closestEntityHit = new EntityHitResult(ent, hitPos);
                    }
                }
            }

            return new TargetInfo(closestEntityHit, blockHit);
        }
        // Удаляем perform, continueBreaking, resetBreaking, isBreakingBlock, lastBlockHit
        // так как их функционал перенесен в AutoClickerManager
    }
}