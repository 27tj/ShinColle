package com.lulan.shincolle.ai;

import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.utility.EntityHelper;
import com.lulan.shincolle.utility.LogHelper;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.util.MathHelper;

/**FLEE AI
 * if ship's HP is below fleeHP, ship will stop attack and try to flee
 */
public class EntityAIShipFlee extends EntityAIBase {
	
	private BasicEntityShip host;
	private EntityLivingBase owner;
	private PathNavigate PetPathfinder;
	private static final double TP_DIST = 2048D;	//teleport condition ~ 45 blocks
	private float distSq, distSqrt;
	private float fleehp;		//flee HP (percent)
	private int findCooldown;	//find path cooldown
	//���u�e�i���\��
    private double distX, distY, distZ, motX, motY, motZ;	//��ؼЪ����u�Z��(������)
    private float rotYaw;

	public EntityAIShipFlee(BasicEntityShip entity) {
        this.host = entity;
        this.PetPathfinder = entity.getNavigator();
        this.setMutexBits(7);
    }
	
	@Override
	public boolean shouldExecute() {
		this.fleehp = (float)host.getStateMinor(ID.N.FleeHP) / 100F;
		
		//��q�C��fleeHP �B���O���U�]���O�j�����A�~����flee AI
		if(!host.isSitting() && !host.getLeashed() && 
		   (host.getHealth() / host.getMaxHealth()) < fleehp) {
			
			EntityLivingBase OwnerEntity = host.getOwner();
			
			if(OwnerEntity != null) {
				owner = OwnerEntity;
				
				//�p�⪽�u�Z��
		    	this.distX = this.owner.posX - this.host.posX;
				this.distY = this.owner.posY - this.host.posY;
				this.distZ = this.owner.posZ - this.host.posZ;
		    	this.distSq = (float) (this.distX*this.distX + this.distY*this.distY + this.distZ*this.distZ);

		    	if(distSq > 6F) return true;
			}
		}
		
		return false;
	}
	
    public boolean continueExecuting() {
    	//�Z���Y�u��2�椺�N����
    	return shouldExecute();
    }

    public void startExecuting() {
    	this.findCooldown = 0;
        this.PetPathfinder.setAvoidsWater(false);
        this.PetPathfinder.setEnterDoors(true);
        this.PetPathfinder.setCanSwim(true);
    }

    public void resetTask() {
        this.owner = null;
        this.PetPathfinder.clearPathEntity();
    }
    
    public void updateTask() {
    	this.findCooldown--;
    	this.motY = 0D;
    	
    	//�]�w�Y����V
        this.host.getLookHelper().setLookPositionWithEntity(this.owner, 10.0F, (float)this.host.getVerticalFaceSpeed());

    	//�Z���W�L�ǰe�Z��, �����ǰe��ؼФW
    	if(this.distSq > this.TP_DIST) {
    		this.host.posX = this.owner.posX;
    		this.host.posY = this.owner.posY + 1D;
    		this.host.posZ = this.owner.posZ;
    		this.host.setPosition(this.host.posX, this.host.posY, this.host.posZ);
    	}
    	
		//�b�G�餤, �Ī��u����
    	if(this.host.getShipDepth() > 0D) {
    		//�B�~�[�Wy�b�t��, getPathToXYZ��Ů��G�����L��, �]��y�b�t�׭n�t�~�[
    		if(this.distY > 1.5D && this.host.getShipDepth() > 1.5D) {  //�קK�����u��
    			this.motY = 0.2F;
    		}
    		else if(this.distY < -1D) {
    			this.motY = -0.2F;
    		}
    		else {
        		this.motY = 0F;
        	}
    		
    		//�������u����
			double speed = this.host.getStateFinal(ID.MOV);
			this.distSqrt = MathHelper.sqrt_double(this.distSq);
			
			this.motX = (this.distX / this.distSqrt) * speed * 1D;
			this.motZ = (this.distZ / this.distSqrt) * speed * 1D;
			
			this.host.motionY = this.motY;
			this.host.motionX = this.motX;
			this.host.motionZ = this.motZ;
			
			//���騤�׳]�w
			float[] degree = EntityHelper.getLookDegree(motX, motY, motZ);
			this.host.rotationYaw = degree[0];
			this.host.rotationPitch = degree[1];
			
			//�Y��������F��, �h���ո���
    		if(this.host.isCollidedHorizontally) {
    			this.host.motionY += 0.2D;
    		}
			return;
       	}
    	
    	//�Ccd���@�����|
    	if(this.findCooldown <= 0) {
			this.findCooldown = 10;

        	if(!this.PetPathfinder.tryMoveToEntityLiving(this.owner, 1D)) {
        		LogHelper.info("DEBUG : Flee AI try move fail, teleport entity");
        		if(this.distSq > 200F) {
        			//�ۦPdim�~�ǰe
        			if(this.host.dimension == this.owner.dimension) {
        				this.host.setLocationAndAngles(this.owner.posX, this.owner.posY + 0.5D, this.owner.posZ, this.host.rotationYaw, this.host.rotationPitch);
                    	this.PetPathfinder.clearPathEntity();
                        return;
        			}
                }
            }//end !try move to owner
        }//end path find cooldown
    }
	
	

}
