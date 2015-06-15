package com.lulan.shincolle.client.render;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.lulan.shincolle.client.model.ModelNorthernHime;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.IShipEmotion;
import com.lulan.shincolle.init.ModItems;
import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.utility.LogHelper;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderNorthernHime extends RenderLiving {
	
	//�K���ɸ��|
	private static final ResourceLocation mobTextures = new ResourceLocation(Reference.TEXTURES_ENTITY+"EntityNorthernHime.png");
	private ModelNorthernHime model = null;
	private ItemStack holdItem = new ItemStack(ModItems.ToyAirplane);
	private Random rand = new Random();
	
	public RenderNorthernHime(ModelNorthernHime par1, float par2) {
		super(par1, par2);
		this.model = par1;
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity par1Entity) {
		return mobTextures;
	}

	@Override
	protected void renderEquippedItems(EntityLivingBase host, float swing) {
		//��_���`�C��
		GL11.glColor3f(1.0F, 1.0F, 1.0F);

		IShipEmotion host1 = (IShipEmotion) host;
        ItemStack itemstack = host.getHeldItem();	//������~(�Ω�morph��)
        Item item;
        float f1;
        
        //�����ʧ@���e�X���~
        if(((IShipEmotion)host).getIsSitting()) {
        	return;
        }
        
        //�Y�S��������~, �h�w�]�����㭸��
        if(itemstack == null) {
        	itemstack = holdItem;
        }

        //�N���~�e�b��W
        if(itemstack != null && itemstack.getItem() != null) {
            item = itemstack.getItem();
            GL11.glPushMatrix();
            
            IItemRenderer customRenderer = MinecraftForgeClient.getItemRenderer(itemstack, IItemRenderer.ItemRenderType.EQUIPPED);
            boolean is3D = (customRenderer != null && customRenderer.shouldUseRenderHelper(IItemRenderer.ItemRenderType.EQUIPPED, itemstack, IItemRenderer.ItemRendererHelper.BLOCK_3D));

            //�̷Ӽҫ����u��m�Ϊ��~����, �]�w�����I, �ϱ����I�ŦX���u��ʭ��I
            //�Y�Ӫ��~�������
            if(item instanceof ItemBlock && (is3D || RenderBlocks.renderItemIn3d(Block.getBlockFromItem(item).getRenderType()))) {
            	f1 = 0.2F;
            	GL11.glTranslatef(0F, 0.9F, 0F);
                GL11.glScalef(f1, f1, f1);
            }
            //�Y�Ӫ��~�n��3D�e�X ex: �Z��, �u����
            else if(item.isFull3D()) {
                f1 = 0.375F;
                GL11.glScalef(f1, f1, f1);
                GL11.glTranslatef(0F, 2.7F, 0F);
            }
            //��L�@�몫�~
            else {
                f1 = 0.25F;	//SIZE
                GL11.glScalef(f1, f1, f1);
                GL11.glTranslatef(0F, 3.7F, 0F);
            }
	
            if(host.ridingEntity instanceof BasicEntityShip) {
	    		GL11.glTranslatef(0F, 0F, 1.0F);
	    	}
            
	    	if(host.ridingEntity instanceof EntityPlayer) {
	    		GL11.glTranslatef(0F, 7.6F, 1.0F);
	    	}
            
            //�I�s�ⳡpost render, �Ϫ��~��̷Ӥⳡ�ҫ����ק��ܦ�m
            //�Ҧ��I�s�Lpost render������y��������, ���|�M�Ψ쪫�~���ʤW
            GL11.glPushMatrix();
            this.model.BodyMain.postRender(0.0625F);
            this.model.ArmRight01.postRender(0.0625F);
            this.model.ArmRight02.postRender(0.0625F);
            
            //�̷Ӽҫ����u��m�Ϊ��~����, �������~��m, ����ʶb���ײŦX���u����
            //�Y�Ӫ��~�������
            if(item instanceof ItemBlock && (is3D || RenderBlocks.renderItemIn3d(Block.getBlockFromItem(item).getRenderType()))) {
            	GL11.glTranslatef(0.1F, 1.6F, -0.7F);
                GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
                GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
            }
            //�Y�Ӫ��~�n��3D�e�X ex: �Z��, �u����
            else if(item.isFull3D()) {
                if(item.shouldRotateAroundWhenRendering()) {
                    GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);
                    GL11.glTranslatef(0.0F, -0.125F, 0.0F);
                }
            	//��ʪZ������
            	GL11.glTranslatef(0F, 0.6F, 0F);
            	GL11.glRotatef(-60.0F, 1.0F, 0.0F, 0.0F);
                GL11.glRotatef(35.0F, 0.0F, 1.0F, 0.0F);
                GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
            }
            //��L�@�몫�~
            else {
            	GL11.glTranslatef(-0.4F, 1.5F, -0.4F);
            	GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);
            	GL11.glRotatef(15.0F, 0.0F, 1.0F, 1.0F);
            	GL11.glRotatef(50.0F, 0.0F, 0.0F, 1.0F);
            }
            
            //�e�X�Ӫ��~
            float f2;
            float f5;
            int i;
            //�Y�Ӫ��~���S��render pass, ex:�B, �z�������
            if(itemstack.getItem().requiresMultipleRenderPasses()) {
                for(i = 0; i < itemstack.getItem().getRenderPasses(itemstack.getItemDamage()); ++i) {
                    int j = itemstack.getItem().getColorFromItemStack(itemstack, i);
                    f5 = (float)(j >> 16 & 255) / 255.0F;
                    f2 = (float)(j >> 8 & 255) / 255.0F;
                    float f3 = (float)(j & 255) / 255.0F;
                    GL11.glColor4f(f5, f2, f3, 1.0F);
                    this.renderManager.itemRenderer.renderItem(host, itemstack, i);
                }
            }
            //��L�@�몫�~
            else {
                i = itemstack.getItem().getColorFromItemStack(itemstack, 0);
                float f4 = (float)(i >> 16 & 255) / 255.0F;
                f5 = (float)(i >> 8 & 255) / 255.0F;
                f2 = (float)(i & 255) / 255.0F;
                GL11.glColor4f(f4, f5, f2, 1.0F);
                this.renderManager.itemRenderer.renderItem(host, itemstack, 0);
            }

            GL11.glPopMatrix();
            GL11.glPopMatrix();
        }
	}
}


