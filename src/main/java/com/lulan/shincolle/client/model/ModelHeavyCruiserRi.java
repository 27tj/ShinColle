package com.lulan.shincolle.client.model;

import org.lwjgl.opengl.GL11;

import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.EntityHeavyCruiserRi;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.reference.Values;
import com.lulan.shincolle.utility.LogHelper;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;

/**
 * HeavyCruiserRi - PinkaLulan 2015/2/3
 * Created using Tabula 4.1.1
 */
public class ModelHeavyCruiserRi extends ModelBase {
    public ModelRenderer BodyMain;
    public ModelRenderer Butt;
    public ModelRenderer ArmLeft;
    public ModelRenderer ArmRight;
    public ModelRenderer Neck;
    public ModelRenderer EquipBase;
    public ModelRenderer LegRight;
    public ModelRenderer LegLeft;
    public ModelRenderer BoobR;
    public ModelRenderer BoobL;
    public ModelRenderer EquipLeftBase;
    public ModelRenderer EquipLeftTube1;
    public ModelRenderer EquipLeftBase2;
    public ModelRenderer EquipLeftBase3;
    public ModelRenderer EquipLeftBase4;
    public ModelRenderer EquipLeftTube2;
    public ModelRenderer EquipLeftTube3;
    public ModelRenderer EquipLeftTooth;
    public ModelRenderer EquipRightBase;
    public ModelRenderer EquipRightTube1;
    public ModelRenderer EquipRightBase1;
    public ModelRenderer EquipRightBase2;
    public ModelRenderer EquipRightBase3;
    public ModelRenderer EquipRightBase4;
    public ModelRenderer EquipRightTube2;
    public ModelRenderer EquipRightTube3;
    public ModelRenderer EquipRightTooth1;
    public ModelRenderer EquipRightTooth2;
    public ModelRenderer Head;
    public ModelRenderer Cloak;
    public ModelRenderer Hair;
    public ModelRenderer GlowBodyMain;
    public ModelRenderer GlowNeck;
    public ModelRenderer GlowHead;
    public ModelRenderer GlowArmLeft;
    public ModelRenderer GlowEquipLeftBase;
    public ModelRenderer GlowEquipLeftBase3;
    public ModelRenderer GlowArmRight;
    public ModelRenderer GlowEquipRightBase;
    public ModelRenderer GlowEquipRightBase2;
    public ModelRenderer GlowEquipRightBase3;
    public ModelRenderer Face0;
    public ModelRenderer Face1;
    public ModelRenderer Face2;
    public ModelRenderer Face3;
    public ModelRenderer Face4;
    public ModelRenderer ShoesRight;
    public ModelRenderer ShoesLeft;
    public ModelRenderer HeadTail0;
    public ModelRenderer HeadTail1;
    public ModelRenderer HeadTail2;
    
    private int startEmo2 = 0;

