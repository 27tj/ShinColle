package com.lulan.shincolle.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIMoveTowardsTarget;
import net.minecraft.entity.ai.EntityAIOpenDoor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.pathfinding.PathEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import com.lulan.shincolle.ShinColle;
import com.lulan.shincolle.ai.EntityAIShipAttackOnCollide;
import com.lulan.shincolle.ai.EntityAIShipFloating;
import com.lulan.shincolle.ai.EntityAIShipFollowOwner;
import com.lulan.shincolle.ai.EntityAIShipGuarding;
import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.ai.EntityAIShipWander;
import com.lulan.shincolle.ai.path.ShipMoveHelper;
import com.lulan.shincolle.ai.path.ShipPathNavigate;
import com.lulan.shincolle.entity.mounts.EntityMountSeat;
import com.lulan.shincolle.entity.other.EntityAbyssMissile;
import com.lulan.shincolle.entity.other.EntityRensouhou;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.network.S2CEntitySync;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.reference.Values;
import com.lulan.shincolle.utility.CalcHelper;
import com.lulan.shincolle.utility.EntityHelper;
import com.lulan.shincolle.utility.LogHelper;
import com.lulan.shincolle.utility.ParticleHelper;

import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import cpw.mods.fml.common.network.internal.FMLNetworkHandler;

/**MOUNT ENTITY
 * mount use cannon attack, no aircraft attack
 * all states get from host ex: sitting, leashed, sprinting...
 */
abstract public class BasicEntityMount extends EntityCreature implements IShipMount, IShipCannonAttack, IShipGuardian {
	
	public BasicEntityShip host;  				//host
	public EntityMountSeat seat2;				//seat 2
	public EntityLivingBase riddenByEntity2;	//second rider
	protected World world;
	protected ShipPathNavigate shipNavigator;	//���Ų��ʥ�navigator
	protected ShipMoveHelper shipMoveHelper;
	protected Entity atkTarget;
    
    //attributes
	protected float atkRange;			//attack range
	protected float movSpeed;			//def value
    
    //model display
    /**EntityState: 0:HP State 1:Emotion 2:Emotion2*/
	protected byte StateEmotion;		//��1
	protected byte StateEmotion2;		//��2
	protected int StartEmotion;			//��1 �}�l�ɶ�
	protected int StartEmotion2;		//��2 �}�l�ɶ�
	protected boolean headTilt;
	protected float[] ridePos;
	
	//AI
	protected double ShipDepth;			//���`, �Ω�������קP�w
	public int keyPressed;				//key(bit): 0:W 1:S 2:A 3:D 4:Jump
	
    public BasicEntityMount(World world) {	//client side
		super(world);
		isImmuneToFire = true;
		ignoreFrustumCheck = true;	//�Y�Ϥ��b���u���@��render
		stepHeight = 3F;
		keyPressed = 0;
		shipNavigator = new ShipPathNavigate(this, worldObj);
		shipMoveHelper = new ShipMoveHelper(this);
		ridePos = new float[] {0F,0F,0F};
		
	}
    
    //���`����
    @Override
	protected String getLivingSound() {
		if(ConfigHandler.useWakamoto && rand.nextInt(30) == 0) {
			return Reference.MOD_ID+":ship-waka_idle";
		}
		return null;
	}
  	
  	//���˭���
    @Override
	protected String getHurtSound() {
		if(ConfigHandler.useWakamoto && rand.nextInt(30) == 0) {
			return Reference.MOD_ID+":ship-waka_hurt";
		}
		return null;
	}

	//���`����
    @Override
	protected String getDeathSound() {
		if(ConfigHandler.useWakamoto) {
			return Reference.MOD_ID+":ship-waka_death";
		}
		return null;
	}

	//���Ĥj�p
    @Override
	protected float getSoundVolume() {
		return ConfigHandler.shipVolume * 0.4F;
	}
	
    //���ĭ���
    @Override
	protected float getSoundPitch() {
    	return 1F;
    }
	
    //setup AI
	public void setAIList() {
		if(!this.worldObj.isRemote) {
			this.clearAITasks();
			this.clearAITargetTasks();

			this.getNavigator().setEnterDoors(true);
			this.getNavigator().setAvoidsWater(false);
			this.getNavigator().setCanSwim(true);

			//high priority
			this.tasks.addTask(1, new EntityAIShipGuarding(this));		//0111
			this.tasks.addTask(2, new EntityAIShipFollowOwner(this));	//0111
			
			//use melee attack
			if(this.getStateFlag(ID.F.UseMelee)) {
				this.tasks.addTask(12, new EntityAIShipAttackOnCollide(this, 1D, true));//0011
				this.tasks.addTask(13, new EntityAIMoveTowardsTarget(this, 1D, 48F));	//0001
			}
			
			//idle AI
			//moving
			this.tasks.addTask(21, new EntityAIOpenDoor(this, true));	//0000
			this.tasks.addTask(22, new EntityAIShipFloating(this));		//0101
			this.tasks.addTask(25, new EntityAIShipWander(this, 0.8D));	//0001
		}
	}
	
