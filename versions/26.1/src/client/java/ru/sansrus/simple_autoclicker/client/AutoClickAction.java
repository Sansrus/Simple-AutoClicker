package ru.sansrus.simple_autoclicker.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundPickItemFromBlockPacket;
import net.minecraft.network.protocol.game.ServerboundPickItemFromEntityPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public enum AutoClickAction {
    FORWARD  (c -> pressKey(c, "key.forward"), c -> releaseKey(c, "key.forward")),
    BACKWARD (c -> pressKey(c, "key.back"),    c -> releaseKey(c, "key.back")),
    LEFT     (c -> pressKey(c, "key.left"),    c -> releaseKey(c, "key.left")),
    RIGHT    (c -> pressKey(c, "key.right"),   c -> releaseKey(c, "key.right")),
    SPRINT    (c -> { if (c != null) c.options.keySprint.setDown(true); }, c -> { if (c != null) c.options.keySprint.setDown(false); }),
    JUMP(c -> {
        if (c == null || c.player == null) return;
        LocalPlayer p = c.player;

        try {
            if (p.isCreative() || (p.getAbilities() != null && p.getAbilities().mayfly)) {
                p.jumpFromGround();
                return;
            }
        } catch (Throwable ignored) {}

        boolean canJumpSafely =
                p.onGround()
                        || p.isUnderWater()
                        || p.isInLava()
                        || p.onClimbable()
                        || p.isPassenger();

        if (canJumpSafely) {
            p.jumpFromGround();
        }
    }),

    SNEAK(c -> {
        if (c != null && c.options != null) {
            c.options.keyShift.setDown(true);
        }
    }, c -> {
        if (c != null && c.options != null) {
            c.options.keyShift.setDown(false);
        }
    }),

    USE_ITEM(
            c -> {
                if (c == null || c.player == null || c.level == null || c.gameMode == null) return;
                if (c.player.isUsingItem()) return;

                double blockReach = c.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
                double entityReach = c.player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
                Vec3 eyePos = c.player.getEyePosition(0.0F);
                Vec3 look = c.player.getViewVector(0.0F);
                Vec3 entityEnd = eyePos.add(look.scale(entityReach));

                HitResult hr = c.level.clip(new ClipContext(
                        eyePos, eyePos.add(look.scale(blockReach)),
                        ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, c.player));

                double maxEntitySq = entityReach * entityReach;
                if (hr.getType() == HitResult.Type.BLOCK) {
                    maxEntitySq = Math.min(maxEntitySq, eyePos.distanceToSqr(hr.getLocation()));
                }
                AABB box = c.player.getBoundingBox().expandTowards(look.scale(entityReach)).inflate(0.2D, 0.2D, 0.2D);
                EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                        c.player, eyePos, entityEnd, box,
                        e -> !e.isSpectator() && e != c.player, maxEntitySq);

                if (entityHit != null) {
                    for (InteractionHand hand : InteractionHand.values()) {
                        InteractionResult result = c.gameMode.interact(c.player, entityHit.getEntity(), entityHit, hand);
                        if (result == InteractionResult.SUCCESS || result == InteractionResult.CONSUME) {
                            c.player.swing(hand);
                            return;
                        }
                    }
                }

                if (hr.getType() == HitResult.Type.BLOCK && hr instanceof BlockHitResult bhr) {
                    for (InteractionHand hand : InteractionHand.values()) {
                        InteractionResult result = c.gameMode.useItemOn(c.player, hand, bhr);
                        if (result == InteractionResult.SUCCESS || result == InteractionResult.CONSUME) {
                            c.player.swing(hand);
                            return;
                        }
                    }
                }

                for (InteractionHand hand : InteractionHand.values()) {
                    ItemStack stack = c.player.getItemInHand(hand);
                    if (stack.isEmpty()) continue;
                    if (stack.getItem() instanceof FireworkRocketItem && !c.player.isFallFlying()) continue;
                    InteractionResult result = c.gameMode.useItem(c.player, hand);
                    if (result == InteractionResult.SUCCESS || result == InteractionResult.CONSUME) {
                        if (c.player.isUsingItem() && c.options != null && c.options.keyUse != null) {
                            c.options.keyUse.setDown(true);
                        }
                        return;
                    }
                }
            },

            c -> {
                if (c != null && c.options != null && c.options.keyUse != null) {
                    c.options.keyUse.setDown(false);
                }
            }
    ),


    ATTACK(c -> {
        if (c.player == null || c.gameMode == null) return;
        if (c.player.getAttackStrengthScale(0.0F) < 1.0F) return;

        AutoClickerManager.playHandSwing(c, InteractionHand.MAIN_HAND, true);

        double reach = c.player.isCreative() ? 6.0D : 3.0D;
        Vec3 eyePos = c.player.getEyePosition(0.0F);
        Vec3 look = c.player.getViewVector(0.0F);
        Vec3 end = eyePos.add(look.scale(reach));
        HitResult hit = c.level.clip(new ClipContext(eyePos, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, c.player));

        if (hit instanceof EntityHitResult entityHit) {
            Entity target = entityHit.getEntity();
            if (target.isAlive()) {
                c.gameMode.attack(c.player, target);
                c.player.connection.send(new ServerboundInteractPacket(target.getId(), InteractionHand.MAIN_HAND, Vec3.ZERO, c.player.isShiftKeyDown()));
            }
        }
    }),

    DROP(c -> {
        if (c.player != null) {
            c.player.drop(false);
            AutoClickerManager.playHandSwing(c, InteractionHand.MAIN_HAND, true);
        }
    }),

    PICK_BLOCK(c -> {
        if (c.player == null || c.level == null || c.getConnection() == null) return;

        double reach = c.player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
        Vec3 eyePos = c.player.getEyePosition(0.0F);
        Vec3 look = c.player.getViewVector(0.0F);
        Vec3 end = eyePos.add(look.scale(reach));
        HitResult hr = c.level.clip(new ClipContext(eyePos, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, c.player));

        if (hr instanceof BlockHitResult bhr) {
            BlockPos pos = bhr.getBlockPos();
            c.getConnection().send(new ServerboundPickItemFromBlockPacket(pos, true));
            return;
        }

        if (hr instanceof EntityHitResult ehr) {
            Entity ent = ehr.getEntity();
            c.getConnection().send(new ServerboundPickItemFromEntityPacket(ent.getId(), true));
        }
    }),

    SWAP_HANDS(c -> {
        if (c.player == null || c.getConnection() == null) return;
        c.getConnection().send(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                BlockPos.ZERO,
                Direction.UP
        ));
    }),

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
        opts.setCameraType(opts.getCameraType().cycle());
    }),

    CUSTOM_KEYBIND(c -> {}, c -> {}),

    SCREENSHOT(c -> {
        if (c == null) return;
        try {
            RenderTarget fb = c.getMainRenderTarget();
            if (fb != null) {
                Screenshot.grab(
                        c.gameDirectory,
                        fb,
                        (component) -> {
                            c.execute(() -> {
                                if (c.gui != null && c.gui.getChat() != null) {
                                    c.gui.getChat().addClientSystemMessage(component);
                                }
                            });
                        }
                );
            }
        } catch (Exception ex) {
        }
    });


    private final Action press;
    private final Action release;

    AutoClickAction(Action press, Action release) {
        this.press = press;
        this.release = release;
    }
    AutoClickAction(Action press) { this(press, c -> {}); }


    public void press(Minecraft c) {
        try { if (X.a()) return; } catch (Throwable ignored) {}
        press.run(c);
    }
    public void release(Minecraft c) {
        try { if (X.a()) return; } catch (Throwable ignored) {}
        release.run(c);
    }

    @FunctionalInterface
    interface Action { void run(Minecraft c); }

    private static void pressKey(Minecraft c, String key) {
        if (c.options == null) return;
        if (key.equals("key.forward")) { c.options.keyUp.setDown(true); return; }
        if (key.equals("key.back")) { c.options.keyDown.setDown(true); return; }
        if (key.equals("key.left")) { c.options.keyLeft.setDown(true); return; }
        if (key.equals("key.right")) { c.options.keyRight.setDown(true); return; }
    }

    private static void releaseKey(Minecraft c, String key) {
        if (c.options == null) return;
        if (key.equals("key.forward")) { c.options.keyUp.setDown(false); return; }
        if (key.equals("key.back")) { c.options.keyDown.setDown(false); return; }
        if (key.equals("key.left")) { c.options.keyLeft.setDown(false); return; }
        if (key.equals("key.right")) { c.options.keyRight.setDown(false); return; }
    }

    private static void selectHotbar(Minecraft c, int hotbarIndex) {
        if (c.player == null || c.getConnection() == null) return;
        c.player.getInventory().setSelectedSlot(hotbarIndex);
        c.getConnection().send(new ServerboundSetCarriedItemPacket(hotbarIndex));
    }

}
