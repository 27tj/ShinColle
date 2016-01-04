package com.lulan.shincolle.ai;

import java.util.Random;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;

import com.lulan.shincolle.entity.BasicEntityAirplane;
import com.lulan.shincolle.utility.EntityHelper;
import com.lulan.shincolle.utility.LogHelper;

/**AIRCRAFT ATTACK AI
 * entity������@attackEntityWithAmmo, attackEntityWithHeavyAmmo ��Ӥ�k
 */
public class EntityAIShipAircraftAttack extends EntityAIBase {
	
	private Random rand = new Random();
    private BasicEntityAirplane host;  	//entity with AI
    private Entity target;  //entity of target
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
    @Override
	public boolean shouldExecute() {
    	Entity target = this.host.getEntityTarget();

        if (this.host.ticksExisted > 18 && target != null && target.isEntityAlive() && 
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
    	this.maxDelay = (int)(80F / (this.host.atkSpeed));
        this.attackRange = 7F;
        this.rangeSq = this.attackRange * this.attackRange;
        distSq = distX = distY = distZ = motX = motY = motZ = 0D;
        //AI���ʳ]�w
        randPos[0] = target.posX;
        randPos[1] = target.posX;
        randPos[2] = target.posX;
    }

    //�P�w�O�_�~��AI�G ��target�N�~��, �Ϊ̤w�g���ʧ����N�~��
    @Override
	public boolean continueExecuting() {
        return this.shouldExecute()  || (target != null && target.isEntityAlive() && !this.host.getShipNavigate().noPath());
    }

    //���mAI��k
    @Override
	public void resetTask() {
        this.target = null;
        //keep moving, do not stop in air
        randPos = EntityHelper.findRandomPosition(this.host, this.host, 4D, 2D, 2);
        this.host.getShipNavigate().tryMoveToXYZ(randPos[0], randPos[1], randPos[2], 1D);
    }

    //�i��AI
    @Override
	public void updateTask() {
    	boolean onSight = false;	//�P�w���g�O�_�L��ê��
    	  	
    	if(this.target != null) {
            onSight = this.host.getEntitySenses().canSee(this.target);
//            LogHelper.info("DEBUG : rand pos: "+this.target);
            
            //�ؼжZ���p��
            this.distX = this.target.posX - this.host.posX;
    		this.distY = this.target.posY+2D - this.host.posY;
    		this.distZ = this.target.posZ - this.host.posZ;	
    		this.distSq = distX*distX + distY*distY + distZ*distZ;

        	if(this.host.ticksExisted % 16 == 0) {
	        	randPos = EntityHelper.findRandomPosition(this.host, this.target, 3D, 3D, 1);
//	        	LogHelper.info("DEBUG : rand pos: "+this.host+" "+randPos[0]+" "+randPos[1]+" "+randPos[2]);
	        	//�ؼЦb�g�{�~, �h100%�t�׫e�i
	        	if(this.distSq > this.rangeSq) {
		        	this.host.getShipNavigate().tryMoveToXYZ(randPos[0], randPos[1], randPos[2], 1D);
	        	}
	        	//�ؼЦb�g�{��, �h�w�t����
	        	else {
		        	this.host.getShipNavigate().tryMoveToXYZ(randPos[0], randPos[1], randPos[2], 0.4D);
	        	}
        	}
	        
	        //delay time decr
	        this.atkDelay--;
	        
	        onSight = this.host.getEntitySenses().canSee(this.target);

	        //�Yattack delay�˼Ƨ��F�B�˷Ǯɶ����[, �h�}�l����
	        if(this.atkDelay <= 0 && onSight) {
	        	//�ѩ�ĥ�����u�|�� or ���䤤�@�ا���, �]��AI�o��@��cooldown
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
