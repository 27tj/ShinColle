package com.lulan.shincolle.entity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import com.lulan.shincolle.ShinColle;
import com.lulan.shincolle.ai.EntityAIShipSit;
import com.lulan.shincolle.client.inventory.ContainerShipInventory;
import com.lulan.shincolle.client.particle.EntityFXMiss;
import com.lulan.shincolle.client.particle.EntityFXSpray;
import com.lulan.shincolle.crafting.EquipCalc;
import com.lulan.shincolle.entity.EntityAbyssMissile;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.init.ModBlocks;
import com.lulan.shincolle.init.ModItems;
import com.lulan.shincolle.network.S2CEntitySync;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.reference.Values;
import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.utility.EntityHelper;
import com.lulan.shincolle.utility.LogHelper;

import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import cpw.mods.fml.common.network.internal.FMLNetworkHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAISit;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityLargeFireball;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.pathfinding.PathEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;

/**SHIP DATA <br>
 * Explanation in crafting/ShipCalc.class
 */
public abstract class BasicEntityShip extends EntityTameable {

	protected ExtendShipProps ExtProps;	//entity�B�~NBT����
	
	//for attribute calc
	protected byte ShipType;			//ship type
	protected byte ShipID;
	//for AI calc
	protected int StartEmotion;			//���}�l�ɶ�
	protected double ShipDepth;			//���`, �Ω�������קP�w
	protected double ShipPrevX;			//ship posX 5 sec ago
	protected double ShipPrevY;			//ship posY 5 sec ago
	protected double ShipPrevZ;			//ship posZ 5 sec ago
	/**equip states: 0:HP 1:ATK 2:DEF 3:SPD 4:MOV 5:HIT 6:ATK_Heavy 7:ATK_AirLight 8:ATK_AirHeavy*/
	protected float[] StateEquip;
	/**final states: 0:HP 1:ATK 2:DEF 3:SPD 4:MOV 5:HIT 6:ATK_Heavy 7:ATK_AirLight 8:ATK_AirHeavy*/
	protected float[] StateFinal;
	/**minor states: 0:ShipLevel 1:Kills 2:ExpCurrent 3:ExpNext 4:NumAmmoLight 5:NumAmmoHeavy 6:NumGrudge 7:NumAirLight 8:NumAirHeavy*/
	protected int[] StateMinor;
	/**EntityState: 0:HP State 1:Emotion 2:SwimType*/
	protected byte[] StateEmotion;
	/**EntityFlag: 0:canFloatUp 1:isMarried 2:noFuel 3:canMelee 4:canAmmoLight 5:canAmmoHeavy 6:canAirLight 7:canAirHeavy*/
	protected boolean[] StateFlag;
	/**BonusPoint: 0:HP 1:ATK 2:DEF 3:SPD 4:MOV 5:HIT*/
	protected byte[] BonusPoint;
	/**TypeModify: 0:HP 1:ATK 2:DEF 3:SPD 4:MOV 5:HIT*/
	protected float[] TypeModify;
	
	
	public BasicEntityShip(World world) {
		super(world);
		//init value
		isImmuneToFire = true;	//set ship immune to lava
		StateEquip = new float[] {0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F};
		StateFinal = new float[] {0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F};
		StateMinor = new int[] {1, 0, 0, 40, 0, 0, 0, 0, 0};
		StateEmotion = new byte[] {0, 0, 0};
		StateFlag = new boolean[] {false, false, false, true, true, true, true, true};
		BonusPoint = new byte[] {0, 0, 0, 0, 0, 0};
		TypeModify = new float[] {1F, 1F, 1F, 1F, 1F, 1F};
		//for AI
		StartEmotion = -1;		//emotion start time
		ShipDepth = 0D;			//water block above ship (within ship position)
		ShipPrevX = posX;		//ship position 5 sec ago
		ShipPrevY = posY;
		ShipPrevZ = posZ;

	}
	
	@Override
	public boolean isAIEnabled() {
		return true;
	}
	
	@Override
	public float getEyeHeight() {
		return this.height * 1F;
	}
	
	//get owner name (for player owner only)
	public String getOwnerName() {
		if(this.getOwner() != null) {
			if(this.getOwner() instanceof EntityPlayer) {
				return ((EntityPlayer)this.getOwner()).getDisplayName();
			}
		}		
		return null;
	}

	@Override
	public EntityAgeable createChild(EntityAgeable p_90011_1_) {
		return null;
	}
	
	//setup AI
	protected void setAIList() {
		this.getNavigator().setEnterDoors(true);
		this.getNavigator().setAvoidsWater(false);
		this.getNavigator().setCanSwim(true);
	}
	
	//setup target AI
	abstract protected void setAITargetList();

	//clear AI
	protected void clearAITasks() {
//		LogHelper.info("DEBUG : ai clear1 "+tasks.taskEntries.size()+" "+this.getCustomNameTag());
		tasks.taskEntries.clear();
//		LogHelper.info("DEBUG : ai clear2 "+tasks.taskEntries.size()+" "+this.getCustomNameTag());
	}
	
	//clear target AI
	protected void clearAITargetTasks() {
//		LogHelper.info("DEBUG : target ai clear1 "+targetTasks.taskEntries.size()+" "+this.getCustomNameTag());
		this.setAttackTarget(null);
		targetTasks.taskEntries.clear();
//		LogHelper.info("DEBUG : target ai clear2 "+targetTasks.taskEntries.size()+" "+this.getCustomNameTag());
	}
	