    public ModelHeavyCruiserRi() {
        this.textureWidth = 128;
        this.textureHeight = 128;
        this.EquipRightTube2 = new ModelRenderer(this, 82, 56);
        this.EquipRightTube2.setRotationPoint(0.0F, -15.0F, 0.0F);
        this.EquipRightTube2.addBox(-1.5F, -13.0F, -1.5F, 3, 14, 3, 0.0F);
        this.setRotateAngle(EquipRightTube2, 0.7853981633974483F, -0.17453292519943295F, 0.0F);
        this.EquipRightTooth2 = new ModelRenderer(this, 59, 24);
        this.EquipRightTooth2.setRotationPoint(-1.6F, 2.3F, 0.0F);
        this.EquipRightTooth2.addBox(0.0F, 0.0F, -2.5F, 2, 5, 5, 0.0F);
        this.LegRight = new ModelRenderer(this, 0, 84);
        this.LegRight.setRotationPoint(-4.7F, 12.0F, 1.0F);
        this.LegRight.addBox(-3.5F, 0.0F, -3.5F, 7, 17, 7, 0.0F);
        this.setRotateAngle(LegRight, 0.0F, 0.0F, -0.08726646259971647F);
        this.EquipRightTooth1 = new ModelRenderer(this, 44, 13);
        this.EquipRightTooth1.setRotationPoint(0.0F, 4.0F, 0.0F);
        this.EquipRightTooth1.addBox(0.0F, 0.0F, -4.0F, 2, 5, 8, 0.0F);
        this.BoobL = new ModelRenderer(this, 0, 26);
        this.BoobL.mirror = true;
        this.BoobL.setRotationPoint(2.8F, -8.5F, -3.2F);
        this.BoobL.addBox(-4.0F, 0.0F, -1.0F, 8, 5, 5, 0.0F);
        this.setRotateAngle(BoobL, -0.7853981633974483F, -0.08726646259971647F, -0.17453292519943295F);
        this.BoobR = new ModelRenderer(this, 0, 26);
        this.BoobR.setRotationPoint(-2.8F, -8.5F, -3.2F);
        this.BoobR.addBox(-4.0F, 0.0F, -1.0F, 8, 5, 5, 0.0F);
        this.setRotateAngle(BoobR, -0.7853981633974483F, 0.08726646259971647F, 0.17453292519943295F);
        this.EquipLeftBase2 = new ModelRenderer(this, 82, 5);
        this.EquipLeftBase2.setRotationPoint(-2.0F, 0.0F, 0.0F);
        this.EquipLeftBase2.addBox(-3.0F, -7.0F, -5.0F, 8, 7, 10, 0.0F);
        this.setRotateAngle(EquipLeftBase2, 0.0F, 0.0F, 0.025481807079117208F);
        this.EquipRightBase1 = new ModelRenderer(this, 85, 4);
        this.EquipRightBase1.setRotationPoint(-5.0F, 0.0F, -5.5F);
        this.EquipRightBase1.addBox(0.0F, -20.0F, 0.0F, 4, 21, 11, 0.0F);
        this.setRotateAngle(EquipRightBase1, 0.0F, 0.0F, -0.08726646259971647F);
        this.EquipRightTube3 = new ModelRenderer(this, 82, 56);
        this.EquipRightTube3.setRotationPoint(2.0F, -12.0F, 0.0F);
        this.EquipRightTube3.addBox(-3.5F, -23.5F, -1.4F, 3, 25, 3, 0.0F);
        this.setRotateAngle(EquipRightTube3, 1.3962634015954636F, -0.3490658503988659F, 0.0F);
        this.Hair = new ModelRenderer(this, 34, 68);
        this.Hair.setRotationPoint(0.0F, -7.0F, 0.0F);
        this.Hair.addBox(-8.0F, -8.0F, -8.0F, 16, 16, 16, 0.0F);
        this.EquipLeftTube2 = new ModelRenderer(this, 82, 56);
        this.EquipLeftTube2.setRotationPoint(0.0F, -15.0F, 0.0F);
        this.EquipLeftTube2.addBox(-1.5F, -12.0F, -1.5F, 3, 12, 3, 0.0F);
        this.setRotateAngle(EquipLeftTube2, 0.8726646259971648F, 0.0F, 0.0F);
        this.EquipLeftBase4 = new ModelRenderer(this, 83, 9);
        this.EquipLeftBase4.setRotationPoint(0.0F, 6.5F, 2.5F);
        this.EquipLeftBase4.addBox(-6.5F, 0.0F, 0.0F, 11, 16, 6, 0.0F);
        this.setRotateAngle(EquipLeftBase4, 0.08726646259971647F, 0.0F, 0.0F);
        this.Head = new ModelRenderer(this, 42, 100);
        this.Head.setRotationPoint(0.0F, 0.0F, 0.0F);
        this.Head.addBox(-7.0F, -14.0F, -7.0F, 14, 14, 14, 0.0F);
        this.LegLeft = new ModelRenderer(this, 0, 84);
        this.LegLeft.setRotationPoint(4.7F, 12.0F, 1.0F);
        this.LegLeft.addBox(-3.5F, 0.0F, -3.5F, 7, 17, 7, 0.0F);
        this.setRotateAngle(LegLeft, 0.0F, 0.0F, 0.08726646259971647F);
        this.EquipLeftBase3 = new ModelRenderer(this, 77, 5);
        this.EquipLeftBase3.setRotationPoint(0.0F, 0.0F, 0.0F);
        this.EquipLeftBase3.addBox(-7.5F, 5.0F, -10.0F, 13, 19, 10, 0.0F);
        this.setRotateAngle(EquipLeftBase3, -0.08726646259971647F, 0.0F, 0.0F);
        this.EquipRightBase4 = new ModelRenderer(this, 81, 0);
        this.EquipRightBase4.setRotationPoint(-5.0F, 0.0F, -7.5F);
        this.EquipRightBase4.addBox(0.0F, 0.0F, 0.0F, 4, 25, 15, 0.0F);
        this.setRotateAngle(EquipRightBase4, 0.0F, 0.0F, -0.08726646259971647F);
        this.ArmRight = new ModelRenderer(this, 0, 53);
        this.ArmRight.setRotationPoint(-7F, -10.0F, 0.0F);
        this.ArmRight.addBox(-5F, 0.0F, -2.5F, 5, 25, 5, 0.0F);
        this.setRotateAngle(ArmRight, 0.0F, 0.0F, 0.2617993877991494F);
        this.ArmLeft = new ModelRenderer(this, 0, 53);
        this.ArmLeft.mirror = true;
        this.ArmLeft.setRotationPoint(7.0F, -10.0F, 0.0F);
        this.ArmLeft.addBox(0.0F, 0.0F, -2.5F, 5, 25, 5, 0.0F);
        this.setRotateAngle(ArmLeft, 0.0F, 0.0F, -0.2617993877991494F);
        this.Neck = new ModelRenderer(this, 76, 3);
        this.Neck.setRotationPoint(0.0F, -13.0F, 0.0F);
        this.Neck.addBox(-6F, 0F, -6F, 12, 3, 12, 0.0F);
        this.ShoesRight = new ModelRenderer(this, 50, 51);
        this.ShoesRight.setRotationPoint(0.0F, 0.0F, 0.0F);
        this.ShoesRight.addBox(-4.0F, 17.0F, -4.0F, 8, 9, 8, 0.0F);
        this.ShoesLeft = new ModelRenderer(this, 50, 51);
        this.ShoesLeft.mirror = true;
        this.ShoesLeft.setRotationPoint(0.0F, 0.0F, 0.0F);
        this.ShoesLeft.addBox(-4.0F, 17.0F, -4.0F, 8, 9, 8, 0.0F);
        this.EquipRightBase3 = new ModelRenderer(this, 90, 8);
        this.EquipRightBase3.setRotationPoint(1.0F, 14.0F, 0.0F);
        this.EquipRightBase3.addBox(0.0F, 0.0F, -3.5F, 3, 8, 7, 0.0F);
        this.setRotateAngle(EquipRightBase3, 0.0F, 0.0F, -0.2617993877991494F);
        this.Butt = new ModelRenderer(this, 0, 36);
        this.Butt.setRotationPoint(0.0F, 5.0F, 0.0F);
        this.Butt.addBox(-8.0F, 0.0F, -4.1F, 16, 8, 9, 0.0F);
        this.setRotateAngle(Butt, 0.2617993877991494F, 0.0F, 0.0F);
        this.EquipBase = new ModelRenderer(this, 82, 12);
        this.EquipBase.setRotationPoint(0.0F, -11.0F, 4.0F);
        this.EquipBase.addBox(-5.0F, 0.0F, 0.0F, 10, 7, 4, 0.0F);
        this.EquipLeftTube1 = new ModelRenderer(this, 82, 56);
        this.EquipLeftTube1.setRotationPoint(-2.0F, 8.0F, 3.0F);
        this.EquipLeftTube1.addBox(-1.5F, -16.0F, -1.5F, 3, 16, 3, 0.0F);
        this.setRotateAngle(EquipLeftTube1, -0.6981317007977318F, 0.5235987755982988F, 0.0F);
        this.EquipLeftTooth = new ModelRenderer(this, 44, 0);
        this.EquipLeftTooth.setRotationPoint(0.0F, 14.0F, -1.2F);
        this.EquipLeftTooth.addBox(-5.5F, 0.0F, 0.0F, 9, 7, 6, 0.0F);
        this.setRotateAngle(EquipLeftTooth, 0.08726646259971647F, 0.0F, 0.0F);
        this.BodyMain = new ModelRenderer(this, 0, 0);
        this.BodyMain.setRotationPoint(0.0F, -14.0F, 0.0F);
        this.BodyMain.addBox(-7.0F, -10.0F, -4.0F, 14, 16, 8, 0.0F);
        this.EquipLeftBase = new ModelRenderer(this, 76, 1);
        this.EquipLeftBase.setRotationPoint(7.0F, 16.0F, 0.0F);
        this.EquipLeftBase.addBox(-6.0F, 0.0F, -7.0F, 10, 14, 14, 0.0F);
        this.setRotateAngle(EquipLeftBase, 0.0F, 0.0F, 0.08726646259971647F);
        this.EquipRightBase = new ModelRenderer(this, 78, 6);
        this.EquipRightBase.setRotationPoint(-6.0F, 16.0F, 0.0F);
        this.EquipRightBase.addBox(-7.5F, 0.0F, -4.5F, 13, 14, 9, 0.0F);
        this.setRotateAngle(EquipRightBase, 0.0F, 0.0F, -0.08726646259971647F);
        this.EquipRightBase2 = new ModelRenderer(this, 85, 5);
        this.EquipRightBase2.setRotationPoint(-4.2F, 13.0F, 0.0F);
        this.EquipRightBase2.addBox(-5.0F, 0.0F, -5.0F, 5, 10, 10, 0.0F);
        this.setRotateAngle(EquipRightBase2, 0.0F, 0.0F, 0.08726646259971647F);
        this.EquipRightTube1 = new ModelRenderer(this, 82, 56);
        this.EquipRightTube1.setRotationPoint(1.0F, 8.0F, 3.0F);
        this.EquipRightTube1.addBox(-1.5F, -16.0F, -1.5F, 3, 16, 3, 0.0F);
        this.setRotateAngle(EquipRightTube1, -1.0471975511965976F, 0.0F, 0.0F);
        this.Cloak = new ModelRenderer(this, 0, 112);
        this.Cloak.setRotationPoint(0.0F, 2.0F, 8.0F);
        this.Cloak.addBox(-8.0F, 0.0F, 0.0F, 16, 16, 0, 0.0F);
        this.setRotateAngle(Cloak, 1.3089969389957472F, 0.0F, 0.0F); 
        this.EquipLeftTube3 = new ModelRenderer(this, 82, 56);
        this.EquipLeftTube3.setRotationPoint(0.0F, -11.0F, 0.0F);
        this.EquipLeftTube3.addBox(-1.5F, -20.0F, -1.5F, 3, 20, 3, 0.0F);
        this.setRotateAngle(EquipLeftTube3, 1.4486232791552935F, 0.7853981633974483F, 0.2617993877991494F);   
        this.HeadTail0 = new ModelRenderer(this, 20, 54);
        this.HeadTail0.setRotationPoint(0.0F, -16.0F, 8.0F);
        this.HeadTail0.addBox(-4.5F, 0.0F, -3.0F, 9, 14, 6, 0.0F);
        this.setRotateAngle(HeadTail0, 0.2617993877991494F, 0.0F, 0.0F);
        this.HeadTail1 = new ModelRenderer(this, 24, 54);
        this.HeadTail1.setRotationPoint(0.0F, 12.0F, 0.0F);
        this.HeadTail1.addBox(-3.5F, 0.0F, -3.0F, 7, 16, 6, 0.0F);
        this.setRotateAngle(HeadTail1, 0.09F, 0.0F, 0.0F);
        this.HeadTail2 = new ModelRenderer(this, 21, 55);
        this.HeadTail2.setRotationPoint(0.0F, 14.0F, 0.0F);
        this.HeadTail2.addBox(-4F, 0.0F, -2.5F, 8, 18, 5, 0.0F);
        this.setRotateAngle(HeadTail2, -0.1745F, 0.0F, 0.0F);
        this.Face0 = new ModelRenderer(this, 98, 53);
        this.Face0.setRotationPoint(0.0F, 0.0F, -0.1F);
        this.Face0.addBox(-7.0F, -14.0F, -7.0F, 14, 14, 1, 0.0F);
        this.Face1 = new ModelRenderer(this, 98, 68);
        this.Face1.setRotationPoint(0.0F, 0.0F, -0.1F);
        this.Face1.addBox(-7.0F, -14.0F, -7.0F, 14, 14, 1, 0.0F);
        this.Face2 = new ModelRenderer(this, 98, 83);
        this.Face2.setRotationPoint(0.0F, 0.0F, -0.1F);
        this.Face2.addBox(-7.0F, -14.0F, -7.0F, 14, 14, 1, 0.0F);
        this.Face3 = new ModelRenderer(this, 98, 98);
        this.Face3.setRotationPoint(0.0F, 0.0F, -0.1F);
        this.Face3.addBox(-7.0F, -14.0F, -7.0F, 14, 14, 1, 0.0F);
        this.Face4 = new ModelRenderer(this, 98, 113);
        this.Face4.setRotationPoint(0.0F, 0.0F, -0.1F);
        this.Face4.addBox(-7.0F, -14.0F, -7.0F, 14, 14, 1, 0.0F);
        this.EquipRightTube1.addChild(this.EquipRightTube2);
        this.BodyMain.addChild(this.LegRight);
        this.BodyMain.addChild(this.BoobL);
        this.EquipLeftBase.addChild(this.EquipLeftBase2);
        this.EquipRightBase.addChild(this.EquipRightBase1);
        this.EquipRightTube2.addChild(this.EquipRightTube3);
        this.Head.addChild(this.Hair);
        this.EquipLeftTube1.addChild(this.EquipLeftTube2);
        this.EquipLeftBase.addChild(this.EquipLeftBase4);
        this.Neck.addChild(this.Head);
        this.BodyMain.addChild(this.LegLeft);
        this.EquipLeftBase.addChild(this.EquipLeftBase3);
        this.EquipRightBase.addChild(this.EquipRightBase4);
        this.LegLeft.addChild(this.ShoesLeft);
        this.BodyMain.addChild(this.ArmRight);
        this.BodyMain.addChild(this.Neck);
        this.LegRight.addChild(this.ShoesRight);
        this.EquipRightBase.addChild(this.EquipRightBase3);
        this.BodyMain.addChild(this.Butt);   
        this.BodyMain.addChild(this.EquipBase);
        this.EquipLeftBase.addChild(this.EquipLeftTube1);     
        this.BodyMain.addChild(this.BoobR);
        this.ArmLeft.addChild(this.EquipLeftBase);
        this.ArmRight.addChild(this.EquipRightBase);
        this.EquipRightBase.addChild(this.EquipRightBase2);
        this.EquipRightBase.addChild(this.EquipRightTube1);
        this.Neck.addChild(this.Cloak);       
        this.BodyMain.addChild(this.ArmLeft);
        this.EquipLeftTube2.addChild(this.EquipLeftTube3);
        this.Head.addChild(this.HeadTail0);
        this.HeadTail0.addChild(this.HeadTail1);
        this.HeadTail1.addChild(this.HeadTail2);
        
        //�H�U���o���ҫ���[, �������ҫ���ӫG�׳]��240
        //�y����[
        this.GlowBodyMain = new ModelRenderer(this, 0, 0);
        this.GlowBodyMain.setRotationPoint(0.0F, -14.0F, 0.0F);
        this.GlowNeck = new ModelRenderer(this, 0, 0);
        this.GlowNeck.setRotationPoint(0.0F, -13.0F, 0.0F);
        this.GlowHead = new ModelRenderer(this, 42, 100);
        this.GlowHead.setRotationPoint(0.0F, 0.0F, 0.0F);
        //�˳Ƥ�����[, �ѩ�˳Ƥ���, �i�X�֨���u��[
        this.GlowArmLeft = new ModelRenderer(this, 0, 0);
        this.GlowArmLeft.setRotationPoint(7F, -10F, 0F);
        this.setRotateAngle(GlowArmLeft, 0.0F, 0.0F, -0.2617993877991494F);      
        this.GlowEquipLeftBase = new ModelRenderer(this, 0, 0);
        this.GlowEquipLeftBase.setRotationPoint(7F, 16F, 0F);
        this.setRotateAngle(GlowEquipLeftBase, 0.0F, 0.0F, 0.08726646259971647F);   
        this.GlowEquipLeftBase3 = new ModelRenderer(this, 0, 0);
        this.GlowEquipLeftBase3.setRotationPoint(0F, 0F, 0F);
        this.setRotateAngle(GlowEquipLeftBase3, -0.08726646259971647F, 0.0F, 0.0F); 
        this.GlowArmRight = new ModelRenderer(this, 0, 53);
        this.GlowArmRight.setRotationPoint(-7F, -10F, 0F);
        this.setRotateAngle(GlowArmRight, 0.0F, 0.0F, 0.2617993877991494F);
        this.GlowEquipRightBase = new ModelRenderer(this, 0, 0);
        this.GlowEquipRightBase.setRotationPoint(-6F, 16F, 0F);
        this.setRotateAngle(GlowEquipRightBase, 0.0F, 0.0F, -0.08726646259971647F);
        this.GlowEquipRightBase2 = new ModelRenderer(this, 0, 0);
        this.GlowEquipRightBase2.setRotationPoint(-4.2F, 13F, 0F);
        this.setRotateAngle(GlowEquipRightBase2, 0.0F, 0.0F, 0.08726646259971647F);
        this.GlowEquipRightBase3 = new ModelRenderer(this, 0, 0);
        this.GlowEquipRightBase3.setRotationPoint(1.0F, 14.0F, 0.0F);
        this.setRotateAngle(GlowEquipRightBase3, 0.0F, 0.0F, -0.2617993877991494F);
        
        this.GlowBodyMain.addChild(this.GlowNeck);
        this.GlowBodyMain.addChild(this.GlowArmLeft);
        this.GlowBodyMain.addChild(this.GlowArmRight);
        this.GlowNeck.addChild(this.GlowHead);
        this.GlowHead.addChild(this.Face0);
        this.GlowHead.addChild(this.Face1);
        this.GlowHead.addChild(this.Face2);
        this.GlowHead.addChild(this.Face3);
        this.GlowHead.addChild(this.Face4);
        this.GlowArmLeft.addChild(this.GlowEquipLeftBase);
        this.GlowEquipLeftBase.addChild(this.GlowEquipLeftBase3);
        this.GlowEquipLeftBase3.addChild(this.EquipLeftTooth);
        this.GlowArmRight.addChild(this.GlowEquipRightBase);
        this.GlowEquipRightBase.addChild(this.GlowEquipRightBase2);
        this.GlowEquipRightBase.addChild(this.GlowEquipRightBase3);
        this.GlowEquipRightBase2.addChild(this.EquipRightTooth1);
        this.GlowEquipRightBase3.addChild(this.EquipRightTooth2);
        
    }

