package com.lulan.shincolle.model;


import java.util.Random;

import org.lwjgl.opengl.GL11;

import com.lulan.shincolle.reference.AttrID;
import com.lulan.shincolle.reference.AttrValues;
import com.lulan.shincolle.utility.LogHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;

import com.lulan.shincolle.entity.EntityDestroyerI;

public class ModelDestroyerI extends ModelBase {
    //fields
	public ModelRenderer PBack;
	public ModelRenderer PNeck;
	public ModelRenderer PHead;
	public ModelRenderer[] PEyeLightL = new ModelRenderer[3];
	public ModelRenderer[] PEyeLightR = new ModelRenderer[3];
	public ModelRenderer PJawBottom;
	public ModelRenderer PBody;
	public ModelRenderer PLegLeft;
	public ModelRenderer PLegLeftEnd;
	public ModelRenderer PLegRight;
	public ModelRenderer PLegRightEnd;
	public ModelRenderer PTail;
	public ModelRenderer PTailLeft;
	public ModelRenderer PTailLeftEnd;
	public ModelRenderer PTailRight;
	public ModelRenderer PTailRightEnd;
	public ModelRenderer PTailEnd;
	
	private static final int cooldown = 300;
	public float scale = 1F;			//�w�]�j�p��1.0��
	
