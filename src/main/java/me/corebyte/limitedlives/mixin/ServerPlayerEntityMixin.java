package me.corebyte.limitedlives.mixin;

import me.corebyte.limitedlives.LimitedLives;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {

	@Inject(method = "getPlayerListName", at = @At("HEAD"), cancellable = true)
	private void onGetPlayerListName(CallbackInfoReturnable<Text> cir) {
		if ((Object) this instanceof ServerPlayerEntity player) {
			cir.setReturnValue(LimitedLives.formatTabName(player));
		}
	}

	@Inject(at = @At(value = "TAIL"), method = "onDeath")
	private void onPlayerDeath(DamageSource source, CallbackInfo info) {
		if ((Object) this instanceof ServerPlayerEntity player) {
			int gracePeriod = LimitedLives.getGracePeriod(player);
			if (gracePeriod != 0) return;
			int lives = LimitedLives.getLives(player) - 1;
			LimitedLives.setLives(player, lives);

			if (lives <= 0) player.changeGameMode(GameMode.SPECTATOR);
		}
	}
}
