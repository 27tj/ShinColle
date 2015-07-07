package com.lulan.shincolle.ai;

import java.util.Random;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;

import com.lulan.shincolle.entity.IShipAircraftAttack;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.utility.LogHelper;

/**CARRIER RANGE ATTACK AI
 * entity������@IUseAircraft
 */
public class EntityAIShipCarrierAttack extends EntityAIBase {
	
	private Random rand = new Random();
    private IShipAircraftAttack host;  	//entity with AI
    private EntityLiving host2;
    private EntityLivingBase target;  	//entity of target
    private int launchDelay;			//aircraft launch delay
    private int launchDelayMax;			//max launch delay
    private boolean launchType;			//airplane type, true = light
    private float range;			//attack range
    private float rangeSq;				//attack range square
    
    //���u�e�i���\��
    private double distSq, distX, distY, distZ;	//��ؼЪ����u�Z��(������)
    
 
    //parm: host, move speed, p4, attack delay, p6
    public EntityAIShipCarrierAttack(IShipAircraftAttack host) {
        if (!(host instanceof IShipAircraftAttack)) {
            throw new IllegalArgumentException("CarrierAttack AI requires IShipAircraftAttack");
        }
        else {
            this.host = host;
            this.host2 = (EntityLiving) host;
            this.setMutexBits(4);
            
            //init value
            this.launchDelay = 20;
            this.launchDelayMax = 40;
        }
    }

    //check ai start condition
    public boolean shouldExecute() {
//    	LogHelper.info("DEBUG : carrier attack "+target);
    	if(this.host.getIsSitting()) return false;
    	
    	EntityLivingBase target = this.host.getTarget();

        if((target != null && target.isEntityAlive()) &&
           ((this.host.getAttackType(ID.F.AtkType_AirLight) && this.host.getStateFlag(ID.F.UseAirLight) && this.host.hasAmmoLight() && this.host.hasAirLight()) || 
            (this.host.getAttackType(ID.F.AtkType_AirHeavy) && this.host.getStateFlag(ID.F.UseAirHeavy) && this.host.hasAmmoHeavy() && this.host.hasAirHeavy()))) {   
        	this.target = target;
        	return true;
        }
        
        return false;
    }
    
    //init AI parameter, call once every target, DO NOT reset delay time here
    @Override
    public void startExecuting() {
        distSq = distX = distY = distZ = 0D;
    }

    //�P�w�O�_�~��AI�G ��target�N�~��, �Ϊ��٦b���ʤ��h�~��
    public boolean continueExecuting() {
        return this.shouldExecute() || (target != null && target.isEntityAlive() && !this.host.getShipNavigate().noPath());
//    	return this.shouldExecute();
    }

    //���mAI��k, DO NOT reset delay time here
    public void resetTask() {
//    	LogHelper.info("DEBUG : air attack AI "+target);
        this.target = null;
        if(host != null) {
        	this.host2.setAttackTarget(null);
        	this.host.getShipNavigate().clearPathEntity();
        }
    }

    //�i��AI
    public void updateTask() {
    	if(this.target != null && host != null) {  //for lots of NPE issue-.-
    		boolean onSight = this.host2.getEntitySenses().canSee(this.target);
//    		boolean onSight = true;		 //for debug
    		
    		//�Y���b���u��, �ˬdflag
    		if(!onSight) {
    			if(host.getStateFlag(ID.F.OnSightChase)) {
	            	this.resetTask();
	            	return;
	            }
    		}
    		
    		//get update attributes
        	if(this.host2.ticksExisted % 60 == 0) {	
        		this.launchDelayMax = (int)(60F / (this.host.getAttackSpeed())) + 10;
                this.range = this.host.getAttackRange();
                this.rangeSq = this.range * this.range;
        	}

    		if(this.distSq >= this.rangeSq) {
    			this.distX = this.target.posX - this.host2.posX;
        		this.distY = this.target.posY - this.host2.posY;
        		this.distZ = this.target.posZ - this.host2.posZ;	
        		this.distSq = distX*distX + distY*distY + distZ*distZ;
    	
        		//�Y�ؼжi�J�g�{, �B�ؼеL��ê������, �h�M��AI���ʪ��ؼ�, �H�����~�򲾰�      
		        if(distSq < (double)this.rangeSq && onSight) {
		        	this.host.getShipNavigate().clearPathEntity();
		        }
		        else {	//�ؼв���, �h�~��l��		        	
		        	if(host2.ticksExisted % 32 == 0) {
		        		this.host.getShipNavigate().tryMoveToEntityLiving(this.target, 1D);
		        	}
	            }
    		}//end dist > range
	
	        //�]�w������, �Y���[�ݪ�����
    		this.host2.getLookHelper().setLookPosition(this.target.posX, this.target.posY+2D, this.target.posZ, 30.0F, 60.0F);
	         
	        //delay time decr
	        this.launchDelay--;

	        //�Y�u�ϥγ�@�ؼu��, �h���Ϋ��A����, �u�o�g�P�@�ح���
	        if(!this.host.getStateFlag(ID.F.UseAirLight)) {
	        	this.launchType = false;
	        }
	        if(!this.host.getStateFlag(ID.F.UseAirHeavy)) {
	        	this.launchType = true;
	        }
	        
	        //�Yattack delay�˼Ƨ��F, �h�}�l����
	        if(onSight && this.distSq <= this.rangeSq && this.launchDelay <= 0) {
	        	//�ϥλ�����
	        	if(this.launchType && this.host.hasAmmoLight() && this.host.hasAirLight()) {
	        		this.host.attackEntityWithAircraft(this.target);
		            this.launchDelay = this.launchDelayMax;	//reset delay
	        	}
	        	
	        	//�ϥέ�����
	        	if(!this.launchType && this.host.hasAmmoHeavy() && this.host.hasAirHeavy()) {
	        		this.host.attackEntityWithHeavyAircraft(this.target);
	        		this.launchDelay = this.launchDelayMax;	//reset delay
	        	}
	        	
	        	this.launchType = !this.launchType;		//change type
	        }
	        
	        //�Y�W�L�Ӥ[��������ؼ�(�άO�l����), �h���m�ؼ�
	        if(this.launchDelay < -40) {
	        	this.launchDelay = 20;
	        	this.resetTask();
	        	return;
	        }
    	}
    }
}
