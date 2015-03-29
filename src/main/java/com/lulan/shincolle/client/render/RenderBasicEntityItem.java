package com.lulan.shincolle.client.render;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.lulan.shincolle.client.model.ModelBasicEntityItem;
import com.lulan.shincolle.item.BasicEntityItem;
import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.utility.LogHelper;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderBasicEntityItem extends Render {
    
	//�K���ɸ��|
	private static final ResourceLocation entityTexture = new ResourceLocation(Reference.TEXTURES_ENTITY+"ModelBasicEntityItem.png");
	private ModelBasicEntityItem model;

    public RenderBasicEntityItem(float scale) {   
    	this.model = new ModelBasicEntityItem(scale);
	}
    
    @Override
	protected ResourceLocation getEntityTexture(Entity entity) {
		return entityTexture;
	}

    public void doRender(BasicEntityItem entity, double offsetX, double offsetY, double offsetZ, float p_76986_8_, float p_76986_9_) {

    	//bind texture
        this.bindEntityTexture(entity);  		//call getEntityTexture
        
        //render start
        GL11.glPushMatrix();
//        GL11.glDisable(GL11.GL_CULL_FACE);	//�O��model�������e�X��, ���O�u�e�ݱo�쪺��
        
        //model position set to center
        GL11.glTranslatef((float)offsetX, (float)offsetY+0.3F, (float)offsetZ);   		

        //parm: entity, f�̲��ʳt��, f1�̲��ʳt��, f2���W, f3���k����, f4�W�U����, f5(scale)
        this.model.render(entity, entity.ticksExisted, 0F, 0F, 0F, 0F, 0.0625F);

        GL11.glPopMatrix();       
    }

    //�ǤJentity�����নabyssmissile
    public void doRender(Entity entity, double offsetX, double offsetY, double offsetZ, float p_76986_8_, float p_76986_9_) {
    	this.doRender((BasicEntityItem)entity, offsetX, offsetY, offsetZ, p_76986_8_, p_76986_9_);
    }
}
