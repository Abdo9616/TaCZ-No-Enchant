package com.tacznoenchant;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mod("tacznoenchant")
@Mod.EventBusSubscriber(modid = "tacznoenchant")
public class TaczNoEnchant {
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("tacznoenchant-common.toml");
    private static final List<String> ALLOWED_GUN_IDS = new ArrayList<>();
    private static boolean stripExistingEnchantments = false;

    private static final String DEFAULT_CONFIG = "" +
            "# TaCZ No Enchant\n" +
            "# This blocks normal enchanted-book application to TACZ guns in survival.\n" +
            "# Warning: stripExistingEnchantments is irreversible. Use it only if you know what you are doing.\n\n" +
            "stripExistingEnchantments = false\n\n" +
            "# Allow specific TACZ GunId values to ignore the enchant restriction.\n" +
            "# Example: allowedGunIds = [\"tacz:ak47\"]\n" +
            "allowedGunIds = []\n";

    static {
        ensureConfig();
        loadConfig();
    }

    public TaczNoEnchant() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("tacznoenchant")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("reload")
                                .executes(context -> {
                                    try {
                                        reloadConfig();
                                        context.getSource().sendSuccess(() -> Component.literal("TaCZ No Enchant config reloaded.").withStyle(ChatFormatting.GREEN), true);
                                        return 1;
                                    } catch (Exception exception) {
                                        context.getSource().sendFailure(Component.literal("Failed to reload TaCZ No Enchant config.").withStyle(ChatFormatting.RED));
                                        return 0;
                                    }
                                }))
        );
    }

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        if (event == null || event.getPlayer() == null || event.getPlayer().isCreative()) {
            return;
        }

        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();

        if (left.isEmpty() || right.isEmpty()) {
            return;
        }

        boolean leftIsTaczGun = isTaczGun(left);
        boolean rightIsTaczGun = isTaczGun(right);
        boolean leftIsEnchantedBook = isEnchantedBook(left);
        boolean rightIsEnchantedBook = isEnchantedBook(right);

        if ((leftIsTaczGun && rightIsEnchantedBook) || (rightIsTaczGun && leftIsEnchantedBook)) {
            event.setOutput(ItemStack.EMPTY);
            event.setCost(0);
            event.setMaterialCost(0);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!stripExistingEnchantments) {
            return;
        }

        Player player = event.getEntity();
        if (player == null || player.isCreative() || player.isSpectator()) {
            return;
        }

        stripEnchantmentsFromInventory(player.getInventory().items);
        stripEnchantmentsFromInventory(player.getInventory().armor);
        stripEnchantmentsFromInventory(player.getInventory().offhand);
    }

    public static void reloadConfig() {
        ensureConfig();
        loadConfig();
    }

    private static void ensureConfig() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            if (Files.notExists(CONFIG_PATH)) {
                Files.writeString(CONFIG_PATH, DEFAULT_CONFIG, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create TaCZ No Enchant config file", e);
        }
    }

    private static void loadConfig() {
        try {
            String content = Files.readString(CONFIG_PATH, StandardCharsets.UTF_8);
            stripExistingEnchantments = false;
            ALLOWED_GUN_IDS.clear();

            Matcher stripMatcher = Pattern.compile("(?m)^\\s*stripExistingEnchantments\\s*=\\s*(true|false)\\s*$").matcher(content);
            if (stripMatcher.find()) {
                stripExistingEnchantments = Boolean.parseBoolean(stripMatcher.group(1));
            }

            Matcher listMatcher = Pattern.compile("(?m)^\\s*allowedGunIds\\s*=\\s*\\[(.*)\\]\\s*$").matcher(content);
            if (listMatcher.find()) {
                String listValue = listMatcher.group(1).trim();
                if (!listValue.isEmpty()) {
                    String[] values = listValue.split(",");
                    for (String value : values) {
                        String normalized = value.trim().replaceAll("^\"|\"$", "");
                        if (!normalized.isEmpty()) {
                            ALLOWED_GUN_IDS.add(normalized);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read TaCZ No Enchant config file", e);
        }
    }

    private static void stripEnchantmentsFromInventory(List<ItemStack> inventory) {
        if (inventory == null) {
            return;
        }

        for (ItemStack stack : inventory) {
            if (stack == null || stack.isEmpty() || !isTaczGun(stack) || !stack.isEnchanted()) {
                continue;
            }

            if (stack.getTag() != null) {
                stack.getTag().remove("Enchantments");
            }
        }
    }

    private static boolean isTaczGun(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (key == null || !key.getNamespace().equals("tacz")) {
            return false;
        }

        return !isAllowedGun(stack);
    }

    private static boolean isAllowedGun(ItemStack stack) {
        String gunId = getGunIdFromStack(stack);
        if (gunId.isEmpty()) {
            return false;
        }

        for (String allowed : ALLOWED_GUN_IDS) {
            if (normalizeGunId(gunId).equals(normalizeGunId(allowed))) {
                return true;
            }
        }

        return false;
    }

    private static String getGunIdFromStack(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getTag() == null) {
            return "";
        }

        return stack.getTag().getString("GunId");
    }

    private static String normalizeGunId(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("tacz:")) {
            normalized = normalized.substring("tacz:".length());
        }
        return normalized;
    }

    private static boolean isEnchantedBook(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof EnchantedBookItem;
    }
}
