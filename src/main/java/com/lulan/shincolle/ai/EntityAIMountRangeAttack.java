package com.lulan.shincolle.ai;

import java.util.Random;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;

import com.lulan.shincolle.entity.BasicEntityMount;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.IShipAttack;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.utility.EntityHelper;

/**ENTITY RANGE ATTACK AI
 * �q���}���g�bAI�ק�Ө�
 * entity������@attackEntityWithAmmo, attackEntityWithHeavyAmmo ��Ӥ�k
 */
public class EntityAIMountRangeAttack extends EntityAIBase {
	
	private Random rand = new Random();
    private BasicEntityShip host;  	//AI host entity
    private BasicEntityMount mount;	//mount
    private EntityLivingBase attackTarget;  //entity of target
    private int delayLight = 0;			//light attack delay (attack when time 0)
    private int maxDelayLight;	    //light attack max delay (calc from ship attack speed)
    private int delayHeavy = 0;			//heavy attack delay
    private int maxDelayHeavy;	    //heavy attack max delay (= light delay x5)    
    private int onSightTime;		//target on sight time
    private float attackRange;		//attack range
    private float rangeSq;			//attack range square
    private int aimTime;			//time before fire
    
    //���u�e�i���\��
    private double distSq, distSqrt, distX, distY, distZ, motX, motY, motZ;	//��ؼЪ����u�Z��(������)
    
 
    //parm: host, move speed, p4, attack delay, p6
    public EntityAIMountRangeAttack(BasicEntityMount mount) {
    	this.mount = mount;
        this.host = (BasicEntityShip) mount.getOwner();
        this.setMutexBits(3);
    }

    //check ai start condition
    public boolean shouldExecute() {
    	//for entity ship
    	if(host != null) {
    		if(this.host.isSitting()) return false;
        	
        	EntityLivingBase target = this.host.getAttackTarget();
        	
            if(target != null && !target.isDead &&
            	((this.host.getStateFlag(ID.F.UseAmmoLight) && this.host.hasAmmoLight()) || 
            	(this.host.getStateFlag(ID.F.UseAmmoHeavy) && this.host.hasAmmoHeavy()))) {   
            	this.attackTarget = target;
                return true;
            }
    	}

        return false;
    }
    
    //init AI parameter, call once every target
    @Override
    public void startExecuting() {
    	if(host != null) {
	    	this.maxDelayLight = (int)(40F / (this.host.getStateFinal(ID.SPD)));
	    	this.maxDelayHeavy = (int)(80F / (this.host.getStateFinal(ID.SPD)));
	    	this.aimTime = (int) (20F * (float)( 150 - this.host.getStateMinor(ID.N.ShipLevel) ) / 150F) + 10;
	    	
	    	//if target changed, check the delay time from prev attack
	    	if(this.delayLight <= this.aimTime) {
	    		this.delayLight = this.aimTime;
	    	}
	    	if(this.delayHeavy <= this.aimTime * 2) {
	    		this.delayHeavy = this.aimTime * 2;
	    	}
	    	
	        this.attackRange = this.host.getStateFinal(ID.HIT) + 1F;
	        this.rangeSq = this.attackRange * this.attackRange;
	        
	        distSq = distX = distY = distZ = motX = motY = motZ = 0D;
    	}
    }

    //�P�w�O�_�~��AI�G ��target�N�~��, �Ϊ̤w�g���ʧ����N�~��
    public boolean continueExecuting() {
    	if(host != null) return this.shouldExecute() || !this.host.getShipNavigate().noPath();
    	
    	return false;
    }

    //���mAI��k
    public void resetTask() {
        this.attackTarget = null;
        this.onSightTime = 0;
        this.delayLight = this.aimTime;
        this.delayHeavy = this.aimTime;
    }

