package com.lulan.shincolle.ai;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIBase;

import com.lulan.shincolle.entity.BasicEntityMount;
import com.lulan.shincolle.entity.IShipCannonAttack;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.utility.LogHelper;

/**ENTITY RANGE ATTACK AI
 * �q���}���g�bAI�ק�Ө�
 * entity������@attackEntityWithAmmo, attackEntityWithHeavyAmmo ��Ӥ�k
 */
public class EntityAIShipRangeAttack extends EntityAIBase {
	
    private IShipCannonAttack host;  	//AI host entity
    private EntityLiving host2;
    private Entity target;  			//entity of target
    private int delayLight;				//light attack delay
    private int maxDelayLight;	    	//light attack max delay
    private int delayHeavy;				//heavy attack delay
    private int maxDelayHeavy;	    	//heavy attack max delay (= light delay x5)    
    private int onSightTime;			//target on sight time
    private float range;				//attack range
    private float rangeSq;				//attack range square
    private int aimTime;				//time before fire
    private double distSq, distSqrt, distX, distY, distZ;	//��ؼЪ����u�Z��(������)
    
    
    //parm: host, move speed, p4, attack delay, p6
    public EntityAIShipRangeAttack(IShipCannonAttack host) {
        if(!(host instanceof IShipCannonAttack)) {
            throw new IllegalArgumentException("RangeAttack AI requires interface IShipCannonAttack");
        }
        else {
            this.host = host;
            this.host2 = (EntityLiving) host;
            this.setMutexBits(3);
            
            //init value
            this.delayLight = 20;
            this.delayHeavy = 40;
            this.maxDelayLight = 20;
            this.maxDelayHeavy = 40;
        }
    }

    //check ai start condition
    @Override
	public boolean shouldExecute() {
    	//for entity ship
    	if(host2 != null) {
    		//���U��������
    		if(this.host.getIsSitting()) return false;
    		
    		//�Y�M��ship���y�M, �h�����浹mount�P�w
    		if(this.host.getIsRiding()) {
    			if(this.host2.ridingEntity instanceof BasicEntityMount) {
    				return false;
    			}
    		}
        	
        	Entity target = this.host.getEntityTarget();
        	
            if(target != null && target.isEntityAlive() &&
               ((this.host.getAttackType(ID.F.AtkType_Light) && this.host.getStateFlag(ID.F.UseAmmoLight) && this.host.hasAmmoLight()) || 
                (this.host.getAttackType(ID.F.AtkType_Heavy) && this.host.getStateFlag(ID.F.UseAmmoHeavy) && this.host.hasAmmoHeavy()))) {   
            	this.target = target;
                return true;
            }
    	}       
        
        return false;
    }
    
    //init AI parameter, call once every target
    @Override
    public void startExecuting() {
    	if(host != null) {
    		this.updateAttackParms();
	    	
	    	//if target changed, check the delay time from prev attack
	    	if(this.delayLight <= this.aimTime) {
	    		this.delayLight = this.aimTime;
	    	}
	    	if(this.delayHeavy <= this.aimTime * 2) {
	    		this.delayHeavy = this.aimTime * 2;
	    	}
	        
	        distSq = distX = distY = distZ = 0D;
    	}      
    }

    //�P�w�O�_�~��AI�G ��target�N�~��, �Ϊ̤w�g���ʧ����N�~��
    @Override
	public boolean continueExecuting() {
    	if(host != null) {
    		if(target != null && target.isEntityAlive() && !this.host.getShipNavigate().noPath()) {
        		return true;
        	}
        	
            return this.shouldExecute();
    	}
    	
    	return false;
    }

    //���mAI��k
    @Override
	public void resetTask() {
        this.target = null;
        this.onSightTime = 0;
        if(host != null) {
        	this.host2.setAttackTarget(null);
        	this.host.setEntityTarget(null);
        	this.host.getShipNavigate().clearPathEntity();
        }
    }

    //�i��AI
    @Override
	public void updateTask() {
    	boolean onSight = false;	//�P�w���g�O�_�L��ê��
    	
    	if(this.host != null && this.target != null) {
//    		LogHelper.info("DEBUG: update attack AI: "+this.host+" "+this.target);
    		
    		//get update attributes every second
    		if(this.host2.ticksExisted % 64 == 0) {
    			this.updateAttackParms();
    		}

    		//delay time decr
	        this.delayLight--;
	        this.delayHeavy--;

    		this.distX = this.target.posX - this.host2.posX;
    		this.distY = this.target.posY - this.host2.posY;
    		this.distZ = this.target.posZ - this.host2.posZ;	
    		this.distSq = distX*distX + distY*distY + distZ*distZ;
    		    		
            onSight = this.host2.getEntitySenses().canSee(this.target);

	        //�i����, �hon sight time++, �_�h���m��0
	        if(onSight) {
	            ++this.onSightTime;
	        }
	        else {
	            this.onSightTime = 0;
	            
	            if(host.getStateFlag(ID.F.OnSightChase)) {
	            	this.resetTask();
	            	return;
	            }
	        }
	
	        //�Y�ؼжi�J�g�{, �B�ؼеL��ê������, �h�M��AI���ʪ��ؼ�, �H�����~�򲾰�      
	        if(distSq < this.rangeSq && onSight) {
	        	this.host.getShipNavigate().clearPathEntity();
	        }
	        else {	//�ؼв���, �h�~��l	        	
	        	if(host2.ticksExisted % 32 == 0) {
	        		this.host.getShipNavigate().tryMoveToEntityLiving(this.target, 1D);
	        	}
            }
	
	        //�]�w������, �Y���[�ݪ�����
	        this.host2.getLookHelper().setLookPositionWithEntity(this.target, 30.0F, 30.0F);
	        
	        if(host2.isRiding() && host2.ridingEntity instanceof BasicEntityMount) {
	        	((BasicEntityMount)host2.ridingEntity).getLookHelper().setLookPositionWithEntity(this.target, 30.0F, 30.0F);
	        }

	        //�Yattack delay�˼Ƨ��F�B�˷Ǯɶ����[, �h�}�l����
	        if(onSight && distSq <= this.rangeSq && this.onSightTime >= this.aimTime) {
	        	//�ϥλ�����
	        	if(this.delayLight <= 0 && this.host.useAmmoLight() && this.host.hasAmmoLight()) {
	        		this.host.attackEntityWithAmmo(this.target);
		            this.delayLight = this.maxDelayLight;
//		            LogHelper.info("dEBUG: ranged attack: host: "+this.host);
	        	}
	        	
	        	//�ϥέ�����
	        	if(this.delayHeavy <= 0 && this.host.useAmmoHeavy() && this.host.hasAmmoHeavy()) {
	        		this.host.attackEntityWithHeavyAmmo(this.target);
		            this.delayHeavy = this.maxDelayHeavy;
	        	}
	        }
	        
	        //�Y�W�L�Ӥ[��������ؼ�(�άO�l����), �h���m�ؼ�
	        if(this.delayHeavy < -40 || this.delayLight < -40) {
	        	this.delayLight = 20;
	        	this.delayHeavy = 20;
	        	this.resetTask();
	        	return;
	        }

    	}//end target != null
    }//end update task
    
    private void updateAttackParms() {
    	this.maxDelayLight = (int)(80F / (this.host.getAttackSpeed()));
    	this.maxDelayHeavy = (int)(160F / (this.host.getAttackSpeed()));
    	this.aimTime = (int) (20F * (150 - this.host.getLevel()) / 150F) + 10;
    	
    	this.range = this.host.getAttackRange();
        this.rangeSq = this.range * this.range;
    }
    
}
