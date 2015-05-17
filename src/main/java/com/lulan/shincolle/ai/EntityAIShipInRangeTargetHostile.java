package com.lulan.shincolle.ai;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.lulan.shincolle.entity.BasicEntityAirplane;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.EntityAirplane;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.utility.EntityHelper;
import com.lulan.shincolle.utility.LogHelper;

import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityFlying;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAITarget;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntityWaterMob;
import net.minecraft.entity.player.EntityPlayer;


/**GET TARGET WITHIN SPECIFIC RANGE for HOSTILE MOB
 * mode: 
 * 0:target between range1 and range2 only (�u���~��)
 * 1:target < range1 => < range2 (��������, �A���~��)
 * 2:target between range1 and range2 => < range1 (�����~��, �A������)
 * 
 * @parm host, target, range1, range2, mode
 */
public class EntityAIShipInRangeTargetHostile extends EntityAITarget {
	
    private final Class targetClass;
    private final EntityAIShipInRangeTargetHostile.Sorter targetSorter;
    private final IEntitySelector targetSelector;
    private EntityCreature host;
    private EntityLivingBase target;
    private int range1;
    private int range2;
    private int targetMode;
    private float rangeMod;
    

    //�NmaxRange ���W�@�Ӥ�ҷ�@range1
    public EntityAIShipInRangeTargetHostile(EntityCreature host, int range1, int range2, int mode) {
    	super(host, true, false);	//check onSight and not nearby(collision) only
    	this.host = host;
    	this.targetClass = EntityLiving.class;
        this.targetSorter = new EntityAIShipInRangeTargetHostile.Sorter(host);
        this.setMutexBits(1);

        //�d����w
        this.range2 = range2;
        this.range1 = range1;
        this.targetMode = mode;
        
        //�ˬd�d��, ��range2 > range1 > 1
        if(this.range1 < 1) {
        	this.range1 = 1;
        }
        if(this.range2 <= this.range1) {
        	this.range2 = this.range1 + 1;
        }
 
        //target selector init
        this.targetSelector = new IEntitySelector() {
            public boolean isEntityApplicable(Entity target2) {
            	if((target2 instanceof EntityPlayer || target2 instanceof BasicEntityShip ||
            	   target2 instanceof BasicEntityAirplane) && !target2.isDead && !target2.isInvisible()) {
            		return true;
            	}
            	return false;
            }
        };
    }

    @Override
    public boolean shouldExecute() {
    	//entity list < range1
        List list1 = this.taskOwner.worldObj.selectEntitiesWithinAABB(this.targetClass, 
        		this.taskOwner.boundingBox.expand(this.range1, this.range1 * 0.6D, this.range1), this.targetSelector);
        
        //entity list < range2
        List list2 = this.taskOwner.worldObj.selectEntitiesWithinAABB(this.targetClass, 
        		this.taskOwner.boundingBox.expand(this.range2, this.range2 * 0.6D, this.range2), this.targetSelector);
        
        //��ؼа�distance sort (increment)
        Collections.sort(list1, this.targetSorter);
        Collections.sort(list2, this.targetSorter);
		
        switch(this.targetMode) {
        case 0:  //mode 0:target between range1 and range2 only
        	list2.removeAll(list1);	 //list2�ư�range1�H�����ؼ�
        	if(list2.isEmpty()) {
                return false;
            }
            else {
            	this.target = (EntityLivingBase)list2.get(0);
            	if(list2.size() > 2) {
            		this.target = (EntityLivingBase)list2.get(this.host.worldObj.rand.nextInt(3));
            	}             
                return true;
            }
		case 1:  //mode 1:target < range1 => < range2
			if(list1.isEmpty()) {	//range1�H���S���ؼ�, �h��range2
				if(list2.isEmpty()) {
	                return false;
	            }
				else {				//range2�H�����ؼ�
					this.target = (EntityLivingBase)list2.get(0);
					if(list2.size() > 2) {
	            		this.target = (EntityLivingBase)list2.get(this.host.worldObj.rand.nextInt(3));
	            	}
	                return true;
				}
            }
            else {					//range1�H�����ؼ�
                this.target = (EntityLivingBase)list1.get(0);
                if(list1.size() > 2) {
            		this.target = (EntityLivingBase)list1.get(this.host.worldObj.rand.nextInt(3));
            	}
                return true;
            }
        case 2:  //mode 2:target between range1 and range2 => < range1
        	list2.removeAll(list1);	 //list2�ư�range1�H�����ؼ�
        	if(list2.isEmpty()) {	 //range2~range1���S���ؼ�, ���range1�H��
        		if(list1.isEmpty()) {
                    return false;
                }
        		else {				 //range1�H�����ؼ�
        			this.target = (EntityLivingBase)list1.get(0);
        			if(list1.size() > 2) {
                		this.target = (EntityLivingBase)list1.get(this.host.worldObj.rand.nextInt(3));
                	}
                    return true;
        		}
            }
            else {					 //range2�H�����ؼ�
                this.target = (EntityLivingBase)list2.get(0);
                if(list2.size() > 2) {
            		this.target = (EntityLivingBase)list2.get(this.host.worldObj.rand.nextInt(3));
            	}
                return true;
            }
        }
     
        return false;
    }
    
    @Override
    public void resetTask() {
//    	LogHelper.info("DEBUG : reset target ai "+this.host.getAttackTarget());
    }

    @Override
    public void startExecuting() { 	
        this.taskOwner.setAttackTarget(this.target);
        super.startExecuting();
//        LogHelper.info("DEBUG : start target "+this.host.getAttackTarget());
    }

    /**SORTER CLASS
     */
    public static class Sorter implements Comparator {
        private final Entity target;

        public Sorter(Entity entity) {
            this.target = entity;
        }
        
        public int compare(Object target1, Object target2) {
            return this.compare((Entity)target1, (Entity)target2);
        }

        //�t�ȷ|�Ʀblist�e��, �ȶV�j�V�᭱, list(0)�|�O�Z���̪񪺥ؼ�
        public int compare(Entity target1, Entity target2) {
            double d0 = this.target.getDistanceSqToEntity(target1);
            double d1 = this.target.getDistanceSqToEntity(target2);
            return d0 < d1 ? -1 : (d0 > d1 ? 1 : 0);
        }       
    }//end sorter class
  
}

