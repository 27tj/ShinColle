package com.lulan.shincolle.ai;

import java.util.Random;

import com.lulan.shincolle.entity.BasicEntityShipLarge;
import com.lulan.shincolle.reference.AttrID;
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
    private BasicEntityShipLarge host;  	//entity with AI
    private EntityLivingBase attackTarget;  //entity of target
    private int delayLaunch = 0;		//aircraft launch delay
    private int maxDelayLaunch;			//max launch delay
    private boolean typeLaunch = false;	//aircraft launch type, true = light
    private float attackRange;		//attack range
    private float rangeSq;			//attack range square
    private int onSightTime;		//on sight time
    
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
    	EntityLivingBase target = this.host.getAttackTarget();
    	
        if (target != null && 
        	((this.host.getEntityFlag(AttrID.F_UseAmmoLight) && this.host.hasAmmoLight() && this.host.getNumAircraftLight() > 0) || 
        	(this.host.getEntityFlag(AttrID.F_UseAmmoHeavy) && this.host.hasAmmoHeavy() && this.host.getNumAircraftHeavy() > 0))) {   
        	this.attackTarget = target;
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
    	if(this.host != null && this.host.ticksExisted % 20 == 0) {
    		this.maxDelayLaunch = (int)(40F / (this.host.getFinalState(AttrID.SPD)));
            this.attackRange = this.host.getFinalState(AttrID.HIT) + 1F;
            this.rangeSq = this.attackRange * this.attackRange;
    	}
  	
    	if(this.attackTarget != null) {  //for lots of NPE issue-.-	
    		this.distX = this.attackTarget.posX - this.host.posX;
    		this.distY = this.attackTarget.posY - this.host.posY;
    		this.distZ = this.attackTarget.posZ - this.host.posZ;	
    		this.distSq = distX*distX + distY*distY + distZ*distZ;
    		    		
            onSight = this.host.getEntitySenses().canSee(this.attackTarget);  

	        //�i����, �honsight++, �_�h���m��0
	        if(onSight) {
	            ++this.onSightTime;
	        }
	        else {
	            this.onSightTime = 0;
	        }
	
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
	        			double speed = this.host.getFinalState(AttrID.MOV);
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
	
	        //�]�w������, �Y���[�ݪ�����
	        this.host.getLookHelper().setLookPositionWithEntity(this.attackTarget, 30.0F, 30.0F);
	        
	        //delay time decr
	        this.delayLaunch--;

	        //�Y�u�ϥγ�@�ؼu��, �h����typeLaunch
	        if(!this.host.getEntityFlag(AttrID.F_UseAmmoLight)) {
	        	this.typeLaunch = false;
	        }
	        if(!this.host.getEntityFlag(AttrID.F_UseAmmoHeavy)) {
	        	this.typeLaunch = true;
	        }
	        
	        //�Yattack delay�˼Ƨ��F�B�˷Ǯɶ����[, �h�}�l����, no onSight check
	        if(this.typeLaunch && this.distSq < this.rangeSq && this.delayLaunch <= 0 && this.host.hasAmmoLight() && this.host.getEntityFlag(AttrID.F_UseAmmoLight) && this.host.getNumAircraftLight() > 0) {
	            this.host.attackEntityWithAircraft(this.attackTarget);
	            this.delayLaunch = this.maxDelayLaunch;
	            this.typeLaunch = !this.typeLaunch;
	        }
	        
	        //�Yattack delay�˼Ƨ��F�B�˷Ǯɶ����[, �h�}�l����, no onSight check
	        if(!this.typeLaunch && this.distSq < this.rangeSq && this.delayLaunch <= 0 && this.host.hasAmmoHeavy() && this.host.getEntityFlag(AttrID.F_UseAmmoHeavy) && this.host.getNumAircraftHeavy() > 0) {	            
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
