package com.lulan.shincolle.ai;

import com.lulan.shincolle.ai.path.ShipPathNavigate;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.utility.LogHelper;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;

/**FLEE AI
 * if ship's HP is below fleeHP, ship will stop attack and try to flee
 */
public class EntityAIShipFlee extends EntityAIBase {
	
	private BasicEntityShip host;
	private EntityLivingBase owner;
//	private PathNavigate PetPathfinder;
	private ShipPathNavigate ShipPathfinder;
	private static final double TP_DIST = 2048D;	//teleport condition ~ 45 blocks
	private float distSq, distSqrt;
	private float fleehp;		//flee HP (percent)
	private int findCooldown;	//find path cooldown
	//���u�e�i���\��
    private double distX, distY, distZ, motX, motY, motZ;	//��ؼЪ����u�Z��(������)
    private float rotYaw;

	public EntityAIShipFlee(BasicEntityShip entity) {
        this.host = entity;
        this.ShipPathfinder = entity.getShipNavigate();
        this.setMutexBits(7);
    }
	
	@Override
	public boolean shouldExecute() {
		this.fleehp = host.getStateMinor(ID.M.FleeHP) / 100F;
		
		//��q�C��fleeHP �B���O���U�]���O�j�����A�~����flee AI
		if(!host.isSitting() && !host.getLeashed() && 
		   (host.getHealth() / host.getMaxHealth()) <= fleehp) {
			
			EntityLivingBase OwnerEntity = (EntityLivingBase) host.getHostEntity();
			
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
	
    @Override
	public boolean continueExecuting() {
    	//�Z���Y�u��2�椺�N����
    	return shouldExecute();
    }

    @Override
	public void startExecuting() {
    	this.findCooldown = 0;
    }

    @Override
	public void resetTask() {
        this.owner = null;
        this.ShipPathfinder.clearPathEntity();
    }
    
    @Override
	public void updateTask() {
    	this.findCooldown--;
    	this.motY = 0D;
    	
    	//�]�w�Y����V
        this.host.getLookHelper().setLookPositionWithEntity(this.owner, 10.0F, this.host.getVerticalFaceSpeed());

    	//�Z���W�L�ǰe�Z��, �����ǰe��ؼФW
    	if(this.distSq > EntityAIShipFlee.TP_DIST) {
    		this.host.posX = this.owner.posX;
    		this.host.posY = this.owner.posY + 1D;
    		this.host.posZ = this.owner.posZ;
    		this.host.setPosition(this.host.posX, this.host.posY, this.host.posZ);
    	}
    	
    	//�Ccd���@�����|
    	if(this.findCooldown <= 0) {
			this.findCooldown = 10;

        	if(!this.ShipPathfinder.tryMoveToEntityLiving(this.owner, 1.2D)) {
        		LogHelper.info("DEBUG : Flee AI try move fail, teleport entity");
        		if(this.distSq > 200F) {
        			//�ۦPdim�~�ǰe
        			if(this.host.dimension == this.owner.dimension) {
        				this.host.setLocationAndAngles(this.owner.posX, this.owner.posY + 0.5D, this.owner.posZ, this.host.rotationYaw, this.host.rotationPitch);
                    	this.ShipPathfinder.clearPathEntity();
                        return;
        			}
                }
            }//end !try move to owner
        }//end path find cooldown
    }
	
	

}
