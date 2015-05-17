package com.lulan.shincolle.entity;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import com.lulan.shincolle.ai.EntityAIShipInRangeTargetHostile;
import com.lulan.shincolle.ai.path.ShipMoveHelper;
import com.lulan.shincolle.ai.path.ShipPathEntity;
import com.lulan.shincolle.ai.path.ShipPathNavigate;
import com.lulan.shincolle.ai.path.ShipPathPoint;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.network.S2CEntitySync;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.utility.ParticleHelper;

import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;

public class BasicEntityShipHostile extends EntityMob implements IShipAttack, IShipEmotion, IShipFloating {

	//attributes
	protected float atk;				//damage
	protected float atkSpeed;			//attack speed
	protected float atkRange;			//attack range
	protected float defValue;			//def value
	protected float movSpeed;			//def value
    protected float kbValue;			//knockback value
    protected double ShipDepth;			//���`, �Ω�������קP�w
    
    //model display
    /**EntityState: 0:HP State 1:Emotion 2:Emotion2*/
	protected byte[] StateEmotion;		//��1
	protected int StartEmotion;			//��1 �}�l�ɶ�
	protected int StartEmotion2;		//��2 �}�l�ɶ�
	protected boolean headTilt;
	
	//misc
	protected ItemStack dropItem;
	
	//AI
	public boolean canDrop;				//drop item flag
	protected ShipPathNavigate shipNavigator;	//���Ų��ʥ�navigator
	protected ShipMoveHelper shipMoveHelper;
		
	
	public BasicEntityShipHostile(World world) {
		super(world);
		this.isImmuneToFire = true;	//set ship immune to lava
		ignoreFrustumCheck = true;	//�Y�Ϥ��b���u���@��render
		this.StateEmotion = new byte[] {ID.State.EQUIP00, 0, 0, 0, 0, 0};
		this.stepHeight = 4F;
		this.canDrop = true;
		shipNavigator = new ShipPathNavigate(this, worldObj);
		shipMoveHelper = new ShipMoveHelper(this);
	}
	
	@Override
	public boolean isAIEnabled() {
		return true;
	}
	
	@Override
	public boolean isBurning() {	//display fire effect
		return this.getStateEmotion(ID.S.HPState) == ID.HPState.HEAVY;
	}
	
	//setup AI
	protected void setAIList() {
		this.clearAITasks();
		this.clearAITargetTasks();
		
		this.getNavigator().setEnterDoors(true);
		this.getNavigator().setAvoidsWater(false);
		this.getNavigator().setCanSwim(true);
	}
	
	//setup target AI: par1: 0:passive 1:active
	public void setAITargetList() {
		this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, false));
		this.targetTasks.addTask(2, new EntityAINearestAttackableTarget(this, BasicEntityShip.class, 0, true, false));
		this.targetTasks.addTask(3, new EntityAIShipInRangeTargetHostile(this, 16, 32, 1));
