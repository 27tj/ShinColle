package com.lulan.shincolle.ai;

import java.util.Random;

import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.IShipAttack;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.utility.EntityHelper;
import com.lulan.shincolle.utility.LogHelper;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;

/**ENTITY RANGE ATTACK AI
 * �q���}���g�bAI�ק�Ө�
 * entity������@attackEntityWithAmmo, attackEntityWithHeavyAmmo ��Ӥ�k
 */
public class EntityAIShipRangeAttack extends EntityAIBase {
	
	private Random rand = new Random();
    private BasicEntityShip host;  	//AI host entity
    private EntityLiving host2;
    private IShipAttack host2i;  	//AI host entity, non-ship entity
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
    public EntityAIShipRangeAttack(BasicEntityShip host) {
        if (!(host instanceof IShipAttack)) {
            throw new IllegalArgumentException("RangeAttack AI requires interface IShipAttack");
        }
        else {
            this.host = host;
            this.host2 = null;
            this.host2i = null;
            this.setMutexBits(3);
        }
    }
    
    //parm: host, move speed, p4, attack delay, p6
    public EntityAIShipRangeAttack(IShipAttack host) {
        if (!(host instanceof IShipAttack)) {
            throw new IllegalArgumentException("RangeAttack AI requires interface IShipAttack");
        }
        else {
            this.host = null;
            this.host2 = (EntityLiving) host;
            this.host2i = host;
            this.setMutexBits(3);
        }
    }

