package com.lulan.shincolle.client.render;

import org.lwjgl.opengl.GL11;

import com.lulan.shincolle.client.model.ModelSmallShipyard;
import com.lulan.shincolle.reference.Reference;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer;

public class RenderSmallShipyardItem implements IItemRenderer  {
	
	TileEntitySpecialRenderer tesr;
	private static TileEntity entity;
	
	public RenderSmallShipyardItem(TileEntitySpecialRenderer tesr, TileEntity entity) {
		this.tesr = tesr;
		this.entity = entity;	
		this.entity.blockMetadata = -2;	//distinguish itemblock(-2)/block(0~7)/non-init state(-1)
	}

	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		return true;
	}

	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
		return true;
	}

	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		//type:ENTITY=��b�a�W, EQUIPPED=���b��W, EQUIPPED_FIRST_PERSON=���b��W�Ĥ@�H��
		//     INVENTORY=�b���~�椤, FIRST_PERSON_MAP=�a���������~
		if(type == IItemRenderer.ItemRenderType.ENTITY) {
			GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
		}
		if(type == IItemRenderer.ItemRenderType.EQUIPPED_FIRST_PERSON) {
			GL11.glTranslatef(0F, 0.3F, 0F);
		}
		//�e�Xmodel
		this.tesr.renderTileEntityAt(this.entity, 0D, 0D, 0D, 0F);

	}

}