    //�i��AI
    public void updateTask() {
    	boolean onSight = false;	//�P�w���g�O�_�L��ê��
    	
    	//get update attributes every second
    	if(this.host != null && this.host.ticksExisted % 20 == 0) {
    		this.maxDelayLight = (int)(20F / (this.host.getStateFinal(ID.SPD)));
        	this.maxDelayHeavy = (int)(100F / (this.host.getStateFinal(ID.SPD)));
        	this.aimTime = (int) (20F * (float)( 150 - this.host.getShipLevel() ) / 150F) + 10;
        	this.attackRange = this.host.getStateFinal(ID.HIT) + 1F;
            this.rangeSq = this.attackRange * this.attackRange;
    	}
    	  	
    	if(this.attackTarget != null) {  //for lots of NPE issue-.-
    		//delay time decr
	        this.delayLight--;
	        this.delayHeavy--;
	        
    		if(host != null && mount != null) {
	    		this.distX = this.attackTarget.posX - this.mount.posX;
	    		this.distY = this.attackTarget.posY - this.mount.posY;
	    		this.distZ = this.attackTarget.posZ - this.mount.posZ;	
	    		this.distSq = distX*distX + distY*distY + distZ*distZ;
	    		    		
	            onSight = this.mount.getEntitySenses().canSee(this.attackTarget);
	
		        //�i����, �hparf++, �_�h���m��0
		        if(onSight) {
		            ++this.onSightTime;
		        }
		        else {
		            this.onSightTime = 0;
		        }
		        
		        //�Y�����Ӥ[(�C12���ˬd�@��), ��50%���v�M���ؼ�, �Ϩ䭫�s��@���ؼ�
		        if((this.onSightTime > 240) && (this.onSightTime % 240 == 0)) {
		        	if(rand.nextInt(2) > 0)  {
		        		this.resetTask();
		        		return;
		        	}
		        }
		
		        //�Y�ؼжi�J�g�{, �B�ؼеL��ê������, �h�M��AI���ʪ��ؼ�, �H�����~�򲾰�      
		        if(distSq < (double)this.rangeSq && onSight) {
		        	this.host.getShipNavigate().clearPathEntity();
		        }
		        else {	//�ؼв���, �h�~��l��		        	
		        	if(host.ticksExisted % 20 == 0) {
		        		this.host.getShipNavigate().tryMoveToEntityLiving(this.attackTarget, 1D);
		        	}
	            }
		
		        //�]�w������, �Y���[�ݪ�����
		        this.mount.getLookHelper().setLookPositionWithEntity(this.attackTarget, 30.0F, 30.0F);

		        //�Yattack delay�˼Ƨ��F�B�˷Ǯɶ����[, �h�}�l����
		        if(this.delayHeavy <= 0 && this.onSightTime >= this.aimTime && this.host.hasAmmoHeavy() && this.host.getStateFlag(ID.F.UseAmmoHeavy)) {
		        	//�Y�ؼж]�X�d�� or �ؼгQ����, �h�������, �i��U�@��ai�P�w
		            if(distSq > (double)this.rangeSq || !onSight) { return; }
		            
		            //�ϥ�entity��attackEntityWithAmmo�i��ˮ`�p��
		            this.mount.attackEntityWithHeavyAmmo(this.attackTarget);
		            this.delayHeavy = this.maxDelayHeavy;
		        } 
		        
		        //�Yattack delay�˼Ƨ��F�B�˷Ǯɶ����[, �h�}�l����
		        if(this.delayLight <= 0 && this.onSightTime >= this.aimTime && this.host.hasAmmoLight() && this.host.getStateFlag(ID.F.UseAmmoLight)) {
		        	//�Y�ؼж]�X�d�� or �ؼгQ����, �h�������, �i��U�@��ai�P�w
		            if(distSq > (double)this.rangeSq || !onSight) { return; }
		            
		            //�ϥ�entity��attackEntityWithAmmo�i��ˮ`�p��
		            this.mount.attackEntityWithAmmo(this.attackTarget);
		            this.delayLight = this.maxDelayLight;
		        }
		        
		        //�Y�W�L�Ӥ[��������ؼ�(�άO�l����), �h���m�ؼ�
		        if(this.delayHeavy < -120 || this.delayLight < -120) {
		        	this.resetTask();
		        	this.host.setAttackTarget(null);
		        	this.mount.setAttackTarget(null);
		        	return;
		        }
    		}//end host attack
    	}//end target != null
    }//end update task
}
