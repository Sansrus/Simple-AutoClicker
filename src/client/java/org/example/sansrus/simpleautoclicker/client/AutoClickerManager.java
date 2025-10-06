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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class AutoClickerManager {
    private final MinecraftClient client = MinecraftClient.getInstance();
    private final AutoClickerConfig cfg = AutoClickerConfig.getInstance();
    private final static Logger LOGGER = LoggerFactory.getLogger("AutoClickManager");

    // Переменные для отслеживания состояния ломания блока
    private boolean isBreakingBlock = false;
    private BlockHitResult lastBlockHit = null;
    private boolean lastGlobalEnabled;


    public AutoClickerManager() {
        this.lastGlobalEnabled = cfg.globalEnabled;
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(MinecraftClient client) {
        if (client.world == null || client.player == null) {
            fullReset();
            lastGlobalEnabled = cfg.globalEnabled;
            return;
        }

        if (!cfg.globalEnabled) {
            if (lastGlobalEnabled) {
                // только один раз при переключении в OFF
                simpleReset();
                lastGlobalEnabled = false;
            }
            return;
        }

        // Если дошли сюда — глобально включено. Обновляем флаг и выполняем обычную логику.
        lastGlobalEnabled = true;

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
                        AutoClickerManager.playHandSwing(client, Hand.MAIN_HAND, true);
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
                        if (client.interactionManager == null) return;
                        client.interactionManager.cancelBlockBreaking();
                        isBreakingBlock = false;
                        lastBlockHit = null;
                    }
                    continue;
                }

                if (spam) {
                    if (performAttack(targetInfo)) {
                        e.pressed = true;
                    } else {
                        // если целью был entity, но кулдаун не готов -> не помечаем pressed
                        e.pressed = false;
                    }
                    if (hasBlock && isBreakingBlock) {
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
                AutoClickerManager.playHandSwing(client, Hand.MAIN_HAND, true);
                if (performAttack(targetInfo)) {
                    e.pressed = true;
                }
                if (targetInfo.hasBlock()) {
                    continueBreaking();
                }
            } else {
                e.tickCounter++;
                if (!e.pressed && e.tickCounter >= e.intervalTicks) {
                    performAttack(targetInfo);
                    AutoClickerManager.playHandSwing(client, Hand.MAIN_HAND, true);
                    e.pressed = true;
                    e.tickCounter = 0;
                } else if (e.pressed && e.tickCounter >= e.useDurationTicks) {
                    // Остановить ломание, если было
                    if (isBreakingBlock) {
                        if (client.interactionManager == null) return;
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

//    /**
//     * Полный сброс: и внутренние флаги, и сброс нажатий всех клавиш (использовался до этого).
//     */
//    private void fullReset() {
//        if (client == null) return;
//
//        // 1) Сбрасываем все записи конфигурации полностью
//        for (AutoClickerConfig.Entry e : cfg.entries) {
//            if (e == null) continue;
//            // Вызываем release на всякий случай (чтобы enum отпустил клавиши)
//            try {
//                if (e.action != null) e.action.release(client);
//            } catch (Exception ignored) {}
//
//            e.pressed = false;
//            e.tickCounter = 0;
//        }
//
//        // 2) Останавливаем ломание блока
//        stopBlockIfNeeded();
//
//        // 3) Принудительно отпускаем все известные клавиши (как в simpleReset, но тут делаем полностью)
//        if (client.options != null) {
//            try { if (client.options.forwardKey != null) client.options.forwardKey.setPressed(false); } catch (Throwable ignored) {}
//            try { if (client.options.backKey != null) client.options.backKey.setPressed(false); } catch (Throwable ignored) {}
//            try { if (client.options.leftKey != null) client.options.leftKey.setPressed(false); } catch (Throwable ignored) {}
//            try { if (client.options.rightKey != null) client.options.rightKey.setPressed(false); } catch (Throwable ignored) {}
//            try { if (client.options.jumpKey != null) client.options.jumpKey.setPressed(false); } catch (Throwable ignored) {}
//            try { if (client.options.sneakKey != null) client.options.sneakKey.setPressed(false); } catch (Throwable ignored) {}
//            try { if (client.options.useKey != null) client.options.useKey.setPressed(false); } catch (Throwable ignored) {}
//            try { if (client.options.dropKey != null) client.options.dropKey.setPressed(false); } catch (Throwable ignored) {}
//            try { if (client.options.sprintKey != null) client.options.sprintKey.setPressed(false); } catch (Throwable ignored) {}
//            try { if (client.options.pickItemKey != null) client.options.pickItemKey.setPressed(false); } catch (Throwable ignored) {}
//            try { if (client.options.swapHandsKey != null) client.options.swapHandsKey.setPressed(false); } catch (Throwable ignored) {}
//            try { if (client.options.togglePerspectiveKey != null) client.options.togglePerspectiveKey.setPressed(false); } catch (Throwable ignored) {}
//            try { if (client.options.screenshotKey != null) client.options.screenshotKey.setPressed(false); } catch (Throwable ignored) {}
//
//            // Хотбар
//            try {
//                if (client.options.hotbarKeys != null) {
//                    for (KeyBinding kb : client.options.hotbarKeys) {
//                        if (kb != null) kb.setPressed(false);
//                    }
//                }
//            } catch (Throwable ignored) {}
//
//            // Общий массив клавиш (если есть)
//            try {
//                if (client.options.allKeys != null) {
//                    for (KeyBinding kb : client.options.allKeys) {
//                        if (kb != null) kb.setPressed(false);
//                    }
//                }
//            } catch (Throwable ignored) {}
//        }
//
//        // 4) Сбрасываем состояние клавиши атаки и связанные флаги
//        try { if (client.options != null && client.options.attackKey != null) client.options.attackKey.setPressed(false); } catch (Throwable ignored) {}
//
//        // 5) Если игрок использовал предмет — безопасно остановить использование (fullReset — "жёсткий" сброс)
//        try {
//            if (client.player != null) client.player.stopUsingItem();
//        } catch (Throwable ignored) {}
//    }
//
//
//    /**
//     * Упрощённый сброс при отключении мода:
//     * — сбрасываем свои флаги и прерываем ломание, но НЕ сбрасываем attackKey.
//     */
//    private void simpleReset() {
//        // Не трогаем stopUsingItem() здесь — simpleReset вызывается при отключении модa и
//        // не должен ломать обычное удержание использования игрока в мире.
//        if (client == null) return;
//        if (client.player == null) return;
//
//        // 1) Сбрасываем внутренние флаги и вызываем release для действий, которые могли что-то держать
//        for (AutoClickerConfig.Entry e : cfg.entries) {
//            if (e == null) continue;
//            // Если запись была в состоянии "pressed", вызовем release у её действия,
//            // чтобы enum мог отпустить связанные KeyBinding'и / состояния.
//            try {
//                if (e.pressed && e.action != null) {
//                    e.action.release(client);
//                }
//            } catch (Exception ignored) {}
//
//            // Сбрасываем флаги записи
//            e.pressed = false;
//            e.tickCounter = 0;
//        }
//
//        // 2) Отменяем ломание блока, если оно было
//        stopBlockIfNeeded();
//
//        // 3) Резервная принудительная отжатие известных клавиш (на случай, если enum-release не закрывал всё)
//        if (client.options != null) {
//            try { if (client.options.forwardKey != null) client.options.forwardKey.setPressed(false); } catch (Throwable ignored) {}
//            try { if (client.options.backKey != null) client.options.backKey.setPressed(false); } catch (Throwable ignored) {}
//            try { if (client.options.leftKey != null) client.options.leftKey.setPressed(false); } catch (Throwable ignored) {}
//            try { if (client.options.rightKey != null) client.options.rightKey.setPressed(false); } catch (Throwable ignored) {}
//            try { if (client.options.jumpKey != null) client.options.jumpKey.setPressed(false); } catch (Throwable ignored) {}
//            try { if (client.options.sneakKey != null) client.options.sneakKey.setPressed(false); } catch (Throwable ignored) {}
//            try { if (client.options.useKey != null) client.options.useKey.setPressed(false); } catch (Throwable ignored) {}
//            try { if (client.options.dropKey != null) client.options.dropKey.setPressed(false); } catch (Throwable ignored) {}
//            try { if (client.options.sprintKey != null) client.options.sprintKey.setPressed(false); } catch (Throwable ignored) {}
//            try { if (client.options.pickItemKey != null) client.options.pickItemKey.setPressed(false); } catch (Throwable ignored) {}
//            try { if (client.options.swapHandsKey != null) client.options.swapHandsKey.setPressed(false); } catch (Throwable ignored) {}
//            try { if (client.options.togglePerspectiveKey != null) client.options.togglePerspectiveKey.setPressed(false); } catch (Throwable ignored) {}
//            try { if (client.options.screenshotKey != null) client.options.screenshotKey.setPressed(false); } catch (Throwable ignored) {}
//
//            // Хотбар (если есть)
//            try {
//                if (client.options.hotbarKeys != null) {
//                    for (KeyBinding kb : client.options.hotbarKeys) {
//                        if (kb != null) kb.setPressed(false);
//                    }
//                }
//            } catch (Throwable ignored) {}
//
//            // Попытка очистить общий массив клавиш, если он есть
//            try {
//                if (client.options.allKeys != null) {
//                    for (KeyBinding kb : client.options.allKeys) {
//                        if (kb != null) kb.setPressed(false);
//                    }
//                }
//            } catch (Throwable ignored) {}
//        }
//
//        // Не трогаем attackKey здесь (если не хотите), но можно гарантировать её состояние:
//        try { if (client.options != null && client.options.attackKey != null) client.options.attackKey.setPressed(false); } catch (Throwable ignored) {}
//    }

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
        if (client.player == null) return;
        for (AutoClickerConfig.Entry e : cfg.entries) {
            e.pressed = false;
            e.tickCounter = 0;
        }
        stopBlockIfNeeded();
        client.player.stopUsingItem();
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
     * Попытаться выполнить атаку (по сущности) или начать ломание блока.
     * Возвращает true, если действие действительно выполнено (пакет/вызов отправлен / ломание начато).
     */
    private boolean performAttack(TargetInfo targetInfo) {
        if (client.world == null || client.player == null || client.interactionManager == null) {
            return false;
        }

        // Если есть сущность — требуем готовность атаки (кулдаун)
        if (targetInfo.hasEntity()) {
            // Проверяем прогресс кулдауна орудия/руки — требуется >= 1.0
            if (client.player.getAttackCooldownProgress(0.0F) < 1.0F) {
                return false;
            }

            client.interactionManager.attackEntity(client.player, targetInfo.entityHitResult.getEntity());
            // локальная анимация + уведомить сервер (если нужно)
            AutoClickerManager.playHandSwing(client, net.minecraft.util.Hand.MAIN_HAND, true);

            // если было ломание — отменяем
            if (isBreakingBlock) {
                if (client.interactionManager != null) client.interactionManager.cancelBlockBreaking();
                isBreakingBlock = false;
                lastBlockHit = null;
            }
            return true;
        }

        // Если есть блок — начать ломание (attackBlock) — не зависит от attack cooldown
        if (targetInfo.hasBlock()) {
            BlockHitResult bhr = targetInfo.blockHitResult;
            client.interactionManager.attackBlock(bhr.getBlockPos(), bhr.getSide());
            isBreakingBlock = true;
            lastBlockHit = bhr;
            // Мах рукой/информирование сервера полезно при ломании
            AutoClickerManager.playHandSwing(client, net.minecraft.util.Hand.MAIN_HAND, true);
            return true;
        }

        return false;
    }


    private void handleNonAttack(AutoClickerConfig.Entry e, MinecraftClient client) {
        boolean spam = e.spamMode || e.intervalTicks == 0;

        // Если это спам — держим/повторяем
        if (spam) {
            press(e);
            if (isHoldAction(e.action)) {
                // удерживаемое действие — помечаем как удержание
                e.pressed = true;
            } else {
                // одноразовое действие — сразу отпускаем, не помечаем как удержание
                release(e);
                e.pressed = false;
            }
            return;
        }

        // Интервальный режим
        e.tickCounter++;
        if (!e.pressed && e.tickCounter >= e.intervalTicks) {
            // Сработал интервал — нажимаем
            press(e);

            if (isHoldAction(e.action)) {
                // помечаем как удержание и начнём считать useDurationTicks
                e.pressed = true;
            } else {
                // одноразовое — сразу отпускаем
                release(e);
                e.pressed = false;
            }
            e.tickCounter = 0;
        } else if (e.pressed && e.tickCounter >= e.useDurationTicks) {
            // Завершаем удержание
            release(e);
            e.pressed = false;
            e.tickCounter = 0;
        }
    }

    /**
     * Возвращает true, если действие должно обрабатываться как удержание (hold),
     * т.е. press() -> пометить pressed=true и позже release() после useDurationTicks.
     * Вредные/одноразовые действия: DROP, PICK_BLOCK, SWAP_HANDS, HOTBAR_*, TOGGLE_PERSPECTIVE, SCREENSHOT.
     * Удерживаемые: движение, прыжок, приседание, использование предмета, бег (если используется).
     */
    private boolean isHoldAction(AutoClickAction action) {
        return switch (action) {
            case FORWARD, BACKWARD, LEFT, RIGHT,
                 JUMP, SNEAK, USE_ITEM, SPRINT, ATTACK -> true;
            default -> false;
        };
    }



    private void press(AutoClickerConfig.Entry e) {
        if (e.action == AutoClickAction.ATTACK) {
            // логика атаки отдельно
            return;
        }
        // вызываем реализацию, определённую в AutoClickAction
        e.action.press(client);
    }

    private void release(AutoClickerConfig.Entry e) {
        if (e.action == AutoClickAction.ATTACK) {
            return;
        }
        e.action.release(client);
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
        AutoClickerManager.playHandSwing(client, Hand.MAIN_HAND, true);
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
            Vec3d eyePos = client.player.getCameraPosVec(0.0F);

            // 1) Блоковый рэйкаст
            HitResult rawHit = client.player.raycast(reach, 0.0F, false);
            BlockHitResult blockHit = rawHit instanceof BlockHitResult bhr ? bhr : null;
            double blockDistSq = blockHit != null
                    ? eyePos.squaredDistanceTo(blockHit.getPos())
                    : Double.MAX_VALUE;

            // 2) Собираем энтити в зоне перед игроком
            Vec3d look = client.player.getRotationVec(0.0F);
            Vec3d end = eyePos.add(look.multiply(reach));
            Box searchBox = client.player.getBoundingBox()
                    .stretch(look.multiply(reach))
                    .expand(1.0D, 1.0D, 1.0D);
            double closestEntityDistSq = Double.MAX_VALUE;
            EntityHitResult closestEntityHit = null;

            for (LivingEntity ent : client.world.getEntitiesByClass(LivingEntity.class, searchBox,
                    e -> e != client.player && !e.isSpectator())) {
                Box entBox = ent.getBoundingBox().expand(0.3D);
                Optional<Vec3d> optHit = entBox.raycast(eyePos, end);
                if (optHit.isPresent()) {
                    Vec3d hitPos = optHit.get();
                    double distSq = eyePos.squaredDistanceTo(hitPos);

                    // КЛЮЧЕВАЯ ПРОВЕРКА: если entity дальше блока - пропускаем
                    if (distSq >= blockDistSq) {
                        continue; // Блок мешает, entity за ним
                    }

                    if (distSq < closestEntityDistSq) {
                        closestEntityDistSq = distSq;
                        closestEntityHit = new EntityHitResult(ent, hitPos);
                    }
                }
            }

            // Если нашли entity, которая ближе блока - возвращаем её
            // Иначе возвращаем только блок
            return new TargetInfo(closestEntityHit, blockHit);
        }
        // Удаляем perform, continueBreaking, resetBreaking, isBreakingBlock, lastBlockHit
        // так как их функционал перенесен в AutoClickerManager
    }

    public static void playHandSwing(MinecraftClient client, Hand hand, boolean notifyServer) {
        if (client == null || client.player == null) return;

        // Локальная анимация (будет видна сразу этому клиенту)
        client.player.swingHand(hand);

        // Уведомление сервера, чтобы другие клиенты увидели анимацию
        if (notifyServer && client.getNetworkHandler() != null) {
            client.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
        }
    }
}