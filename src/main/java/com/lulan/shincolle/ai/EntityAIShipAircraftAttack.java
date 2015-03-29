package com.lulan.shincolle.ai;

import java.util.Random;

import com.lulan.shincolle.entity.BasicEntityAirplane;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.EntityAirplane;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.utility.EntityHelper;
import com.lulan.shincolle.utility.LogHelper;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.util.MathHelper;

/**AIRCRAFT ATTACK AI
 * entity������@attackEntityWithAmmo, attackEntityWithHeavyAmmo ��Ӥ�k
 */
public class EntityAIShipAircraftAttack extends EntityAIBase {
	
	private Random rand = new Random();
    private BasicEntityAirplane host;  	//entity with AI
    private EntityLivingBase target;  //entity of target
    private int atkDelay = 0;		//attack delay (attack when time <= 0)
    private int maxDelay = 0;	    //attack max delay (calc from attack speed)  
    private float attackRange = 4F;	//attack range
    private float rangeSq;			//attack range square
    private double[] randPos = new double[3];		//random position
    
    //���u�e�i���\��
    private double distSq, distX, distY, distZ, motX, motY, motZ;	//��ؼЪ����u�Z��(������)    
    private double distRanSqrt, distRanX, distRanY, distRanZ, ranX, ranY, ranZ;	//�H���䪺�ت��a
    
    public EntityAIShipAircraftAttack(BasicEntityAirplane host) {
        if (!(host instanceof BasicEntityAirplane)) {
            throw new IllegalArgumentException("AircraftAttack AI requires BasicEntityAirplane");
        }
        else {
            this.host = host;
            this.setMutexBits(3);
        }
    }

    //check ai start condition
    public boolean shouldExecute() {
    	EntityLivingBase target = this.host.getAttackTarget();

        if (this.host.ticksExisted > 30 && target != null && target.isEntityAlive() && 
        	((this.host.useAmmoLight && this.host.numAmmoLight > 0) || 
        	(this.host.useAmmoHeavy && this.host.numAmmoHeavy > 0))) {   
        	this.target = target;

            return true;
        }
        return false;
    }
    
    //init AI parameter, call once every target
    @Override
    public void startExecuting() {
    	this.maxDelay = (int)(60F / (this.host.atkSpeed));
    	this.atkDelay = 0;
        this.attackRange = 6F;
        this.rangeSq = this.attackRange * this.attackRange;
        distSq = distX = distY = distZ = motX = motY = motZ = 0D;
        //AI���ʳ]�w
        randPos[0] = target.posX;
        randPos[1] = target.posX;
        randPos[2] = target.posX;
    }

    //�P�w�O�_�~��AI�G ��target�N�~��, �Ϊ̤w�g���ʧ����N�~��
    public boolean continueExecuting() {
        return this.shouldExecute();
    }

    //���mAI��k
    public void resetTask() {
        this.target = null;
        this.atkDelay = 0;
    }

    //�i��AI
    public void updateTask() {
    	boolean onSight = false;	//�P�w���g�O�_�L��ê��
    	  	
    	if(this.target != null) {  //for lots of NPE issue-.-
            onSight = this.host.getEntitySenses().canSee(this.target);
            //�ؼжZ���p��
            this.distX = this.target.posX - this.host.posX;
    		this.distY = this.target.posY+2D - this.host.posY;
    		this.distZ = this.target.posZ - this.host.posZ;	
    		this.distSq = distX*distX + distY*distY + distZ*distZ;

        	if(this.host.ticksExisted % 10 == 0) {
	        	randPos = EntityHelper.findRandomPosition(this.host, this.target, 3D, 3D, 0);    	
        	}
        	
        	this.distRanX = randPos[0] - this.host.posX;
    		this.distRanY = randPos[1] - this.host.posY;
    		this.distRanZ = randPos[2] - this.host.posZ;	
    		this.distRanSqrt = MathHelper.sqrt_double(distX*distX + distY*distY + distZ*distZ);
    		
	        //moving
        	double speed = this.host.getEntityAttribute(SharedMonsterAttributes.movementSpeed).getBaseValue();
	        if(this.distSq > this.rangeSq) {
				this.motX = (this.distRanX / this.distRanSqrt) * speed * 1.0D;
				this.motY = (this.distRanY / this.distRanSqrt) * speed * 0.5D;
				this.motZ = (this.distRanZ / this.distRanSqrt) * speed * 1.0D;
	        }
	        else {
	        	this.motX = (this.distRanX / this.distRanSqrt) * speed * 0.3D;
				this.motY = (this.distRanY / this.distRanSqrt) * speed * 0.15D;
				this.motZ = (this.distRanZ / this.distRanSqrt) * speed * 0.3D;
	        }
	        
//	        LogHelper.info("DEBUG : motX?"+motX+" "+motY+" "+motZ);
	        if(this.motX > 0.7D) this.motX = 0.7D;
	        if(this.motX < -0.7D) this.motX = -0.7D;
	        if(this.motY > 0.5D) this.motY = 0.5D;
	        if(this.motY < -0.7D) this.motY = -0.5D;
	        if(this.motZ > 0.7D) this.motZ = 0.7D;
	        if(this.motZ < -0.7D) this.motZ = -0.7D;
            
	        this.host.motionX = motX;
			this.host.motionY = motY;
			this.host.motionZ = motZ;
	        
	        //delay time decr
	        this.atkDelay--;
	        
	        onSight = this.host.getEntitySenses().canSee(this.target);

	        //�Yattack delay�˼Ƨ��F�B�˷Ǯɶ����[, �h�}�l����
	        if(this.atkDelay <= 0 && onSight) {
	        	//�ѩ�ĥ�����u�|�� or ���䤤�@�ا���, �]��AI�o��@��cooldown, ���|�y���v�T
	        	if(this.distSq < this.rangeSq && this.host.numAmmoLight > 0 && this.host.useAmmoLight) {
		            //attack method
		            this.host.attackEntityWithAmmo(this.target);
		            this.atkDelay = this.maxDelay;
	        	}
	        	
	        	if(this.distSq < this.rangeSq && this.host.numAmmoHeavy > 0 && this.host.useAmmoHeavy) {
		            //attack method
		            this.host.attackEntityWithHeavyAmmo(this.target);
		            this.atkDelay = this.maxDelay;
	        	}	
	        }
    	}//end attack target != null
    }
}