	//getter
	public ExtendShipProps getExtProps() {
		return ExtProps;
	}
	public int getShipLevel() {
		return StateMinor[ID.ShipLevel];
	}
	public byte getShipType() {
		return ShipType;
	}
	public byte getShipID() {
		return ShipID;
	}
	public int getStartEmotion() {
		return StartEmotion;
	}
	public boolean hasAmmoLight() {
		return StateMinor[ID.NumAmmoLight] > 0;
	}
	public boolean hasAmmoHeavy() {
		return StateMinor[ID.NumAmmoHeavy] > 0;
	}
	public double getShipDepth() {
		return ShipDepth;
	}
	public boolean getStateFlag(int flag) {	//get flag (boolean)
		return StateFlag[flag];		
	}
	public byte getStateFlagI(int flag) {		//get flag (byte)
		if(StateFlag[flag]) {
			return 1;
		}
		else {
			return 0;
		}
	}
	public float getStateEquip(int id) {
		return StateEquip[id];
	}
	public float getStateFinal(int id) {
		return StateFinal[id];
	}
	public int getStateMinor(int id) {
		return StateMinor[id];
	}
	public byte getStateEmotion(int id) {
		return StateEmotion[id];
	}
	public byte getBonusPoint(int id) {
		return BonusPoint[id];
	}
	public float getTypeModify(int id) {
		return TypeModify[id];
	}
	
	//replace isInWater, check water block with NO extend AABB
	private void checkDepth() {
		Block BlockCheck = checkBlockWithOffset(0);
		
		if(BlockCheck == Blocks.water || BlockCheck == Blocks.lava) {
			ShipDepth = 1;
			for(int i=1; (this.posY+i)<255D; i++) {
				BlockCheck = checkBlockWithOffset(i);
				if(BlockCheck == Blocks.water || BlockCheck == Blocks.lava) {
					ShipDepth++;
				}
				else {
					if(BlockCheck == Blocks.air) {
						setStateFlag(ID.F_CanFloatUp, true);
					}
					else {
						setStateFlag(ID.F_CanFloatUp, false);
					}
					break;
				}
			}		
			ShipDepth = ShipDepth - (this.posY - (int)this.posY);
		}
		else {
			ShipDepth = 0;
		}
	}
	
	//check block from entity posY + offset
	public Block checkBlockWithOffset(int par1) {
		int blockX = MathHelper.floor_double(this.posX);
	    int blockY = MathHelper.floor_double(this.boundingBox.minY);
	    int blockZ = MathHelper.floor_double(this.posZ);

	    return this.worldObj.getBlock(blockX, blockY + par1, blockZ);    
	}
	
	//called when entity equip changed
	//this method get equip state from slots, no input parm
	public void calcEquipAndUpdateState() {
		ItemStack itemstack = null;
		float[] equipStat = {0F,0F,0F,0F,0F,0F};
		StateEquip[ID.HP] = 0F;
		StateEquip[ID.DEF] = 0F;
		StateEquip[ID.SPD] = 0F;
		StateEquip[ID.MOV] = 0F;
		StateEquip[ID.HIT] = 0F;
		StateEquip[ID.ATK] = 0F;
		StateEquip[ID.ATK_H] = 0F;
		StateEquip[ID.ATK_AL] = 0F;
		StateEquip[ID.ATK_AH] = 0F;
		
		//calc equip slots
		for(int i=0; i<ContainerShipInventory.SLOTS_EQUIP; i++) {
			itemstack = this.ExtProps.slots[i];
			if(itemstack != null) {
				equipStat = EquipCalc.getEquipStat(this, itemstack);
				StateEquip[ID.HP] += equipStat[ID.HP];
				StateEquip[ID.DEF] += equipStat[ID.DEF];
				StateEquip[ID.SPD] += equipStat[ID.SPD];
				StateEquip[ID.MOV] += equipStat[ID.MOV];
				StateEquip[ID.HIT] += equipStat[ID.HIT];
				StateEquip[ID.ATK] += equipStat[ID.ATK];
				StateEquip[ID.ATK_H] += equipStat[ID.ATK_H];
				StateEquip[ID.ATK_AL] += equipStat[ID.ATK_AL];
				StateEquip[ID.ATK_AH] += equipStat[ID.ATK_AH];
			}
		}
		//update value
		calcShipAttributes(this.ShipID);
	}
	
