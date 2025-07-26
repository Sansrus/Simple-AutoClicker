package org.example.sansrus.simpleautoclicker.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.slf4j.LoggerFactory;

public enum AutoClickAction {
    FORWARD  (c -> pressKey(c, "key.forward"), c -> releaseKey(c, "key.forward")),
    BACKWARD (c -> pressKey(c, "key.back"),    c -> releaseKey(c, "key.back")),
    LEFT     (c -> pressKey(c, "key.left"),    c -> releaseKey(c, "key.left")),
    RIGHT    (c -> pressKey(c, "key.right"),   c -> releaseKey(c, "key.right")),
    JUMP     (c -> {
        ClientPlayerEntity p = c.player;
        if (p != null) {
            p.jump();
        }
    }),
    SNEAK    (c -> {
        ClientPlayerEntity p = c.player;
        if (p != null) {
            p.setSneaking(true);
        }
    }, c -> {
        ClientPlayerEntity p = c.player;
        if (p != null) {
            p.setSneaking(false);
        }
    }),
    USE_ITEM (c -> {
        if (c.player != null && c.interactionManager != null) {
            c.interactionManager.interactItem(c.player, c.player.getActiveHand());
        }
    }),
    ATTACK(c -> {
        // Проверяем, что игрок и менеджер взаимодействий доступны
        if (c.player == null || c.interactionManager == null) return;

        // Проверяем кулдаун атаки
        if (c.player.getAttackCooldownProgress(0.0F) < 1.0F) return;

        // Махаем рукой (анимация атаки)
        c.player.swingHand(c.player.getActiveHand());

        // Определяем дистанцию атаки (3 блока в обычном режиме, 6 в креативе)
        double reach = c.player.isCreative() ? 6.0D : 3.0D;
        HitResult hit = c.player.raycast(reach, 0.0F, true);

        // Проверяем, попал ли луч в сущность
        if (hit instanceof EntityHitResult entityHit) {
            Entity target = entityHit.getEntity();
            // Убеждаемся, что цель жива и может быть атакована
            if (target.isAlive() && target.isAttackable()) {
                // Атакуем сущность
                c.interactionManager.attackEntity(c.player, target);
                // Отправляем пакет серверу
                c.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(target, c.player.isSneaking()));
            }
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
}