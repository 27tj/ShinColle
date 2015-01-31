package com.lulan.shincolle.entity;

import java.util.Random;
import java.util.UUID;

import com.lulan.shincolle.crafting.EquipCalc;
import com.lulan.shincolle.entity.EntityAbyssMissile;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.init.ModItems;
import com.lulan.shincolle.inventory.ContainerShipInventory;
import com.lulan.shincolle.network.createPacketS2C;
import com.lulan.shincolle.reference.AttrID;
import com.lulan.shincolle.reference.AttrValues;
import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.utility.LogHelper;

import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityLargeFireball;
import net.minecraft.init.Items;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;

/**SHIP DATA <br>
 * Explanation in crafting/ShipCalc.class
 */
public abstract class BasicEntityShip extends EntityTameable implements IEntityShip {

	public ExtendShipProps ExtProps;		//entity�B�~NBT����
	//for attribute calc
	public short ShipLevel;				//ship level
	public int Kills;					//kill mobs (= exp)
	public byte ShipType;				//ship type
	public byte ShipID;
	//for AI calc
	public int StartEmotion;			//���}�l�ɶ�
	public String BlockUnderName;		//�}�U����W��, ���ݭn�sNBT
	public ItemStack ammotype;	//for inventory check
	public boolean hasAmmo;				//�ˬd���L�u��
	public boolean hasHeavyAmmo;		//�ˬd���L�����u��
	//equip states: 0:HP 1:ATK 2:DEF 3:SPD 4:MOV 5:HIT
	public float[] ArrayEquip;
	//final states: 0:HP 1:ATK 2:DEF 3:SPD 4:MOV 5:HIT
	public float[] ArrayFinal;
	//EntityState: 0:State 1:Emotion 2:SwimType
	public byte[] EntityState;
	//BonusPoint: 0:HP 1:ATK 2:DEF 3:SPD 4:MOV 5:HIT
	public byte[] BonusPoint;
	//TypeModify: 0:HP 1:ATK 2:DEF 3:SPD 4:MOV 5:HIT
	public float[] TypeModify;
	
	
	public BasicEntityShip(World world) {
		super(world);
		//init value
		ShipLevel = 1;				//ship level
		Kills = 0;					//kill mobs (= exp)
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
		hasAmmo = false;			//ammo check
		hasHeavyAmmo = false;
		StartEmotion = 0;			//���}�l�ɶ�
		BlockUnderName = "";		//�}�U����W��, ���ݭn�sNBT

	}
	
	@Override
	public boolean isAIEnabled() {
		return true;
	}

	@Override
	public EntityAgeable createChild(EntityAgeable p_90011_1_) {
		return null;
	}
	
	public void setOwner(String string) {
		 this.dataWatcher.updateObject(17, string);
	}
	
	public String getOwnerName() {
		 return this.dataWatcher.getWatchableObjectString(17);
	}
	
	//setup AI
	abstract protected void setAIList();
	
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
	
