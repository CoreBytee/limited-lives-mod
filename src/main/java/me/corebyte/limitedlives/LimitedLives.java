package me.corebyte.limitedlives;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.serialization.Codec;
import java.util.EnumSet;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LimitedLives implements ModInitializer {

	public static final String MOD_ID = "limited-lives";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final AttachmentType<Integer> GRACE_PERIOD_ATTACHMENT =
		AttachmentRegistry.create(
			Identifier.of(MOD_ID, "grace_period"),
			builder -> builder.persistent(Codec.INT).copyOnDeath()
		);

	public static final AttachmentType<Integer> LIVES_ATTACHMENT =
		AttachmentRegistry.create(Identifier.of(MOD_ID, "lives"), builder ->
			builder.persistent(Codec.INT).copyOnDeath()
		);

	public static int getGracePeriod(ServerPlayerEntity player) {
		return player.getAttachedOrSet(GRACE_PERIOD_ATTACHMENT, 2 * 60 * 60);
	}

	public static int setGracePeriod(
		ServerPlayerEntity player,
		int gracePeriod
	) {
		return player.setAttached(GRACE_PERIOD_ATTACHMENT, gracePeriod);
	}

	public static int getLives(ServerPlayerEntity player) {
		return player.getAttachedOrSet(LIVES_ATTACHMENT, 3);
	}

	public static void setLives(ServerPlayerEntity player, int lives) {
		player.setAttached(LIVES_ATTACHMENT, lives < 0 ? 0 : lives);
	}

	public static void updatePlayerList(MinecraftServer server) {
		PlayerManager players = server.getPlayerManager();

		PlayerListS2CPacket packet = new PlayerListS2CPacket(
			EnumSet.of(PlayerListS2CPacket.Action.UPDATE_DISPLAY_NAME),
			players.getPlayerList()
		);

		players.sendToAll(packet);
	}

	public static String formatGracePeriod(int seconds) {
		if (seconds >= 60) {
			int minutes = (seconds / 60) + 1;
			return minutes + (minutes == 1 ? " minute" : " minutes");
		} else {
			return seconds + (seconds == 1 ? " second" : " seconds");
		}
	}

	private static Text formatName(ServerPlayerEntity player) {
		Formatting nameColor;
		int lives = getLives(player);

		if (lives >= 3) {
			nameColor = Formatting.GREEN;
		} else if (lives == 2) {
			nameColor = Formatting.YELLOW;
		} else if (lives == 1) {
			nameColor = Formatting.RED;
		} else {
			nameColor = Formatting.DARK_RED;
		}

		return Text.literal(player.getDisplayName().getString()).formatted(
			nameColor
		);
	}

	public static Text formatTabName(ServerPlayerEntity player) {
		int gracePeriod = getGracePeriod(player);
		int lives = getLives(player);

		boolean isGrace = gracePeriod > 0;

		Text hourglass = Text.literal("⏳").formatted(Formatting.GOLD);
		Text heart = Text.literal("❤").formatted(Formatting.RED);

		return Text.literal("[")
			.append(Text.literal(Integer.toString(lives)))
			.append(" ")
			.append(isGrace ? hourglass : heart)
			.append("] ")
			.append(formatName(player));
	}

	@Override
	public void onInitialize() {
		LOGGER.info("Hello Fabric world!");

		CommandRegistrationCallback.EVENT.register(
			(dispatcher, registryAccess, environment) -> {
				dispatcher.register(
					CommandManager.literal("limitedlives")
						.requires(source -> source.hasPermissionLevel(4))
						.then(
							CommandManager.literal("setgrace").then(
								CommandManager.argument(
									"player",
									EntityArgumentType.player()
								).then(
									CommandManager.argument(
										"seconds",
										IntegerArgumentType.integer()
									).executes(context -> {
										ServerPlayerEntity player =
											EntityArgumentType.getPlayer(
												context,
												"player"
											);

										int seconds =
											IntegerArgumentType.getInteger(
												context,
												"seconds"
											);

										LimitedLives.setGracePeriod(
											player,
											seconds
										);

										context
											.getSource()
											.sendFeedback(
												() ->
													Text.literal(
														"Changed grace period for " +
															player
																.getDisplayName()
																.getString() +
															" to " +
															seconds +
															" seconds."
													),
												true
											);

										return 1;
									})
								)
							)
						)
						.then(
							CommandManager.literal("getgrace").then(
								CommandManager.argument(
									"player",
									EntityArgumentType.player()
								).executes(context -> {
									ServerPlayerEntity player =
										EntityArgumentType.getPlayer(
											context,
											"player"
										);

									int grace = LimitedLives.getGracePeriod(
										player
									);

									context
										.getSource()
										.sendFeedback(
											() ->
												Text.literal(
													"Grace period for " +
														player
															.getDisplayName()
															.getString() +
														" is " +
														grace +
														" seconds."
												),
											true
										);

									return 1;
								})
							)
						)
						.then(
							CommandManager.literal("setlives").then(
								CommandManager.argument(
									"player",
									EntityArgumentType.player()
								).then(
									CommandManager.argument(
										"lives",
										IntegerArgumentType.integer()
									).executes(context -> {
										ServerPlayerEntity player =
											EntityArgumentType.getPlayer(
												context,
												"player"
											);

										int lives =
											IntegerArgumentType.getInteger(
												context,
												"lives"
											);

										LimitedLives.setLives(player, lives);

										context
											.getSource()
											.sendFeedback(
												() ->
													Text.literal(
														"Changed lives for " +
															player
																.getDisplayName()
																.getString() +
															" to " +
															lives +
															"."
													),
												true
											);

										return 1;
									})
								)
							)
						)
						.then(
							CommandManager.literal("getlives").then(
								CommandManager.argument(
									"player",
									EntityArgumentType.player()
								).executes(context -> {
									ServerPlayerEntity player =
										EntityArgumentType.getPlayer(
											context,
											"player"
										);

									int lives = LimitedLives.getLives(player);

									context
										.getSource()
										.sendFeedback(
											() ->
												Text.literal(
													"Lives for " +
														player
															.getDisplayName()
															.getString() +
														" is " +
														lives +
														"."
												),
											true
										);

									return 1;
								})
							)
						)
				);
			}
		);

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTicks() % 20 != 0) return;

			server
				.getPlayerManager()
				.getPlayerList()
				.stream()
				.filter(player -> player.getGameMode() == GameMode.SURVIVAL)
				.forEach(player -> {
					int gracePeriod = getGracePeriod(player);
					if (gracePeriod > 0) setGracePeriod(
						player,
						gracePeriod - 1
					);

					if (gracePeriod > 0) {
						Text text = Text.literal("You have ")
							.append(
								Text.literal(
									formatGracePeriod(gracePeriod)
								).formatted(Formatting.RED)
							)
							.append(Text.literal(" of grace period left!"));

						player.sendMessage(text, true);
					}
				});

			updatePlayerList(server);
		});
	}
}