    //check ai start condition
    public boolean shouldExecute() {
    	//for entity ship
    	if(host2 == null) {
    		if(this.host.isSitting()) return false;
        	
        	EntityLivingBase target = this.host.getAttackTarget();
        	
            if (target != null && !target.isDead &&
            	((this.host.getStateFlag(ID.F.UseAmmoLight) && this.host.hasAmmoLight()) || 
            	(this.host.getStateFlag(ID.F.UseAmmoHeavy) && this.host.hasAmmoHeavy()))) {   
            	this.attackTarget = target;
                return true;
            }
    	}
    	
    	//for non entity ship
    	if(host == null) { 	
        	EntityLivingBase target = this.host2i.getTarget();
        	
            if (target != null && !target.isDead &&
            	(this.host2i.hasAmmoLight() || this.host2i.hasAmmoHeavy())) {
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
    	
    	if(host2 != null) {
    		this.maxDelayLight = (int)(40F / (this.host2i.getAttackSpeed()));
	    	this.maxDelayHeavy = (int)(80F / (this.host2i.getAttackSpeed()));
	    	this.aimTime = 0;
	    	
	    	this.delayLight = this.maxDelayLight;
	    	this.delayHeavy = this.maxDelayHeavy;
	    	
	        this.attackRange = this.host2i.getAttackRange() + 1F;
	        this.rangeSq = this.attackRange * this.attackRange;
	        
	        distSq = distX = distY = distZ = motX = motY = motZ = 0D;
    	}
       
    }

    //�P�w�O�_�~��AI�G ��target�N�~��, �Ϊ̤w�g���ʧ����N�~��
    public boolean continueExecuting() {
    	if(host != null) return this.shouldExecute() || !this.host.getNavigator().noPath();
    	if(host2 != null) return this.shouldExecute();
    	
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
	        
    		if(host != null) {
	    		this.distX = this.attackTarget.posX - this.host.posX;
	    		this.distY = this.attackTarget.posY - this.host.posY;
	    		this.distZ = this.attackTarget.posZ - this.host.posZ;	
	    		this.distSq = distX*distX + distY*distY + distZ*distZ;
	    		    		
	            onSight = this.host.getEntitySenses().canSee(this.attackTarget);
	
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
		            this.host.getNavigator().clearPathEntity();
		        }
		        else {	//�ؼв���, �h�~��l��
		        	//�B�~�[�Wy�b�t��, getPathToXYZ��Ů��G�����L��, �]��y�b�t�׭n�t�~�[
		        	
	        		if(this.host.getShipDepth() > 0.55D) {
	        			if(MathHelper.abs((float)distY) < 4F && this.host.getShipDepth() < 4D) {  //�p�G�������, �h�����B�b����
//	        				LogHelper.info("DEBUG : move AAAAAAAAAAAAAAA");
		        			this.motY = 0.08F;
	        			}
	        			else if(this.distY > 2D) {		//�Y�S���������, ����m����, �h�W�B
//		        			LogHelper.info("DEBUG : move BBBBBBBBBBBBBBB");
		        			this.motY = 0.2F;
		        		}
		        		else if(this.distY <= -2D) {	//�Y�S���������, ����m���C, �h�U�I
//		        			LogHelper.info("DEBUG : move CCCCCCCCCCCCCCC");
		        			this.motY = -0.2F;
		        		}
		        		else {
//		        			LogHelper.info("DEBUG : move DDDDDDDDDDDDDDD");
		        			this.motY = 0F;
		        		}
	        		}	
	        		
		        	//�b�G�餤, �Ī��u�e�i
		        	if(this.host.isInWater()) {
		        		//�Y���u�i��, �h�������u����
		        		if(this.host.getEntitySenses().canSee(this.attackTarget)) {
		        			double speed = this.host.getStateFinal(ID.MOV);
		        			this.distSqrt = MathHelper.sqrt_double(this.distSq);
		        			this.motX = (this.distX / this.distSqrt) * speed * 1D;
		        			this.motZ = (this.distZ / this.distSqrt) * speed * 1D;
		        			
		        			if(this.motX > 0.8D) this.motX = 0.8D;
		        	        if(this.motX < -0.8D) this.motX = -0.8D;
		        	        if(this.motZ > 0.8D) this.motZ = 0.8D;
		        	        if(this.motZ < -0.8D) this.motZ = -0.8D;
		        	        
		        	        this.host.motionX = motX;
		        			this.host.motionY = motY;
		        			this.host.motionZ = motZ;
		        			
		        			//���騤�׳]�w
		        			float[] degree = EntityHelper.getLookDegree(motX, motY, motZ);
		        			this.host.rotationYaw = degree[0];
		        			this.host.rotationPitch = degree[1];
	//	        			this.host.getMoveHelper().setMoveTo(this.host.posX+this.motX, this.host.posY+this.motY, this.host.posZ+this.motZ, 1D);
		        		}
		        		
		        		//�Y��������F��, �h���ո���
		        		if(this.host.isCollidedHorizontally) {
		        			this.host.motionY += 0.25D;
		        		}
		           	}
	            	else {	//�D�G�餤, �ĥΤ@��M����|�k
	            		this.host.getNavigator().tryMoveToEntityLiving(this.attackTarget, 1D);
	            	}
	            }
		
		        //�]�w������, �Y���[�ݪ�����
		        this.host.getLookHelper().setLookPositionWithEntity(this.attackTarget, 30.0F, 30.0F);

		        //�Yattack delay�˼Ƨ��F�B�˷Ǯɶ����[, �h�}�l����
		        if(this.delayHeavy <= 0 && this.onSightTime >= this.aimTime && this.host.hasAmmoHeavy() && this.host.getStateFlag(ID.F.UseAmmoHeavy)) {
		        	//�Y�ؼж]�X�d�� or �ؼгQ����, �h�������, �i��U�@��ai�P�w
		            if(distSq > (double)this.rangeSq || !onSight) { return; }
		            
		            //�ϥ�entity��attackEntityWithAmmo�i��ˮ`�p��
		            this.host.attackEntityWithHeavyAmmo(this.attackTarget);
		            this.delayHeavy = this.maxDelayHeavy;
		        } 
		        
		        //�Yattack delay�˼Ƨ��F�B�˷Ǯɶ����[, �h�}�l����
		        if(this.delayLight <= 0 && this.onSightTime >= this.aimTime && this.host.hasAmmoLight() && this.host.getStateFlag(ID.F.UseAmmoLight)) {
		        	//�Y�ؼж]�X�d�� or �ؼгQ����, �h�������, �i��U�@��ai�P�w
		            if(distSq > (double)this.rangeSq || !onSight) { return; }
		            
		            //�ϥ�entity��attackEntityWithAmmo�i��ˮ`�p��
		            this.host.attackEntityWithAmmo(this.attackTarget);
		            this.delayLight = this.maxDelayLight;
		        }
		        
		        //�Y�W�L�Ӥ[��������ؼ�(�άO�l����), �h���m�ؼ�
		        if(this.delayHeavy < -120 || this.delayLight < -120) {
		        	this.resetTask();
		        	this.host.setAttackTarget(null);
		        	return;
		        }
    		}//end host attack
    		
    		if(host2 != null) {
    			//�]�w������, �Y���[�ݪ�����
		        this.host2.getLookHelper().setLookPositionWithEntity(this.attackTarget, 30.0F, 30.0F);
		        
		        onSight = this.host2.getEntitySenses().canSee(this.attackTarget);
		        
		        this.distX = this.attackTarget.posX - this.host2.posX;
	    		this.distY = this.attackTarget.posY - this.host2.posY;
	    		this.distZ = this.attackTarget.posZ - this.host2.posZ;	
	    		this.distSq = distX*distX + distY*distY + distZ*distZ;
	    		
	    		//�Y�ؼжi�J�g�{, �B�ؼеL��ê������, �h�M��AI���ʪ��ؼ�, �H�����~�򲾰�      
		        if(distSq < (double)this.rangeSq && onSight) {
		            this.host2.getNavigator().clearPathEntity();
		        }
		        else {	//�ؼв���, �h�~��l��	
		        	//�b�G�餤, �Ī��u�e�i
		        	if(this.host2.isInWater()) {
		        		//�B�~�[�Wy�b�t��, getPathToXYZ��Ů��G�����L��, �]��y�b�t�׭n�t�~�[
		        		if(this.host2.worldObj.getBlock((int)this.host2.posX, (int)this.host2.posY + 1, (int)this.host2.posZ) != Blocks.water) {  //�p�G�������, �h�����B�b����
		        			int depthInt = (int)host2.posY;
		        			float depth = (float)host2.posY - (float)depthInt;
		        			
		        			if(depth > 0.4F && this.distY > -2D) {	//���`�W�L0.6, �ؼа��׮t�j��-2, �h�y�L�W�B
		        				this.motY = 0.1F;
		        			}
		        			else if(this.distY < -4D) {	//���b���C��m, �y�L�U��
		        				this.motY = -0.1F;
		        			}
		        			else {					//�������`
		        				this.motY = 0F;
		        			}
		        		}
		        		else if(this.distY > 2D) {	//�Y�S���������, ����m����, �h�W�B
		        			this.motY = 0.2F;
		        		}
		        		else if(this.distY < -4D) {	//�Y�S���������, ����m���C, �h�U�I
		        			this.motY = -0.2F;
		        		}
		        		else {
		        			this.motY = 0F;
		        		}
		  		
		        		//�Y���u�i��, �h�������u����
		        		if(this.host2.getEntitySenses().canSee(this.attackTarget)) {
		        			double speed = this.host2i.getMoveSpeed();
		        			this.distSqrt = MathHelper.sqrt_double(this.distSq);
		        			this.motX = (this.distX / this.distSqrt) * speed * 1D;
		        			this.motZ = (this.distZ / this.distSqrt) * speed * 1D;
		        			
		        			if(this.motX > 0.8D) this.motX = 0.8D;
		        	        if(this.motX < -0.8D) this.motX = -0.8D;
		        	        if(this.motZ > 0.8D) this.motZ = 0.8D;
		        	        if(this.motZ < -0.8D) this.motZ = -0.8D;
		        	        
		        	        this.host2.motionX = motX;
		        			this.host2.motionY = motY;
		        			this.host2.motionZ = motZ;
		        			
		        			//���騤�׳]�w
		        			float[] degree = EntityHelper.getLookDegree(motX, motY, motZ);
		        			this.host2.rotationYaw = degree[0];
		        			this.host2.rotationPitch = degree[1];
		        		}
		        		
		        		//�Y��������F��, �h���ո���
		        		if(this.host2.isCollidedHorizontally) {
		        			this.host2.motionY += 0.25D;
		        		}
		           	}
	            	else {	//�D�G�餤, �ĥΤ@��M����|�k
	            		this.host2.getNavigator().tryMoveToEntityLiving(this.attackTarget, 1D);
	            	}
	            }
		
		        //�]�w������, �Y���[�ݪ�����
		        this.host2.getLookHelper().setLookPositionWithEntity(this.attackTarget, 30.0F, 30.0F);
		        
		        //�Yattack delay�˼Ƨ��F, �h�}�l����
		        if(this.delayHeavy <= 0 && this.host2i.hasAmmoHeavy()) {
		        	//�Y�ؼж]�X�d�� or �ؼгQ���� or �Z���Ӫ�, �h�������, �i��U�@��ai�P�w
		            if(distSq > (double)this.rangeSq || !onSight) { return; }
		            
		            //�ϥ�entity��attackEntityWithAmmo�i��ˮ`�p��
		            this.host2i.attackEntityWithHeavyAmmo(this.attackTarget);
		            this.delayHeavy = this.maxDelayHeavy;
		        } 
		        
		        //�Yattack delay�˼Ƨ��F�B�˷Ǯɶ����[, �h�}�l����
		        if(this.delayLight <= 0 && this.host2i.hasAmmoLight()) {
		        	//�Y�ؼж]�X�d�� or �ؼгQ����, �h�������, �i��U�@��ai�P�w
		            if(distSq > (double)this.rangeSq || !onSight) { return; }
		            
		            //�ϥ�entity��attackEntityWithAmmo�i��ˮ`�p��
		            this.host2i.attackEntityWithAmmo(this.attackTarget);
		            this.delayLight = this.maxDelayLight;
		        }
    		}
    	}//end target != null
    }//end update task
}
