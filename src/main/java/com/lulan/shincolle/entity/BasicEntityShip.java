package com.lulan.shincolle.entity;

import java.util.Iterator;
import java.util.Random;
import java.util.UUID;

import com.lulan.shincolle.ShinColle;
import com.lulan.shincolle.ai.EntityAIShipSit;
import com.lulan.shincolle.client.particle.EntityFXSpray;
import com.lulan.shincolle.crafting.EquipCalc;
import com.lulan.shincolle.entity.EntityAbyssMissile;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.init.ModItems;
import com.lulan.shincolle.inventory.ContainerShipInventory;
import com.lulan.shincolle.network.createPacketS2C;
import com.lulan.shincolle.reference.AttrID;
import com.lulan.shincolle.reference.AttrValues;
import com.lulan.shincolle.reference.GUIs;
import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.utility.LogHelper;

import cpw.mods.fml.common.network.internal.FMLNetworkHandler;
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
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityLargeFireball;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.pathfinding.PathEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;

/**SHIP DATA <br>
 * Explanation in crafting/ShipCalc.class
 */
public abstract class BasicEntityShip extends EntityTameable implements IEntityShip {

	protected ExtendShipProps ExtProps;	//entity�B�~NBT����
	
	//for attribute calc
	protected short ShipLevel;			//ship level
	protected int Kills;				//kill mobs
	protected int ExpCurrent;			//total number of attacks = experience
	protected int ExpNext;				//exp require for next level
	protected byte ShipType;			//ship type
	protected byte ShipID;
	//for AI calc
	protected int StartEmotion;			//���}�l�ɶ�
	protected boolean HasAmmoLight;		//�ˬd���L�u��
	protected boolean HasAmmoHeavy;		//�ˬd���L�����u��
	protected int NumAmmoLight;			//�u�Ħs�q
	protected int NumAmmoHeavy;			//�����u�Ħs�q
	protected double ShipDepth;			//���`, �Ω�������קP�w
	protected boolean CanFloatUp;		//�O�_���Ů����i�H�W�B
	protected boolean IsMarried;		//�O�_�w�B
	protected boolean IsFollowing;		//�O�_���b���H
	//equip states: 0:HP 1:ATK 2:DEF 3:SPD 4:MOV 5:HIT
	protected float[] ArrayEquip;
	//final states: 0:HP 1:ATK 2:DEF 3:SPD 4:MOV 5:HIT
	protected float[] ArrayFinal;
	//EntityState: 0:State 1:Emotion 2:SwimType
	protected byte[] EntityState;
	//BonusPoint: 0:HP 1:ATK 2:DEF 3:SPD 4:MOV 5:HIT
	protected byte[] BonusPoint;
	//TypeModify: 0:HP 1:ATK 2:DEF 3:SPD 4:MOV 5:HIT
	protected float[] TypeModify;
	
	
	public BasicEntityShip(World world) {
		super(world);
		//init value
		ShipLevel = 1;				//ship level
		Kills = 0;					//kill mobs
		ExpCurrent = 0;				//current exp
		ExpNext = 40;				//lv1 -> lv2 exp
		//equip states: 0:HP 1:ATK 2:DEF 3:SPD 4:MOV 5:HIT
		ArrayEquip = new float[] {0F, 0F, 0F, 0F, 0F, 0F};
		//final states: 0:HP 1:ATK 2:DEF 3:SPD 4:MOV 5:HIT
		ArrayFinal = new float[] {0F, 0F, 0F, 0F, 0F, 0F};
		//EntityState: 0:State 1:Emotion 2:SwimType
		EntityState = new byte[] {0, 0, 0};
		//BonusPoint: 0:HP 1:ATK 2:DEF 3:SPD 4:MOV 5:HIT
		BonusPoint = new byte[] {0, 0, 0, 0, 0, 0};
		//TypeModify: 0:HP 1:ATK 2:DEF 3:SPD 4:MOV 5:HIT
		TypeModify = new float[] {1F, 1F, 1F, 1F, 1F, 1F};
		//for AI
		HasAmmoLight = false;		//ammo check
		HasAmmoHeavy = false;
		NumAmmoLight = 0;
		NumAmmoHeavy = 0;
		StartEmotion = -1;			//���}�l�ɶ�
		IsMarried = false;
		IsFollowing = false;
		ShipDepth = 0D;
		CanFloatUp = false;
		this.isImmuneToFire = true;
	}
	
