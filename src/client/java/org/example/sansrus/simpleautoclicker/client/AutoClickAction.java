package org.example.sansrus.simpleautoclicker.client;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public enum AutoClickAction {
    FORWARD  (c -> pressKey(c, "key.forward"), c -> releaseKey(c, "key.forward")),
    BACKWARD (c -> pressKey(c, "key.back"),    c -> releaseKey(c, "key.back")),
    LEFT     (c -> pressKey(c, "key.left"),    c -> releaseKey(c, "key.left")),
    RIGHT    (c -> pressKey(c, "key.right"),   c -> releaseKey(c, "key.right")),
    SPRINT    (c -> { if (c != null) c.options.sprintKey.setPressed(true); }, c -> { if (c != null) c.options.sprintKey.setPressed(false); }),
    // Замените существующую запись JUMP(...) на этот вариант
    JUMP(c -> {
        if (c == null || c.player == null) return;
        ClientPlayerEntity p = c.player;

        // Всегда разрешаем в креативе / если у игрока включён режим полёта
        // (creativeMode / allowFlying — дублирующие проверки на разные mappings)
        try {
            if (p.isCreative() || (p.getAbilities() != null && p.getAbilities().allowFlying)) {
                p.jump();
                return;
            }
        } catch (Throwable ignored) {}

        // Для выживания/приключения разрешаем прыжок только когда это законно:
        // - на земле
        // - в воде/лаве (плавание/всплытие)
        // - на лестнице/лианах (climbing)
        // - верхом на сущности / в седле (hasVehicle)
        //
        // Это покрывает все "честные" случаи и предотвращает многократную установку jump() в воздухе.
        boolean canJumpSafely =
                p.isOnGround()
                        || p.isSubmergedInWater()   // подпрыгивание в воде
                        || p.isInLava()          // если ваша mappings имеет isInLava (иначе можно убрать)
                        || p.isClimbing()        // лестницы/лианы
                        || p.hasVehicle();       // сидите на сущности

        if (canJumpSafely) {
            p.jump();
        }
    }),

    SNEAK(c -> {
        if (c != null && c.options != null) {
            c.options.sneakKey.setPressed(true);
        }
    }, c -> {
        if (c != null && c.options != null) {
            c.options.sneakKey.setPressed(false);
        }
    }),

    USE_ITEM(
            c -> {
                if (c == null || c.player == null || c.world == null || c.interactionManager == null) return;

                // Попробуем обе руки: сначала главную, потом вторую
                for (Hand hand : Hand.values()) {
                    var stack = c.player.getStackInHand(hand);

                    // Пропускаем пустую руку
                    if (stack.isEmpty()) continue;

                    // 1) Пробуем интеракт предметом в руке
                    ActionResult itemResult = c.interactionManager.interactItem(c.player, hand);
                    if (itemResult == ActionResult.SUCCESS || itemResult == ActionResult.CONSUME) {
                        if (c.options != null && c.options.useKey != null) {
                            c.options.useKey.setPressed(true);
                        }
                        return; // Успешно использовали - выходим
                    }

                    // 2) Делаем рейкаст
                    double reach = c.player.getAttributeValue(EntityAttributes.ENTITY_INTERACTION_RANGE);
                    HitResult hr = c.player.raycast(reach, 0.0F, true);

                    switch (hr) {
                        case null -> {
                            // Продолжаем цикл для второй руки
                            continue;
                        }

                        // 3) Если попали в блок
                        case BlockHitResult bhr -> {
                            BlockPos placePos = bhr.getBlockPos().offset(bhr.getSide());

                            // Проверка опоры для специальных предметов
                            if (requiresSupport(stack.getItem())) {
                                if (!hasSupport((ClientWorld) c.world, placePos)) {
                                    continue; // Нет опоры - пробуем другую руку
                                }
                            }

                            ActionResult blockResult = c.interactionManager.interactBlock(c.player, hand, bhr);
                            if (blockResult == ActionResult.SUCCESS || blockResult == ActionResult.CONSUME) {
                                if (c.options != null && c.options.useKey != null) {
                                    c.options.useKey.setPressed(true);
                                }
                                return; // Успешно - выходим
                            }
                            // Не успешно - пробуем другую руку
                            continue;
                        }

                        // 4) Если попали в сущность
                        case EntityHitResult ehr -> {
                            ActionResult entResult = c.interactionManager.interactEntityAtLocation(
                                    c.player, ehr.getEntity(), ehr, hand
                            );
                            if (entResult == ActionResult.SUCCESS || entResult == ActionResult.CONSUME) {
                                if (c.options != null && c.options.useKey != null) {
                                    c.options.useKey.setPressed(true);
                                }
                                return; // Успешно - выходим
                            }
                            continue;
                        }

                        default -> {
                            continue;
                        }
                    }
                }

                // 5) Если ничего не сработало - держим use как фолбэк
                if (c.options != null && c.options.useKey != null) {
                    c.options.useKey.setPressed(true);
                }
            },

            // release: отпустить удержание use
            c -> {
                if (c == null) return;
                if (c.options != null && c.options.useKey != null) {
                    c.options.useKey.setPressed(false);
                }
            }
    ),

    ATTACK(c -> {
        if (c.player == null || c.interactionManager == null) return;
        if (c.player.getAttackCooldownProgress(0.0F) < 1.0F) return;

        AutoClickerManager.playHandSwing(c, Hand.MAIN_HAND, true);

        double reach = c.player.isCreative() ? 6.0D : 3.0D;
        HitResult hit = c.player.raycast(reach, 0.0F, true);

        if (hit instanceof EntityHitResult entityHit) {
            Entity target = entityHit.getEntity();
            if (target.isAlive() && target.isAttackable()) {
                c.interactionManager.attackEntity(c.player, target);
                c.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(target, c.player.isSneaking()));
            }
        }
    }),

    // одноразовые / спец-действия:
    DROP(c -> {
        if (c.player != null) {
            // drop single item (false) — аналог нажатия Q
            c.player.dropSelectedItem(false);
            AutoClickerManager.playHandSwing(c, Hand.MAIN_HAND, true);
        }
    }),

    PICK_BLOCK(c -> {
        if (c.player == null || c.world == null || c.getNetworkHandler() == null) return;

        double reach = c.player.getAttributeValue(EntityAttributes.ENTITY_INTERACTION_RANGE);
        HitResult hr = c.player.raycast(reach, 0.0F, false);

        if (hr instanceof BlockHitResult bhr) {
            BlockPos pos = bhr.getBlockPos();
            // includeData = true — чтобы корректно получить блок/состояние с данными
            c.getNetworkHandler().sendPacket(new PickItemFromBlockC2SPacket(pos, true));
            return;
        }

        if (hr instanceof EntityHitResult ehr) {
            Entity ent = ehr.getEntity();
            // отправляем пакет для выбора предмета из сущности
            c.getNetworkHandler().sendPacket(new PickItemFromEntityC2SPacket(ent.getId(), true));
        }
    }),

    SWAP_HANDS(c -> {
        if (c.player == null || c.getNetworkHandler() == null) return;
        // отправляем пакет swap offhand
        c.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                BlockPos.ORIGIN,
                Direction.UP
        ));
    }),

    // хотбар: выбираем слот 0..8 (HOTBAR_1 -> index 0, HOTBAR_9 -> index 8)
    HOTBAR_1(c -> selectHotbar(c, 0)),
    HOTBAR_2(c -> selectHotbar(c, 1)),
    HOTBAR_3(c -> selectHotbar(c, 2)),
    HOTBAR_4(c -> selectHotbar(c, 3)),
    HOTBAR_5(c -> selectHotbar(c, 4)),
    HOTBAR_6(c -> selectHotbar(c, 5)),
    HOTBAR_7(c -> selectHotbar(c, 6)),
    HOTBAR_8(c -> selectHotbar(c, 7)),
    HOTBAR_9(c -> selectHotbar(c, 8)),

    TOGGLE_PERSPECTIVE(c -> {
        if (c == null || c.options == null) return;
        var opts = c.options;
        opts.setPerspective(opts.getPerspective().next());
    }),

    SCREENSHOT(c -> {
        if (c == null) return;
        try {
            Framebuffer fb = c.getFramebuffer();
            if (fb != null) {
                // Показываем сообщение в чат через ScreenshotRecorder callback
                ScreenshotRecorder.saveScreenshot(
                        // runDirectory может называться runDirectory или быть доступен как поле
                        c.runDirectory,
                        fb,
                        (text) -> {
                            if (c.inGameHud != null && c.inGameHud.getChatHud() != null) {
                                c.inGameHud.getChatHud().addMessage(text);
                            }
                        }
                );
            }
        } catch (Exception ex) {
            // молча игнорируем — не фатально
        }
    });


    private final Action press;
    private final Action release;

    AutoClickAction(Action press, Action release) {
        this.press = press;
        this.release = release;
    }
    AutoClickAction(Action press) { this(press, c -> {}); }


    public void press(MinecraftClient c)   { press.run(c); }
    public void release(MinecraftClient c) { release.run(c); }

    @FunctionalInterface
    interface Action { void run(MinecraftClient c); }

    /* ---------- Утилита для KeyBinding ---------- */
    private static void pressKey(MinecraftClient c, String key) {
        for (KeyBinding kb : c.options.allKeys) {
            if (key.equals(kb.getTranslationKey())) {
                kb.setPressed(true);
                return;
            }
        }
    }

    private static void releaseKey(MinecraftClient c, String key) {
        for (KeyBinding kb : c.options.allKeys) {
            if (key.equals(kb.getTranslationKey())) {
                kb.setPressed(false);
                return;
            }
        }
    }

    private static void selectHotbar(MinecraftClient c, int hotbarIndex) {
        if (c.player == null || c.getNetworkHandler() == null) return;
        // Меняем локально выбранный хотбар
        c.player.getInventory().setSelectedSlot(hotbarIndex);
        // Уведомляем сервер
        c.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(hotbarIndex));
    }

    private static boolean hasSupport(ClientWorld world, BlockPos placePos) {
        if (world == null) return false;

        // 1) Проверяем блок снизу: если его верхняя сторона "полностью твёрдая" -> поддержка
        BlockPos below = placePos.down();
        BlockState belowState = world.getBlockState(below);
        try {
            if (belowState.isSideSolidFullSquare(world, below, Direction.UP)) return true;
        } catch (Throwable ignored) {}

        // 2) Проверяем соседей (по всем шести направлениям, кроме вверх)
        for (Direction dir : Direction.values()) {
            if (dir == Direction.UP) continue; // опора сверху обычно не считается
            BlockPos neighbor = placePos.offset(dir);
            BlockState ns = world.getBlockState(neighbor);
            // Нужна "твёрдая" сторона соседа, обращённая к placePos
            try {
                if (ns.isSideSolidFullSquare(world, neighbor, dir.getOpposite())) {
                    return true;
                }
            } catch (Throwable ignored) {}
        }

        // Нет подходящей опоры
        return false;
    }

    private static boolean requiresSupport(net.minecraft.item.Item item) {
        // сюда добавляйте предметы, которым нужна опора (BlockItem всегда, Firework — пример)
        if (item instanceof BlockItem) return true;
        if (item instanceof FireworkRocketItem) return true;
        // при необходимости — добавить другие: e.g. Redstone-related, certain spawn eggs? (обычно не нужно)
        return false;
    }
}