    public ModelDestroyerI() {
    textureWidth = 256;
    textureHeight = 128;
    
    setTextureOffset("PBack.Back", 128, 8);
    setTextureOffset("PNeck.NeckBack", 128, 0);
    setTextureOffset("PNeck.NeckNeck", 128, 28);
    setTextureOffset("PNeck.NeckBody", 0, 70);
    setTextureOffset("PHead.Head", 0, 0);
    setTextureOffset("PEyeLightL0.LEye", 138, 64);
    setTextureOffset("PEyeLightR0.REye", 138, 64);
    setTextureOffset("PEyeLightL1.LEye", 138, 84);
    setTextureOffset("PEyeLightR1.REye", 138, 84);
    setTextureOffset("PEyeLightL2.LEye", 138, 104);
    setTextureOffset("PEyeLightR2.REye", 138, 104);
    setTextureOffset("PHead.ToothTopMid", 96, 0);
    setTextureOffset("PHead.ToothTopRight", 128, 54);
    setTextureOffset("PHead.ToothTopLeft", 128, 54);
    setTextureOffset("PHead.JawTop", 0, 102);
    setTextureOffset("PJawBottom.JawBottom", 92, 64);
    setTextureOffset("PJawBottom.ToothBottomLeft", 96, 19);
    setTextureOffset("PJawBottom.ToothBottomRight", 96, 19);
    setTextureOffset("PJawBottom.ToothBottomMid", 0, 0);
    setTextureOffset("PBody.Body", 0, 64);
    setTextureOffset("PLegLeft.LegLeftFront", 0, 80);
    setTextureOffset("PLegLeftEnd.LegLeftEnd", 0, 90);
    setTextureOffset("PLegRight.LegRightFront", 0, 80);
    setTextureOffset("PLegRightEnd.LegRightEnd", 0, 90);
    setTextureOffset("PTail.TailBack", 128, 16);
    setTextureOffset("PTail.TailBody", 0, 68);
    setTextureOffset("PTailLeft.TailLeftFront", 128, 28);
    setTextureOffset("PTailLeftEnd.TailLeftEnd", 128, 36);
    setTextureOffset("PTailRight.TailRightFront", 128, 28);
    setTextureOffset("PTailRightEnd.TailRightEnd", 128, 36);
    setTextureOffset("PTailEnd.TailEnd", 128, 26);
    
    PBack = new ModelRenderer(this, "PBack");
    PBack.setRotationPoint(-8F, -16F, 0F);
    setRotation(PBack, 0F, 0F, -0.31F);
    PBack.mirror = true;
      PBack.addBox("Back", -12F, -10F, -12F, 28, 20, 24);
    PNeck = new ModelRenderer(this, "PNeck");
    PNeck.setRotationPoint(15F, 0F, 0F);
    setRotation(PNeck, 0F, 0F, 0.2F);
    PNeck.mirror = true;
      PNeck.addBox("NeckBack", -3F, -11F, -13F, 30, 26, 26);
      PNeck.addBox("NeckNeck", 6F, 15F, -10F, 21, 4, 20);
      PNeck.addBox("NeckBody", -8F, 7F, -9F, 18, 14, 18);
    PHead = new ModelRenderer(this, "PHead");
    PHead.setRotationPoint(26F, 0F, 0F);
    setRotation(PHead, 0F, 0F, 0.3F);
    PHead.mirror = true;
      PHead.addBox("Head", -3F, -12F, -16F, 32, 32, 32);
      PHead.addBox("ToothTopMid", 14.5F, 20F, -6F, 4, 6, 12);
      PHead.addBox("ToothTopRight", 0F, 20F, -10F, 18, 6, 4);
      PHead.addBox("ToothTopLeft", 0F, 20F, 6F, 18, 6, 4);
      PHead.addBox("JawTop", -3F, 20F, -11F, 22, 2, 22);
      
    //3 emotion eye
    PEyeLightL[0] = new ModelRenderer(this, "PEyeLightL0");
    PEyeLightR[0] = new ModelRenderer(this, "PEyeLightR0");
    PEyeLightL[0].addBox("LEye", -3F, 0F, 16.1F, 24, 20, 0);
    PEyeLightR[0].addBox("REye", -3F, 0F, -16.1F, 24, 20, 0);
    PEyeLightL[1] = new ModelRenderer(this, "PEyeLightL1");
    PEyeLightR[1] = new ModelRenderer(this, "PEyeLightR1");
    PEyeLightL[1].addBox("LEye", -3F, 0F, 16.1F, 24, 20, 0).isHidden = true;
    PEyeLightR[1].addBox("REye", -3F, 0F, -16.1F, 24, 20, 0).isHidden = true;
    PEyeLightL[2] = new ModelRenderer(this, "PEyeLightL2");
    PEyeLightR[2] = new ModelRenderer(this, "PEyeLightR2");
    PEyeLightL[2].addBox("LEye", -3F, 0F, 16.1F, 24, 20, 0).isHidden = true;
    PEyeLightR[2].addBox("REye", -3F, 0F, -16.1F, 24, 20, 0).isHidden = true;
    
    PJawBottom = new ModelRenderer(this, "PJawBottom");
    PJawBottom.setRotationPoint(-6F, 18F, 0F);
    setRotation(PJawBottom, 0F, 0F, -0.2F);
    PJawBottom.mirror = true;
      PJawBottom.addBox("JawBottom", -3F, 0F, -10F, 3, 18, 20);
      PJawBottom.addBox("ToothBottomLeft", -1F, 7.5F, 6F, 4, 10, 3);
      PJawBottom.addBox("ToothBottomRight", -1F, 7.5F, -9F, 4, 10, 3);
      PJawBottom.addBox("ToothBottomMid", -1F, 14.5F, -6F, 4, 3, 12);
      PHead.addChild(PJawBottom);
      PHead.addChild(PEyeLightL[0]);
      PHead.addChild(PEyeLightR[0]);
      PHead.addChild(PEyeLightL[1]);
      PHead.addChild(PEyeLightR[1]);
      PHead.addChild(PEyeLightL[2]);
      PHead.addChild(PEyeLightR[2]);
      PNeck.addChild(PHead);
      PBack.addChild(PNeck);
    PBody = new ModelRenderer(this, "PBody");
    PBody.setRotationPoint(0F, 0F, 0F);
//    setRotation(PBody, 0F, 0F, 0F);
    PBody.mirror = true;
      PBody.addBox("Body", -10F, 10F, -11F, 24, 16, 22);
    PLegLeft = new ModelRenderer(this, "PLegLeft");
    PLegLeft.setRotationPoint(-3F, 24F, 6F);
//    setRotation(PLegLeft, 0F, 0F, 0F);
    PLegLeft.mirror = true;
      PLegLeft.addBox("LegLeftFront", -3F, -4F, -1F, 8, 14, 8);
    PLegLeftEnd = new ModelRenderer(this, "PLegLeftEnd");
    PLegLeftEnd.setRotationPoint(1F, 8F, 4F);
//    setRotation(PLegLeftEnd, 0F, 0F, 14F);
    PLegLeftEnd.mirror = true;
      PLegLeftEnd.addBox("LegLeftEnd", -12F, -3F, -4F, 12, 6, 6);
      PLegLeft.addChild(PLegLeftEnd);
      PBody.addChild(PLegLeft);
    PLegRight = new ModelRenderer(this, "PLegRight");
    PLegRight.setRotationPoint(-3F, 24F, -8F);
 //   setRotation(PLegRight, 0F, 0F, 0F);
    PLegRight.mirror = true;
      PLegRight.addBox("LegRightFront", -3F, -4F, -5F, 8, 14, 8);
    PLegRightEnd = new ModelRenderer(this, "PLegRightEnd");
    PLegRightEnd.setRotationPoint(1F, 8F, -1F);
 //   setRotation(PLegRightEnd, 0F, 0F, 14F);
    PLegRightEnd.mirror = true;
      PLegRightEnd.addBox("LegRightEnd", -12F, -3F, -3F, 12, 6, 6);
      PLegRight.addChild(PLegRightEnd);
      PBody.addChild(PLegRight);
      PBack.addChild(PBody);
    PTail = new ModelRenderer(this, "PTail");
    PTail.setRotationPoint(-12F, -2F, 0F);
    setRotation(PTail, 0F, 0F, 0.25F);
    PTail.mirror = true;
      PTail.addBox("TailBack", -22F, -6F, -10F, 26, 16, 20);
      PTail.addBox("TailBody", -8F, 2F, -8F, 18, 18, 14);
    PTailLeft = new ModelRenderer(this, "PTailLeft");
    PTailLeft.setRotationPoint(-12F, 4F, 8F);
    setRotation(PTailLeft, 0.5F, 0F, 0.25F);
    PTailLeft.mirror = true;
      PTailLeft.addBox("TailLeftFront", -8F, -4F, 0F, 12, 18, 6);
    PTailLeftEnd = new ModelRenderer(this, "PTailLeftEnd");
    PTailLeftEnd.setRotationPoint(0F, 9F, 5F);
   setRotation(PTailLeftEnd, 0F, 0F, -0.4F);
    PTailLeftEnd.mirror = true;
      PTailLeftEnd.addBox("TailLeftEnd", -24F, -4F, -2F, 24, 12, 4);
      PTailLeft.addChild(PTailLeftEnd);
      PTail.addChild(PTailLeft);
    PTailRight = new ModelRenderer(this, "PTailRight");
    PTailRight.setRotationPoint(-12F, 4F, -8F);
    setRotation(PTailRight, -0.5F, 0F, 0.25F);
    PTailRight.mirror = true;
      PTailRight.addBox("TailRightFront", -8F, -4F, -6F, 12, 18, 6);
    PTailRightEnd = new ModelRenderer(this, "PTailRightEnd");
    PTailRightEnd.setRotationPoint(0F, 9F, -5F);
    setRotation(PTailRightEnd, 0F, 0F, -0.4F);
    PTailRightEnd.mirror = true;
      PTailRightEnd.addBox("TailRightEnd", -24F, -4F, -2F, 24, 12, 4);
      PTailRight.addChild(PTailRightEnd);
      PTail.addChild(PTailRight);
    PTailEnd = new ModelRenderer(this, "PTailEnd");
    PTailEnd.setRotationPoint(-22F, 2F, 0F);
    setRotation(PTailEnd, 0F, 0F, 0.3F);
    PTailEnd.mirror = true;
      PTailEnd.addBox("TailEnd", -20F, -6F, -8F, 24, 10, 16);
      PTail.addChild(PTailEnd);
      PBack.addChild(PTail);
  }
  