	//setter	
	//setting attributes, called at load nbt data & init mob
	public void calcShipAttributes(byte id) {
		//init or renew bonus value, for short value: discard decimal
		//HP = (base + equip + (point + 1) * level * typeModify) * config scale
		StateFinal[ID.HP] = (Values.BaseHP[id] + StateEquip[ID.HP] + (float)(BonusPoint[ID.HP]+1) * (float)StateMinor[ID.ShipLevel] * TypeModify[ID.HP]) * ConfigHandler.hpRatio; 
		//DEF = base + ((point + 1) * level / 3 * 0.4 + equip) * typeModify
		StateFinal[ID.DEF] = (Values.BaseDEF[id] + StateEquip[ID.DEF] + ((float)(BonusPoint[ID.DEF]+1) * ((float)StateMinor[ID.ShipLevel])/3F) * 0.4F * TypeModify[ID.DEF]) * ConfigHandler.defRatio;
		//SPD = base + ((point + 1) * level / 10 * 0.02 + equip) * typeModify
		StateFinal[ID.SPD] = (Values.BaseSPD[id] + StateEquip[ID.SPD] + ((float)(BonusPoint[ID.SPD]+1) * ((float)StateMinor[ID.ShipLevel])/10F) * 0.02F * TypeModify[ID.SPD]) * ConfigHandler.spdRatio;
		//MOV = base + ((point + 1) * level / 10 * 0.01 + equip) * typeModify
		StateFinal[ID.MOV] = (Values.BaseMOV[id] + StateEquip[ID.MOV] + ((float)(BonusPoint[ID.MOV]+1) * ((float)StateMinor[ID.ShipLevel])/10F) * 0.01F * TypeModify[ID.MOV]) * ConfigHandler.movRatio;
		//HIT = base + ((point + 1) * level / 10 * 0.8 + equip) * typeModify
		StateFinal[ID.HIT] = (Values.BaseHIT[id] + StateEquip[ID.HIT] + ((float)(BonusPoint[ID.HIT]+1) * ((float)StateMinor[ID.ShipLevel])/10F) * 0.8F * TypeModify[ID.HIT]) * ConfigHandler.hitRatio;
		//ATK = (base + equip + ((point + 1) * level / 3) * typeModify) * config scale
		float atk = Values.BaseATK[id] + ((float)(BonusPoint[ID.ATK]+1) * ((float)StateMinor[ID.ShipLevel])/3F) * 0.5F * TypeModify[ID.ATK];
		StateFinal[ID.ATK] = (atk + StateEquip[ID.ATK]) * ConfigHandler.atkRatio;
		StateFinal[ID.ATK_H] = (atk * 4F + StateEquip[ID.ATK_H]) * ConfigHandler.atkRatio;
		StateFinal[ID.ATK_AL] = (atk + StateEquip[ID.ATK_AL]) * ConfigHandler.atkRatio;
		StateFinal[ID.ATK_AH] = (atk * 4F + StateEquip[ID.ATK_AH]) * ConfigHandler.atkRatio;
		//KB Resistance = Level / 10 * 0.04
		float resisKB = (((float)StateMinor[ID.ShipLevel])/10F) * 0.067F;

		if(StateFinal[ID.DEF] > 95F) {
			StateFinal[ID.DEF] = 95F;	//max def = 95%
		}
		if(StateFinal[ID.SPD] > 4F) {
			StateFinal[ID.SPD] = 4F;	//min attack delay = 0.5 sec
		}
		if(StateFinal[ID.SPD] < 0F) {
			StateFinal[ID.SPD] = 0F;
		}
		if(StateFinal[ID.MOV] > 0.8F) {
			StateFinal[ID.MOV] = 0.8F;	//high move speed is buggy
		}
		if(StateFinal[ID.MOV] < 0F) {
			StateFinal[ID.MOV] = 0F;
		}
//		if(StateFinal[ID.HIT] > 80F) {
//			StateFinal[ID.HIT] = 80F;
//		}
		
		this.jumpMovementFactor = (1F + StateFinal[ID.MOV]) * 0.03F;
		
		//set attribute by final value
		/**
		 * DO NOT SET ATTACK DAMAGE to non-EntityMob!!!!!
		 */
		getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(StateFinal[ID.HP]);
		getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(StateFinal[ID.MOV]);
		getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(StateFinal[ID.HIT]+16); //������ؼ�, ���|���d��
		getEntityAttribute(SharedMonsterAttributes.knockbackResistance).setBaseValue(resisKB);
		//for new ship
		if(this.getHealth() == 20F) this.setHealth(this.getMaxHealth());
		
		//for server side
		if(!worldObj.isRemote) {
			sendSyncPacket();		//sync nbt data
		}
	}
	
	//set next exp value, no sync or update (for client load nbt data, gui display)
	public void setExpNext() {
		StateMinor[ID.ExpNext] = StateMinor[ID.ShipLevel] * 20 + 20;
	}
		
	//called when entity exp++
	public void addShipExp(int exp) {
		int CapLevel = getStateFlag(ID.F_IsMarried) ? 150 : 100;
		
		if(StateMinor[ID.ShipLevel] != CapLevel && StateMinor[ID.ShipLevel] < 150) {	//level is not cap level
			StateMinor[ID.ExpCurrent] += exp;
			if(StateMinor[ID.ExpCurrent] >= StateMinor[ID.ExpNext]) {
				//level up sound
				this.worldObj.playSoundAtEntity(this, "random.levelup", 0.75F, 1.0F);
				StateMinor[ID.ExpCurrent] -= StateMinor[ID.ExpNext];	//level up
				StateMinor[ID.ExpNext] = (StateMinor[ID.ShipLevel] + 1) * 20 + 20;
				setShipLevel(++StateMinor[ID.ShipLevel], true);
			}
		}	
	}
	
	//called when entity level up
	public void setShipLevel(int par1, boolean update) {
		//set level
		if(par1 < 151) {
			StateMinor[ID.ShipLevel] = par1;
		}
		//update attributes
		if(update) { 
			calcShipAttributes(ShipID); 
			this.setHealth(this.getMaxHealth());
		}
	}
	
	//called when a mob die near the entity (used in event handler)
	public void addKills() {
		StateMinor[ID.Kills]++;
	}
	//ship attribute setter, sync packet in method: calcShipAttributes 
	public void setStateFinal(int state, float par1) {
		StateFinal[state] = par1;
	}
	
	public void setStateMinor(int state, int par1) {
		StateMinor[state] = par1;
	}
	