    @Override
    public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5) { 
    	GL11.glPushMatrix();
    	GL11.glEnable(GL11.GL_BLEND);			//�}�ҳz���׼Ҧ�
    	GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    	GL11.glScalef(0.5F, 0.5F, 0.5F); 	
    	
    	setRotationAngles(f, f1, f2, f3, f4, f5, entity);
    	
    	this.BodyMain.render(f5);
    	
    	//�G�׳]��240
    	GL11.glDisable(GL11.GL_LIGHTING);
    	OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
    	this.GlowBodyMain.render(f5);	
    	GL11.glEnable(GL11.GL_LIGHTING);
    	
    	GL11.glDisable(GL11.GL_BLEND);			//�}�ҳz���׼Ҧ�
    	GL11.glPopMatrix();
    }

    /**
     * This is a helper function from Tabula to set the rotation of model parts
     */
    public void setRotateAngle(ModelRenderer modelRenderer, float x, float y, float z) {
        modelRenderer.rotateAngleX = x;
        modelRenderer.rotateAngleY = y;
        modelRenderer.rotateAngleZ = z;
    }
    
    //for idle/run animation
    public void setRotationAngles(float f, float f1, float f2, float f3, float f4, float f5, Entity entity) {
      super.setRotationAngles(f, f1, f2, f3, f4, f5, entity);
  
      BasicEntityShip ent = (BasicEntityShip)entity;
      
      showEquip(ent);
      
      rollEmotion(ent);
      
      motionHumanPos(f, f1, f2, f3, f4, ent);

      setGlowRotation();
    }
    
    //�]�w�ҫ��o��������rotation
    private void setGlowRotation() {
		this.GlowArmLeft.rotateAngleX = this.ArmLeft.rotateAngleX;
		this.GlowArmLeft.rotateAngleY = this.ArmLeft.rotateAngleY;
		this.GlowArmLeft.rotateAngleZ = this.ArmLeft.rotateAngleZ;
		this.GlowArmRight.rotateAngleX = this.ArmRight.rotateAngleX;
		this.GlowArmRight.rotateAngleY = this.ArmRight.rotateAngleY;
		this.GlowArmRight.rotateAngleZ = this.ArmRight.rotateAngleZ;
		this.GlowBodyMain.rotateAngleX = this.BodyMain.rotateAngleX;
		this.GlowBodyMain.rotateAngleY = this.BodyMain.rotateAngleY;
		this.GlowBodyMain.rotateAngleZ = this.BodyMain.rotateAngleZ;
		this.GlowNeck.rotateAngleX = this.Neck.rotateAngleX;
		this.GlowNeck.rotateAngleY = this.Neck.rotateAngleY;
		this.GlowNeck.rotateAngleZ = this.Neck.rotateAngleZ;
		this.GlowHead.rotateAngleX = this.Head.rotateAngleX;
		this.GlowHead.rotateAngleY = this.Head.rotateAngleY;
		this.GlowHead.rotateAngleZ = this.Head.rotateAngleZ;
	}

	//���}���ʭp��
  	private void motionHumanPos(float f, float f1, float f2, float f3, float f4, BasicEntityShip ent) {   
  		float angleZ = MathHelper.cos(f2*0.08F);
  		float addk1 = 0;
  		float addk2 = 0;
  		
  		//leg move parm
  		addk1 = MathHelper.cos(f * 0.6662F) * 1.4F * f1;
	  	addk2 = MathHelper.cos(f * 0.6662F + 3.1415927F) * 1.4F * f1;

  	    //�����Y�� �Ϩ�ݤH, ���ݤH�ɫ����\���Y��
	    this.Head.rotateAngleY = f3 / 57.29578F;	//���k���� �����নrad �Y���H57.29578
	    this.Head.rotateAngleX = f4 / 57.29578F; 	//�W�U����
	    
	    //���`���߰ʧ@
	    GL11.glTranslatef(0F, 1.5F, 0F);
	    this.Cloak.rotateAngleX = angleZ * 0.2F + 1F;	    
  	    this.BoobL.rotateAngleX = -angleZ * 0.06F - 0.73F;
  	    this.BoobR.rotateAngleX = -angleZ * 0.06F - 0.73F;
	  	this.ArmLeft.rotateAngleZ = angleZ * -0.15F - 0.3F;
  	    this.ArmRight.rotateAngleZ = angleZ * 0.15F + 0.3F;
	    this.ArmLeft.rotateAngleX = 0F;
		this.ArmRight.rotateAngleX = 0F;
		this.BodyMain.rotateAngleX = 0F;
		this.LegLeft.rotateAngleZ = 0.087F;
		this.LegRight.rotateAngleZ = -0.087F;
		this.LegLeft.rotateAngleY = 0F;
		this.LegRight.rotateAngleY = 0F;
		this.HeadTail0.rotateAngleX = angleZ * 0.05F + 0.26F;
		this.HeadTail1.rotateAngleX = angleZ * 0.1F + 0.09F;
	    
	    if(ent.isSprinting() || f1 > 0.9F) {	//�b�]�ʧ@
  			this.ArmLeft.rotateAngleX = 1F;
  			this.ArmRight.rotateAngleX = 1F;
  			this.BodyMain.rotateAngleX = 0.5F;
  			this.HeadTail0.rotateAngleX = angleZ * 0.05F + 0.8F;
  			addk1 -= 0.4F;
			addk2 -= 0.4F;
  		}
	    else {
	    	startEmo2 = ent.getStartEmotion2();
	    	
	    	//�Y���ɱװʧ@, �u�b�b�]�H�~��roll
	    	if(startEmo2 > 0) {
	    		--startEmo2;
	    		ent.setStartEmotion2(startEmo2);
	    	}
	    	
		    if(startEmo2 <= 0) {
		    	startEmo2 = 360;
		    	ent.setStartEmotion2(startEmo2);	//cd = 6sec  	
		    	
		    	if(ent.getRNG().nextInt(3) == 0) {
		    		ent.setStateFlag(ID.F.HeadTilt, true);
		    	}
		    	else {
		    		ent.setStateFlag(ID.F.HeadTilt, false);
		    	}
		    }
	    }//end if sprint
	    
	    //roll�Y���ɱת�
	    if(ent.getStateFlag(ID.F.HeadTilt)) {
	    	if(ent.getStateEmotion(ID.S.Emotion2) == 1) {	//���e�w�g�ɱ�, �h�~��ɱ�
	    		this.Head.rotateAngleZ = -0.24F;
	    	}
	    	else {
		    	this.Head.rotateAngleZ = (360 - startEmo2) * -0.03F;
		    	
		    	if(this.Head.rotateAngleZ < -0.24F) {
		    		ent.setStateEmotion(ID.S.Emotion2, 1, false);
		    		this.Head.rotateAngleZ = -0.24F;
		    	}
	    	}	
	    }
	    else {
	    	if(ent.getStateEmotion(ID.S.Emotion2) == 0) {	//�������e����
	    		this.Head.rotateAngleZ = 0F;
	    	}
	    	else {
		    	this.Head.rotateAngleZ = -0.24F + (360 - startEmo2) * 0.03F;
		    	
		    	if(this.Head.rotateAngleZ > 0F) {
		    		this.Head.rotateAngleZ = 0F;
		    		ent.setStateEmotion(ID.S.Emotion2, 0, false);
		    	}
	    	}
	    }
  		
	    if(ent.isSneaking()) {		//���ʧ@
  			this.ArmLeft.rotateAngleX = 0.7F;
  			this.ArmRight.rotateAngleX = 0.7F;
  			this.BodyMain.rotateAngleX = 0.5F;
  			addk1 -= 0.6F;
			addk2 -= 0.6F;
  		}
  		
	    if(ent.isSitting() || ent.isRiding()) {  //�M���ʧ@ 			
  			if(ent.getStateEmotion(ID.S.Emotion) == ID.Emotion.BORED) {
		    	GL11.glTranslatef(0F, 1.4F, 0F);
				this.ArmLeft.rotateAngleX = 0.6F;
	  			this.ArmRight.rotateAngleX = 0.6F;
	  			this.ArmLeft.rotateAngleZ = -0.6F;
	  			this.ArmRight.rotateAngleZ = 0.6F;
				this.BodyMain.rotateAngleX = -0.6F;
				this.Head.rotateAngleX -= 0.35F;
				addk1 = -1.58F;
				addk2 = -1.58F;		
				this.LegLeft.rotateAngleZ = 1.2F;
				this.LegRight.rotateAngleZ = -1.2F;
				this.LegLeft.rotateAngleY = -0.75F;
				this.LegRight.rotateAngleY = 0.75F;
				this.HeadTail0.rotateAngleX += 0.7F;
  			}
  			else {
  				GL11.glTranslatef(0F, 1.4F, 0F);
  				this.ArmLeft.rotateAngleX = -0.6F;
  	  			this.ArmRight.rotateAngleX = -0.6F;
  	  			this.ArmLeft.rotateAngleZ = 0.5F;
  	  			this.ArmRight.rotateAngleZ = -0.5F;
  				this.BodyMain.rotateAngleX = 0.3F;
  				this.Head.rotateAngleX -= 0.35F;
  				addk1 = -2F;
  				addk2 = -2F;
  				this.LegLeft.rotateAngleY = 0.15F;
  				this.LegRight.rotateAngleY = -0.15F;
  				this.LegLeft.rotateAngleZ = 1.2F;
  				this.LegRight.rotateAngleZ = -1.2F; 				
  			}			
  		}
	    
	    //leg motion
	    this.LegLeft.rotateAngleX = addk1;
	    this.LegRight.rotateAngleX = addk2;
	    
	    //�����ɶ��K�N������V���	    
	    if(ent.attackTime > 0) {
	    	this.ArmLeft.rotateAngleX = f4 / 57.29578F - 1.5F;
	    	this.ArmRight.rotateAngleZ = 0.7F; 
	    	this.ArmRight.rotateAngleX = 0.4F; 
	    }
	}
    
    private void showEquip(BasicEntityShip ent) {
    	switch(ent.getStateEmotion(ID.S.State)) {
    	case ID.State.EQUIP00:	//equip only
    	case ID.State.EQUIP03:
    		this.EquipBase.isHidden = false;
			this.EquipLeftBase.isHidden = false;
			this.EquipRightBase.isHidden = false;
			this.GlowEquipLeftBase.isHidden = false;
			this.GlowEquipRightBase.isHidden = false;
			this.HeadTail0.isHidden = true;
			break;
    	case ID.State.EQUIP01:	//hair only
    	case ID.State.EQUIP04:
    		this.EquipBase.isHidden = true;
			this.EquipLeftBase.isHidden = true;
			this.EquipRightBase.isHidden = true;
			this.GlowEquipLeftBase.isHidden = true;
			this.GlowEquipRightBase.isHidden = true;
			this.HeadTail0.isHidden = false;
			break;
    	case ID.State.EQUIP02:	//hair + equip
    	case ID.State.EQUIP05:
    		this.EquipBase.isHidden = false;
			this.EquipLeftBase.isHidden = false;
			this.EquipRightBase.isHidden = false;
			this.GlowEquipLeftBase.isHidden = false;
			this.GlowEquipRightBase.isHidden = false;
			this.HeadTail0.isHidden = false;
			break;
    	case ID.State.NORMAL:
    		this.EquipBase.isHidden = true;
			this.EquipLeftBase.isHidden = true;
			this.EquipRightBase.isHidden = true;
			this.GlowEquipLeftBase.isHidden = true;
			this.GlowEquipRightBase.isHidden = true;
			this.HeadTail0.isHidden = true;
			break;
    	}
  	}
  	
    //�H�������ܪ��� 
    private void rollEmotion(BasicEntityShip ent) { 
    	switch(ent.getStateEmotion(ID.S.Emotion)) {
    	case ID.Emotion.BLINK:	//blink
    		EmotionBlink(ent);
    		break;
    	case ID.Emotion.T_T:	//cry
    		if(ent.getStartEmotion() <= 0) { setFace(2); }
    		break;
    	case ID.Emotion.O_O:
    		EmotionStaring(ent);
			break;
    	case ID.Emotion.HUNGRY:
    		if(ent.getStartEmotion() <= 0) { setFace(4); }
			break;
    	case ID.Emotion.BORED:
    	default:						//normal face
    		//reset face to 0
    		if(ent.getStartEmotion() <= 0) setFace(0); 			    
    		//roll emotion (3 times) every 6 sec
    		//1 tick in entity = 3 tick in model class (20 vs 60 fps)
    		if(ent.ticksExisted % 120 == 0) {  			
        		int emotionRand = ent.getRNG().nextInt(10);   		
        		if(emotionRand > 7) {
        			EmotionBlink(ent);
        		} 		
        	}
    		break;
    	}	
    }
    
	private void EmotionStaring(BasicEntityShip ent) {	
    	if(ent.getStartEmotion() == -1) {
			ent.setStartEmotion(ent.ticksExisted);		//���}�l�ɶ�
		}
    	
    	int EmoTime = ent.ticksExisted - ent.getStartEmotion();
    	
    	if(EmoTime > 41) {	//reset face
    		setFace(0);
			ent.setStateEmotion(ID.S.Emotion, ID.Emotion.NORMAL, false);
			ent.setStartEmotion(-1);
    	}
    	else if(EmoTime > 1) {
    		setFace(3);
    	}
	}

	//�w���ʧ@, this emotion is CLIENT ONLY, no sync packet required
  	private void EmotionBlink(BasicEntityShip ent) {
  		if(ent.getStateEmotion(ID.S.Emotion) == ID.Emotion.NORMAL) {	//�n�b�S�����A�~����		
  			ent.setStartEmotion(ent.ticksExisted);		//���}�l�ɶ�
  			ent.setStateEmotion(ID.S.Emotion, ID.Emotion.BLINK, false);	//�аO����blink
  		}
  		
  		int EmoTime = ent.ticksExisted - ent.getStartEmotion();
    	 		
    	if(EmoTime > 46) {	//reset face
    		setFace(0);
			ent.setStateEmotion(ID.S.Emotion, ID.Emotion.NORMAL, false);
			ent.setStartEmotion(-1);
    	}
    	else if(EmoTime > 35) {
    		setFace(1);
    	}
    	else if(EmoTime > 25) {
    		setFace(0);
    	}
    	else if(EmoTime > 1) {
    		setFace(1);
    	}		
  	}
  	
  	//�]�w��ܪ��y��
  	private void setFace(int emo) {
  		switch(emo) {
  		case 0:
  			this.Face0.isHidden = false;
  			this.Face1.isHidden = true;
  			this.Face2.isHidden = true;
  			this.Face3.isHidden = true;
  			this.Face4.isHidden = true;
  			break;
  		case 1:
  			this.Face0.isHidden = true;
  			this.Face1.isHidden = false;
  			this.Face2.isHidden = true;
  			this.Face3.isHidden = true;
  			this.Face4.isHidden = true;
  			break;
  		case 2:
  			this.Face0.isHidden = true;
  			this.Face1.isHidden = true;
  			this.Face2.isHidden = false;
  			this.Face3.isHidden = true;
  			this.Face4.isHidden = true;
  			break;
  		case 3:
  			this.Face0.isHidden = true;
  			this.Face1.isHidden = true;
  			this.Face2.isHidden = true;
  			this.Face3.isHidden = false;
  			this.Face4.isHidden = true;
  			break;
  		case 4:
  			this.Face0.isHidden = true;
  			this.Face1.isHidden = true;
  			this.Face2.isHidden = true;
  			this.Face3.isHidden = true;
  			this.Face4.isHidden = false;
  			break;
  		default:
  			break;
  		}
  	}
  	
  	
  	
}

