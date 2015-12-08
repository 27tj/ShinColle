package com.lulan.shincolle.ai;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;

import com.lulan.shincolle.entity.BasicEntityShip;

/**SIT AI FOR SHIP
 * �i�H�b�G�餤���U
 */
public class EntityAIShipSit extends EntityAIBase
{
    private BasicEntityShip host;
    private EntityLivingBase owner;

    public EntityAIShipSit(BasicEntityShip entity) {
        this.host = entity;
        this.setMutexBits(5);
    }

    @Override
	public boolean shouldExecute() {
//    	LogHelper.info("DEBUG : exec sitting "+(this.owner == null));
        return this.host.isSitting();
    }

    @Override
	public void startExecuting() {
    	this.host.setSitting(true);
    	this.host.setJumping(false);
    }
    
    @Override
	public void updateTask() {
//    	LogHelper.info("DEBUG : exec sitting");
    	this.host.getNavigator().clearPathEntity();    
        this.host.setPathToEntity(null);
        this.host.setTarget(null);
        this.host.setAttackTarget(null);
        this.host.setEntityTarget(null);
    }

    @Override
	public void resetTask() {
        this.host.setSitting(false);
    }

}