	public void setBonusPoint(int state, byte par1) {
		BonusPoint[state] = par1;
	}
	//called when load nbt data or GUI click
	public void setStateFlag(int flag, boolean par1) {
		this.StateFlag[flag] = par1;
		
		//�Y�ק�melee flag, �hreload AI
		if(flag == ID.F_UseMelee) {
			clearAITasks();
    		setAIList();
		}
	}
	//called when load nbt data or GUI click
	public void setEntityFlagI(int flag, int par1) {
		if(par1 == 1) {
			this.StateFlag[flag] = true;
		}
		else {
			this.StateFlag[flag] = false;
		}
		
		//�Y�ק�melee flag, �hreload AI
		if(flag == ID.F_UseMelee) {
			clearAITasks();
    		setAIList();
		}
	}
	
	//called when entity spawn, set the type modify
	public void initTypeModify() {
		TypeModify[ID.HP] = Values.ModHP[ShipID];
		TypeModify[ID.ATK] = Values.ModATK[ShipID];
		TypeModify[ID.DEF] = Values.ModDEF[ShipID];
		TypeModify[ID.SPD] = Values.ModSPD[ShipID];
		TypeModify[ID.MOV] = Values.ModMOV[ShipID];
		TypeModify[ID.HIT] = Values.ModHIT[ShipID];
	}