	//setting attributes, called at load nbt data & init mob
	public void setShipAttributes(byte id) {
		//init or renew bonus value, for short value: discard decimal
		//HP = (base + (point + 1) * level * typeModify) * config HP ratio
		ArrayFinal[AttrID.HP] = ((float)AttrValues.BaseHP[id] + (float)(BonusPoint[AttrID.HP]+1) * (float)ShipLevel * TypeModify[AttrID.HP]) * ConfigHandler.hpRatio; 
		//ATK = base + ((point + 1) * level / 3 + equip) * typeModify
		ArrayFinal[AttrID.ATK] = (float)AttrValues.BaseATK[id] + ((float)(BonusPoint[AttrID.ATK]+1) * (((float)ShipLevel)/3F) + ArrayEquip[AttrID.ATK]) * TypeModify[AttrID.ATK];
		//DEF = base + ((point + 1) * level / 5 * 0.6 + equip) * typeModify
		ArrayFinal[AttrID.DEF] = (float)AttrValues.BaseDEF[id] + ((float)(BonusPoint[AttrID.DEF]+1) * (((float)ShipLevel)/5F) * 0.6F + ArrayEquip[AttrID.DEF]) * TypeModify[AttrID.DEF];
		//SPD = base + ((point + 1) * level / 10 * 0.02 + equip) * typeModify
		ArrayFinal[AttrID.SPD] = AttrValues.BaseSPD[id] + ((float)(BonusPoint[AttrID.SPD]+1) * (((float)ShipLevel)/10F) * 0.02F + ArrayEquip[AttrID.SPD]) * TypeModify[AttrID.SPD];
		//MOV = base + ((point + 1) * level / 10 * 0.01 + equip) * typeModify
		ArrayFinal[AttrID.MOV] = AttrValues.BaseMOV[id] + ((float)(BonusPoint[AttrID.MOV]+1) * (((float)ShipLevel)/10F) * 0.01F + ArrayEquip[AttrID.MOV]) * TypeModify[AttrID.MOV];
		//HIT = base + ((point + 1) * level / 10 * 0.8 + equip) * typeModify
		ArrayFinal[AttrID.HIT] = AttrValues.BaseHIT[id] + ((float)(BonusPoint[AttrID.HIT]+1) * (((float)ShipLevel)/10F) * 0.8F + ArrayEquip[AttrID.HIT]) * TypeModify[AttrID.HIT];
		//KB Resistance = Level / 10 * 0.04
		float resisKB = (((float)ShipLevel)/10F) * 0.04F;
		
		if(ArrayFinal[AttrID.SPD] > 2F) {
			ArrayFinal[AttrID.SPD] = 2F;	//delay < 0.5sec is meaningless
		}
		if(ArrayFinal[AttrID.SPD] < 0F) {
			ArrayFinal[AttrID.SPD] = 0F;
		}
		if(ArrayFinal[AttrID.MOV] > 1F) {
			ArrayFinal[AttrID.MOV] = 1F;	//move speed > 1 is buggy
		}
		if(ArrayFinal[AttrID.MOV] < 0F) {
			ArrayFinal[AttrID.MOV] = 0F;
		}
		
		//set attribute by final value
		/**
		 * DO NOT set ATTACK DAMAGE to non-EntityMob!!!!!
		 */
		getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(ArrayFinal[AttrID.HP]);
		getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(ArrayFinal[AttrID.MOV]);
		getEntityAttribute(SharedMonsterAttributes.followRange).setBaseValue(ArrayFinal[AttrID.HIT]+12); //������ؼнd��
		getEntityAttribute(SharedMonsterAttributes.knockbackResistance).setBaseValue(resisKB);
			
		//reset target AI (update hit range)
		clearAITargetTasks();
		setAITargetList();
		
		sendSyncPacket();	//sync nbt data
	}
	
	//called when entity level up
	public void setShipLevel(short par1) {
		//update level
		if(par1 < 151) {
			ShipLevel = par1;
		}
		else {	//max level = 150
			ShipLevel = 150;
		}
		
		//update attributes
		setShipAttributes(ShipID);
	}
	
	//called when a mob die near the entity
	public void setKills(int par1, boolean sync) {
		Kills = par1;		

		//calc exp for level up
		
	}
	
	//called when entity equip changed
	public void setAttrEquip() {
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
		if(!this.worldObj.isRemote) {
			setShipAttributes(this.ShipID);
		}
	}
	
	//called when entity spawn, set the type modify
	public void setTypeModify() {
		TypeModify[AttrID.HP] = AttrValues.ModHP[ShipID];
		TypeModify[AttrID.ATK] = AttrValues.ModATK[ShipID];
		TypeModify[AttrID.DEF] = AttrValues.ModDEF[ShipID];
		TypeModify[AttrID.SPD] = AttrValues.ModSPD[ShipID];
		TypeModify[AttrID.MOV] = AttrValues.ModMOV[ShipID];
		TypeModify[AttrID.HIT] = AttrValues.ModHIT[ShipID];
	}

	//called when entity AI/HP change
	public void setEntityState(byte[] par1, boolean sync) {
		if(sync && !worldObj.isRemote) {
			EntityState[AttrID.State] = par1[AttrID.State];
			EntityState[AttrID.Emotion] = par1[AttrID.Emotion];
			EntityState[AttrID.SwimType] = par1[AttrID.SwimType];
				
			createPacketS2C.sendS2CEntitySync(this);     
		}
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
		
		//use repair bucket
		if(itemstack != null) {
			if(itemstack.getItem() == ModItems.BucketRepair) {	//�ϥέ״_��
				//hp����max hp�ɥi�H�ϥ�bucket
				if(this.getHealth() < this.getMaxHealth()) {
	                if (!player.capabilities.isCreativeMode) {  //stack-1 in non-creative mode
	                    --itemstack.stackSize;
	                }
	
	                if(this instanceof BasicEntitySmallShip) {
	                	this.heal(this.getMaxHealth() * 0.1F);	 //1 bucket = 10% hp for small ship
	                }
	                else {
	                	this.heal(this.getMaxHealth() * 0.05F);	 //1 bucket = 5% hp for large ship
	                }
	                
	                if (itemstack.stackSize <= 0) {  //���~�Χ��ɭn�]�w��null�M�Ÿ�slot
	                	player.inventory.setInventorySlotContents(player.inventory.currentItem, (ItemStack)null);
	                }
	                
	                return true;
	            }			
			}	
			//use lead
			else if(itemstack.getItem() == Items.lead && this.allowLeashing()) {
				this.setLeashedToEntity(player, true);
				return true;
	        }
		}

		return false;
	}
	