	//called client+server both side!!
	@Override
    public boolean attackEntityFrom(DamageSource attacker, float atk) {
		//disalbe inWall damage
		if(attacker.getDamageType() == "inWall") {
			return false;
		}
		
		if(attacker.getDamageType() == "fall") {
			return false;
		}
     
        //�L�Ī�entity�ˮ`�L��
  		if(this.isEntityInvulnerable()) {	
        	return false;
        }
		
		//server side
		if(!this.worldObj.isRemote) {
			if(host == null) {
				this.setDead();
				return false;
			}
			
			//set host hurt face
	    	if(this.host.getStateEmotion(ID.S.Emotion) != ID.Emotion.O_O) {
	    		this.host.setStateEmotion(ID.S.Emotion, ID.Emotion.O_O, true);
	    	}
	        
	        if(attacker.getSourceOfDamage() != null) {
	  			Entity entity = attacker.getSourceOfDamage();
	  			
	  			//���|��ۤv�y���ˮ`, �i�K�̬r/����/�������ˮ` (�����ۤv��ۤv�y���ˮ`)
	  			if(entity.equals(this)) {
	  				return false;
	  			}
	  			
	  			//�Y�����謰player, �h�ץ��ˮ`
	  			if(entity instanceof EntityPlayer) {
					//�Y�T��friendlyFire, �h�ˮ`�]��0
					if(!ConfigHandler.friendlyFire) {
						return false;
					}
	  			}
	  			
	  			//�i��def�p��
				float reduceAtk = atk * (1F - (this.getDefValue() - rand.nextInt(20) + 10F) * 0.01F);    
				
				//ship vs ship, config�ˮ`�վ�
				if(entity instanceof BasicEntityShip || entity instanceof BasicEntityAirplane || 
				   entity instanceof EntityRensouhou || entity instanceof BasicEntityMount) {
					reduceAtk = reduceAtk * (float)ConfigHandler.dmgSummon * 0.01F;
				}
				
				//ship vs ship, damage type�ˮ`�վ�
				if(entity instanceof IShipAttackBase) {
					//get attack time for damage modifier setting (day, night or ...etc)
					int modSet = this.worldObj.provider.isDaytime() ? 0 : 1;
					reduceAtk = CalcHelper.calcDamageByType(reduceAtk, ((IShipAttackBase) entity).getDamageType(), this.getDamageType(), modSet);
				}
				
				//min damage�]��1
		        if(reduceAtk < 1) reduceAtk = 1;
		        
		        //����host�����U�ʧ@
		        if(host != null) {
		        	this.host.setSitting(false);
		        }
				
		        return super.attackEntityFrom(attacker, reduceAtk);
	  		}
		}
		
		return false;
	}
	
	//BUG: NOT WORKING
	@Override
	public boolean canBePushed() {
        return false;
    }
	
