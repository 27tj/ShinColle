package com.lulan.shincolle.entity;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.world.World;

import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.utility.LogHelper;
import com.lulan.shincolle.utility.ParticleHelper;

public class EntityMountAfH extends BasicEntityMount {
	
    public EntityMountAfH(World world) {	//client side
		super(world);
		this.setSize(1.9F, 1.3F);
		this.isImmuneToFire = true;
	}
    
    public EntityMountAfH(World world, BasicEntityShip host) {	//server side
		super(world);
        this.host = host;
        this.isImmuneToFire = true;
        
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
	    getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(host.getStateFinal(ID.HP) * 0.5D);
		getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(movSpeed);
		getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(host.getStateFinal(ID.HIT) + 16); //������ؼ�, ���|���d��
		getEntityAttribute(SharedMonsterAttributes.knockbackResistance).setBaseValue((double)host.getShipLevel() / 150D);
		
		if(this.getHealth() < this.getMaxHealth()) this.setHealth(this.getMaxHealth());
				
		//�]�wAI
		this.setAIList();
	}
    
    @Override
	public float getEyeHeight() {
		return this.height * 1.3F;
	}
    
    @Override
    public double getMountedYOffset() {
//        return (double)this.height * 0D;
    	return this.height;
    }

	@Override
	public float[] getRidePos() {
		return new float[] {-1F, -1F, 1.5F};
	}

	@Override
	public void onUpdate() {
		super.onUpdate();
//		LogHelper.info("DEBUG : mount depth "+this.ShipDepth);
		
		//client side
		if(this.worldObj.isRemote) {
			if(this.ticksExisted % 8 == 0) {
				//�L�ګ_���ϯS��
				float[] partPos1 = ParticleHelper.rotateParticleByAxis(0F, -1.0F, this.renderYawOffset / 57.2958F, 1F);
				float[] partPos2 = ParticleHelper.rotateParticleByAxis(0F, -1.8F, this.renderYawOffset / 57.2958F, 1F);
				ParticleHelper.spawnAttackParticleAt(this.posX + partPos1[1], this.posY + 0.9F, this.posZ + partPos1[0], 
							0D, 0.1D, 0D, (byte)18);
				ParticleHelper.spawnAttackParticleAt(this.posX + partPos2[1], this.posY + 0.9F, this.posZ + partPos2[0], 
						0D, 0.1D, 0D, (byte)18);
			}
		}
	}

}


