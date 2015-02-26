package com.lulan.shincolle.ai;

import java.util.Random;

import com.lulan.shincolle.entity.BasicEntityShipLarge;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.utility.LogHelper;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.util.MathHelper;

/**CARRIER RANGE ATTACK AI
 * entity������@attackEntityWithAircraft, attackEntityWithHeavyAircraft ��Ӥ�k
 */
public class EntityAIShipCarrierAttack extends EntityAIBase {
	
	private Random rand = new Random();
    private BasicEntityShipLarge host;  		//entity with AI
    private EntityLivingBase attackTarget;  	//entity of target
    private EntityLivingBase attackTarget2;  	//for target continue reset to null bug
    private int delayLaunch = 0;		//aircraft launch delay
    private int maxDelayLaunch;			//max launch delay
    private boolean typeLaunch = false;	//aircraft launch type, true = light
    private float attackRange;		//attack range
    private float rangeSq;			//attack range square
    
    //���u�e�i���\��
    private double distSq, distX, distY, distZ, motX, motY, motZ;	//��ؼЪ����u�Z��(������)
    
 
    //parm: host, move speed, p4, attack delay, p6
    public EntityAIShipCarrierAttack(BasicEntityShipLarge host) {
        if (!(host instanceof BasicEntityShipLarge)) {
            throw new IllegalArgumentException("CarrierAttack AI requires BasicEntityShipLarge");
        }
        else {
            this.host = host;
            this.setMutexBits(3);
        }
    }

    //check ai start condition
    public boolean shouldExecute() {
    	if(this.host.isSitting()) return false;
    	
    	EntityLivingBase target = this.host.getAttackTarget();
//    	LogHelper.info("DEBUG : carrier attack "+target);
        if (((target != null && target.isEntityAlive()) || 
        	  this.attackTarget2 != null && this.attackTarget2.isEntityAlive()) &&
        	((this.host.getStateFlag(ID.F.UseAirLight) && this.host.hasAmmoLight() && this.host.hasAirLight()) || 
        	(this.host.getStateFlag(ID.F.UseAirHeavy) && this.host.hasAmmoHeavy() && this.host.hasAirHeavy()))) {   
        	this.attackTarget = target;
        	this.attackTarget2 = target;
        	return true;
        }
        return false;
    }
    
    //init AI parameter, call once every target
    @Override
    public void startExecuting() {
        distSq = distX = distY = distZ = motX = motY = motZ = 0D;
    }

    //�P�w�O�_�~��AI�G ��target�N�~��, �Ϊ̤w�g���ʧ����N�~��
    public boolean continueExecuting() {
        return this.shouldExecute() || !this.host.getNavigator().noPath();
    }

    //���mAI��k
    public void resetTask() {
        this.attackTarget = null;
        this.delayLaunch = this.maxDelayLaunch;
    }

    //�i��AI
    public void updateTask() {
    	boolean onSight = false;	//�P�w���g�O�_�L��ê��
    	//get update attributes
    	if(this.host != null && this.host.ticksExisted % 80 == 0) {	
    		this.maxDelayLaunch = (int)(80F / (this.host.getStateFinal(ID.SPD))) + 20;
            this.attackRange = this.host.getStateFinal(ID.HIT);
            this.rangeSq = this.attackRange * this.attackRange;
    	}
    	
    	if(this.attackTarget != null) {  //for lots of NPE issue-.-
    		if(this.distSq >= this.rangeSq) {
    			this.distX = this.attackTarget.posX - this.host.posX;
        		this.distY = this.attackTarget.posY - this.host.posY;
        		this.distZ = this.attackTarget.posZ - this.host.posZ;	
        		this.distSq = distX*distX + distY*distY + distZ*distZ;
    	
    	        //�Y�ؼжi�J�g�{, �B�ؼеL��ê������, �h�M��AI���ʪ��ؼ�, �H�����~�򲾰�      
    	        if(distSq < (double)this.rangeSq && onSight) {
    	            this.host.getNavigator().clearPathEntity();
    	        }
    	        else {	//�ؼв���, �h�~��l��	
    	        	//�b�G�餤, �Ī��u�e�i
    	        	if(this.host.getShipDepth() > 0D) {
    	        		//�B�~�[�Wy�b�t��, getPathToXYZ��Ů��G�����L��, �]��y�b�t�׭n�t�~�[
    	        		if(this.distY > 1.5D && this.host.getShipDepth() > 1.5D) {  //�קK�����u��
    	        			this.motY = 0.2F;
    	        		}
    	        		else if(this.distY < -1D) {
    	        			this.motY = -0.2F;
    	        		}
    	  		
    	        		//�Y��������F��, �h���ո���
    	        		if(this.host.isCollidedHorizontally) {
    	        			this.host.setPosition(this.host.posX, this.host.posY + 0.3D, this.host.posZ);
    	        		}
    	        		
    	        		//�Y���u�i��, �h�������u����
    	        		if(this.host.getEntitySenses().canSee(this.attackTarget)) {
    	        			double speed = this.host.getStateFinal(ID.MOV);
    	        			this.motX = (this.distX / this.distSq) * speed * 6D;
    	        			this.motZ = (this.distZ / this.distSq) * speed * 6D;

    	        			this.host.motionY = this.motY;
    	        			this.host.getMoveHelper().setMoveTo(this.host.posX+this.motX, this.host.posY+this.motY, this.host.posZ+this.motZ, 1D);
    	        		}
    	           	}
                	else {	//�D�G�餤, �ĥΤ@��M����|�k
                		this.host.getNavigator().tryMoveToEntityLiving(this.attackTarget, 1D);
                	}
                }
    		}//end dist > range
	
	        //�]�w������, �Y���[�ݪ�����
	        this.host.getLookHelper().setLookPosition(this.attackTarget.posX, this.attackTarget.posY+30D, this.attackTarget.posZ, 40.0F, 90.0F);
	        
	        //delay time decr
	        this.delayLaunch--;

	        //�Y�u�ϥγ�@�ؼu��, �h����typeLaunch
	        if(!this.host.getStateFlag(ID.F.UseAirLight)) {
	        	this.typeLaunch = false;
	        }
	        if(!this.host.getStateFlag(ID.F.UseAirHeavy)) {
	        	this.typeLaunch = true;
	        }
	        
	        //�Yattack delay�˼Ƨ��F�B�˷Ǯɶ����[, �h�}�l����, no onSight check
	        if(this.typeLaunch && this.distSq < this.rangeSq && this.delayLaunch <= 0 && this.host.hasAmmoLight() && this.host.getStateFlag(ID.F.UseAirLight) && this.host.hasAirHeavy()) {
	            this.host.attackEntityWithAircraft(this.attackTarget);
	            this.delayLaunch = this.maxDelayLaunch;
	            this.typeLaunch = !this.typeLaunch;
	        }
	        
	        //�Yattack delay�˼Ƨ��F�B�˷Ǯɶ����[, �h�}�l����, no onSight check
	        if(!this.typeLaunch && this.distSq < this.rangeSq && this.delayLaunch <= 0 && this.host.hasAmmoHeavy() && this.host.getStateFlag(ID.F.UseAirHeavy) && this.host.hasAirHeavy()) {	            
	            this.host.attackEntityWithHeavyAircraft(this.attackTarget);
	            this.delayLaunch = this.maxDelayLaunch;
	            this.typeLaunch = !this.typeLaunch;      
	        } 
	        
	        //�Y�W�L�Ӥ[��������ؼ�(�άO�l����), �h���m�ؼ�
	        if(this.delayLaunch < -100) {
	        	this.resetTask();
	        	return;
	        }
    	}
    }
}
