package net.litetex.devauthneo.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.litetex.devauthneo.DevAuthNeo;
import net.minecraft.client.main.Main;


@Mixin(Main.class)
public abstract class MainMixin
{
	@ModifyVariable(method = "main", at = @At("HEAD"), argsOnly = true, remap = false)
	private static String[] modifyArgs(final String[] args)
	{
		final long startMs = System.currentTimeMillis();
		try
		{
			return new DevAuthNeo().processArguments(args);
		}
		finally
		{
			DevAuthNeo.LOG.debug("Total mixin overhead time: {}ms", System.currentTimeMillis() - startMs);
		}
	}
}
