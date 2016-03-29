package com.lulan.shincolle.ai;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import com.lulan.shincolle.ai.path.ShipPathEntity;
import com.lulan.shincolle.ai.path.ShipPathPoint;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.IShipAttackBase;
import com.lulan.shincolle.entity.IShipEmotion;

/**ATTACK ON COLLIDE SHIP VERSION
 * host必須實作IShipAttack跟IShipEmotion, 且extend EntityCreature
 */
public class EntityAIShipAttackOnCollide extends EntityAIBase {
	
    World worldObj;
    IShipAttackBase host;
    EntityLiving host2;
    /** An amount of decrementing ticks that allows the entity to attack once the tick reaches 0. */
    int attackTick;
    /** The speed with which the mob will approach the target */
    double speedTowardsTarget;
    /** When true, the mob will continue chasing its target, even if it can't find a path to them right now. */
    boolean longMemory;
    /** The PathEntity of our entity. */
    ShipPathEntity entityPathEntity;
    private int delayAttack;
    private double tarX;
    private double tarY;
    private double tarZ;

    private int failedPathFindingPenalty;

    public EntityAIShipAttackOnCollide(IShipAttackBase host, double speed, boolean longMemory) {
        this.host = host;
        this.host2 = (EntityLiving) host;
        this.worldObj = host2.worldObj;
        this.speedTowardsTarget = speed;
        this.longMemory = longMemory;
        this.setMutexBits(3);
    }

    @Override
	public boolean shouldExecute() {
    	if(this.host2.isRiding()) {
    		return false;
    	}
    	
        Entity target = this.host.getEntityTarget();

        //無目標 or 目標死亡 or 正在坐下時 不啟動AI
        if(target == null || ((IShipEmotion)host).getIsSitting()) {
            return false;
        }
        else if(target != null && target.isDead) {
        	return false;
        }
        else {
            if(-- this.delayAttack <= 0) {
                this.entityPathEntity = this.host.getShipNavigate().getPathToEntityLiving(target);
                this.delayAttack = 4 + this.host2.getRNG().nextInt(7);
                return this.entityPathEntity != null;
            }
            else {
                return true;
            }
        }
    }

    @Override
	public boolean continueExecuting() {
    	if(this.host2.isRiding()) {
    		return false;
    	}
    	
        Entity target = this.host.getEntityTarget();
        //chase target until dead
        return (target == null || !target.isEntityAlive()) ? false : true;
    }

    @Override
	public void startExecuting() {
        this.host.getShipNavigate().setPath(this.entityPathEntity, speedTowardsTarget);
        this.delayAttack = 0;
    }

    @Override
	public void resetTask() {
        this.host.getShipNavigate().clearPathEntity();
        this.host.setEntityTarget(null);
    }

    @Override
	public void updateTask() {
    	if(this.host2.isRiding()) {
    		return;
    	}
    	
        Entity target = this.host.getEntityTarget();
        
        //null check for target continue set null bug (set target -> clear target in one tick)
        if(target == null || target.isDead) {
        	resetTask();
        	return;
        }
        
        this.host2.getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);
        
        double distTarget = this.host2.getDistanceSq(target.posX, target.boundingBox.minY, target.posZ);
        double distAttack = this.host2.width * this.host2.width * 10F + target.width * 3F;
        
        --this.delayAttack;

        //官方內建的水平移動AI
        if((this.longMemory || this.host2.getEntitySenses().canSee(target)) && this.delayAttack <= 0 && (this.tarX == 0.0D && this.tarY == 0.0D && this.tarZ == 0.0D || target.getDistanceSq(this.tarX, this.tarY, this.tarZ) >= 1.0D || this.host2.getRNG().nextFloat() < 0.1F)) {
            this.tarX = target.posX;
            this.tarY = target.boundingBox.minY;
            this.tarZ = target.posZ;
            this.delayAttack = failedPathFindingPenalty + 4 + this.host2.getRNG().nextInt(7);

            if(this.host.getShipNavigate().getPath() != null) {
                ShipPathPoint finalPathPoint = this.host.getShipNavigate().getPath().getFinalPathPoint();
                if(finalPathPoint != null && target.getDistanceSq(finalPathPoint.xCoord, finalPathPoint.yCoord, finalPathPoint.zCoord) < 1) {
                    failedPathFindingPenalty = 0;
                }
                else {
                    failedPathFindingPenalty += 10;
                }
            }
            else {
                failedPathFindingPenalty += 10;
            }

            if(distTarget > 1024.0D) {
                this.delayAttack += 10;
            }
            else if (distTarget > 256.0D) {
                this.delayAttack += 5;
            }

            if(!this.host.getShipNavigate().tryMoveToEntityLiving(target, speedTowardsTarget)) {
                this.delayAttack += 10;
            }
        }

        this.attackTick = Math.max(this.attackTick - 1, 0);

        if(distTarget <= distAttack && this.attackTick <= 20) {
            this.attackTick = 20;

            if(this.host2.getHeldItem() != null) {
                this.host2.swingItem();
            }

            this.host2.attackEntityAsMob(target);
        }
    }
}