	public void setStateEmotion(int id, int value, boolean sync) {
		StateEmotion[id] = (byte)value;
		if(sync && !worldObj.isRemote) {
			TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 32D);
			CommonProxy.channel.sendToAllAround(new S2CEntitySync(this, 1), point);
		}
	}
	
	//emotion start time (CLIENT ONLY), called from model class
	public void setStartEmotion(int par1) {
		StartEmotion = par1;
	}
	
	//manual send sync packet
	public void sendSyncPacket() {
		if(!worldObj.isRemote) {
			if(this.getOwner() != null) {
				EntityPlayerMP player = EntityHelper.getOnlinePlayer(this.getOwner().getUniqueID());
				CommonProxy.channel.sendTo(new S2CEntitySync(this, 0), player);
			}
		}
	}
	
	//right click on ship
	@Override
	public boolean interact(EntityPlayer player) {	
		ItemStack itemstack = player.inventory.getCurrentItem();  //get item in hand
		
		//�p�G�w�g�Q�i�j, �A�I�@�U�i�H�Ѱ��i�j
		if(this.getLeashed() && this.getLeashedToEntity() == player) {
            this.clearLeashed(true, !player.capabilities.isCreativeMode);
            return true;
        }
	
		//shift+right click�ɥ��}GUI
		if(player.isSneaking() && this.getOwner() != null && player.getUniqueID().equals(this.getOwner().getUniqueID())) {  
			int eid = this.getEntityId();
			//player.openGui vs FMLNetworkHandler ?
    		FMLNetworkHandler.openGui(player, ShinColle.instance, ID.SHIPINVENTORY, this.worldObj, this.getEntityId(), 0, 0);
    		return true;
		}
		
		//use item
		if(itemstack != null) {
			//use cake to change state
			if(itemstack.getItem() == Items.cake) {
				int ShipState = getStateEmotion(ID.State) - Values.State.EQUIP;
				
				switch(getStateEmotion(ID.State)) {
				case Values.State.NORMAL:			//�쥻�����, �אּ���
					setStateEmotion(ID.State, Values.State.EQUIP, true);
					break;
				case Values.State.NORMAL_MINOR:
					setStateEmotion(ID.State, Values.State.EQUIP_MINOR, true);
					break;
				case Values.State.NORMAL_MODERATE:
					setStateEmotion(ID.State, Values.State.EQUIP_MODERATE, true);
					break;
				case Values.State.NORMAL_HEAVY:
					setStateEmotion(ID.State, Values.State.EQUIP_HEAVY, true);
					break;
				case Values.State.EQUIP:			//�쥻��ܸ˳�, �אּ�����
					setStateEmotion(ID.State, Values.State.NORMAL, true);
					break;
				case Values.State.EQUIP_MINOR:
					setStateEmotion(ID.State, Values.State.NORMAL_MINOR, true);
					break;
				case Values.State.EQUIP_MODERATE:
					setStateEmotion(ID.State, Values.State.NORMAL_MODERATE, true);
					break;
				case Values.State.EQUIP_HEAVY:
					setStateEmotion(ID.State, Values.State.NORMAL_HEAVY, true);
					break;			
				}
				return true;
			}
			
			//use repair bucket
			if(itemstack.getItem() == ModItems.BucketRepair) {	
				//hp����max hp�ɥi�H�ϥ�bucket
				if(this.getHealth() < this.getMaxHealth()) {
	                if (!player.capabilities.isCreativeMode) {  //stack-1 in non-creative mode
	                    --itemstack.stackSize;
	                }
	
	                if(this instanceof BasicEntityShipSmall) {
	                	this.heal(this.getMaxHealth() * 0.1F);	//1 bucket = 10% hp for small ship
	                }
	                else {
	                	this.heal(this.getMaxHealth() * 0.05F);	//1 bucket = 5% hp for large ship
	                }
	                
	                if (itemstack.stackSize <= 0) {  //���~�Χ��ɭn�]�w��null�M�Ÿ�slot
	                	player.inventory.setInventorySlotContents(player.inventory.currentItem, (ItemStack)null);
	                }
	                
	                return true;
	            }			
			}	
			
			//use lead
			if(itemstack.getItem() == Items.lead && this.allowLeashing()) {	
				this.setLeashedToEntity(player, true);
				return true;
	        }
			
		}
		
		//click ship without shift = sit
		if(!this.worldObj.isRemote && !player.isSneaking() && this.getOwner() != null && player.getUniqueID().equals(this.getOwner().getUniqueID())) {			
			this.setSitting(!this.isSitting());
            this.isJumping = false;
            this.setPathToEntity((PathEntity)null);
            this.setTarget((Entity)null);
            this.setAttackTarget((EntityLivingBase)null);
            return true;
        }

		return false;
	}
	
	/**�קﲾ�ʤ�k, �Ϩ�water��lava�����ʮɹ��Oflying entity
     * Moves the entity based on the specified heading.  Args: strafe, forward
     */
	@Override
    public void moveEntityWithHeading(float movX, float movZ) {
        double d0;

        if(this.isInWater() || this.handleLavaMovement()) { //�P�w���G�餤��, ���|�۰ʤU�I
            d0 = this.posY;
            this.moveFlying(movX, movZ, this.getStateFinal(ID.MOV)*0.4F); //�������t�׭p��(�t�}���ĪG)
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
//            LogHelper.info("DEBUG : f2 "+f2+" ");
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
	
	//update entity position
	@Override
	public void onUpdate() {
		super.onUpdate();

		this.checkDepth();	//check depth every tick

		Block CheckBlock = checkBlockWithOffset(0);
		if(this.getShipDepth() > 0D) {
//			//�b�G�餤��������, �����۵M�U���t�׬�0.02
//			this.motionY += 0.02D;
//			//�b�����[�t
//			this.motionX *= 1.1D;
//			this.motionZ *= 1.1D;

			if(this.worldObj.isRemote) {
				//�����ʮ�, ���ͤ���S��
				//(�`�N��entity�]���]���D���t��s, client�ݤ��|��smotionX���ƭ�, �ݦۦ�p��)
				double motX = this.posX - this.prevPosX;
				double motZ = this.posZ - this.prevPosZ;
				double parH = this.posY - (int)this.posY;
				
				EntityFX particleSpray = new EntityFXSpray(worldObj, 
				          this.posX + motX*1.5D, this.posY + 0.4D, this.posZ + motZ*1.5D, 
				          -motX*0.5D, 0D, -motZ*0.5D,
				          1F, 1F, 1F, 1F);
				    
				if(motX != 0 || motZ != 0) {
					Minecraft.getMinecraft().effectRenderer.addEffect(particleSpray);
				}
			}
		}
	}

	//check entity state every tick
	@Override
	public void onLivingUpdate() {
        super.onLivingUpdate();
//        LogHelper.info("DEBug : tick "+this.ticksExisted);
        //server side check
        if((!worldObj.isRemote)) {
        	//sync client and reset AI after server start 1 sec
        	if(ticksExisted == 20) {
        		clearAITasks();
        		clearAITargetTasks();	//reset AI for get owner after loading NBT data
        		setAIList();
        		setAITargetList();
        		sendSyncPacket();		//sync packet to client
        	}
        	    	
        	//check every 100 ticks
        	if(ticksExisted % 100 == 0) {
        		//roll emtion: hungry > T_T > bored > O_O
        		if(getStateFlag(ID.F_NoFuel)) {
        			if(this.getStateEmotion(ID.Emotion) != Values.Emotion.HUNGRY) {
//        				LogHelper.info("DEBUG : set emotion HUNGRY");
	    				this.setStateEmotion(ID.Emotion, Values.Emotion.HUNGRY, true);
	    			}
        		}
        		else {
        			if(this.getHealth()/this.getMaxHealth() < 0.5F) {
    	    			if(this.getStateEmotion(ID.Emotion) != Values.Emotion.T_T) {
//    	    				LogHelper.info("DEBUG : set emotion T_T");
    	    				this.setStateEmotion(ID.Emotion, Values.Emotion.T_T, true);
    	    			}			
    	    		}
        			else {
        				if(this.isSitting() && this.getRNG().nextInt(3) > 1) {	//30% for bored
        	    			if(this.getStateEmotion(ID.Emotion) != Values.Emotion.BORED) {
//        	    				LogHelper.info("DEBUG : set emotion BORED");
        	    				this.setStateEmotion(ID.Emotion, Values.Emotion.BORED, true);
        	    			}
        	    		}
        	    		else {	//back to normal face
        	    			if(this.getStateEmotion(ID.Emotion) != Values.Emotion.NORMAL) {
//        	    				LogHelper.info("DEBUG : set emotion NORMAL");
        	    				this.setStateEmotion(ID.Emotion, Values.Emotion.NORMAL, true);
        	    			}
        	    		}
        			}     			
        		}

        		//set air value
        		if(this.getAir() < 300) {
                	setAir(300);
                }
        		
        		//get ammo if no ammo
        		if(!this.hasAmmoLight()) { this.decrAmmoNum(2); }
        		if(!this.hasAmmoHeavy()) { this.decrAmmoNum(3); }
        		
        		//calc move distance and eat grudge (check position 5 sec ago)
        		double distX = posX - ShipPrevX;
        		double distY = posY - ShipPrevY;
        		double distZ = posZ - ShipPrevZ;
        		ShipPrevX = posX;
        		ShipPrevY = posY;
        		ShipPrevZ = posZ;
	        	int distSqrt = (int) MathHelper.sqrt_double(distX*distX + distY*distY + distZ*distZ);
	        	decrGrudgeNum(distSqrt+5);	//eat grudge or change movement speed
        		
        	}//end every 100 ticks
        	
        	//auto recovery every 60 sec
        	if(this.ticksExisted % 1200 == 0) {
        		if(this.getHealth() < this.getMaxHealth()) {
        			this.setHealth(this.getHealth() + this.getMaxHealth() * 0.01F);
        		}
        	}
        }//end if(server side)
    }

	//melee attack method, no ammo cost, no attack speed, damage = 12.5% atk
	@Override
	public boolean attackEntityAsMob(Entity target) {
		//get attack value
		float atk = StateFinal[ID.ATK] * 0.125F;
		//set knockback value (testing)
		float kbValue = 0.15F;
		
		//experience++
		addShipExp(1);
		
		//grudge--
		decrGrudgeNum(1);
				
	    //�Natk��attacker�ǵ��ؼЪ�attackEntityFrom��k, �b�ؼ�class���p��ˮ`
	    //�åB�^�ǬO�_���\�ˮ`��ؼ�
	    boolean isTargetHurt = target.attackEntityFrom(DamageSource.causeMobDamage(this), atk);

	    //play entity attack sound
        if(this.getRNG().nextInt(10) > 6) {
        	this.playSound(Reference.MOD_ID+":ship-hitsmall", 1.0F, 1.0F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
        }
	    
	    //if attack success
	    if(isTargetHurt) {
	    	//calc kb effect
	        if(kbValue > 0) {
	            target.addVelocity((double)(-MathHelper.sin(rotationYaw * (float)Math.PI / 180.0F) * kbValue), 
	                   0.1D, (double)(MathHelper.cos(rotationYaw * (float)Math.PI / 180.0F) * kbValue));
	            motionX *= 0.6D;
	            motionZ *= 0.6D;
	        }

	        //send packet to client for display partical effect   
	        if (!worldObj.isRemote) {
	        	TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64D);
	    		CommonProxy.channel.sendToAllAround(new S2CSpawnParticle(target, 1), point);
			}
	    }

	    return isTargetHurt;
	}
	
	//range attack method, cost light ammo, attack delay = 20 / attack speed, damage = 100% atk 
	public boolean attackEntityWithAmmo(Entity target) {	
		//get attack value
		float atk = StateFinal[ID.ATK];
		//set knockback value (testing)
		float kbValue = 0.05F;
		//update entity look at vector (for particle spawn)
        //����k��getLook�٥��T (client sync���D)
        float lookX = (float) (target.posX - this.posX);
        float lookY = (float) (target.posY - this.posY);
        float lookZ = (float) (target.posZ - this.posZ);
        float lookDist = MathHelper.sqrt_float(lookX*lookX + lookY*lookY + lookZ*lookZ);
        lookX = lookX / lookDist;
        lookY = lookY / lookDist;
        lookZ = lookZ / lookDist;
        
        //�o�g�̷����S��
        TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64D);
		CommonProxy.channel.sendToAllAround(new S2CSpawnParticle(this, 6, this.posX, this.posY, this.posZ, lookX, lookY, lookZ), point);

		//play cannon fire sound at attacker
        playSound(Reference.MOD_ID+":ship-firesmall", 0.4F, 0.7F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
        //play entity attack sound
        if(this.rand.nextInt(10) > 7) {
        	this.playSound(Reference.MOD_ID+":ship-hitsmall", 1F, 1F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
        }
        
        //experience++
  		addShipExp(2);
  		
  		//grudge--
  		decrGrudgeNum(1);
        
        //light ammo -1
        if(!decrAmmoNum(0)) {		//not enough ammo
        	atk = atk * 0.125F;	//reduce damage to 12.5%
        }
        
        //calc miss chance
        int missChance = (int) (StateFinal[ID.HIT] / (2F + 3F * (lookDist / StateFinal[ID.HIT])));
        if(missChance < 3) missChance = 3;	//max miss chance = 33% 
        if(this.rand.nextInt(missChance) == 0) {
        	atk = 0;	//still attack, but no damage
        	//spawn miss particle
    		EntityFX particleMiss = new EntityFXMiss(worldObj, 
    		          this.posX, this.posY+this.height, this.posZ, 1F);	    
    		Minecraft.getMinecraft().effectRenderer.addEffect(particleMiss);
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
	        
        	//send packet to client for display partical effect  
	        TargetPoint point1 = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64D);
			CommonProxy.channel.sendToAllAround(new S2CSpawnParticle(target, 9), point1);
        }

	    return isTargetHurt;
	}

	//range attack method, cost heavy ammo, attack delay = 100 / attack speed, damage = 500% atk
	public boolean attackEntityWithHeavyAmmo(Entity target) {	
		//get attack value
		float atk = StateFinal[ID.ATK_H];
		
		//set knockback value (testing)
		float kbValue = 0.15F;
		//���u�O�_�ĥΪ��g
		boolean isDirect = false;
		//�p��ؼжZ��
		double tarX = target.posX;	//for miss chance calc
		double tarY = target.posY;
		double tarZ = target.posZ;
        double distX = tarX - this.posX;
        double distY = tarY - this.posY;
        double distZ = tarZ - this.posZ;
        float distSqrt = MathHelper.sqrt_double(distX*distX + distY*distY + distZ*distZ);
        double launchPos = this.posY + this.height * 0.7D;
        
        //�W�L�@�w�Z��/���� , �h�ĥΩߪ��u,  �b�����ɵo�g���׸��C
        if((distX*distX+distY*distY+distZ*distZ) < 36) {
        	isDirect = true;
        }
        if(this.getShipDepth() > 0D) {
        	LogHelper.info("DEBUG : depth > 0 , use under missile");
        	isDirect = true;
        	launchPos = this.posY;
        }
		
		//experience++
		this.addShipExp(16);
		
		//grudge--
		decrGrudgeNum(1);
	
		//play cannon fire sound at attacker
        this.playSound(Reference.MOD_ID+":ship-fireheavy", 0.4F, 0.7F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
        //play entity attack sound
        if(this.getRNG().nextInt(10) > 7) {
        	this.playSound(Reference.MOD_ID+":ship-hitsmall", 1F, 1F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
        }
        
        //heavy ammo -1
        if(!decrAmmoNum(1)) {	//not enough ammo
        	atk = atk * 0.125F;	//reduce damage to 12.5%
        }
        
        //calc miss chance, miss: add random offset(0~6) to missile target 
        int missChance = (int) (StateFinal[ID.HIT] / (2F + 3F * (distSqrt / StateFinal[ID.HIT])));
        if(missChance < 3) missChance = 3;	//max miss chance = 33% 
        if(this.rand.nextInt(missChance) == 0) {
        	tarX = tarX - 3D + this.rand.nextDouble() * 6D;
        	tarY = tarY + this.rand.nextDouble() * 3D;
        	tarZ = tarZ - 3D + this.rand.nextDouble() * 6D;
        	//spawn miss particle
    		EntityFX particleMiss = new EntityFXMiss(worldObj, 
    		          this.posX, this.posY+this.height, this.posZ, 1F);	    
    		Minecraft.getMinecraft().effectRenderer.addEffect(particleMiss);
        }

        //spawn missile
        EntityAbyssMissile missile = new EntityAbyssMissile(this.worldObj, this, 
        		tarX, tarY+target.height*0.2D, tarZ, launchPos, atk, kbValue, isDirect);
        this.worldObj.spawnEntityInWorld(missile);
        
        return true;
	}
	
	//be attacked method, �]�A��Lentity����, anvil����, arrow����, fall damage���ϥΦ���k 
	@Override
    public boolean attackEntityFrom(DamageSource attacker, float atk) {		
		//set hurt face
    	if(this.getStateEmotion(ID.Emotion) != Values.Emotion.O_O) {
    		this.setStateEmotion(ID.Emotion, Values.Emotion.O_O, true);
    	}
		
		//�i��def�p��
        float reduceAtk = atk * (1F - StateFinal[ID.DEF] / 100F);    
        if(atk < 0) { atk = 0; }
        
        //�Y����@�ɥ~, �h�ǰe�^y=4
        if(attacker.getDamageType().equals("outOfWorld")) {
        	this.posY = 4D;
        	return true;
        }
        
        //�L�Ī�entity�ˮ`�L��
		if(this.isEntityInvulnerable()) {	
            return false;
        }
		else if(attacker.getSourceOfDamage() != null) { 
			//���|��ۤv�y���ˮ`
			if(attacker.getSourceOfDamage().equals(this)) {  
				return false;
			}
			
			//�Y������P�ˬ�ship, �h�ˮ`-95% (��ship vs ship�ॴ�[�@�I)
			if(attacker.getSourceOfDamage() instanceof BasicEntityShip) {
				reduceAtk *= 0.05F;
			}

			//�������U�ʧ@
			this.setSitting(false);
   
            //�����class���Q�����P�w, �]�A���mlove�ɶ�, �p����r�ܩ�, �p���K�z/�����ˮ`, 
            //hurtResistantTime(0.5sec�L�Įɶ�)�p��, 
            return super.attackEntityFrom(attacker, reduceAtk);
        }
		
		return false;
    }
	
	//decrese ammo number with type, or find ammo item from inventory
	protected boolean decrAmmoNum(int type) {
		switch(type) {
		case 0:  //use 1 light ammo
			if(hasAmmoLight()) { 
				--StateMinor[ID.NumAmmoLight];
				return true;
			}
			else {
				if(decrSupplies(0)) {  //find ammo item
					StateMinor[ID.NumAmmoLight] += 29;
					return true;
				}
				else if(decrSupplies(2)) {  //find ammo item
					StateMinor[ID.NumAmmoLight] += 269;
					return true;
				}
				else {				   //no ammo
					return false;
				}
			}
		case 1:  //use 1 heavy ammo
			if(hasAmmoHeavy()) { 
				--StateMinor[ID.NumAmmoHeavy];
				return true;
			}
			else {
				if(decrSupplies(1)) {  //find ammo item
					StateMinor[ID.NumAmmoHeavy] += 14;
					return true;
				}
				else if(decrSupplies(3)) {  //find ammo item
					StateMinor[ID.NumAmmoHeavy] += 134;
					return true;
				}
				else {				   //no ammo
					return false;
				}
			}
		case 2:	//no ammo light, use item
			if(decrSupplies(0)) {  //find ammo item
				StateMinor[ID.NumAmmoLight] += 30;
				return true;
			}
			else if(decrSupplies(2)) {  //find ammo item
				StateMinor[ID.NumAmmoLight] += 270;
				return true;
			}
			else {				   //no ammo
				return false;
			}
		case 3:	//no ammo heavy, use item
			if(decrSupplies(1)) {  //find ammo item
				StateMinor[ID.NumAmmoHeavy] += 15;
				return true;
			}
			else if(decrSupplies(3)) {  //find ammo item
				StateMinor[ID.NumAmmoHeavy] += 135;
				return true;
			}
			else {				   //no ammo
				return false;
			}
		case 4:  //use 4 light ammo
			if(StateMinor[ID.NumAmmoLight] > 3) { 
				StateMinor[ID.NumAmmoLight] -= 4;
				return true;
			}
			else {
				if(decrSupplies(0)) {  //find ammo item
					StateMinor[ID.NumAmmoLight] += 26;
					return true;
				}
				else if(decrSupplies(2)) {  //find ammo item
					StateMinor[ID.NumAmmoLight] += 266;
					return true;
				}
				else {				   //no ammo
					return false;
				}
			}
		case 5:  //use 2 heavy ammo
			if(StateMinor[ID.NumAmmoHeavy] > 1) { 
				StateMinor[ID.NumAmmoHeavy] -= 2;
				return true;
			}
			else {
				if(decrSupplies(1)) {  //find ammo item
					StateMinor[ID.NumAmmoHeavy] += 13;
					return true;
				}
				else if(decrSupplies(3)) {  //find ammo item
					StateMinor[ID.NumAmmoHeavy] += 133;
					return true;
				}
				else {				   //no ammo
					return false;
				}
			}
		}
		
		return false;	//unknow attack type
	}
	
	//eat grudge and change movement speed
	protected void decrGrudgeNum(int par1) {
		boolean PrevNoFuel = getStateFlag(ID.F_NoFuel);
		
		if(par1 > 215) {	//max cost = 215 (calc from speed 1 moving 5 sec)
			par1 = 215;
		}
		
		if(StateMinor[ID.NumGrudge] >= (int)par1) { //has enough fuel
			StateMinor[ID.NumGrudge] -= (int)par1;
		}
		else {
			if(decrSupplies(4)) {		//find grudge
				StateMinor[ID.NumGrudge] += 1200;
				StateMinor[ID.NumGrudge] -= (int)par1;
			}
			else if(decrSupplies(5)) {	//find grudge block
				StateMinor[ID.NumGrudge] += 10800;
				StateMinor[ID.NumGrudge] -= (int)par1;
			}
//�קK�Y���t���x�s��T�����, �]�����Φ�����@��grudge�ɥR�D��
//			else if(decrSupplies(6)) {	//find grudge heavy block
//				NumGrudge += 97200;
//				NumGrudge -= (int)par1;
//			}
		}
		
		if(StateMinor[ID.NumGrudge] <= 0) {
			setStateFlag(ID.F_NoFuel, true);
		}
		else {
			setStateFlag(ID.F_NoFuel, false);
		}

		//get fuel, set AI
		if(!getStateFlag(ID.F_NoFuel) && PrevNoFuel) {
			LogHelper.info("DEBUG : !NoFuel set AI");
			clearAITasks();
			clearAITargetTasks();
			setAIList();
			setAITargetList();
			sendSyncPacket();
		}
		
		//no fuel, clear AI
		if(getStateFlag(ID.F_NoFuel)) {
			LogHelper.info("DEBUG : NoFuel clear AI");
			clearAITasks();
			clearAITargetTasks();
			sendSyncPacket();
		}
		
	}
	
	//decrese ammo amount with type, return true or false(not enough item)
	protected boolean decrSupplies(int type) {
		boolean isEnoughItem = true;
		int itemNum = 1;
		ItemStack itemType = null;
		
		//find ammo
		switch(type) {
		case 0:	//use 1 light ammo
			itemType = new ItemStack(ModItems.Ammo,1,0);
			break;
		case 1: //use 1 heavy ammo
			itemType = new ItemStack(ModItems.Ammo,1,2);
			break;
		case 2:	//use 1 light ammo container
			itemType = new ItemStack(ModItems.Ammo,1,1);
			break;
		case 3: //use 1 heavy ammo container
			itemType = new ItemStack(ModItems.Ammo,1,3);
			break;
		case 4: //use 1 grudge
			itemType = new ItemStack(ModItems.Grudge,1);
			break;
		case 5: //use 1 grudge block
			itemType = new ItemStack(ModBlocks.BlockGrudge,1);
			break;
		case 6: //use 1 grudge block
			itemType = new ItemStack(ModBlocks.BlockGrudgeHeavy,1);
			break;
		}
		
		//search item in ship inventory
		int i = findItemInSlot(itemType);
		if(i == -1) {		//item not found
			return false;
		}
		
		//decr item stacksize
		ItemStack getItem = this.ExtProps.slots[i];

		if(getItem.stackSize >= itemNum) {
			getItem.stackSize -= itemNum;
		}
		else {	//not enough item
			getItem.stackSize = 0;
			isEnoughItem = false;
		}
				
		if(getItem.stackSize == 0) {
			getItem = null;
		}
		
		//save back itemstack
		//no need to sync because no GUI opened
		this.ExtProps.slots[i] = getItem;	
		
		return isEnoughItem;	
	}

	//find item in ship inventory
	protected int findItemInSlot(ItemStack parItem) {
		ItemStack slotitem = null;

		//search ship inventory (except equip slots)
		for(int i=ContainerShipInventory.SLOTS_EQUIP; i<ContainerShipInventory.SLOTS_TOTAL; i++) {
			slotitem = this.ExtProps.slots[i];
			if(slotitem != null && 
			   slotitem.getItem().equals(parItem.getItem()) && 
			   slotitem.getItemDamage() == parItem.getItemDamage()) {
				return i;	//found item
			}		
		}	
		
		return -1;	//item not found
	}

	
}