	@Override
	public boolean isAIEnabled() {
		return true;
	}
	
	@Override
	public float getEyeHeight() {
		return this.height * 1F;
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
	   tasks.taskEntries.clear();
	}
	
	//clear target AI
	protected void clearAITargetTasks() {
	   targetTasks.taskEntries.clear();
	}
	
	//getter
	public ExtendShipProps getExtProps() {
		return ExtProps;
	}
	public short getShipLevel() {
		return ShipLevel;
	}
	public int getKills() {
		return Kills;
	}
	public int getExpCurrent() {
		return ExpCurrent;
	}
	public int getExpNext() {
		return ExpNext;
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
		return HasAmmoLight;
	}
	public boolean hasAmmoHeavy() {
		return HasAmmoHeavy;
	}
	public int getNumAmmoLight() {
		return NumAmmoLight;
	}
	public int getNumAmmoHeavy() {
		return NumAmmoHeavy;
	}
	public double getShipDepth() {
		return ShipDepth;
	}
	public boolean isMarried() {
		return IsMarried;
	}
	public boolean isFollowing() {
		return IsFollowing;
	}
	public boolean CanFloatUp() {
		return CanFloatUp;
	}
	public float getEquipHP() {
		return ArrayEquip[AttrID.HP];
	}
	public float getEquipATK() {
		return ArrayEquip[AttrID.ATK];
	}
	public float getEquipDEF() {
		return ArrayEquip[AttrID.DEF];
	}
	public float getEquipSPD() {
		return ArrayEquip[AttrID.SPD];
	}
	public float getEquipMOV() {
		return ArrayEquip[AttrID.MOV];
	}
	public float getEquipHIT() {
		return ArrayEquip[AttrID.HIT];
	}
	public float getFinalHP() {
		return ArrayFinal[AttrID.HP];
	}
	public float getFinalATK() {
		return ArrayFinal[AttrID.ATK];
	}
	public float getFinalDEF() {
		return ArrayFinal[AttrID.DEF];
	}
	public float getFinalSPD() {
		return ArrayFinal[AttrID.SPD];
	}
	public float getFinalMOV() {
		return ArrayFinal[AttrID.MOV];
	}
	public float getFinalHIT() {
		return ArrayFinal[AttrID.HIT];
	}
	public byte getEntityState() {
		return EntityState[AttrID.State];
	}
	public byte getEntityEmotion() {
		return EntityState[AttrID.Emotion];
	}
	public byte getEntitySwinType() {
		return EntityState[AttrID.SwimType];
	}
	public byte getBonusHP() {
		return BonusPoint[AttrID.HP];
	}
	public byte getBonusATK() {
		return BonusPoint[AttrID.ATK];
	}
	public byte getBonusDEF() {
		return BonusPoint[AttrID.DEF];
	}
	public byte getBonusSPD() {
		return BonusPoint[AttrID.SPD];
	}
	public byte getBonusMOV() {
		return BonusPoint[AttrID.MOV];
	}
	public byte getBonusHIT() {
		return BonusPoint[AttrID.HIT];
	}
	public float getTypeModifyHP() {
		return TypeModify[AttrID.HP];
	}
	public float getTypeModifyATK() {
		return TypeModify[AttrID.ATK];
	}
	public float getTypeModifyDEF() {
		return TypeModify[AttrID.DEF];
	}
	public float getTypeModifySPD() {
		return TypeModify[AttrID.SPD];
	}
	public float getTypeModifyMOV() {
		return TypeModify[AttrID.MOV];
	}
	public float getTypeModifyHIT() {
		return TypeModify[AttrID.HIT];
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
						CanFloatUp = true;
					}
					else {
						CanFloatUp = false;
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
	
	//setter	
	//setting attributes, called at load nbt data & init mob
	public void calcShipAttributes(byte id) {
		//init or renew bonus value, for short value: discard decimal
		//HP = (base + (point + 1) * level * typeModify) * config HP ratio
		ArrayFinal[AttrID.HP] = ((float)AttrValues.BaseHP[id] + (float)(BonusPoint[AttrID.HP]+1) * (float)ShipLevel * TypeModify[AttrID.HP]) * ConfigHandler.hpRatio; 
		//ATK = base + ((point + 1) * level / 3 + equip) * typeModify
		ArrayFinal[AttrID.ATK] = (float)AttrValues.BaseATK[id] + ((float)(BonusPoint[AttrID.ATK]+1) * (((float)ShipLevel)/3F) + ArrayEquip[AttrID.ATK]) * TypeModify[AttrID.ATK];
		//DEF = base + ((point + 1) * level / 3 * 0.4 + equip) * typeModify
		ArrayFinal[AttrID.DEF] = (float)AttrValues.BaseDEF[id] + ((float)(BonusPoint[AttrID.DEF]+1) * (((float)ShipLevel)/3F) * 0.4F + ArrayEquip[AttrID.DEF]) * TypeModify[AttrID.DEF];
		//SPD = base + ((point + 1) * level / 10 * 0.02 + equip) * typeModify
		ArrayFinal[AttrID.SPD] = AttrValues.BaseSPD[id] + ((float)(BonusPoint[AttrID.SPD]+1) * (((float)ShipLevel)/10F) * 0.02F + ArrayEquip[AttrID.SPD]) * TypeModify[AttrID.SPD];
		//MOV = base + ((point + 1) * level / 10 * 0.01 + equip) * typeModify
		ArrayFinal[AttrID.MOV] = AttrValues.BaseMOV[id] + ((float)(BonusPoint[AttrID.MOV]+1) * (((float)ShipLevel)/10F) * 0.01F) * TypeModify[AttrID.MOV] + ArrayEquip[AttrID.MOV];
		//HIT = base + ((point + 1) * level / 10 * 0.8 + equip) * typeModify
		ArrayFinal[AttrID.HIT] = AttrValues.BaseHIT[id] + ((float)(BonusPoint[AttrID.HIT]+1) * (((float)ShipLevel)/10F) * 0.8F + ArrayEquip[AttrID.HIT]) * TypeModify[AttrID.HIT];
		//KB Resistance = Level / 10 * 0.04
		float resisKB = (((float)ShipLevel)/10F) * 0.067F;

		if(ArrayFinal[AttrID.DEF] > 95F) {
			ArrayFinal[AttrID.DEF] = 95F;	//max def = 95%
		}
		if(ArrayFinal[AttrID.SPD] > 2F) {
			ArrayFinal[AttrID.SPD] = 2F;	//min attack delay = 0.5 sec
		}
		if(ArrayFinal[AttrID.SPD] < 0F) {
			ArrayFinal[AttrID.SPD] = 0F;
		}
//		if(ArrayFinal[AttrID.MOV] > 1.2F) {
//			ArrayFinal[AttrID.MOV] = 1.2F;	//move speed > 1.2 is buggy
//		}
		if(ArrayFinal[AttrID.MOV] < 0F) {
			ArrayFinal[AttrID.MOV] = 0F;
		}
		
		//set attribute by final value
		/**
		 * DO NOT SET ATTACK DAMAGE to non-EntityMob!!!!!
		 */
		getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(ArrayFinal[AttrID.HP]);
		getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(ArrayFinal[AttrID.MOV]);
		getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(ArrayFinal[AttrID.HIT]+16); //������ؼ�, ���|���d��
		getEntityAttribute(SharedMonsterAttributes.knockbackResistance).setBaseValue(resisKB);
		//for new ship
		if(this.getHealth() == 20F) this.setHealth(this.getMaxHealth());
		
		//for server side
		if(!worldObj.isRemote) {
			clearAITargetTasks();	//reset target AI (update hit range)
			setAITargetList();	
			sendSyncPacket();		//sync nbt data
		}			
	}
	
	//set exp value, no sync or update (for client load nbt data, gui display)
	public void setExpCurrent(int par1) {
		ExpCurrent = par1;
	}
	
	//set next exp value, no sync or update (for client load nbt data, gui display)
	public void setExpNext() {
		ExpNext = ShipLevel * 20 + 20;
	}
		
	//called when entity exp++
	public void addShipExp(int exp) {
		int CapLevel = IsMarried ? 150 : 100;
		
		if(ShipLevel != CapLevel) {	//level is not cap level
			ExpCurrent += exp;
			if(ExpCurrent >= ExpNext) {
				ExpCurrent -= ExpNext;	//level up
				ExpNext = (ShipLevel + 1) * 20 + 20;
				setShipLevel(++ShipLevel, true);
			}
		}	
	}
	
	//called when entity level up
	public void setShipLevel(short par1, boolean update) {
		//set level
		if(par1 < 151) {
			ShipLevel = par1;
		}
		//update attributes
		if(update) { 
			calcShipAttributes(ShipID); 
			this.setHealth(this.getMaxHealth());
		}
	}
	
	//called when a mob die near the entity
	public void addKills() {
		Kills++;
	}
	
	//called when load nbt data
	public void setKills(int par1) {
		Kills = par1;
	}
	
	//NYI
	public void setMarried(boolean par1) {
		IsMarried = par1;
	}
	
	//called in AI
	public void setFollowing(boolean par1) {
		IsFollowing = par1;
	}

	//called when load nbt data
	public void setNumAmmoLight(int par1) {
		NumAmmoLight = par1;
	}
	
	//called when load nbt data
	public void setNumAmmoHeavy(int par1) {
		NumAmmoHeavy = par1;
	}
	
	//ship attribute setter, sync packet in method: calcShipAttributes 
	public void setFinalHP(float par1) {
		ArrayFinal[AttrID.HP] = par1;
	}
	public void setFinalATK(float par1) {
		ArrayFinal[AttrID.ATK] = par1;
	}
	public void setFinalDEF(float par1) {
		ArrayFinal[AttrID.DEF] = par1;
	}
	public void setFinalSPD(float par1) {
		ArrayFinal[AttrID.SPD] = par1;
	}
	public void setFinalMOV(float par1) {
		ArrayFinal[AttrID.MOV] = par1;
	}
	public void setFinalHIT(float par1) {
		ArrayFinal[AttrID.HIT] = par1;
	}
	public void setBonusHP(byte par1) {
		BonusPoint[AttrID.HP] = par1;
	}
	public void setBonusATK(byte par1) {
		BonusPoint[AttrID.ATK] = par1;
	}
	public void setBonusDEF(byte par1) {
		BonusPoint[AttrID.DEF] = par1;
	}
	public void setBonusSPD(byte par1) {
		BonusPoint[AttrID.SPD] = par1;
	}
	public void setBonusMOV(byte par1) {
		BonusPoint[AttrID.MOV] = par1;
	}
	public void setBonusHIT(byte par1) {
		BonusPoint[AttrID.HIT] = par1;
	}
	
	//called when entity equip changed
	//this method get equip state from slots, no input parm
	public void setEquipAndUpdateState() {
		ItemStack itemstack = null;
		float[] equipStat = {0F,0F,0F,0F,0F,0F};
		ArrayEquip[AttrID.HP] = 0F;
		ArrayEquip[AttrID.ATK] = 0F;
		ArrayEquip[AttrID.DEF] = 0F;
		ArrayEquip[AttrID.SPD] = 0F;
		ArrayEquip[AttrID.MOV] = 0F;
		ArrayEquip[AttrID.HIT] = 0F;
		
		//calc equip slots
		for(int i=0; i<ContainerShipInventory.SLOTS_EQUIP; i++) {
			itemstack = this.ExtProps.slots[i];
			if(itemstack != null) {
				equipStat = EquipCalc.getEquipStat(itemstack);
				ArrayEquip[AttrID.HP] += equipStat[AttrID.HP];
				ArrayEquip[AttrID.ATK] += equipStat[AttrID.ATK];
				ArrayEquip[AttrID.DEF] += equipStat[AttrID.DEF];
				ArrayEquip[AttrID.SPD] += equipStat[AttrID.SPD];
				ArrayEquip[AttrID.MOV] += equipStat[AttrID.MOV];
				ArrayEquip[AttrID.HIT] += equipStat[AttrID.HIT];
			}
		}
		//update value
		calcShipAttributes(this.ShipID);
	}
	
	//called when entity spawn, set the type modify
	public void initTypeModify() {
		TypeModify[AttrID.HP] = AttrValues.ModHP[ShipID];
		TypeModify[AttrID.ATK] = AttrValues.ModATK[ShipID];
		TypeModify[AttrID.DEF] = AttrValues.ModDEF[ShipID];
		TypeModify[AttrID.SPD] = AttrValues.ModSPD[ShipID];
		TypeModify[AttrID.MOV] = AttrValues.ModMOV[ShipID];
		TypeModify[AttrID.HIT] = AttrValues.ModHIT[ShipID];
	}

	public void setEntityState(int par1, boolean sync) {
		EntityState[AttrID.State] = (byte)par1;	
		if(sync && !worldObj.isRemote) {
			createPacketS2C.sendS2CEntityStateSync(this);    
		}
	}
	
	public void setEntityEmotion(int par1, boolean sync) {
		EntityState[AttrID.Emotion] = (byte)par1;
		if(sync && !worldObj.isRemote) {
			createPacketS2C.sendS2CEntityStateSync(this);    
		}
	}
	
	public void setEntitySwimType(int par1, boolean sync) {
		EntityState[AttrID.SwimType] = (byte)par1;			
		if(sync && !worldObj.isRemote) {
			createPacketS2C.sendS2CEntityStateSync(this);    
		}   
	}
	
	//emotion start time (CLIENT ONLY), called from model class
	public void setStartEmotion(int par1) {
		StartEmotion = par1;
	}
	
	//manual send sync packet
	public void sendSyncPacket() {
		if (!worldObj.isRemote) {
			createPacketS2C.sendS2CEntitySync(this);     
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
		if(player.isSneaking() && player.getUniqueID().equals(this.getOwner().getUniqueID())) {  
			int eid = this.getEntityId();
			//player.openGui vs FMLNetworkHandler ?
		//	player.openGui(ShinColle.instance, GUIs.SHIPINVENTORY, this.worldObj, eid, 0, 0);
    		FMLNetworkHandler.openGui(player, ShinColle.instance, GUIs.SHIPINVENTORY, this.worldObj, this.getEntityId(), 0, 0);
    		return true;
		}
		
		//click ship without shift = sit
		if(!this.worldObj.isRemote && !player.isSneaking() && player.getUniqueID().equals(this.getOwner().getUniqueID())) {			
            this.setSitting(!this.isSitting());
            this.isJumping = false;
            this.setPathToEntity((PathEntity)null);
            this.setTarget((Entity)null);
            this.setAttackTarget((EntityLivingBase)null);
        }
		
		//use item
		if(itemstack != null) {
			//use cake to change state
			if(itemstack.getItem() == Items.cake) {
				int ShipState = getEntityState() - AttrValues.State.EQUIP;
				
				switch(getEntityState()) {
				case AttrValues.State.NORMAL:			//�쥻�����, �אּ���
					setEntityState(AttrValues.State.EQUIP, true);
					break;
				case AttrValues.State.NORMAL_MINOR:
					setEntityState(AttrValues.State.EQUIP_MINOR, true);
					break;
				case AttrValues.State.NORMAL_MODERATE:
					setEntityState(AttrValues.State.EQUIP_MODERATE, true);
					break;
				case AttrValues.State.NORMAL_HEAVY:
					setEntityState(AttrValues.State.EQUIP_HEAVY, true);
					break;
				case AttrValues.State.EQUIP:			//�쥻��ܸ˳�, �אּ�����
					setEntityState(AttrValues.State.NORMAL, true);
					break;
				case AttrValues.State.EQUIP_MINOR:
					setEntityState(AttrValues.State.NORMAL_MINOR, true);
					break;
				case AttrValues.State.EQUIP_MODERATE:
					setEntityState(AttrValues.State.NORMAL_MODERATE, true);
					break;
				case AttrValues.State.EQUIP_HEAVY:
					setEntityState(AttrValues.State.NORMAL_HEAVY, true);
					break;			
				}
			}
			
			//use repair bucket
			if(itemstack.getItem() == ModItems.BucketRepair) {	
				//hp����max hp�ɥi�H�ϥ�bucket
				if(this.getHealth() < this.getMaxHealth()) {
	                if (!player.capabilities.isCreativeMode) {  //stack-1 in non-creative mode
	                    --itemstack.stackSize;
	                }
	
	                if(this instanceof BasicEntitySmallShip) {
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

		return false;
	}
	
	//update entity position
	@Override
	public void onUpdate() {
		super.onUpdate();
		
		this.checkDepth();	//check depth every tick
		
		//�b�G�餤��������, �����۵M�U���t�׬�0.02
		Block CheckBlock = checkBlockWithOffset(0);
		if(CheckBlock == Blocks.water || CheckBlock == Blocks.lava) {
			this.motionY += 0.02D;
			
			if(this.worldObj.isRemote) {
				//�����ʮ�, ���ͤ���S��
				//(�`�N��entity�]���]���D���t��s, client�ݤ��|��smotionX���ƭ�, �ݦۦ�p��)
				double motX = this.posX - this.prevPosX;
				double motZ = this.posZ - this.prevPosZ;
				double parH = this.posY - (int)this.posY;
				
				EntityFX particleSpray = new EntityFXSpray(worldObj, 
				          this.posX + motX*1.5D, this.posY + 0.5D, this.posZ + motZ*1.5D, 
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

        //server side check
        if((!worldObj.isRemote)) {
        	//sync client and reset AI after server start 2 sec
        	if(ticksExisted == 40) {
        		clearAITargetTasks();	//reset AI for get owner after loading NBT data
        		setAIList();
        		setAITargetList();
        		sendSyncPacket();		//sync packet to client
        	}
        	
        	//�Ѧ�q�M�wcry emotion
        	if(this.ticksExisted % 40 == 0) {
	    		if(this.getHealth()/this.getMaxHealth() < 0.5F) {
	    			if(this.getEntityEmotion() != AttrValues.Emotion.T_T) {
	    				this.setEntityEmotion(AttrValues.Emotion.T_T, true);
	    			}			
	    		}
	    		else {	//back to normal face
	    			if(this.getEntityEmotion() == AttrValues.Emotion.T_T) {
	    				this.setEntityEmotion(AttrValues.Emotion.NORMAL, true);
	    			}
	    		}
    		}
        	    	
        	//check ammo every 100 ticks
        	if(ticksExisted % 100 == 0) {
        		//roll bored emtion when sitting
        		if(this.isSitting() && this.getRNG().nextInt(3) > 1) {	//30% for bored
	    			if(this.getEntityEmotion() != AttrValues.Emotion.BORED) {
	    				this.setEntityEmotion(AttrValues.Emotion.BORED, true);
	    			}
	    		}
	    		else {	//back to normal face
	    			if(this.getEntityEmotion() == AttrValues.Emotion.BORED) {
	    				this.setEntityEmotion(AttrValues.Emotion.NORMAL, true);
	    			}
	    		}
        		
        		//set air value
        		if(this.getAir() < 120) {
                	setAir(300);
                }
        		
        		//check ammo number
	        	HasAmmoLight = false;
	        	HasAmmoHeavy = false;
	        	if(getNumAmmoLight() > 0) { HasAmmoLight = true; }
	        	if(getNumAmmoHeavy() > 0) { HasAmmoHeavy = true; }	        	
	        	//check ship inventory
	        	ItemStack SlotItem = null;
	        	for(int i=ContainerShipInventory.SLOTS_EQUIP; i<ContainerShipInventory.SLOTS_TOTAL; i++) {
	        		SlotItem = this.ExtProps.slots[i];
	        		
	    			if(SlotItem != null) {
	    				if(SlotItem.getItem() == ModItems.Ammo) { HasAmmoLight = true; }
	    				if(SlotItem.getItem() == ModItems.HeavyAmmo) { HasAmmoHeavy = true; }
	    			}
	    		}
	        	
        	}//end every 100 ticks        	
        }//end if(server side)
        
    }
	
	//melee attack method, no ammo cost, no attack speed, damage = 12.5% atk
	@Override
	public boolean attackEntityAsMob(Entity target) {
		//get attack value
		float atk = ArrayFinal[AttrID.ATK] * 0.125F;
		//set knockback value (testing)
		float kbValue = 0.15F;
		
		//experience++
		addShipExp(1);
				
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
				createPacketS2C.sendS2CAttackParticle(target, 1);     
			}
	    }

	    return isTargetHurt;
	}
	
	//range attack method, cost light ammo, attack delay = 20 / attack speed, damage = 100% atk 
	public boolean attackEntityWithAmmo(Entity target) {	
		//get attack value
		float atk = ArrayFinal[AttrID.ATK];
		//set knockback value (testing)
		float kbValue = 0.05F;
		
		//experience++
		this.addShipExp(2);

		//play cannon fire sound at attacker
        this.playSound(Reference.MOD_ID+":ship-firesmall", 0.4F, 0.7F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
        //play entity attack sound
        if(this.getRNG().nextInt(10) > 7) {
        	this.playSound(Reference.MOD_ID+":ship-hitsmall", 1F, 1F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
        }
        
        //light ammo -1
        if(!decrAmmoNum(0)) {		//not enough ammo
        	atk = atk * 0.125F;	//reduce damage to 12.5%
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
	        	    
	        //update entity look at vector (for particle)
	        //����k��getLook�٥��T (client sync���D)
	        float lookX = (float) (target.posX - this.posX);
	        float lookY = (float) (target.posY - this.posY);
	        float lookZ = (float) (target.posZ - this.posZ);
	        float lookDist = MathHelper.sqrt_float(lookX*lookX + lookY*lookY + lookZ*lookZ);
	        lookX = lookX / lookDist;
	        lookY = lookY / lookDist;
	        lookZ = lookZ / lookDist;
	        
        	//send packet to client for display partical effect  
        	createPacketS2C.sendS2CAttackParticle(target, 9);	//�ؼФ��u�S��  
        	if(this.getLookVec() != null) {  					//�o�g�̷����S��
        		createPacketS2C.sendS2CAttackParticle2(this.getEntityId(), this.posX, this.posY, this.posZ, lookX, lookY, lookZ, 6);		
        	}
        }

	    return isTargetHurt;
	}

	//range attack method, cost heavy ammo, attack delay = 100 / attack speed, damage = 500% atk
	public boolean attackEntityWithHeavyAmmo(Entity target) {	
		//get attack value
		float atk = ArrayFinal[AttrID.ATK] * 5F;
		//set knockback value (testing)
		float kbValue = 0.15F;
		//���u�O�_�ĥΪ��g
		boolean isDirect = true;
		
		//experience++
		this.addShipExp(16);
	
		//play cannon fire sound at attacker
        this.playSound(Reference.MOD_ID+":ship-fireheavy", 0.4F, 0.7F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
        //play entity attack sound
        if(this.getRNG().nextInt(10) > 7) {
        	this.playSound(Reference.MOD_ID+":ship-hitsmall", 1F, 1F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
        }
        
        //heavy ammo -1
        if(!decrAmmoNum(1)) {		//not enough ammo
        	atk = atk * 0.25F;	//reduce damage to 25%
        }
        
        //�p��ؼжZ��
        double distX = target.posX - this.posX;
        double distY = target.posY - this.posY;
        double distZ = target.posZ - this.posZ;
        //�W�L7��Z�� or (NYI: �o�g�̦b����/���W), �h�ĥΩߪ��u
        if((distX*distX+distY*distY+distZ*distZ) > 49) {
        	isDirect = false;
        }

        //spawn missile   NYI: target position + random offset by ship HIT value
        EntityAbyssMissile missile = new EntityAbyssMissile(this.worldObj, this, 
        		target.posX, target.posY+target.height*0.2D, target.posZ, atk, kbValue, isDirect);
        this.worldObj.spawnEntityInWorld(missile);
        
        return true;
	}
	
	//be attacked method, �]�A��Lentity����, anvil����, arrow����, fall damage���ϥΦ���k 
	@Override
    public boolean attackEntityFrom(DamageSource attacker, float atk) {		
		//�i��def�p��
        float reduceAtk = atk * (1F - this.ArrayFinal[AttrID.DEF] / 100F);    
        if(atk < 0) { atk = 0; }
        
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
	private boolean decrAmmoNum(int type) {
		switch(type) {
		case 0:  //use 1 light ammo
			if(NumAmmoLight > 0) { 
				--NumAmmoLight;
				return true;
			}
			else {
				if(decrAmmoItem(0)) {  //find ammo item
					NumAmmoLight += 29;
					return true;
				}
				else {				   //no ammo
					return false;
				}
			}
		case 1:  //use 1 heavy ammo
			if(NumAmmoHeavy > 0) { 
				--NumAmmoHeavy;
				return true;
			}
			else {
				if(decrAmmoItem(1)) {  //find ammo item
					NumAmmoHeavy += 14;
					return true;
				}
				else {				   //no ammo
					return false;
				}
			}
		}
		
		return false;	//unknow attack type
	}
	
	//decrese ammo amount with type, return true or false(not enough item)
	private boolean decrAmmoItem(int type) {
		boolean isEnoughAmmo = true;
		int ammonum = 0;
		Item ammotype = null;
		
		//find ammo
		switch(type) {
		case 0:	//use 1 light ammo
			ammonum = 1;
			ammotype = ModItems.Ammo;
			break;
		case 1: //use 1 heavy ammo
			ammonum = 1;
			ammotype = ModItems.HeavyAmmo;
			break;
		}
		
		//search item in ship inventory
		int i = findItemInSlot(ammotype);
		if(i == -1) {		//item not found
			return false;
		}
		ItemStack ammo = this.ExtProps.slots[i];
		
		//decr item stacksize		
		if(ammo.stackSize >= ammonum) {
			ammo.stackSize -= ammonum;
		}
		else {	//not enough ammo, damage will reduce
			ammo.stackSize = 0;
			isEnoughAmmo = false;
		}
				
		if(ammo.stackSize == 0) {
			ammo = null;
		}
		
		//save back itemstack
		//no need to sync because no GUI opened
		this.ExtProps.slots[i] = ammo;	
		
		return isEnoughAmmo;	
	}

	//find item in ship inventory
	private int findItemInSlot(Item parItem) {
		ItemStack slotitem = null;

		//search ship inventory (except equip slots)
		for(int i=ContainerShipInventory.SLOTS_EQUIP; i<ContainerShipInventory.SLOTS_TOTAL; i++) {
			slotitem = this.ExtProps.slots[i];
			if(slotitem != null && slotitem.getItem() == parItem) {
				return i;	//found item
			}		
		}	
		
		return -1;	//item not found
	}	
	

	
}
