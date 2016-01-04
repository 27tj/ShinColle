package com.lulan.shincolle.ai;

import java.util.Collections;
import java.util.List;

import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityFlying;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.player.EntityPlayerMP;

import com.lulan.shincolle.entity.BasicEntityAirplane;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.BasicEntityShipHostile;
import com.lulan.shincolle.entity.IShipAttackBase;
import com.lulan.shincolle.entity.IShipInvisible;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.utility.CalcHelper;
import com.lulan.shincolle.utility.LogHelper;
import com.lulan.shincolle.utility.TargetHelper;


/**GET TARGET WITHIN SPECIFIC RANGE
 * 
 * target priority:
 * PVP > AntiAir > AntiSubm > normal target
 * 
 */
public class EntityAIShipRangeTarget extends EntityAIBase {
	
    protected Class targetClass;
    protected TargetHelper.Sorter targetSorter;
    protected IEntitySelector targetSelector;
    protected IShipAttackBase host;
    protected EntityLiving host2;
    protected BasicEntityShip hostShip;
    protected Entity targetEntity;
    protected int range;
    

    //�NmaxRange ���W�@�Ӥ�ҷ�@range1
    public EntityAIShipRangeTarget(IShipAttackBase host, Class targetClass) {
    	this.setMutexBits(1);
    	this.host = host;
    	this.host2 = (EntityLiving) host;
    	
        //�����������w
        this.targetClass = targetClass;
        this.targetSorter = new TargetHelper.Sorter(host2);
        
        if(host instanceof BasicEntityShipHostile) {
        	this.targetSelector = new TargetHelper.SelectorForHostile(host2);
        }
        else if(host instanceof BasicEntityShip) {
        	this.hostShip = (BasicEntityShip) host;
    		this.targetSelector = new TargetHelper.Selector(host2);
        }
        else {
        	this.targetSelector = new TargetHelper.Selector(host2);
        }

        //�d����w
        this.range = (int)this.host.getAttackRange();
        //�̤p�l��16��d��
        if(this.range < 16) {
        	this.range = 16;
        }
    }

    @Override
    public boolean shouldExecute() {
    	//update range
    	if(this.host != null) {
//    		LogHelper.info("DEBUG : target AI: should exec: "+host);
    		
    		//check if not sitting and every 8 ticks
        	if(this.host.getIsSitting() || this.host.getTickExisted() % 8 != 0) {
        		return false;
        	}
    		
    		this.range = (int)this.host.getAttackRange();
    		//�̤p�l��16��d��
            if(this.range < 16) {
            	this.range = 16;
            }
            
            //find target
            List list1 = null;
            List list2 = null;
            
            if(this.hostShip != null) {  //ship: pvp, AA, ASM mode
            	//Anti Air first
            	if(this.hostShip.getStateFlag(ID.F.AntiAir)) {
            		//find air target
            		list1 = this.host2.worldObj.selectEntitiesWithinAABB(BasicEntityAirplane.class, 
                    		this.host2.boundingBox.expand(this.range, this.range * 0.75D, this.range), this.targetSelector);
            		list2 = this.host2.worldObj.selectEntitiesWithinAABB(EntityFlying.class, 
                    		this.host2.boundingBox.expand(this.range, this.range * 0.75D, this.range), this.targetSelector);
            		
            		//union list
            		list1 = CalcHelper.listUnion(list1, list2);
            	}
            	
            	//if no AA target, find ASM target
            	if(list1 == null || list1.isEmpty()) {
            		if(this.hostShip.getStateFlag(ID.F.AntiSS)) {
        				list1 = this.host2.worldObj.selectEntitiesWithinAABB(IShipInvisible.class, 
                        		this.host2.boundingBox.expand(this.range, this.range * 0.75D, this.range), this.targetSelector);
                	}
            		
            		//find PVP target
            		if(list1 == null || list1.isEmpty()) {
                    	if(this.hostShip.getStateFlag(ID.F.PVPFirst)) {
                    		list1 = this.host2.worldObj.selectEntitiesWithinAABB(BasicEntityShip.class, 
                            		this.host2.boundingBox.expand(this.range, this.range * 0.75D, this.range), this.targetSelector);
                    	}
            		}
            	}
            }//end host is ship
            
            //find normal target
            if(list1 == null || list1.isEmpty()) {
	        	list1 = this.host2.worldObj.selectEntitiesWithinAABB(this.targetClass, 
	            		this.host2.boundingBox.expand(this.range, this.range * 0.75D, this.range), this.targetSelector);
            }
            
            //�Y�����target
            if(list1 != null && !list1.isEmpty()) {
            	//��ؼа�distance sort, nearest target�Ƴ̫e��
                Collections.sort(list1, this.targetSorter);

            	//get nearest target
            	this.targetEntity = (Entity) list1.get(0);
            	
            	//get random 0~2 target if >2 targets
            	if(list1.size() > 2) {
            		this.targetEntity = (Entity) list1.get(this.host2.worldObj.rand.nextInt(3));
            	}
            	
                return true;
            }//end list not null
    	}//end host not null
    	
    	return false;
    }
    
    @Override
    public void resetTask() {}

    @Override
    public void startExecuting() {
//    	LogHelper.info("DEBUG : target AI: "+this.host+"   "+this.targetEntity);
    	if(this.host != null) this.host.setEntityTarget(this.targetEntity);
    }
    
    @Override
    public boolean continueExecuting() {
        Entity target = this.host.getEntityTarget();
//        LogHelper.info("DEBUG : target AI: cont exec: "+this.host2.getCustomNameTag()+" "+this.targetMode+" "+target);
        
        //target���`�ή����ɰ������target, �í��s�}�l����target
        if(target == null || !target.isEntityAlive()) {
            return false;
        }
        else {
            double d0 = this.range * this.range;

            //�W�X�����Z��, ���ӥؼ�
            if(this.host2.getDistanceSqToEntity(target) > d0) {
                return false;
            }
            //�Ytarget�O���a, �h����OP
            return !(target instanceof EntityPlayerMP) || !((EntityPlayerMP)target).theItemInWorldManager.isCreative();
        }
    }

  
}
