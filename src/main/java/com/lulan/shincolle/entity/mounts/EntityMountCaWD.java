package com.lulan.shincolle.entity.mounts;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.world.World;

import com.lulan.shincolle.entity.BasicEntityMountLarge;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.utility.ParticleHelper;

public class EntityMountCaWD extends BasicEntityMountLarge {
	
    public EntityMountCaWD(World world) {	//client side
		super(world);
		this.setSize(1.9F, 1.8F);
		this.isImmuneToFire = true;
		this.ridePos = new float[] {-1F, -1F, 1.5F};
	}
    
    public EntityMountCaWD(World world, BasicEntityShip host) {	//server side
		super(world);
        this.host = host;
        this.isImmuneToFire = true;
        this.ridePos = new float[] {-1F, -1F, 1.5F};
        
        //basic attr
        this.atkRange = host.getStateFinal(ID.HIT);
        this.movSpeed = host.getStateFinal(ID.MOV);
        
        //AI flag
        this.StateEmotion = 0;
        this.StateEmotion2 = 0;
        this.StartEmotion = 0;
        this.StartEmotion2 = 0;
        this.headTilt = false;
           
        //�]�w��m
        this.posX = host.posX;
        this.posY = host.posY;
        this.posZ = host.posZ;
        this.setPosition(this.posX, this.posY, this.posZ);
 
	    //�]�w���ݩ�
        setupAttrs();
        
		if(this.getHealth() < this.getMaxHealth()) this.setHealth(this.getMaxHealth());
				
		//�]�wAI
		this.setAIList();
	}
    
    @Override
	public float getEyeHeight() {
		return 1.7F;
	}
    
    @Override
    public double getMountedYOffset() {
    	return this.height;
    }

	@Override
	public void onUpdate() {
		super.onUpdate();
//		this.ridePos = new float[] {-1.0F, 0F, 1.8F};
		this.ridePos = new float[] {0.0F, 0F, 0.3F};
//		//client side
//		if(this.worldObj.isRemote) {
//			if(this.ticksExisted % 8 == 0) {
//				//�L�ګ_���ϯS��
//				float[] partPos1 = ParticleHelper.rotateParticleByAxis(0F, -1.0F, this.renderYawOffset / 57.2958F, 1F);
//				float[] partPos2 = ParticleHelper.rotateParticleByAxis(0F, -1.8F, this.renderYawOffset / 57.2958F, 1F);
//				ParticleHelper.spawnAttackParticleAt(this.posX + partPos1[1], this.posY + 0.9F, this.posZ + partPos1[0], 
//							0D, 0.1D, 0D, (byte)18);
//				ParticleHelper.spawnAttackParticleAt(this.posX + partPos2[1], this.posY + 0.9F, this.posZ + partPos2[0], 
//						0D, 0.1D, 0D, (byte)18);
//			}
//		}
	}
	
	//������, �S�Ĭ���������
	@Override
	public boolean attackEntityWithAmmo(Entity target) {
		if(this.host != null) {
			return host.attackEntityWithAmmo(target);
		}
		
		return false;
	}
	
	//���έ�����
	@Override
	public boolean attackEntityWithHeavyAmmo(Entity target) {
		return false;
	}


}





