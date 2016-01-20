package com.lulan.shincolle.ai;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;

import com.lulan.shincolle.entity.BasicEntityMount;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.IShipFloating;
import com.lulan.shincolle.entity.IShipGuardian;
import com.lulan.shincolle.reference.ID;

/**SHIP FLOATING ON WATER AI
 * �Y�b����, �B���W�@�欰�Ů�, �h�|���դW�B�ï��b�����W
 * (entity����̵M�b����)
 */
public class EntityAIShipFloating extends EntityAIBase {
	
	private IShipFloating host;
    private BasicEntityShip hostShip;
    private BasicEntityMount hostMount;
    private EntityLivingBase hostLiving;
    

    public EntityAIShipFloating(IShipFloating entity) {
    	this.host = entity;
    	this.hostLiving = (EntityLivingBase) entity;
    	
    	if(entity instanceof BasicEntityShip) {
    		this.hostShip = (BasicEntityShip) entity;
    	}
    	else if(entity instanceof BasicEntityMount) {
    		this.hostMount = (BasicEntityMount) entity;
    	}
    	
        this.setMutexBits(5);
    }

    @Override
	public boolean shouldExecute() {
    	//ship��: �ˬdhost���U
    	if(hostShip != null) {
    		if(hostShip.isRiding()) {
    			return false;
    		}
    		
    		if(isInGuardPosition(hostShip)) {
    			return false;
    		}
    		
    		//��L���p
    		return !this.hostShip.isSitting() && this.hostShip.getStateFlag(ID.F.CanFloatUp);
    	}
    	//mount��: �ˬdmount���` & host���U
    	else if(hostMount != null && hostMount.getHostEntity() != null) {
			this.hostShip = (BasicEntityShip) hostMount.getHostEntity();
			
			if(isInGuardPosition(hostMount)) {
    			return false;
    		}
			
			return !this.hostShip.isSitting() && hostMount.getShipDepth() > 0.47D;
		}
    	//��L��
    	else {
    		return host.getShipDepth() > 0.47D;
    	}
    }

    @Override
	public void updateTask() {
    	//�W�B����w���� (���餴�b����)
    	if(this.host.getShipDepth() > 4D) {
    		this.hostLiving.motionY += 0.025D;
    		return;
    	}
    	
    	if(this.host.getShipDepth() > 1D) {
    		this.hostLiving.motionY += 0.015D;
    		return;
    	}
    	
    	if(this.host.getShipDepth() > 0.7D) {
    		this.hostLiving.motionY += 0.007D;
    		return;
    	}
    	
    	if(this.host.getShipDepth() > 0.47D) {
    		this.hostLiving.motionY += 0.0012D;
    		return;
    	}
    	   	
    }
    
    //check is in guard position
    public boolean isInGuardPosition(IShipGuardian entity) {
    	//�Yguard��, �h�ˬd�O�_�F��guard�Z��
		if(!entity.getStateFlag(ID.F.CanFollow)) {
			float fMin = entity.getStateMinor(ID.M.FollowMin) + ((Entity)entity).width * 0.5F;
			fMin = fMin * fMin;
			
			//�Y�u��entity, �ˬdentity�Z��
			if(entity.getGuardedEntity() != null) {
				double distSq = ((Entity)entity).getDistanceSqToEntity(entity.getGuardedEntity());
				if(distSq < fMin) return true;
			}
			//�Y�u�ìY�a�I, �h�ˬd�P���I�Z��
			else if(entity.getStateMinor(ID.M.GuardY) > 0) {
				double distSq = ((Entity)entity).getDistanceSq(entity.getStateMinor(ID.M.GuardX), entity.getStateMinor(ID.M.GuardY), entity.getStateMinor(ID.M.GuardZ));
				if(distSq < fMin && ((Entity)entity).posY >= entity.getStateMinor(ID.M.GuardY)) return true;
			}
		}
		
		return false;
    }
    
    
}
