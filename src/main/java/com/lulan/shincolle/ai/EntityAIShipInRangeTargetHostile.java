package com.lulan.shincolle.ai;

import java.util.Collections;
import java.util.List;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAITarget;

import com.lulan.shincolle.utility.TargetHelper;


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
    private final TargetHelper.Sorter targetSorter;
    private final TargetHelper.SelectorForHostile targetSelector;
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
        this.targetSorter = new TargetHelper.Sorter(host);
        this.targetSelector = new TargetHelper.SelectorForHostile(host);
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

  
}

