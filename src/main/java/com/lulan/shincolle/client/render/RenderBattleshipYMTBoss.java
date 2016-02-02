package com.lulan.shincolle.client.render;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.BossStatus;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.util.ResourceLocation;

import com.lulan.shincolle.reference.Reference;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderBattleshipYMTBoss extends RenderLiving {
	
	//�K���ɸ��|
	private static final ResourceLocation mobTextures = new ResourceLocation(Reference.TEXTURES_ENTITY+"EntityBattleshipYamato.png");

	public RenderBattleshipYMTBoss(ModelBase par1, float par2) {
		super(par1, par2);	
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity par1Entity) {
		return mobTextures;
	}
	
	//���boss���
	@Override
    protected void preRenderCallback(EntityLivingBase entity, float f1) {
		BossStatus.setBossStatus((IBossDisplayData) entity, true);
	}

}