//		this.targetTasks.addTask(4, new EntityAINearestAttackableTarget(this, EntityPlayer.class, 0, true, false));
	}
	
	@Override
    public boolean attackEntityFrom(DamageSource attacker, float atk) {
		//disable 
		if(attacker.getDamageType() == "inWall") {
			return false;
		}
		
		if(attacker.getDamageType() == "outOfWorld") {
			this.setDead();
			return true;
		}
				
		//set hurt face
    	if(this.getStateEmotion(ID.S.Emotion) != ID.Emotion.O_O) {
    		this.setStateEmotion(ID.S.Emotion, ID.Emotion.O_O, true);
    	}
    	
    	//�i��def�p��
        float reduceAtk = atk * (1F - this.defValue / 100F);    
        if(atk < 0) { atk = 0; }
        
        //�L�Ī�entity�ˮ`�L��
  		if(this.isEntityInvulnerable()) {	
        	return false;
        }
  		
  		if(attacker.getSourceOfDamage() != null) {
  			Entity entity = attacker.getSourceOfDamage();
  			
  			//���|��ۤv�y���ˮ`
  			if(entity.equals(this)) {  
  				return false;
  			}
  			
  			//�Y����@�ɥ~, �h�����ϸ�entity����
  	        if(attacker.getDamageType().equals("outOfWorld")) {
  	        	this.setDead();
  	        	return false;
  	        }
  		}
    	
    	return super.attackEntityFrom(attacker, reduceAtk);
	}
	
	//clear AI
	protected void clearAITasks() {
		tasks.taskEntries.clear();
	}
	
	//clear target AI
	protected void clearAITargetTasks() {
		this.setAttackTarget(null);
		targetTasks.taskEntries.clear();
	}
	
	//����egg�]�w
	public ItemStack getDropEgg() {
		return this.dropItem;
	}
	
	//���`����
	@Override
	protected String getLivingSound() {
		return Reference.MOD_ID+":ship-say";
    }
	
	//���˭���
	@Override
    protected String getHurtSound() {
    	
        return Reference.MOD_ID+":ship-hurt";
    }

    //���`����
    @Override
    protected String getDeathSound() {
    	return Reference.MOD_ID+":ship-death";
    }

    //���Ĥj�p
    @Override
    protected float getSoundVolume() {
        return ConfigHandler.shipVolume;
    }

	@Override
	public byte getStateEmotion(int id) {
		return StateEmotion[id];
	}

	@Override
	public void setStateEmotion(int id, int value, boolean sync) {
		StateEmotion[id] = (byte) value;
		
		if(sync && !worldObj.isRemote) {
			TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 32D);
			CommonProxy.channelE.sendToAllAround(new S2CEntitySync(this, 4), point);
		}
	}

	@Override
	public boolean getStateFlag(int flag) {
		return this.headTilt;
	}

	@Override
	public void setStateFlag(int id, boolean flag) {
		if(id == ID.F.HeadTilt) this.headTilt = flag;
	}

	@Override
	public int getStartEmotion() {
		return this.StartEmotion;
	}

	@Override
	public int getStartEmotion2() {
		return this.StartEmotion2;
	}

	@Override
	public void setStartEmotion(int par1) {
		this.StartEmotion = par1;
	}

	@Override
	public void setStartEmotion2(int par1) {
		this.StartEmotion2 = par1;
	}

	@Override
	public int getTickExisted() {
		return this.ticksExisted;
	}
	
	@Override
	public float getAttackDamage() {
		return this.atk;
	}

	@Override
	public int getAttackTime() {
		return this.attackTime;
	}

	@Override
	public boolean attackEntityWithAmmo(Entity target) {
		//get attack value
		float atk = this.atk;
		//set knockback value (testing)
		float kbValue = 0.05F;
		//update entity look at vector (for particle spawn)
        //����k��getLook�٥��T (client sync���D)
        float distX = (float) (target.posX - this.posX);
        float distY = (float) (target.posY - this.posY);
        float distZ = (float) (target.posZ - this.posZ);
        float distSqrt = MathHelper.sqrt_float(distX*distX + distY*distY + distZ*distZ);
        distX = distX / distSqrt;
        distY = distY / distSqrt;
        distZ = distZ / distSqrt;
      
        //�o�g�̷����S��
        TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64D);
		CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 6, this.posX, this.posY+3.5D, this.posZ, distX, 2.8D, distZ, true), point);

		//play cannon fire sound at attacker
        playSound(Reference.MOD_ID+":ship-firesmall", ConfigHandler.fireVolume, 0.7F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
        //play entity attack sound
        if(this.rand.nextInt(10) > 7) {
        	this.playSound(Reference.MOD_ID+":ship-hitsmall", ConfigHandler.shipVolume, 1F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
        }

        //calc miss chance, if not miss, calc cri/multi hit   
        if(this.rand.nextFloat() < 0.2F) {
        	atk = 0;	//still attack, but no damage
        	//spawn miss particle
    		CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 10, false), point);
        }
        else {
        	//roll cri -> roll double hit -> roll triple hit (triple hit more rare)
        	//calc critical
        	if(this.rand.nextFloat() < 0.15F) {
        		atk *= 1.5F;
        		//spawn critical particle
        		CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 11, false), point);
        	}
        	else {
        		//calc double hit
            	if(this.rand.nextFloat() < 0.15F) {
            		atk *= 2F;
            		//spawn double hit particle
            		CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 12, false), point);
            	}
            	else {
            		//calc double hit
                	if(this.rand.nextFloat() < 0.15F) {
                		atk *= 3F;
                		//spawn triple hit particle
                		CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 13, false), point);
                	}
            	}
        	}
        }
        
        //vs player = 25% dmg
  		if(target instanceof EntityPlayer) {
  			atk *= 0.25F;
  			
    		if(atk > 159F) {
    			atk = 159F;	//same with TNT
    		}
  		}
  		
	    //�Natk��attacker�ǵ��ؼЪ�attackEntityFrom��k, �b�ؼ�class���p��ˮ`
	    //�åB�^�ǬO�_���\�ˮ`��ؼ�
	    boolean isTargetHurt = target.attackEntityFrom(DamageSource.causeMobDamage(this), atk);

	    //if attack success
	    if(isTargetHurt) {
	    	//calc kb effect
	        if(kbValue > 0) {
	            target.addVelocity((double)(-MathHelper.sin(rotationYaw * (float)Math.PI / 180.0F) * kbValue), 
	                   0.1D, (double)(MathHelper.cos(rotationYaw * (float)Math.PI / 180.0F) * kbValue));
	            motionX *= 0.6D;
	            motionZ *= 0.6D;
	        }
	        
        	//display hit particle on target
	        TargetPoint point1 = new TargetPoint(this.dimension, target.posX, target.posY, target.posZ, 64D);
			CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(target, 9, false), point1);
        }

	    return isTargetHurt;
	}

	@Override
	public boolean attackEntityWithHeavyAmmo(Entity target) {
		return false;
	}

	@Override
	public EntityLivingBase getOwner() {
		return this;
	}

	@Override
	public float getAttackSpeed() {
		return this.atkSpeed;
	}

	@Override
	public float getAttackRange() {
		return this.atkRange;
	}

	@Override
	public float getMoveSpeed() {
		return this.movSpeed;
	}
	
	@Override
	public EntityLivingBase getTarget() {
		return this.getAttackTarget();
	}

	@Override
	public boolean getIsRiding() {
		return false;
	}

	@Override
	public boolean getIsSprinting() {
		return false;
	}

	@Override
	public boolean getIsSitting() {
		return false;
	}

	@Override
	public boolean getIsSneaking() {
		return false;
	}

	@Override
	public boolean hasAmmoLight() {
		return true;
	}

	@Override
	public boolean hasAmmoHeavy() {
		return true;
	}

	@Override
	public int getAmmoLight() {
		return 100;
	}

	@Override
	public int getAmmoHeavy() {
		return 100;
	}

	@Override
	public void setAmmoLight(int num) {}

	@Override
	public void setAmmoHeavy(int num) {}

	@Override
	public double getShipDepth() {
		return ShipDepth;
	}
	
	//check block from entity posY + offset
	public Block checkBlockWithOffset(int par1) {
		int blockX = MathHelper.floor_double(this.posX);
	    int blockY = MathHelper.floor_double(this.boundingBox.minY);
	    int blockZ = MathHelper.floor_double(this.posZ);

	    return this.worldObj.getBlock(blockX, blockY + par1, blockZ);    
	}
	
	//replace isInWater, check water block with NO extend AABB
	private void checkDepth() {
		Block BlockCheck = checkBlockWithOffset(0);
		
		if(BlockCheck == Blocks.water || BlockCheck == Blocks.lava) {
			ShipDepth = 1D;
			//check 10 blocks
			for(int i = 1; i < 10; ++i) {
				BlockCheck = checkBlockWithOffset(i);
				
				if(BlockCheck == Blocks.water || BlockCheck == Blocks.lava) {
					ShipDepth++;
				}
				else {
					break;
				}
			}		
			ShipDepth = ShipDepth - (this.posY - (int)this.posY);
		}
		else {
			ShipDepth = 0;
		}
	}
	
	@Override
    public void moveEntityWithHeading(float movX, float movZ) {
        double d0;

        if(this.isInWater() || this.handleLavaMovement()) { //�P�w���G�餤��, ���|�۰ʤU�I
            d0 = this.posY;
            this.moveFlying(movX, movZ, this.movSpeed*0.4F); //�������t�׭p��(�t�}���ĪG)
            this.moveEntity(this.motionX, this.motionY, this.motionZ);
            //�������O
            this.motionX *= 0.8D;
            this.motionY *= 0.8D;
            this.motionZ *= 0.8D;
            //��������F��|�W��
            if (this.isCollidedHorizontally && this.isOffsetPositionInLiquid(this.motionX, this.motionY + 0.6D - this.posY + d0, this.motionZ)) {
                this.motionY = 0.3D;
            }
        }
        else {									//��L���ʪ��A
            float f2 = 0.91F;
            
            if(this.onGround) {					//�b�a������
                f2 = this.worldObj.getBlock(MathHelper.floor_double(this.posX), MathHelper.floor_double(this.boundingBox.minY) - 1, MathHelper.floor_double(this.posZ)).slipperiness * 0.91F;
            }

            float f3 = 0.16277136F / (f2 * f2 * f2);
            float f4;
            
            if(this.onGround) {
                f4 = this.getAIMoveSpeed() * f3;
            }
            else {								//���D��
                f4 = this.jumpMovementFactor;
            }
            this.moveFlying(movX, movZ, f4);
            f2 = 0.91F;
            
            if(this.onGround) {
                f2 = this.worldObj.getBlock(MathHelper.floor_double(this.posX), MathHelper.floor_double(this.boundingBox.minY) - 1, MathHelper.floor_double(this.posZ)).slipperiness * 0.91F;
            }

            if(this.isOnLadder()) {				//���ӱ褤
                float f5 = 0.15F;
                //����ӱ�ɪ���V���ʳt��
                if(this.motionX < (double)(-f5)) {
                    this.motionX = (double)(-f5);
                }
                if(this.motionX > (double)f5) {
                    this.motionX = (double)f5;
                }
                if(this.motionZ < (double)(-f5)) {
                    this.motionZ = (double)(-f5);
                }
                if(this.motionZ > (double)f5) {
                    this.motionZ = (double)f5;
                }

                this.fallDistance = 0.0F;
                //����ӱ誺���U�t��
                if (this.motionY < -0.15D) {
                    this.motionY = -0.15D;
                }

                boolean flag = this.isSneaking();
                //�Y�O���ӱ�ɬ�sneaking, �h���|���U(�d�b�ӱ�W)
                if(flag && this.motionY < 0D) {
                    this.motionY = 0D;
                }
            }

            this.moveEntity(this.motionX, this.motionY, this.motionZ);
            //���ӱ����, �h�|���W��
            if(this.isCollidedHorizontally && this.isOnLadder()) {
                this.motionY = 0.4D;
            }
            //�۵M����
            if(this.worldObj.isRemote && (!this.worldObj.blockExists((int)this.posX, 0, (int)this.posZ) || !this.worldObj.getChunkFromBlockCoords((int)this.posX, (int)this.posZ).isChunkLoaded)) {
                if (this.posY > 0.0D) {
                    this.motionY = -0.1D;	//�Ů𤤪�gravity��0.1D
                }
                else {
                    this.motionY = 0.0D;
                }
            }
            else {
                this.motionY -= 0.08D;
            }
            //�Ů𤤪��T��V���O
            this.motionY *= 0.98D;			
            this.motionX *= (double)f2;
            this.motionZ *= (double)f2;
        }
        //�p��|���\�ʭ�
        this.prevLimbSwingAmount = this.limbSwingAmount;
        d0 = this.posX - this.prevPosX;
        double d1 = this.posZ - this.prevPosZ;
        float f6 = MathHelper.sqrt_double(d0 * d0 + d1 * d1) * 4.0F;

        if (f6 > 1.0F)
        {
            f6 = 1.0F;
        }

        this.limbSwingAmount += (f6 - this.limbSwingAmount) * 0.4F;
        this.limbSwing += this.limbSwingAmount;
    }
	
	@Override
	public void onUpdate() {
		super.onUpdate();
		
		checkDepth();
		
		//client side
		if(this.worldObj.isRemote && this.isInWater()) {
			//�����ʮ�, ���ͤ���S��
			//(�`�N��entity�]���]���D���t��s, client�ݤ��|��smotionX���ƭ�, �ݦۦ�p��)
			double motX = this.posX - this.prevPosX;
			double motZ = this.posZ - this.prevPosZ;
			double parH = this.posY - (int)this.posY;
			
			if(motX != 0 || motZ != 0) {
				ParticleHelper.spawnAttackParticleAt(this.posX + motX*1.5D, this.posY, this.posZ + motZ*1.5D, 
						-motX*0.5D, 0D, -motZ*0.5D, (byte)15);
			}
		}
	}
	
	//check entity state every tick
	@Override
	public void onLivingUpdate() {
		super.onLivingUpdate();
		
		//server side
        if((!worldObj.isRemote)) {      	
        	//check every 100 ticks
        	if(ticksExisted % 100 == 0) {
        		//set air value
        		if(this.getAir() < 300) {
                	setAir(300);
                }
        	}//end every 100 ticks

        	//clear dead target for vanilla AI bug
  			if(this.getAttackTarget() != null) {
  				if(this.getAttackTarget().isDead || 
  				   this.getAttackTarget() instanceof BasicEntityShipHostile ||
  				   this.getAttackTarget() instanceof EntityRensouhouBoss) {
  					this.setAttackTarget(null);
  				}
  			}
        }
        //client side
        else {
        	if(this.ticksExisted % 100 == 0) {
	        	//check hp state
	    		float hpState = this.getHealth() / this.getMaxHealth();
	    		if(hpState > 0.75F) {		//normal
	    			this.setStateEmotion(ID.S.HPState, ID.HPState.NORMAL, false);
	    		}
	    		else if(hpState > 0.5F){	//minor damage
	    			this.setStateEmotion(ID.S.HPState, ID.HPState.MINOR, false);
	    		}
				else if(hpState > 0.25F){	//moderate damage
					this.setStateEmotion(ID.S.HPState, ID.HPState.MODERATE, false);   			
				}
				else {						//heavy damage
					this.setStateEmotion(ID.S.HPState, ID.HPState.HEAVY, false);
				}
        	}	
    		
        	if(this.ticksExisted % 20 == 0) {
    			//generate HP state effect
    			switch(getStateEmotion(ID.S.HPState)) {
    			case ID.HPState.MINOR:
    				ParticleHelper.spawnAttackParticleAt(this.posX, this.posY + 0.7D, this.posZ, 
    						this.width * 1.5D, 0.05D, 0D, (byte)4);
    				break;
    			case ID.HPState.MODERATE:
    				ParticleHelper.spawnAttackParticleAt(this.posX, this.posY + 0.7D, this.posZ, 
    						this.width * 1.5D, 0.05D, 0D, (byte)5);
    				break;
    			case ID.HPState.HEAVY:
    				ParticleHelper.spawnAttackParticleAt(this.posX, this.posY + 0.7D, this.posZ, 
    						this.width * 1.5D, 0.05D, 0D, (byte)7);
    				break;
    			default:
    				break;
    			}
    		}
        }
	}
	
	@Override
	public ShipPathNavigate getShipNavigate() {
		return this.shipNavigator;
	}

	@Override
	public ShipMoveHelper getShipMoveHelper() {
		return this.shipMoveHelper;
	}
	
	//update ship move helper
	@Override
	protected void updateAITasks() {
		super.updateAITasks();
        
        //�Y������path, �h��sship navigator
        if(!this.getShipNavigate().noPath()) {
			//��particle���path point
			if(this.ticksExisted % 20 == 0) {
				ShipPathEntity pathtemp = this.getShipNavigate().getPath();
				ShipPathPoint pointtemp;
				
				for(int i = 0; i < pathtemp.getCurrentPathLength(); i++) {
					pointtemp = pathtemp.getPathPointFromIndex(i);
					//�o�g�̷����S��
			        TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 48D);
					//���|�I�e����, �ؼ��I�e���
					if(i == pathtemp.getCurrentPathIndex()) {
						CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 16, pointtemp.xCoord, pointtemp.yCoord+0.5D, pointtemp.zCoord, 0F, 0F, 0F, false), point);
					}
					else {
						CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 18, pointtemp.xCoord, pointtemp.yCoord+0.5D, pointtemp.zCoord, 0F, 0F, 0F, false), point);
					}
				}
			}

			this.worldObj.theProfiler.startSection("ship navi");
	        this.shipNavigator.onUpdateNavigation();
	        this.worldObj.theProfiler.endSection();
	        this.worldObj.theProfiler.startSection("ship move");
	        this.shipMoveHelper.onUpdateMoveHelper();
	        this.worldObj.theProfiler.endSection();
		}
    }
	
	@Override
	public boolean canFly() {
		return false;
	}


}
