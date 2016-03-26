package com.lulan.shincolle.client.render;

import com.lulan.shincolle.reference.Reference;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

@SideOnly(Side.CLIENT)
public class RenderDestroyerI extends RenderLiving {
	
	//貼圖檔路徑
	private static final ResourceLocation mobTextures = new ResourceLocation(Reference.TEXTURES_ENTITY+"EntityDestroyerI.png");

	public RenderDestroyerI(ModelBase par1, float par2) {
		super(par1, par2);	
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity par1Entity) {
		return mobTextures;
	}
/*
	//render前要畫的東西都在此設定, 例如血條顯示
	@Override	
    protected void preRenderCallback(EntityLivingBase entity, float f1) {
		BossStatus.setBossStatus((IBossDisplayData) entity, true);	
	}
*/

}
