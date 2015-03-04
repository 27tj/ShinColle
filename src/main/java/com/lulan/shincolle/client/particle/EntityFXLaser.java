package com.lulan.shincolle.client.particle;

import org.lwjgl.opengl.GL11;

import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.utility.LogHelper;

import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;


/**LASER PARTICLE
 * ���whost, target -> �ͦ��p�g�S��
 * RE-CLASS, �����V�ť�
 * 
 * type: 0:����16 ticks 
 *       1:
 */
@SideOnly(Side.CLIENT)
public class EntityFXLaser extends EntityFX {

	private static final ResourceLocation TEXTURE = new ResourceLocation(Reference.TEXTURES_PARTICLE+"EntityFXLaser.png");
	private int particleType;	//0:16 ticks
	private double posX, posY, posZ, tarX, tarY, tarZ;
	
    public EntityFXLaser(World world, double posX, double posY, double posZ, double tarX, double tarY, double tarZ, float scale, int type) {
        super(world, posX, posY, posZ, 0.0D, 0.0D, 0.0D);
        this.motionX = 0D;
        this.motionZ = 0D;
        this.motionY = 0D;
        this.particleScale = scale;	//not used
        this.particleType = type;
        this.noClip = true;
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        this.tarX = tarX;
        this.tarY = tarY;
        this.tarZ = tarZ;
        
        switch(type) {
        case 0:
        	this.particleMaxAge = 11;
        	break;
        default:
        	this.particleMaxAge = 11;
        	break;
        }
    }

    public void renderParticle(Tessellator tess, float ticks, float par3, float par4, float par5, float par6, float par7) {
    	//stop last tess for bind texture
    	tess.draw();	
    	
		GL11.glPushMatrix();
		//�ϥΦ۱a���K����
		Minecraft.getMinecraft().renderEngine.bindTexture(TEXTURE);
		GL11.glDepthMask(true);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glDisable(GL11.GL_LIGHTING);
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
//		GL11.glEnable(GL11.GL_DEPTH_TEST);	//DEPTH TEST�}�ҫ�~��ϥ�glDepthFunc
//		GL11.glDepthFunc(GL11.GL_ALWAYS);
		
		float minU = 0F;
		float maxU = (float)(rand.nextInt(32)+32);
		float minV = (float)(this.particleAge % 12) / 12F;
		float maxV = minV + 0.08333333F;
		
		//particle�O�H���a������render, �]���y�Эn����interpPos�ഫ�����a�����y��
        float f11 = (float)(this.posX - interpPosX);
        float f12 = (float)(this.posY - interpPosY);
        float f13 = (float)(this.posZ - interpPosZ);
        float f21 = (float)(this.tarX - interpPosX);
        float f22 = (float)(this.tarY - interpPosY);
        float f23 = (float)(this.tarZ - interpPosZ);
      
        //start tess
        tess.startDrawingQuads();
        //�`�N4���I�Φ������u�������|�K�W�K��, �Y���a�b�ӭ��I���|�ݤ��쥿���K��, �]���n�e�⭱�@8���I
        //�n�Ϫ��a�ݨ쥿��, 4�Ӯy��add���ǥ�����: �k�U -> �k�W -> ���W -> ���U
        //add front plane
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

        //restore texture, �N�K���ɦ^�_���x��Ϊ�particles.png
        try {
        	Minecraft.getMinecraft().renderEngine.bindTexture((ResourceLocation)ReflectionHelper.getPrivateValue(EffectRenderer.class, null, new String[] { "particleTextures", "b", "field_110737_b" })); 
		} 
        catch (Exception e) {
        	LogHelper.info("DEBUG : particle restore default texture fail");
        }
        
//        GL11.glDepthFunc(GL11.GL_LEQUAL);
//		GL11.glDisable(GL11.GL_DEPTH_TEST);	//DEPTH TEST����
      GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);
//		GL11.glDepthMask(false);
		GL11.glPopMatrix();
		
		//start tess for other particle
		tess.startDrawingQuads();
    }
    
    //layer: 0:particle 1:terrain 2:items
    @Override
    public int getFXLayer() {
        return 0;
    }

    /**
     * Called to update the entity's position/logic.
     */
    public void onUpdate() {
//    	LogHelper.info("DEBUG : laser time "+this.getEntityId()+" "+this.particleAge);
		this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        
        if(this.particleAge++ > this.particleMaxAge) {
            this.setDead();
        }
    }
}