	@Override
  	public boolean interact(EntityPlayer player) {	
		ItemStack itemstack = player.inventory.getCurrentItem();  //get item in hand
		
		//use cake to change state
		if(itemstack != null && host != null) {
			//use cake
			if(itemstack.getItem() == Items.cake) {
				switch(host.getStateEmotion(ID.S.State)) {
				case ID.State.NORMAL:	//���q���M��
					host.setStateEmotion(ID.S.State, ID.State.EQUIP00, true);
					break;
				case ID.State.EQUIP00:	//�M���ശ�q
					host.setStateEmotion(ID.S.State, ID.State.NORMAL, true);
					this.setRiderNull();
					break;
				default:
					host.setStateEmotion(ID.S.State, ID.State.NORMAL, true);
					break;
				}
				this.host.setPositionAndUpdate(posX, posY + 2D, posZ);
				return true;
			}
			
			//use lead
			if(itemstack.getItem() == Items.lead && this.allowLeashing()) {	
				this.setLeashedToEntity(player, true);
				return true;
	        }
		}
		
		//�p�G�w�g�Q�i�j, �A�I�@�U�i�H�Ѱ��i�j
		if(this.getLeashed() && this.getLeashedToEntity() == player) {
            this.clearLeashed(true, !player.capabilities.isCreativeMode);
            return true;
        }

		//click without shift = host set sitting
		if(!this.worldObj.isRemote && !player.isSneaking()) {			
			//�Y�y��G�w�g���H, �h�k��אּ���U
			if(this.riddenByEntity2 != null) {
				//seat2�w�g�S�H�M, �hrider 2�]��null
				if(this.seat2 == null) {
					//summon seat entity
  	  	  			EntityMountSeat seat = new EntityMountSeat(worldObj, this);
  	  	  			seat.posX = this.posX;
  	  	  			seat.posY = this.posY;
  	  	  			seat.posZ = this.posZ;
  	  	  			this.seat2 = seat;
  	  	  			this.worldObj.spawnEntityInWorld(seat);
	  	  	  			
					//set riding
	  	  			player.mountEntity(seat2);
	  	  			this.riddenByEntity2 = player;
	  	  			this.StateEmotion = 1;
	  	  			TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64D);
	  	  			CommonProxy.channelE.sendToAllAround(new S2CEntitySync(this, 5), point);
					return true;
				}
				
				//�u���D�H�i�H�]�����U
				if(EntityHelper.checkSameOwner(player, this.host)) {
					this.host.setSitting(!this.host.isSitting());
		            this.isJumping = false;
		            this.setPathToEntity(null);
		            this.setTarget(null);
		            this.host.setTarget(null);
		            this.setAttackTarget(null);
		            this.setEntityTarget(null);
		            this.host.setAttackTarget(null);
		            this.host.setEntityTarget(null);
		            return true;
				}	
			}
			//�y��G�S�H, �h�M�W�y�M (�Ҧ����a�ҾA��)
			else {
				if(this.seat2 == null) {
					//summon seat entity
  	  	  			EntityMountSeat seat = new EntityMountSeat(worldObj, this);
  	  	  			seat.posX = this.posX;
	  	  			seat.posY = this.posY;
	  	  			seat.posZ = this.posZ;
  	  	  			this.seat2 = seat;
  	  	  			this.worldObj.spawnEntityInWorld(seat);
				}
				
  	  			//set riding entity
  	  			player.mountEntity(seat2);
  	  			this.riddenByEntity2 = player;
  	  			this.StateEmotion = 1;
  				TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64D);
  	  			CommonProxy.channelE.sendToAllAround(new S2CEntitySync(this, 5), point);
				return true;
			}
        }
		
		//shift+right click�ɥ��}host GUI
		if(player.isSneaking() && EntityHelper.checkSameOwner(player, this.host)) {  
			int eid = this.host.getEntityId();
			//player.openGui vs FMLNetworkHandler ?
    		FMLNetworkHandler.openGui(player, ShinColle.instance, ID.G.SHIPINVENTORY, this.worldObj, this.host.getEntityId(), 0, 0);
    		return true;
		}
		
		return false;
  	}
	
	@Override
	public void onUpdate() {
		super.onUpdate();
		
		//apply movement by key pressed
		if(this.host != null && !host.getStateFlag(ID.F.NoFuel) &&
		   this.seat2 != null && this.seat2.riddenByEntity != null && this.keyPressed > 0) {
			EntityLivingBase rider2 = (EntityLivingBase)this.seat2.riddenByEntity;
			float yaw = rider2.rotationYawHead % 360F * Values.N.RAD_MUL;
			float pitch = rider2.rotationPitch % 360F * Values.N.RAD_MUL;
			
			this.applyMovement(pitch, yaw);
			this.rotationYaw = rider2.rotationYaw;
		}
			
		//check depth
		EntityHelper.checkDepth(this);
		
		//sync rotate angle
		if(this.riddenByEntity != null) {
//			if(!this.worldObj.isRemote) {
			//�Y�S���b���ʤ�, �h�j����rider�¦V
			if(this.getShipNavigate().noPath() && this.getNavigator().noPath()) {
				this.rotationYawHead = ((EntityLivingBase)this.riddenByEntity).rotationYawHead;
				this.prevRotationYaw = ((EntityLivingBase)this.riddenByEntity).prevRotationYaw;
				this.rotationYaw = ((EntityLivingBase)this.riddenByEntity).rotationYaw;
				this.renderYawOffset = ((EntityLivingBase)this.riddenByEntity).renderYawOffset;
			}
			//�Y���ʤ�, �hrider���mount�¦V
			else {
				((EntityLivingBase)this.riddenByEntity).rotationYawHead = this.rotationYawHead;
				((EntityLivingBase)this.riddenByEntity).prevRotationYaw = this.prevRotationYaw;
				((EntityLivingBase)this.riddenByEntity).rotationYaw = this.rotationYaw;
				((EntityLivingBase)this.riddenByEntity).renderYawOffset = this.renderYawOffset;
			}
//			}
			//�Y��rider2�B���ʮ�, �h�אּrider2�¦V
			if(this.riddenByEntity2 != null && this.keyPressed != 0) {
				this.rotationYawHead = riddenByEntity2.rotationYawHead;
				this.prevRotationYaw = riddenByEntity2.prevRotationYaw;
				this.rotationYaw = riddenByEntity2.rotationYaw;
				this.renderYawOffset = riddenByEntity2.renderYawOffset;
				
				((EntityLivingBase)this.riddenByEntity).rotationYawHead = this.rotationYawHead;
				((EntityLivingBase)this.riddenByEntity).prevRotationYaw = this.prevRotationYaw;
				((EntityLivingBase)this.riddenByEntity).rotationYaw = this.rotationYaw;
				((EntityLivingBase)this.riddenByEntity).renderYawOffset = this.renderYawOffset;

				//�M��AI�۰ʨ���, �H�K��ê���a�����
				this.getShipNavigate().clearPathEntity();
			}
		}

		//client side
		if(this.worldObj.isRemote) {
			if(ShipDepth > 0D) {
				//�������ʮ�, ���ͤ���S��
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
		//server side
		else {
			//owner����(�q�`�Oserver restart)
			if(this.host == null) {
				this.setDead();
			}
			else {		
				//owner�٦b, ���O�w�g�M���O�H, �h������entity
				if(this.host.ridingEntity != this) {
					LogHelper.info("DEBUG : ride change "+this.riddenByEntity); 
					this.setDead();
				}
				
				//LogHelper.info("DEBUG : get rider "+riddenByEntity+" "+this.riddenByEntity2);		
				if(this.ticksExisted % 100 == 0) {
					//get new status every 5 second
					this.atkRange = host.getStateFinal(ID.HIT);
					
					//speed reduce to zero if host sitting
					if(this.host.isSitting()) {
						this.movSpeed = 0F;
					}
					else {
						this.movSpeed = host.getStateFinal(ID.MOV);
					}

					//update attribute
					setupAttrs();
					
					//����Ħ�
					if(this.isInWater()) {
						this.setAir(300);
					}
				}//end every 100 ticks
				
				//get target every 8 ticks
				if(this.ticksExisted % 8 == 0) {
					this.setEntityTarget(this.host.getEntityTarget());
					
					//clear dead or same team target 
	      			if(this.getEntityTarget() != null) {
	      				if(!this.getEntityTarget().isEntityAlive() || EntityHelper.checkSameOwner(this, getEntityTarget())) {
	      					this.setEntityTarget(null);
	      				}
	      			}
				}//end every 10 ticks
				
				//sync every 60 ticks
				if(this.ticksExisted % 64 == 0) {
					this.sendSyncPacket();
				}//end every 60 ticks
			}
		}
	}
	
	//set movement by key pressed, pitch/yaw is RAD not DEGREE
	private void applyMovement(float pitch, float yaw) {
		//calc move direction by yaw
		float[] movez = ParticleHelper.rotateXZByAxis(movSpeed, 0F, yaw, 1F);	//�e��
		float[] movex = ParticleHelper.rotateXZByAxis(0F, movSpeed, yaw, 1F);	//���k
		
		if(this.onGround || EntityHelper.checkEntityIsInLiquid(this)) {
			//horizontal move, �ܤ֭n4 tick�~��[��̰��t
			//W (bit 1)
			if((keyPressed & 1) > 0) {
				motionX += movez[1] / 4F;
				if(MathHelper.abs((float) motionX) > MathHelper.abs(movez[1])) motionX = movez[1];
				motionZ += movez[0] / 4F;
				if(MathHelper.abs((float) motionZ) > MathHelper.abs(movez[0])) motionZ = movez[0];
			}
			
			//S (bit 2)
			if((keyPressed & 2) > 0) {
				motionX -= movez[1] / 4F;
				if(MathHelper.abs((float) motionX) > MathHelper.abs(movez[1])) motionX = -movez[1];
				motionZ -= movez[0] / 4F;
				if(MathHelper.abs((float) motionZ) > MathHelper.abs(movez[0])) motionZ = -movez[0];
			}
			
			//A (bit 3)
			if((keyPressed & 4) > 0) {
				motionX += movex[1] / 4F;
				if(MathHelper.abs((float) motionX) > MathHelper.abs(movex[1])) motionX = movex[1];
				motionZ += movex[0] / 4F;
				if(MathHelper.abs((float) motionZ) > MathHelper.abs(movex[0])) motionZ = movex[0];
			}
			
			//D (bit 4)
			if((keyPressed & 8) > 0) {
				motionX -= movex[1] / 4F;
				if(MathHelper.abs((float) motionX) > MathHelper.abs(movex[1])) motionX = -movex[1];
				motionZ -= movex[0] / 4F;
				if(MathHelper.abs((float) motionZ) > MathHelper.abs(movex[0])) motionZ = -movex[0];
			}
			
			//vertical move
			if(pitch > 0.5F) {	//move down
				motionY += -0.1F;
				if(motionY < -movSpeed / 2F) motionY = -movSpeed / 2F;			
			}
			
			if(pitch < -1F) {	//move up
				motionY += 0.1F;
				if(motionY > movSpeed / 2F) motionY = movSpeed / 2F;
			}
			
			//�Y��������F��, �h���ո���
			if(this.isCollidedHorizontally) {
				this.motionY += 0.4D;
			}
//			LogHelper.info("DEBUG : key press "+keyPressed);
			//jump (bit 5)
			if((keyPressed & 16) > 0) {
				this.motionY += this.movSpeed * 2F;
				if(motionY > 1F) motionY = 1F;
				//reset jump flag
				keyPressed -= 16;
			}	
		}
		else {
			//���b�a����, �U��V���Ť��[�t�פ��P, ���e����, ������, ���k�j�T���
			//W (bit 1)
			if((keyPressed & 1) > 0) {
				motionX += movez[1] / 4F;
				if(MathHelper.abs((float) motionX) > MathHelper.abs(movez[1])) motionX = movez[1];
				motionZ += movez[0] / 4F;
				if(MathHelper.abs((float) motionZ) > MathHelper.abs(movez[0])) motionZ = movez[0];
			}
			
			//S (bit 2)
			if((keyPressed & 2) > 0) {
				motionX -= movez[1] / 16F;
				if(MathHelper.abs((float) motionX) > MathHelper.abs(movez[1])) motionX = -movez[1];
				motionZ -= movez[0] / 16F;
				if(MathHelper.abs((float) motionZ) > MathHelper.abs(movez[0])) motionZ = -movez[0];
			}
			
			//A (bit 3)
			if((keyPressed & 4) > 0) {
				motionX += movex[1] / 32F;
				if(MathHelper.abs((float) motionX) > MathHelper.abs(movex[1])) motionX = movex[1];
				motionZ += movex[0] / 32F;
				if(MathHelper.abs((float) motionZ) > MathHelper.abs(movex[0])) motionZ = movex[0];
			}
			
			//D (bit 4)
			if((keyPressed & 8) > 0) {
				motionX -= movex[1] / 32F;
				if(MathHelper.abs((float) motionX) > MathHelper.abs(movex[1])) motionX = -movex[1];
				motionZ -= movex[0] / 32F;
				if(MathHelper.abs((float) motionZ) > MathHelper.abs(movex[0])) motionZ = -movex[0];
			}
		}
	}

	@Override
	public byte getStateEmotion(int id) {
		return id == ID.S.Emotion ? StateEmotion : StateEmotion2;
	}
	
	@Override
	public void setStateEmotion(int id, int value, boolean sync) {	
		switch(id) {
		case ID.S.Emotion:
			StateEmotion = (byte) value;
			break;
		case ID.S.Emotion2:
			StateEmotion2 = (byte) value;
			break;
		default:
			break;
		}
		
		if(sync && !worldObj.isRemote) {
			TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 32D);
			CommonProxy.channelE.sendToAllAround(new S2CEntitySync(this, 4), point);
		}
	}
	
	@Override
	public void setShipDepth(double par1) {
		this.ShipDepth = par1;
	}

	@Override
	public boolean getStateFlag(int flag) {	
		if(host != null) return this.host.getStateFlag(flag);
		return false;
	}

	@Override
	public void setStateFlag(int id, boolean flag) {
		if(host != null) this.host.setStateFlag(id, flag);
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
	public int getAttackTime() {
		return this.attackTime;
	}
	
    @Override
	public boolean isAIEnabled() {
		return true;
	}
  	
    //clear AI
  	protected void clearAITasks() {
  	   tasks.taskEntries.clear();
  	}
  	
  	//clear target AI
  	protected void clearAITargetTasks() {
  	   targetTasks.taskEntries.clear();
  	}
    
  	@Override
	public Entity getEntityTarget() {
		return this.atkTarget;
	}
  	
  	@Override
	public void setEntityTarget(Entity target) {
		this.atkTarget = target;
	}
  	
  	//change melee damage to 100%
  	@Override
  	public boolean attackEntityAsMob(Entity target) {	
  		//get attack value
  		float atk = host.getStateFinal(ID.ATK);
  		//set knockback value (testing)
  		float kbValue = 0.15F;
  				
  	    //�Natk��attacker�ǵ��ؼЪ�attackEntityFrom��k, �b�ؼ�class���p��ˮ`
  	    //�åB�^�ǬO�_���\�ˮ`��ؼ�
  	    boolean isTargetHurt = target.attackEntityFrom(DamageSource.causeMobDamage(this), atk);

  	    //play entity attack sound
  	    if(this.getRNG().nextInt(10) > 8) {
  	    	this.playSound(Reference.MOD_ID+":ship-waka_attack", ConfigHandler.shipVolume * 0.5F, 1F);
  	    }
  	    
  	    //if attack success
  	    if(isTargetHurt) {
  	    	//calc kb effect
  	        if(kbValue > 0) {
  	            target.addVelocity(-MathHelper.sin(rotationYaw * (float)Math.PI / 180.0F) * kbValue, 
  	                   0.1D, MathHelper.cos(rotationYaw * (float)Math.PI / 180.0F) * kbValue);
  	            motionX *= 0.6D;
  	            motionZ *= 0.6D;
  	        }

  	        //send packet to client for display partical effect   
  	        if (!worldObj.isRemote) {
  	        	TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64D);
  	    		CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(target, 1, false), point);
  			}
  	    }

  	    return isTargetHurt;
  	}
    
  	//light attack
    @Override
	public boolean attackEntityWithAmmo(Entity target) {
		float atkLight = this.host.getStateFinal(ID.ATK);
		float kbValue = 0.03F;
		
		//calc equip special dmg: AA, ASM
		atkLight = CalcHelper.calcDamageByEquipEffect(this, target, atkLight, 0);

		//play cannon fire sound at attacker
        playSound(Reference.MOD_ID+":ship-firesmall", ConfigHandler.fireVolume, 0.7F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
        //play entity attack sound
        if(this.rand.nextInt(10) > 8) {
        	this.playSound(Reference.MOD_ID+":ship-waka_attack", ConfigHandler.shipVolume * 0.5F, 1F);
        }
        
        //����k��getLook�٥��T (client sync���D)
        float distX = (float) (target.posX - this.posX);
        float distY = (float) (target.posY - this.posY);
        float distZ = (float) (target.posZ - this.posZ);
        float distSqrt = MathHelper.sqrt_float(distX*distX + distY*distY + distZ*distZ);
        distX = distX / distSqrt;
        distY = distY / distSqrt;
        distZ = distZ / distSqrt;
        
        //experience++
  		host.addShipExp(2);
  		
  		//grudge--
  		host.decrGrudgeNum(1);
  		
  		//light ammo -1
        if(!host.decrAmmoNum(0)) {			//not enough ammo
        	atkLight = atkLight * 0.125F;	//reduce damage to 12.5%
        }
		
		//�o�g�̷����S��
        TargetPoint point0 = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64D);
        CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 19, this.posX, this.posY+1.5D, this.posZ, distX, 1.2F, distZ, true), point0);

        //�o�e����flag��host, �]�w��attack time
		CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this.host, 0, true), point0);
		
		//calc miss chance, if not miss, calc cri/multi hit
		TargetPoint point = new TargetPoint(this.dimension, this.host.posX, this.host.posY, this.host.posZ, 64D);
        float missChance = 0.2F + 0.15F * (distSqrt / host.getStateFinal(ID.HIT)) - 0.001F * host.getLevel();
        missChance -= this.host.getEffectEquip(ID.EF_MISS);		//equip miss reduce
        if(missChance > 0.35F) missChance = 0.35F;	//max miss chance
  		
        //calc miss chance
        if(this.rand.nextFloat() < missChance) {
        	atkLight = 0;	//still attack, but no damage
        	//spawn miss particle
        	
        	CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this.host, 10, false), point);
        }
        else {
        	//roll cri -> roll double hit -> roll triple hit (triple hit more rare)
        	//calc critical
        	if(this.rand.nextFloat() < this.host.getEffectEquip(ID.EF_CRI)) {
        		atkLight *= 1.5F;
        		//spawn critical particle
            	CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this.host, 11, false), point);
        	}
        	else {
        		//calc double hit
            	if(this.rand.nextFloat() < this.host.getEffectEquip(ID.EF_DHIT)) {
            		atkLight *= 2F;
            		//spawn double hit particle
            		CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this.host, 12, false), point);
            	}
            	else {
            		//calc double hit
                	if(this.rand.nextFloat() < this.host.getEffectEquip(ID.EF_THIT)) {
                		atkLight *= 3F;
                		//spawn triple hit particle
                		CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this.host, 13, false), point);
                	}
            	}
        	}
        }
        
        //vs player = 25% dmg
  		if(target instanceof EntityPlayer) {
  			atkLight *= 0.25F;
  			
  			//check friendly fire
    		if(!ConfigHandler.friendlyFire) {
    			atkLight = 0F;
    		}
    		else if(atkLight > 59F) {
    			atkLight = 59F;	//same with TNT
    		}
  		}

	    //�Natk��attacker�ǵ��ؼЪ�attackEntityFrom��k, �b�ؼ�class���p��ˮ`
	    //�åB�^�ǬO�_���\�ˮ`��ؼ�
	    boolean isTargetHurt = false;
	    
	    if(this.host != null) {
	    	isTargetHurt = target.attackEntityFrom(DamageSource.causeMobDamage(this.host).setProjectile(), atkLight);
	    }
	    		
	    //if attack success
	    if(isTargetHurt) {
	    	//calc kb effect
	        if(kbValue > 0) {
	            target.addVelocity(-MathHelper.sin(rotationYaw * (float)Math.PI / 180.0F) * kbValue, 
	                   0.1D, MathHelper.cos(rotationYaw * (float)Math.PI / 180.0F) * kbValue);
	        }
	        
	        //display hit particle on target
	        TargetPoint point1 = new TargetPoint(this.dimension, target.posX, target.posY, target.posZ, 64D);
			CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(target, 9, false), point1);
        }

	    return isTargetHurt;
	}

	@Override
	public boolean attackEntityWithHeavyAmmo(Entity target) {
		//get attack value
		float atkHeavy = this.host.getStateFinal(ID.ATK_H);
		float kbValue = 0.08F;
		
		//play cannon fire sound at attacker
        this.playSound(Reference.MOD_ID+":ship-fireheavy", ConfigHandler.fireVolume, 0.7F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
        //play entity attack sound
        if(this.rand.nextInt(10) > 8) {
        	this.playSound(Reference.MOD_ID+":ship-waka_attack", ConfigHandler.shipVolume * 0.5F, 1F);
        }
        
        //���u�O�_�ĥΪ��g
  		boolean isDirect = false;
  		//�p��ؼжZ��
  		float tarX = (float)target.posX;	//for miss chance calc
  		float tarY = (float)target.posY;
  		float tarZ = (float)target.posZ;
  		float distX = tarX - (float)this.posX;
  		float distY = tarY - (float)this.posY;
  		float distZ = tarZ - (float)this.posZ;
        float distSqrt = MathHelper.sqrt_float(distX*distX + distY*distY + distZ*distZ);
        float launchPos = (float)posY + height * 0.7F;
          
        //�W�L�@�w�Z��/���� , �h�ĥΩߪ��u,  �b�����ɵo�g���׸��C
        if((distX*distX+distY*distY+distZ*distZ) < 36F) {
        	isDirect = true;
        }
        
        if(this.isInWater()) {
          	isDirect = true;
          	launchPos = (float)posY;
        }
        
        //experience++
      	host.addShipExp(16);
      		
      	//grudge--
      	host.decrGrudgeNum(1);
      	
      	//heavy ammo -1
        if(!host.decrAmmoNum(1)) {	//not enough ammo
        	atkHeavy = atkHeavy * 0.125F;	//reduce damage to 12.5%
        }
        
        //calc miss chance, miss: add random offset(0~6) to missile target 
        float missChance = 0.2F + 0.15F * (distSqrt / host.getEffectEquip(ID.EF_DHIT)) - 0.001F * host.getLevel();
        missChance -= this.host.getEffectEquip(ID.EF_MISS);	//equip miss reduce
        if(missChance > 0.35F) missChance = 0.35F;	//max miss chance = 30%
		
        //calc miss chance
        if(this.rand.nextFloat() < missChance) {
        	tarX = tarX - 5F + this.rand.nextFloat() * 10F;
        	tarY = tarY + this.rand.nextFloat() * 5F;
      	  	tarZ = tarZ - 5F + this.rand.nextFloat() * 10F;
        	//spawn miss particle
        	TargetPoint point = new TargetPoint(this.dimension, this.host.posX, this.host.posY, this.host.posZ, 64D);
        	CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this.host, 10, false), point);
        }

        //spawn missile
        EntityAbyssMissile missile = new EntityAbyssMissile(this.worldObj, this.host, 
        		tarX, tarY+target.height*0.2F, tarZ, launchPos, atkHeavy, kbValue, isDirect, -1F);
        this.worldObj.spawnEntityInWorld(missile);
  		
        return true;
	}
	
	/**�קﲾ�ʤ�k, �Ϩ�water��lava�����ʮɹ��Oflying entity
     * Moves the entity based on the specified heading.  Args: strafe, forward
     */
	@Override
    public void moveEntityWithHeading(float movX, float movZ) {
        double d0;

//        if(this.isInWater() || this.handleLavaMovement()) { //�P�w���G�餤��, ���|�۰ʤU�I
        if(EntityHelper.checkEntityIsInLiquid(this)) { //�P�w���G�餤��, ���|�۰ʤU�I
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
                if(this.motionX < (-f5)) {
                    this.motionX = (-f5);
                }
                if(this.motionX > f5) {
                    this.motionX = f5;
                }
                if(this.motionZ < (-f5)) {
                    this.motionZ = (-f5);
                }
                if(this.motionZ > f5) {
                    this.motionZ = f5;
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
            this.motionX *= f2;
            this.motionZ *= f2;
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
	public int getLevel() {
		if(host != null) return this.host.getLevel();
		return 150;
	}

	@Override
	public float getAttackSpeed() {
		if(host != null) return this.host.getStateFinal(ID.SPD);
		return 0F;
	}

	@Override
	public float getAttackRange() {
		if(host != null) return this.host.getStateFinal(ID.HIT);
		return 0F;
	}
	
	@Override
	public float getMoveSpeed() {
		if(host != null) return this.host.getStateFinal(ID.MOV);
		return 0F;
	}
	
	@Override
	public boolean getIsLeashed() {		//�j��host�Ϊ̦ۤv����j��
		if(host != null) {
			return this.host.getIsLeashed() || this.getLeashed();
		}
		return false;
	}
	
	@Override
	public int getStateMinor(int id) {
		if(host != null) return this.host.getStateMinor(id);
		return 0;
	}

	@Override
	public void setStateMinor(int state, int par1) {
		if(host != null) this.host.setStateMinor(state, par1);
	}
	
	@Override
	public int getAmmoLight() {
		if(host != null) return this.host.getStateMinor(ID.M.NumAmmoLight);
		return 0;
	}

	@Override
	public int getAmmoHeavy() {
		if(host != null) return this.host.getStateMinor(ID.M.NumAmmoHeavy);
		return 0;
	}
	
	@Override
	public boolean useAmmoLight() {
		if(host != null) return this.host.useAmmoLight();
		return true;
	}

	@Override
	public boolean useAmmoHeavy() {
		if(host != null) return this.host.useAmmoHeavy();
		return true;
	}

	@Override
	public boolean hasAmmoLight() {
		return this.getAmmoLight() > 0;
	}

	@Override
	public boolean hasAmmoHeavy() {
		return this.getAmmoHeavy() > 0;
	}

	@Override
	public void setAmmoLight(int num) {}	//no use

	@Override
	public void setAmmoHeavy(int num) {}	//no use

	@Override
	public float getAttackDamage() {		//no use
		return 0F;
	}
	
	@Override
	public boolean getAttackType(int par1) {
		if(host != null) return host.getAttackType(par1);
		return true;
	}
	
	@Override
	public float getEffectEquip(int id) {
		if(host != null) return host.getEffectEquip(id);
		return 0F;
	}
	
	@Override
	public float getDefValue() {
		if(host != null) return host.getStateFinal(ID.DEF) * 0.5F;
		return 0F;
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
		if(host != null) return this.host.getIsSitting();
		return false;
	}

	@Override
	public boolean getIsSneaking() {
		return false;
	}

	@Override
	public double getShipDepth() {
		return ShipDepth;
	}

	@Override
	public EntityLivingBase getRiddenByEntity() {
		return (EntityLivingBase) this.riddenByEntity;
	}
	
	@Override
	public boolean shouldDismountInWater(Entity rider) {
        return false;
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
		
        EntityHelper.updateShipNavigator(this);
    }

	@Override
	public boolean canFly() {
		return false;
	}
	
	@Override
	public boolean canBreatheUnderwater() {
		return true;
	}
	
	//get seat position: z, x, height
	public float[] getRidePos() {
		return this.ridePos;
	}
	
	//for clear rider process
	public void setRiderNull() {
		//�M���y��2 entity
		if(this.seat2 != null) {
			seat2.setRiderNull();
		}
		//�M���y�M�D�H
		if(this.riddenByEntity != null) {
			riddenByEntity.ridingEntity = null;
			riddenByEntity = null;
		}
		//�M���y��2�W�������H
		this.riddenByEntity2 = null;
		this.setDead();
	}
	
	//update attribute
	public void setupAttrs() {
		getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(host.getStateFinal(ID.HP) * 0.5D);
		getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(movSpeed);
		getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(host.getStateFinal(ID.HIT) + 32); //������ؼ�, ���|���d��
		getEntityAttribute(SharedMonsterAttributes.knockbackResistance).setBaseValue(host.getLevel() / 150D);
	}
	
	@Override
	public void setEntitySit() {
		if(host != null) host.setEntitySit();
	}
	
	//sync host, rider, rider2, seat...
	public void sendSyncPacket() {
		if(!worldObj.isRemote) {
			//for other player, send ship state for display
			TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 48D);
			CommonProxy.channelE.sendToAllAround(new S2CEntitySync(this, 7), point);
		}
	}
	
	@Override
	public Entity getGuardedEntity() {
		if(host != null) return this.host.getGuardedEntity();
		return null;
	}

	@Override
	public void setGuardedEntity(Entity entity) {
		if(host != null) this.host.setGuardedEntity(entity);
	}

	@Override
	public int getGuardedPos(int vec) {
		if(host != null) return this.host.getGuardedPos(vec);
		return -1;
	}

	@Override
	public void setGuardedPos(int x, int y, int z, int dim, int type) {
		if(host != null) this.host.setGuardedPos(x, y, z, dim, type);
	}
	
    @Override
    public float getModelRotate(int par1) {
    	return 0F;
    }
    
    //set model rotate angle, par1 = 0:X, 1:Y, 2:Z
    @Override
	public void setModelRotate(int par1, float par2) {}
    
    @Override
	public int getPlayerUID() {
		if(host != null) return this.host.getPlayerUID();
		return -1;
	}

	@Override
	public void setPlayerUID(int uid) {}
	
	@Override
	public Entity getHostEntity() {
		return this.host;
	}
    
	
}

