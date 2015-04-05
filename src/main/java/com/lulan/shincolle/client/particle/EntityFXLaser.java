package com.lulan.shincolle.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;

import com.lulan.shincolle.reference.Reference;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;


/**LASER PARTICLE
 * ���whost, target -> �ͦ��p�g�S��
 * RE-CLASS, �����V�ť�
 * 
 * type: 0:����11 ticks 
 *       1:
 */
@SideOnly(Side.CLIENT)
public class EntityFXLaser extends EntityFX {

	private static final ResourceLocation TEXTURE = new ResourceLocation(Reference.TEXTURES_PARTICLE+"EntityFXLaser.png");
	private int particleType;
	private double tarX, tarY, tarZ;
	
    public EntityFXLaser(World world, double posX, double posY, double posZ, double tarX, double tarY, double tarZ, float scale, int type) {
        super(world, posX, posY, posZ, 0.0D, 0.0D, 0.0D);
        this.motionX = 0D;
        this.motionZ = 0D;
        this.motionY = 0D;
        this.particleScale = scale;	//not used
        this.particleType = type;	//not used for now
        this.noClip = true;
        this.tarX = tarX;
        this.tarY = tarY;
        this.tarZ = tarZ;
        
        switch(type) {
        case 0:		//re-class laser
        	this.particleMaxAge = 11;
        	this.particleMaxAge = 6;
        	this.particleRed = 1F;
        	this.particleGreen = 1F;
        	this.particleBlue = 1F;
        	this.particleAlpha = 1F;
        	break;
        case 1:		//NGT speed blur
        	this.particleMaxAge = 11;
        	this.particleAge = 4;
        	this.particleRed = 1F;
        	this.particleGreen = 0F;
        	this.particleBlue = 0F;
        	this.particleAlpha = 1F;
        	break;
        }
        
    }

    public void renderParticle(Tessellator tess, float ticks, float par3, float par4, float par5, float par6, float par7) {	
		GL11.glPushMatrix();
		//�ϥΦ۱a���K����
		Minecraft.getMinecraft().renderEngine.bindTexture(TEXTURE);
		GL11.glDepthMask(true);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glDisable(GL11.GL_LIGHTING);
//		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
		
		float minU = 0F;
		float maxU = (float)(rand.nextInt(32)+32);
		float minV = (float)(this.particleAge % 12) / 12F;
		float maxV = minV + 0.08333333F;
		
		//particle�O�H���a������render, �]���y�Эn����interpPos�ഫ�����a�����y��
        double f11 = (float)(this.posX - interpPosX);
        double f12 = (float)(this.posY - interpPosY);
        double f13 = (float)(this.posZ - interpPosZ);
        double f21 = (float)(this.tarX - interpPosX);
        double f22 = (float)(this.tarY - interpPosY);
        double f23 = (float)(this.tarZ - interpPosZ);
      
        //start tess
        tess.startDrawingQuads();
        tess.setColorRGBA_F(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha);
        //�`�N4���I�Φ������u�������|�K�W�K��, �Y���a�b�ӭ��I���|�ݤ��쥿���K��, �]���n�e�⭱�@8���I
        //�n�Ϫ��a�ݨ쥿��, 4�Ӯy��add���ǥ�����: �k�U -> �k�W -> ���W -> ���U
        //add front plane
        tess.setBrightness(240);
        tess.addVertexWithUV(f21, f22, f23, maxU, maxV);
        tess.addVertexWithUV(f21, f22 + particleScale * 0.3D, f23, maxU, minV);
        tess.addVertexWithUV(f11, f12 + particleScale * 0.3D, f13, minU, minV);
        tess.addVertexWithUV(f11, f12, f13, minU, maxV);
        //add back plane
        tess.addVertexWithUV(f11, f12, f13, minU, maxV);
        tess.addVertexWithUV(f11, f12 + particleScale * 0.3D, f13, minU, minV);
        tess.addVertexWithUV(f21, f22 + particleScale * 0.3D, f23, maxU, minV);
        tess.addVertexWithUV(f21, f22, f23, maxU, maxV);
        
        //stop tess for restore texture
        tess.draw();
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);
		GL11.glDepthMask(false);
		GL11.glPopMatrix();
    }
    
    //layer: 0:particle 1:terrain 2:items 3:custom?
    @Override
    public int getFXLayer() {
        return 3;
    }

    /**
     * Called to update the entity's position/logic.
     */
    public void onUpdate() {
        if(this.particleAge++ > this.particleMaxAge) {
            this.setDead();
        }
    }
}