	//check entity state every tick
	@Override
	public void onLivingUpdate() {
        super.onLivingUpdate();
        
        //server side check
        if((!this.worldObj.isRemote)) {
        	//check ammo every 100 ticks
        	if(this.ticksExisted % 100 == 0) {
        		//set air value
        		if(this.isInWater()) {
                	this.setAir(300);
                }
        		
	        	//set ammo flag to default
	        	this.hasAmmo = false;
	        	this.hasHeavyAmmo = false;
	        	
	        	//search ship inventory
	        	for(int i=ContainerShipInventory.SLOTS_EQUIP; i<ContainerShipInventory.SLOTS_TOTAL; i++) {
	    			ammotype = this.ExtProps.slots[i];
	        		
	    			if(ammotype != null) {
	    				if(ammotype.getItem() == ModItems.Ammo) this.hasAmmo = true;
	    				if(ammotype.getItem() == ModItems.HeavyAmmo) this.hasHeavyAmmo = true;
	    			}		
	    		}	        	
        	}     	
        }//end if(server side)
        
    }
	
	//melee attack method, no ammo cost, no attack speed, damage = 12.5% atk
	@Override
	public boolean attackEntityAsMob(Entity target) {
		//get attack value
		float atk = ArrayFinal[AttrID.ATK] * 0.125F;
		//set knockback value (testing)
		float kbValue = 0.15F;
		
	    //�Natk��attacker�ǵ��ؼЪ�attackEntityFrom��k, �b�ؼ�class���p��ˮ`
	    //�åB�^�ǬO�_���\�ˮ`��ؼ�
	    boolean isTargetHurt = target.attackEntityFrom(DamageSource.causeMobDamage(this), atk);

	    //play entity attack sound
        if(this.getRNG().nextInt(10) > 6) {
        	this.playSound(Reference.MOD_ID_LOW+":ship-hitsmall", 1.0F, 1.0F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
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
		
		//play cannon fire sound at attacker
        this.playSound(Reference.MOD_ID_LOW+":ship-firesmall", 0.4F, 0.7F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
        //play entity attack sound
        if(this.getRNG().nextInt(10) > 7) {
        	this.playSound(Reference.MOD_ID_LOW+":ship-hitsmall", 1F, 1F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
        }
        
        //light ammo -1
        if(!decrAmmo(0)) {		//not enough ammo
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
        		createPacketS2C.sendS2CAttackParticle2(this.posX, this.posY, this.posZ, lookX, lookY, lookZ, 6);		
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
		
		//play cannon fire sound at attacker
        this.playSound(Reference.MOD_ID_LOW+":ship-fireheavy", 0.4F, 0.7F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
        //play entity attack sound
        if(this.getRNG().nextInt(10) > 7) {
        	this.playSound(Reference.MOD_ID_LOW+":ship-hitsmall", 1F, 1F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
        }
        
        //heavy ammo -1
        if(!decrAmmo(1)) {		//not enough ammo
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
		LogHelper.info("DEBUG : source "+attacker.getSourceOfDamage());
		
		//�i��def�p��
        float reduceAtk = atk * (1F - this.ArrayFinal[AttrID.DEF] / 100F);
        //�L�Ī�entity�ˮ`�L��
		if(this.isEntityInvulnerable()) {	
            return false;
        }
		else if(attacker.getSourceOfDamage() != null  && attacker.getSourceOfDamage().equals(this)) {  //���|��ۤv�y���ˮ`
			return false;
		}
        else {
            Entity entity = attacker.getEntity();
            this.aiSit.setSitting(false);
   
            //�����class���Q�����P�w, �]�A���mlove�ɶ�, �p����r�ܩ�, �p���K�z/�����ˮ`, 
            //hurtResistantTime(0.5sec�L�Įɶ�)�p��, 
            return super.attackEntityFrom(attacker, reduceAtk);
        }
    }
	
	//decresse ammo amount with type, return true or false(not enough item)
	private boolean decrAmmo(int type) {
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
		case 2: //use 2 light ammo
			ammonum = 2;
			ammotype = ModItems.Ammo;
			break;
		case 3: //use 2 heavy ammo
			ammonum = 2;
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