  public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
	super.render(entity, f, f1, f2, f3, f4, f5);
	setRotationAngles(f, f1, f2, f3, f4, f5, entity);
	
	// Scale, Translate, Rotate
	// GL11.glScalef(this.scale, this.scale, this.scale);
	GL11.glScalef(0.5F, 0.45F, 0.4F);	//debug��
	GL11.glTranslatef(0F, 2F, 0F);		//�j�p0.45�ɳ]2.3   �j�p0.3�ɳ]3
	GL11.glRotatef(90F, 0F, 1F, 0F);	//���ҫ��Y����V���~ �]��render�ɽվ�^��
	PBack.render(f5);
  }
  
  private void setRotation(ModelRenderer model, float x, float y, float z) {
    model.rotateAngleX = x;
    model.rotateAngleY = y;
    model.rotateAngleZ = z;
  }
  
  //for idle/run animation
  public void setRotationAngles(float f, float f1, float f2, float f3, float f4, float f5, Entity entity) {
    super.setRotationAngles(f, f1, f2, f3, f4, f5, entity);

    float angleZ = MathHelper.cos(f2*0.125F);
       
    EntityDestroyerI ent = (EntityDestroyerI)entity;
    
    rollEmotion(ent);
    
    motionLeg(f,f1);
    
    motionWatch(f3,f4,angleZ);	//include watch head & normal head
    
    motionTail(angleZ);

  }
  
  	//�`���\�ʧ��ڸ�U��
  	private void motionTail(float angleZ) { 	
  	    PTail.rotateAngleZ = angleZ * 0.2F;
  	    PTailEnd.rotateAngleZ = angleZ * 0.3F;
  	    PJawBottom.rotateAngleZ = angleZ * 0.2F -0.3F;
  	}

	//���}���ʭp��
  	private void motionLeg(float f, float f1) {
		//�������} ���ҫ���V�]�� �]���令��Z
	    PLegRight.rotateAngleZ = MathHelper.cos(f * 0.6662F) * 1.4F * f1 - 0.6F;
	    PLegLeft.rotateAngleZ = MathHelper.cos(f * 0.6662F + 3.1415927F) * 1.4F * f1 - 0.6F;
	    PLegRightEnd.rotateAngleZ = MathHelper.sin(f * 0.6662F) * f1 - 0.4F;
	    PLegLeftEnd.rotateAngleZ = MathHelper.sin(f * 0.6662F + 3.1415927F) * f1 - 0.4F;	
	}

  	//�Y���ݤH��ʭp��
	private void motionWatch(float f3, float f4, float angleZ) {
		//�����Y�� �Ϩ�ݤH  ��ʨ��פӤp�ɫh��������
	    if(MathHelper.abs(f4) > 0.5) {
		    PNeck.rotateAngleY = f3 / 160F;		//���k���� �����নrad �Y���H57.29578
		    PNeck.rotateAngleZ = f4 / 130F; 	//�W�U����
		    PHead.rotateAngleY = f3 / 160F;
		    PHead.rotateAngleZ = f4 / 130F;
		    PTail.rotateAngleY = f3 / -130F;	//���ڥH�Ϥ�V�\��
	    }
	    else {
	    	PNeck.rotateAngleY = 0;			//���k���� �����নrad �Y���H57.29578
		    PNeck.rotateAngleZ = 0.2F; 		//�W�U����
		    PHead.rotateAngleY = 0;
		    PHead.rotateAngleZ = angleZ * 0.15F + 0.2F;
		    PTail.rotateAngleY = 0;  	
	    }	
	}

	//�H�������ܪ��� 
    private void rollEmotion(EntityDestroyerI ent) {
    	
    	switch(ent.EntityState[AttrID.Emotion]) {
    	case AttrValues.Emotion.BLINK:	//blink
    		EmotionBlink(ent);
    		break;
    	default:						//normal, other
    		setFace(0);
    		if(ent.ticksExisted % 120 == 0) {  	//roll emotion (3 times) every 6 sec
        		int emotionRand = ent.rand.nextInt(100);   		
        		if(emotionRand > 70) {
        			EmotionBlink(ent);
        		}
        	}
    		break;
    	}	
    }

	//�w���ʧ@
	private void EmotionBlink(EntityDestroyerI ent) {
		if(ent.EntityState[AttrID.Emotion] == AttrValues.Emotion.NORMAL) {	//�n�b�S�����A�~����
			ent.StartEmotion = ent.ticksExisted;			//���oentity�ثe���ɶ�
			ent.EntityState[AttrID.Emotion] = AttrValues.Emotion.BLINK;		//�аO����blink
		}	
		else {			
			switch(ent.ticksExisted - ent.StartEmotion) {
			case 1:
				setFace(2);
				break;
			case 18:
				setFace(1);
				break;
			case 35:
				setFace(2);
				break;
			case 41:
				setFace(1);
				ent.EntityState[AttrID.Emotion] = AttrValues.Emotion.NORMAL;
				break;
			}
		}
	}
	
	//�]�w��ܪ��y��
	private void setFace(int emo) {
		switch(emo) {
		case 1:
			PEyeLightL[0].isHidden = false;
			PEyeLightR[0].isHidden = false;
			PEyeLightL[1].isHidden = true;
			PEyeLightR[1].isHidden = true;
			PEyeLightL[2].isHidden = true;
			PEyeLightR[2].isHidden = true;
			break;
		case 2:
			PEyeLightL[0].isHidden = true;
			PEyeLightR[0].isHidden = true;
			PEyeLightL[1].isHidden = false;
			PEyeLightR[1].isHidden = false;
			PEyeLightL[2].isHidden = true;
			PEyeLightR[2].isHidden = true;
			break;
		case 3:
			PEyeLightL[0].isHidden = true;
			PEyeLightR[0].isHidden = true;
			PEyeLightL[1].isHidden = true;
			PEyeLightR[1].isHidden = true;
			PEyeLightL[2].isHidden = false;
			PEyeLightR[2].isHidden = false;
			break;
		default:
			break;
		}
	}

}
